package com.zqzqq.bootkits.distributed.rpc;

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.distributed.rpc.proto.InvokeReply;
import com.zqzqq.bootkits.distributed.rpc.proto.InvokeRequest;
import com.zqzqq.bootkits.distributed.rpc.proto.PluginInvocationServiceGrpc;
import com.zqzqq.bootkits.distributed.serialization.DistTrace;
import com.zqzqq.bootkits.distributed.serialization.PayloadCodec;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * 插件泛化调用服务实现（执行节点侧）。
 * <p>
 * 接收宿主的泛化调用请求，在本地的插件服务注册中心中查找目标服务实例，
 * 反射执行方法并把结果/异常序列化回传。
 *
 * <p>接口定位具备<b>插件类加载器感知</b>：优先从已注册服务的接口集合里取得由插件
 * 类加载器加载的接口 Class（顺带取得插件类加载器，用于参数/返回值类型解析），
 * 使仅存在于插件 jar 内的接口与 DTO 也能被正确反射；仅当接口为共享 classpath
 * 类型时回退到应用 classloader 加载。</p>
 */
public class PluginInvocationServiceImpl extends PluginInvocationServiceGrpc.PluginInvocationServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(PluginInvocationServiceImpl.class);

    private final PluginServiceRegistry localRegistry;

    public PluginInvocationServiceImpl(PluginServiceRegistry localRegistry) {
        this.localRegistry = localRegistry;
    }

    @Override
    public void invoke(InvokeRequest request, StreamObserver<InvokeReply> responseObserver) {
        // 恢复调用链路的 traceId，使执行节点日志与宿主处于同一条链路
        String traceId = request.getHeadersMap().get(DistTrace.HEADER_NAME);
        DistTrace.put(traceId);
        InvokeReply reply;
        try {
            reply = doInvoke(request);
        } catch (Throwable t) {
            log.error("执行远端插件调用失败: plugin={}, method={}",
                    request.getPluginId(), request.getMethodName(), t);
            reply = InvokeReply.newBuilder()
                    .setStatus(-1)
                    .setErrorMessage(t.getMessage() == null ? t.getClass().getName() : t.getMessage())
                    .setErrorType(t.getClass().getName())
                    .setStackTrace(stackTraceOf(t))
                    .build();
        } finally {
            DistTrace.put(null);
        }
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    private InvokeReply doInvoke(InvokeRequest request) throws Throwable {
        String pluginId = request.getPluginId();
        String interfaceName = request.getServiceInterface();

        // 先定位该插件已加载的接口 Class（含其插件类加载器），确保插件专属
        // （仅存在于插件 jar、不在应用 classpath）的接口也能被正确解析，而不是
        // 硬编码从应用 classloader 用 Class.forName 加载（那会对共享接口失效）。
        Class<?> interfaceClass = resolveInterface(pluginId, interfaceName);
        if (interfaceClass == null) {
            return InvokeReply.newBuilder()
                    .setStatus(-1)
                    .setErrorMessage("插件服务接口未加载: " + pluginId + "@" + interfaceName)
                    .setErrorType("com.zqzqq.bootkits.core.communication.exception.ServiceNotFoundException")
                    .build();
        }
        ClassLoader pluginLoader = interfaceClass.getClassLoader();

        Object service = localRegistry.getService(pluginId, interfaceClass);
        if (service == null) {
            return InvokeReply.newBuilder()
                    .setStatus(-1)
                    .setErrorMessage("本地未找到插件服务: " + pluginId + "@" + interfaceClass.getName())
                    .setErrorType("com.zqzqq.bootkits.core.communication.exception.ServiceNotFoundException")
                    .build();
        }

        Method method = findMethod(interfaceClass, request.getMethodName(),
                request.getParamTypeNamesList(), pluginLoader);
        // 用插件类加载器反序列化参数，保证插件专属的 DTO/参数类型可被加载
        Object[] args = PayloadCodec.fromJsonArray(
                request.getParamValuesList(),
                request.getParamTypeNamesList(),
                pluginLoader);

        Object result;
        try {
            result = method.invoke(service, args);
        } catch (InvocationTargetException e) {
            throw e.getCause() == null ? e : e.getCause();
        }

        InvokeReply.Builder builder = InvokeReply.newBuilder().setStatus(0);
        if (method.getReturnType() != void.class && method.getReturnType() != Void.class) {
            builder.setReturnType(method.getReturnType().getName());
            builder.setReturnValue(PayloadCodec.toJson(result));
        }
        return builder.build();
    }

    /**
     * 定位指定插件已加载的服务接口 Class。
     * <p>
     * 优先遍历 {@link PluginServiceRegistry#getServicesByPlugin(String)}——其中返回的接口
     * {@link Class} 已由插件类加载器加载，既拿到正确的 Class，又顺带取得插件类加载器
     * （供后续参数/返回值的类型解析）。若该接口属于共享 classpath 类型而未出现在
     * 插件接口集合里（例如应用声明的通用接口），则回退到应用 classloader 加载。
     */
    private Class<?> resolveInterface(String pluginId, String interfaceName) {
        try {
            Set<Class<?>> pluginInterfaces = localRegistry.getServicesByPlugin(pluginId);
            if (pluginInterfaces != null) {
                for (Class<?> iface : pluginInterfaces) {
                    if (iface.getName().equals(interfaceName)) {
                        return iface;
                    }
                }
            }
        } catch (Exception ignored) {
            // getServicesByPlugin 可能因实现不同而抛错，忽略并回退
        }
        try {
            return Class.forName(interfaceName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * 在接口上定位目标方法。
     * <p>按方法名 + 参数类型精确匹配，规避同名重载（overload）下仅按方法名
     * 匹配可能选错签名的问题。参数类型名需在传入的类加载器中可解析。</p>
     */
    private Method findMethod(Class<?> interfaceClass, String methodName,
                              List<String> paramTypeNames, ClassLoader loader) {
        for (Method method : interfaceClass.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != paramTypeNames.size()) {
                continue;
            }
            boolean matched = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!typeNameMatches(paramTypeNames.get(i), parameterTypes[i], loader)) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return method;
            }
        }
        throw new IllegalArgumentException(
                "接口中不存在匹配的方法: " + interfaceClass.getName() + "." + methodName
                        + "(" + String.join(", ", paramTypeNames) + ")");
    }

    private boolean typeNameMatches(String declaredTypeName, Class<?> parameterType, ClassLoader loader) {
        String resolvedName = parameterType.getName();
        if (resolvedName.equals(declaredTypeName)) {
            return true;
        }
        // 声明的参数类型可能是插件加载的类，与当前已解析的 Class 即使名字相同也需兼容；
        // 若名字不同（如基本类型装箱），尝试按声明类型名在当前类加载器解析后比较。
        try {
            Class<?> declared = Class.forName(declaredTypeName, false, loader);
            return declared.equals(parameterType);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static String stackTraceOf(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}