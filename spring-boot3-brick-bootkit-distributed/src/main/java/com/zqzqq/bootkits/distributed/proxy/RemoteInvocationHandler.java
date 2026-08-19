package com.zqzqq.bootkits.distributed.proxy;

import com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration;
import com.zqzqq.bootkits.distributed.registry.ServiceDirectory;
import com.zqzqq.bootkits.distributed.rpc.GrpcClientProvider;
import com.zqzqq.bootkits.distributed.rpc.proto.InvokeReply;
import com.zqzqq.bootkits.distributed.rpc.proto.InvokeRequest;
import com.zqzqq.bootkits.distributed.rpc.proto.PluginInvocationServiceGrpc;
import com.zqzqq.bootkits.distributed.serialization.DistTrace;
import com.zqzqq.bootkits.distributed.serialization.PayloadCodec;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 远程插件服务调用处理器。
 * <p>
 * 作为宿主侧 ServiceProxyFactory 的替代实现：拦截对本地接口 Class 的方法调用，
 * 通过 gRPC 将调用转发到持有目标插件的执行节点，由执行节点在本地插件容器中
 * 反射执行后回传结果。对调用方完全透明，仿佛在本地调用一样。
 *
 * <p>三种本地派生接口均通过此处理器透明路由，无需目标插件依赖。</p>
 */
public class RemoteInvocationHandler implements InvocationHandler {

    private static final Logger log = LoggerFactory.getLogger(RemoteInvocationHandler.class);

    private static final java.util.concurrent.atomic.AtomicLong ATOMIC =
            new java.util.concurrent.atomic.AtomicLong(0);

    private final String pluginId;
    private final Class<?> serviceInterface;
    private final ServiceDirectory directory;
    private final GrpcClientProvider clients;

    public RemoteInvocationHandler(String pluginId,
                                   Class<?> serviceInterface,
                                   ServiceDirectory directory,
                                   GrpcClientProvider clients) {
        this.pluginId = pluginId;
        this.serviceInterface = serviceInterface;
        this.directory = directory;
        this.clients = clients;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 处理 Object 基础方法
        if (method.getDeclaringClass() == Object.class) {
            return handleObjectMethod(proxy, method, args);
        }

        // 每次调用实时查目录，收集该插件的全部可用节点，支持多副本 + 故障转移
        List<RemoteServiceRegistration> nodes = directory.lookup(serviceInterface.getName());
        List<RemoteServiceRegistration> candidates = new ArrayList<>();
        for (RemoteServiceRegistration node : nodes) {
            if (node.getPluginId().equals(pluginId)) {
                candidates.add(node);
            }
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException(
                "远端服务[" + pluginId + "." + serviceInterface.getName() + "]不可用，未在任何执行节点注册。"
                + " 请确认对应插件已在某 WORKER 节点启动并完成注册。");
        }

        return invokeRemoteWithFailover(candidates, method, args);
    }

    /**
     * 在多个可用节点间做顺序尝试（首个下标轮询），任一节点失败则自动转移到下一个，
     * 全部失败才抛异常。每次调用重新取目录，天然支持节点上线/下线/迁移。
     */
    private Object invokeRemoteWithFailover(List<RemoteServiceRegistration> candidates,
                                            Method method,
                                            Object[] args) throws Throwable {
        // 健康节点优先：把冷却期内被标记为不可用的节点排在末尾，存在健康节点时直接跳过，
        // 避免目录尚未剔除陈旧节点前，每次调用都对同一宕机节点发起网络超时。
        List<RemoteServiceRegistration> ordered = new ArrayList<>(candidates.size());
        List<RemoteServiceRegistration> unhealthy = new ArrayList<>();
        for (RemoteServiceRegistration node : candidates) {
            if (clients.isHealthy(node.getHost(), node.getPort(), node.isTlsEnabled())) {
                ordered.add(node);
            } else {
                unhealthy.add(node);
            }
        }
        ordered.addAll(unhealthy);
        // [0, healthyGroupEnd) 为健康节点段；healthyGroupEnd==0 表示当前全部节点都不可用
        final int healthyGroupEnd = ordered.size() - unhealthy.size();

        // 轮询起始下标，避免每次都打同一个节点
        int start = (int) (Math.floorMod(
                ATOMIC.getAndIncrement(),
                ordered.size()));
        int attempts = ordered.size();
        for (int i = 0; i < attempts; i++) {
            int idx = (start + i) % ordered.size();
            RemoteServiceRegistration target = ordered.get(idx);

            // 存在健康节点时，遇到冷却期内的不健康节点一律跳过，不再发起网络调用、
            // 也不再累计 failover——本轮已确定有可用副本，无需再去撞死节点。
            // 仅当本轮没有任何健康节点（healthyGroupEnd==0）时，才会真去尝试死节点并快速失败。
            if (healthyGroupEnd > 0
                    && !clients.isHealthy(target.getHost(), target.getPort(), target.isTlsEnabled())) {
                continue;
            }

            try {
                Object result = invokeRemote(target, method, args);
                clients.markSuccess(target.getHost(), target.getPort(), target.isTlsEnabled());
                return result;
            } catch (RemoteNodeUnavailableException e) {
                // 节点传输层不可达：标记冷却（快速失败）、断开连接、累计 failover，尝试下一副本
                clients.markFailure(target.getHost(), target.getPort(), target.isTlsEnabled());
                clients.recordFailover();
                clients.evict(target.getHost(), target.getPort());
                log.warn("节点 {}:{} 不可用，切换到下一副本继续调用: {} (累计 failover={})",
                        target.getHost(), target.getPort(), serviceInterface.getName(),
                        clients.failoverCount());
            }
        }
        throw new IllegalStateException("远端服务[" + pluginId + "." + serviceInterface.getName()
                + "]所有执行节点均不可用，已尝试 " + attempts + " 个节点。");
    }

    private Object invokeRemote(RemoteServiceRegistration target, Method method, Object[] args) throws Throwable {
        // 按节点声明选择传输方式（明文 / TLS），支持混合部署灰度升级
        ManagedChannel channel = clients.channel(target.getHost(), target.getPort(), target.isTlsEnabled());
        PluginInvocationServiceGrpc.PluginInvocationServiceBlockingStub stub =
                PluginInvocationServiceGrpc.newBlockingStub(channel);

        InvokeRequest request = buildRequest(method, args);

        long timeout = clients.getCallTimeoutMillis();
        if (timeout > 0) {
            stub = stub.withDeadlineAfter(timeout, TimeUnit.MILLISECONDS);
        }

        InvokeReply reply;
        try {
            reply = stub.invoke(request);
        } catch (StatusRuntimeException e) {
            // 仅把「节点本身不可达」识别为可 failover 的传输层故障；其余状态码
            // （超时 DEADLINE_EXCEEDED、参数/鉴权类错误等）不代表节点宕机，
            // 直接原样抛出，避免误关健康 channel 或把错误转发到其它节点。
            if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                throw new RemoteNodeUnavailableException(target,
                        "节点不可达: " + target.getHost() + ":" + target.getPort()
                                + ", " + e.getStatus(), e);
            }
            throw e;
        }

        return handleReply(method, reply);
    }

    private InvokeRequest buildRequest(Method method, Object[] args) {
        InvokeRequest.Builder builder = InvokeRequest.newBuilder()
                .setPluginId(pluginId)
                .setServiceInterface(serviceInterface.getName())
                .setMethodName(method.getName());

        // 透传链路追踪 ID：若调用方已通过 SLF4J MDC 建立 traceId，则随 RPC 传递，
        // 执行节点在执行时恢复，从而跨节点串联同一业务链路的日志。
        String traceId = DistTrace.get();
        if (traceId != null && !traceId.isEmpty()) {
            builder.putHeaders(DistTrace.HEADER_NAME, traceId);
        }

        List<String> paramTypeNames = new ArrayList<>();
        List<String> paramValues = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            String typeName = parameters[i].getType().getName();
            Object value = args != null && i < args.length ? args[i] : null;
            paramTypeNames.add(typeName);
            paramValues.add(PayloadCodec.toJson(value));
        }
        builder.addAllParamTypeNames(paramTypeNames);
        builder.addAllParamValues(paramValues);

        return builder.build();
    }

    private Object handleReply(Method method, InvokeReply reply) throws Throwable {
        if (reply.getStatus() != 0) {
            // 远端抛出了业务异常，尽量在调用方侧还原
            throw rebuildException(reply);
        }
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class || returnType == Void.class) {
            return null;
        }
        String returnTypeName = reply.getReturnType();
        if (returnTypeName == null || returnTypeName.isEmpty()) {
            returnTypeName = returnType.getName();
        }
        return PayloadCodec.fromJson(reply.getReturnValue(), returnTypeName);
    }

    private Throwable rebuildException(InvokeReply reply) {
        String message = reply.getErrorMessage();
        if (message == null || message.isEmpty()) {
            message = "远端插件调用失败";
        }
        // 优先还原为业务可感知的具体异常类型。要求：该异常类必须对宿主 classpath 可见
        // （即双方共享契约中包含异常类），此时用 TCCL 精确加载并反射构造；
        // 若不可见（插件 jar 内的私有异常类在宿主侧不存在），统一回退为 RuntimeException，
        // 并在 message 中保留原始 errorType 与错误信息。
        Throwable cause = rebuildCause(reply);
        try {
            Class<?> type = resolveSharedExceptionClass(reply.getErrorType());
            if (type != null && Throwable.class.isAssignableFrom(type)) {
                java.lang.reflect.Constructor<?> ctor = type.getConstructor(String.class);
                Throwable rebuilt = (Throwable) ctor.newInstance(message);
                if (cause != null) {
                    rebuilt.initCause(cause);
                }
                return rebuilt;
            }
        } catch (Exception ignored) {
            // 无法还原具体异常类型，回退 RuntimeException
        }
        RuntimeException fallback = new RuntimeException(
                "远端插件异常[" + serviceInterface.getName() + "@" + reply.getErrorType() + "]: " + message);
        if (cause != null) {
            fallback.initCause(cause);
        }
        return fallback;
    }

    /**
     * 还原远端异常的根因（cause）。根因类型对宿主可见则精确还原，否则用
     * {@link RuntimeException} 包装其消息，确保根因信息不丢失、可被日志/告警追踪。
     */
    private Throwable rebuildCause(InvokeReply reply) {
        String causeType = reply.getCauseErrorType();
        String causeMsg = reply.getCauseErrorMessage();
        if (causeType == null || causeType.isEmpty()) {
            return null;
        }
        if (causeMsg == null || causeMsg.isEmpty()) {
            causeMsg = causeType;
        }
        try {
            Class<?> type = resolveSharedExceptionClass(causeType);
            if (type != null && Throwable.class.isAssignableFrom(type)) {
                java.lang.reflect.Constructor<?> ctor = type.getConstructor(String.class);
                return (Throwable) ctor.newInstance(causeMsg);
            }
        } catch (Exception ignored) {
            // 根因类型不可见或缺合适构造器，回退包装
        }
        return new RuntimeException("远端根因[" + causeType + "]: " + causeMsg);
    }

    /**
     * 在宿主侧解析共享产物异常类型：优先调用线程上下文类加载器，其次系统类加载器。
     * 返回 null 表示该类型在宿主侧不可见（无法精确还原）。
     */
    private Class<?> resolveSharedExceptionClass(String errorType) {
        if (errorType == null || errorType.isEmpty()) {
            return null;
        }
        ClassLoader tcl = Thread.currentThread().getContextClassLoader();
        if (tcl != null) {
            try {
                return Class.forName(errorType, false, tcl);
            } catch (ClassNotFoundException ignored) {
                // 继续尝试系统类加载器
            }
        }
        try {
            return Class.forName(errorType);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "toString":
                return "RemoteProxy[" + pluginId + "@" + serviceInterface.getName() + "]";
            case "hashCode":
                return System.identityHashCode(proxy);
            case "equals":
                return proxy == args[0];
            default:
                return null;
        }
    }

    /**
     * 远端节点<strong>传输层</strong>不可用（连接失败/节点宕机/超时）时抛出，
     * 用于触发多副本故障转移。它不用于包装业务异常——业务异常由
     * {@link #handleReply} 以原始类型抛出，绝不参与 failover。
     */
    static final class RemoteNodeUnavailableException extends Exception {
        private static final long serialVersionUID = 1L;

        RemoteNodeUnavailableException(RemoteServiceRegistration target, String message, Throwable cause) {
            super(message == null ? ("node " + target.getHost() + ":" + target.getPort()) : message, cause);
        }
    }
}