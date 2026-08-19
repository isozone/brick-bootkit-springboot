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
    private final com.zqzqq.bootkits.distributed.metrics.DistributedMetrics metrics;

    /** 按方法覆盖的超时（毫秒）：key 为「接口名.方法名」或「方法名」；命中则覆盖全局超时。 */
    private final java.util.Map<String, Long> methodTimeouts;

    /** 传输层不可达时的有限次整组重试次数（0=不重试）。 */
    private final int maxRetries;

    /** 兼容旧构造（无指标，自动从 clients 取）。 */
    public RemoteInvocationHandler(String pluginId,
                                   Class<?> serviceInterface,
                                   ServiceDirectory directory,
                                   GrpcClientProvider clients) {
        this(pluginId, serviceInterface, directory, clients, null, java.util.Collections.emptyMap(), 0);
    }

    public RemoteInvocationHandler(String pluginId,
                                   Class<?> serviceInterface,
                                   ServiceDirectory directory,
                                   GrpcClientProvider clients,
                                   com.zqzqq.bootkits.distributed.metrics.DistributedMetrics metrics) {
        this(pluginId, serviceInterface, directory, clients, metrics, java.util.Collections.emptyMap(), 0);
    }

    public RemoteInvocationHandler(String pluginId,
                                   Class<?> serviceInterface,
                                   ServiceDirectory directory,
                                   GrpcClientProvider clients,
                                   com.zqzqq.bootkits.distributed.metrics.DistributedMetrics metrics,
                                   java.util.Map<String, Long> methodTimeouts,
                                   int maxRetries) {
        this.pluginId = pluginId;
        this.serviceInterface = serviceInterface;
        this.directory = directory;
        this.clients = clients;
        this.metrics = metrics != null ? metrics : (clients != null ? clients.metrics() : null);
        this.methodTimeouts = methodTimeouts != null ? methodTimeouts : java.util.Collections.emptyMap();
        this.maxRetries = Math.max(0, maxRetries);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 处理 Object 基础方法
        if (method.getDeclaringClass() == Object.class) {
            return handleObjectMethod(proxy, method, args);
        }

        // 每次调用实时查目录，收集该插件的全部可用节点，支持多副本 + 故障转移
        long beginNanos = System.nanoTime();
        if (metrics != null) {
            metrics.recordRemoteCallBegin();
        }
        List<RemoteServiceRegistration> nodes = directory.lookup(serviceInterface.getName());
        List<RemoteServiceRegistration> candidates = new ArrayList<>();
        for (RemoteServiceRegistration node : nodes) {
            if (node.getPluginId().equals(pluginId)) {
                candidates.add(node);
            }
        }
        if (candidates.isEmpty()) {
            recordCallEndUnavailable(beginNanos);
            throw new IllegalStateException(
                "远端服务[" + pluginId + "." + serviceInterface.getName() + "]不可用，未在任何执行节点注册。"
                + " 请确认对应插件已在某 WORKER 节点启动并完成注册。");
        }

        return invokeRemoteWithFailover(candidates, method, args, beginNanos);
    }

    private void recordCallEndUnavailable(long beginNanos) {
        if (metrics != null) {
            metrics.recordRemoteCallEnd(false, (System.nanoTime() - beginNanos) / 1_000_000L);
        }
    }

    /**
     * 在多个可用节点间做顺序尝试（首个下标轮询），任一节点失败则自动转移到下一个，
     * 全部失败才抛异常。每次调用重新取目录，天然支持节点上线/下线/迁移。
     */
    private Object invokeRemoteWithFailover(List<RemoteServiceRegistration> candidates,
                                            Method method,
                                            Object[] args,
                                            long beginNanos) throws Throwable {
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
        // 传输层整体不可达时的有限次整组重试：仅在 maxRetries>0 时启用，
        // 每次整组失败后短暂退避再重来，用于吸收瞬时网络抖动（默认关闭，保持单轮语义）。
        int wholeGroupAttempts = maxRetries + 1;
        // 是否本轮所有候选都被短路（冷却/熔断）跳过，未发起任何真实网络调用：
        // 仅当整轮「真去撞了网络、全部 UNAVAILABLE」时，才认为「整组不可达」，可触发整组重试。
        // 若本轮全是短路跳过（说明节点都在熔断窗口里、没必要反复退避），直接跳出，避免无谓退避。
        java.util.List<String> attemptedNodes = new java.util.ArrayList<>();
        boolean lastRoundHadAnyRealAttempt = false;
        for (int round = 0; round < wholeGroupAttempts; round++) {
            if (round > 0) {
                backoffBeforeRetry(round);
            }
            boolean anyRealAttempt = false;     // 本轮是否真去发了网络调用
            boolean anyUnavailable = false;      // 本轮是否有节点返回 UNAVAILABLE
            for (int i = 0; i < attempts; i++) {
                int idx = (start + i) % ordered.size();
                RemoteServiceRegistration target = ordered.get(idx);
                // 每次节点尝试的起始纳秒，用于把单次节点耗时附到错误信息里，方便排障。
                final long perNodeBeginNanos = System.nanoTime();

                // 存在健康节点时，遇到冷却期内的不健康节点一律跳过，不再发起网络调用、
                // 也不再累计 failover——本轮已确定有可用副本，无需再去撞死节点。
                // 仅当本轮没有任何健康节点（healthyGroupEnd==0）时，才会真去尝试死节点并快速失败。
                if (healthyGroupEnd > 0
                        && !clients.isHealthy(target.getHost(), target.getPort(), target.isTlsEnabled())) {
                    continue;
                }

                // 熔断器：节点已 OPEN（连续失败≥阈值且在短路窗口内）时短路，不发真实网络调用、
                // 直接快速跳过，避免在「全部节点都宕机」时反复发起无谓超时。记录 trip 指标。
                if (!clients.allowAttempt(target.getHost(), target.getPort(), target.isTlsEnabled())) {
                    clients.recordTrip(target.getHost(), target.getPort(), target.isTlsEnabled());
                    // 短路视为一次「安全跳过」：不再 markFailure（否则会刷新 OPEN 窗口），
                    // 但累计一次 failover 供观测，随后尝试下一副本。
                    clients.recordFailover();
                    continue;
                }

                try {
                    anyRealAttempt = true;
                    Object result = invokeRemote(target, method, args);
                    clients.markSuccess(target.getHost(), target.getPort(), target.isTlsEnabled());
                    if (metrics != null) {
                        metrics.recordRemoteCallEnd(true, (System.nanoTime() - beginNanos) / 1_000_000L);
                    }
                    return result;
                } catch (RemoteNodeUnavailableException e) {
                    // 节点传输层不可达：标记冷却（快速失败）、断开连接、累计 failover，尝试下一副本
                    anyUnavailable = true;
                    clients.markFailure(target.getHost(), target.getPort(), target.isTlsEnabled());
                    clients.recordFailover();
                    clients.evict(target.getHost(), target.getPort());
                    attemptedNodes.add(target.getHost() + ":" + target.getPort()
                            + " (耗时=" + (System.nanoTime() - perNodeBeginNanos) / 1_000_000L + "ms)");
                    log.warn("节点 {}:{} 不可用，切换到下一副本继续调用: {} (累计 failover={})",
                            target.getHost(), target.getPort(), serviceInterface.getName(),
                            clients.failoverCount());
                } catch (RemoteBusinessException wrapper) {
                    // 远端业务异常：节点本身是健康的（成功响应了），仅是业务侧抛错；
                    // 不触发 failover，但要把节点信息与耗时附到异常里，方便调用方排障。
                    long nodeElapsed = (System.nanoTime() - perNodeBeginNanos) / 1_000_000L;
                    long totalElapsed = (System.nanoTime() - beginNanos) / 1_000_000L;
                    if (metrics != null) {
                        metrics.recordRemoteCallEnd(false, totalElapsed);
                    }
                    enrichBusinessException(wrapper, target, nodeElapsed, totalElapsed);
                    throw wrapper.getRawThrowable();
                }
            }
            lastRoundHadAnyRealAttempt = anyRealAttempt;
            // 仅当本轮真去发了网络调用且有节点 UNAVAILABLE 时，才视为「整组不可达」并退避重试。
            // 若本轮没有任何真实调用（全是短路跳过）或所有尝试都是非 UNAVAILABLE 错误，
            // 说明重试也不会改善（节点仍在熔断窗口里），直接跳出避免无谓退避。
            if (!(anyRealAttempt && anyUnavailable)) {
                break;
            }
        }
        // lastRoundHadAnyRealAttempt 目前仅供后续观测/调试扩展使用，避免被误判为未使用。
        // 保留赋值以备在不修改控制流的前提下接入观测指标。
        if (!lastRoundHadAnyRealAttempt && log.isDebugEnabled()) {
            log.debug("整组重试轮次全部被短路跳过，未发起任何真实网络调用: {}",
                    serviceInterface.getName());
        }
        if (metrics != null) {
            metrics.recordRemoteCallEnd(false, (System.nanoTime() - beginNanos) / 1_000_000L);
        }
        long elapsed = (System.nanoTime() - beginNanos) / 1_000_000L;
        throw new IllegalStateException("远端服务[" + pluginId + "." + serviceInterface.getName()
                + "]所有执行节点均不可用，已尝试 " + attempts + " 个节点"
                + (attemptedNodes.isEmpty() ? "" : "，故障节点=" + attemptedNodes)
                + "，耗时=" + elapsed + "ms。");
    }

    /**
     * 整组重试前的短暂退避：round 从 1 起，退避递增（如 20ms,40ms,...封顶 500ms）。
     * 仅当启用 max-failover-retries 时调用；任一中断按被中断处理，不吞异常。
     */
    private void backoffBeforeRetry(int round) {
        long wait = Math.min(500L, 20L * (1L << Math.min(round - 1, 4)));
        try {
            Thread.sleep(wait);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private Object invokeRemote(RemoteServiceRegistration target, Method method, Object[] args) throws Throwable {
        // 按节点声明选择传输方式（明文 / TLS），支持混合部署灰度升级
        ManagedChannel channel = clients.channel(target.getHost(), target.getPort(), target.isTlsEnabled());
        PluginInvocationServiceGrpc.PluginInvocationServiceBlockingStub stub =
                PluginInvocationServiceGrpc.newBlockingStub(channel);

        InvokeRequest request = buildRequest(method, args);

        // 超时：优先按方法覆盖（接口名.方法名，其次方法名），否则用全局调用超时。
        long timeout = resolveTimeout(method);
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

    /**
     * 解析本次调用的超时（毫秒）：优先方法级覆盖（连字符紧凑名），其次全局。
     */
    private long resolveTimeout(Method method) {
        if (!methodTimeouts.isEmpty()) {
            String fqn = serviceInterface.getName() + "." + method.getName();
            Long byFqn = methodTimeouts.get(fqn);
            if (byFqn != null) {
                return byFqn;
            }
            Long byName = methodTimeouts.get(method.getName());
            if (byName != null) {
                return byName;
            }
        }
        return clients.getCallTimeoutMillis();
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
            // 远端抛出了业务异常，尽量在调用方侧还原，并包装成 RemoteBusinessException
            // 让外层能把「节点信息 + 单次耗时」附加到错误响应里（节点本身健康，只是业务侧出错）。
            throw new RemoteBusinessException(rebuildException(reply));
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

    /**
     * 把节点信息和耗时附加到业务异常（仅当该异常尚未带过该信息时）。
     * <p>实现策略：用 {@link Throwable#addSuppressed} 挂一条 {@link RemoteInvocationContext}
     * 到原始业务异常上——这样既不修改原异常类型（调用方仍可 {@code catch (IllegalStateException)}），
     * 也不修改原 message（保持业务语义），还能让调用方通过 {@code getSuppressed()} 拿到节点 +
     * 耗时上下文，方便排障与日志输出。
     * <p>不用反射修改 {@code detailMessage}：JDK 17+ 对 {@code java.lang} 反射受模块化限制，
     * 反射方案不稳定。
     */
    private static void enrichBusinessException(RemoteBusinessException wrapper,
                                                RemoteServiceRegistration target,
                                                long nodeElapsedMillis,
                                                long totalElapsedMillis) {
        Throwable raw = wrapper.getRawThrowable();
        if (raw == null) {
            return;
        }
        // 避免同一异常被重复挂多条同样的上下文（虽然本路径每次都是新异常，仍做幂等保护）
        for (Throwable suppressed : raw.getSuppressed()) {
            if (suppressed instanceof RemoteInvocationContext) {
                return;
            }
        }
        raw.addSuppressed(new RemoteInvocationContext(
                target.getHost(), target.getPort(), nodeElapsedMillis, totalElapsedMillis));
    }

    /**
     * 远端业务异常发生时挂在原异常 suppressed 链上的「调用上下文」，
     * 携带节点 host:port + 单节点耗时 + 总耗时。供调用方通过
     * {@code Throwable.getSuppressed()} 取出做日志/告警。
     */
    public static final class RemoteInvocationContext extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final String host;
        private final int port;
        private final long nodeElapsedMillis;
        private final long totalElapsedMillis;

        public RemoteInvocationContext(String host, int port,
                                       long nodeElapsedMillis, long totalElapsedMillis) {
            super("from " + host + ":" + port
                    + " node=" + nodeElapsedMillis + "ms total=" + totalElapsedMillis + "ms");
            this.host = host;
            this.port = port;
            this.nodeElapsedMillis = nodeElapsedMillis;
            this.totalElapsedMillis = totalElapsedMillis;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public long getNodeElapsedMillis() {
            return nodeElapsedMillis;
        }

        public long getTotalElapsedMillis() {
            return totalElapsedMillis;
        }
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

    /**
     * 远端业务异常包装：远端节点本身健康（成功响应了，status!=0 仅是业务侧抛错），
     * 通过这个内部包装类把 {@link #rebuildException} 还原出的精确业务异常类型带回
     * {@link #invokeRemoteWithFailover}，由外层统一附上节点信息与耗时后再抛出。
     * 不参与 failover——节点本身没问题，无需切换副本。
     */
    static final class RemoteBusinessException extends Exception {
        private static final long serialVersionUID = 1L;
        private final Throwable raw;

        RemoteBusinessException(Throwable raw) {
            super(raw == null ? "remote business error" : raw.getMessage(), raw);
            this.raw = raw;
        }

        Throwable getRawThrowable() {
            return raw;
        }
    }
}