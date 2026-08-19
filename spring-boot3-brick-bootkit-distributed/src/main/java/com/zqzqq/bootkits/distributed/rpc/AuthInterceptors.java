package com.zqzqq.bootkits.distributed.rpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC 双侧鉴权拦截器工具。
 * <p>
 * 鉴权基于「两端共享同一 token」的简单方案：
 * <ul>
 *   <li><b>客户端</b>：{@link #client(String)} 在每次 RPC 的 metadata 头注入
 *       {@code authorization: Bearer <token>}；{@code token} 为 null/空时不注入（不启用）。</li>
 *   <li><b>服务端</b>：{@link #server(String)} 校验 incoming 的 token 是否与配置一致，
 *       不一致返回 {@code Status.UNAUTHENTICATED}；配置为空时放行（不启用）。</li>
 * </ul>
 * 适用于内网可信环境的轻量认证；如需更强的安全，请与 TLS（{@code tls-enabled}）配合，
 * 避免 token 明文传输。
 */
public final class AuthInterceptors {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptors.class);

    /** gRPC authorization metadata 键。 */
    public static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private static final String BEARER_PREFIX = "Bearer ";

    private AuthInterceptors() {
    }

    /**
     * 客户端拦截器：注入鉴权 token。token 为空时返回空数组（无鉴权）。
     */
    public static ClientInterceptor client(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    MethodDescriptor<ReqT, RespT> method,
                    CallOptions callOptions,
                    Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                        next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        headers.put(AUTHORIZATION, BEARER_PREFIX + token);
                        super.start(responseListener, headers);
                    }
                };
            }
        };
    }

    /**
     * 服务端拦截器：校验请求携带的 token。配置为空时放行（不启用鉴权）。
     */
    public static ServerInterceptor server(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return new ServerInterceptor() {
            @Override
            public <ReqT, RespT> io.grpc.ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata headers,
                    ServerCallHandler<ReqT, RespT> next) {
                String incoming = headers.get(AUTHORIZATION);
                String expected = BEARER_PREFIX + token;
                if (incoming == null || !expected.equals(incoming)) {
                    log.warn("拒绝未授权调用: 缺少或错误的鉴权 token，方法={}", call.getMethodDescriptor().getFullMethodName());
call.close(Status.UNAUTHENTICATED
                        .withDescription("无效的鉴权凭证"), new Metadata());
                return new ServerCall.Listener<ReqT>() {
                };
            }
            return next.startCall(call, headers);
            }
        };
    }
}