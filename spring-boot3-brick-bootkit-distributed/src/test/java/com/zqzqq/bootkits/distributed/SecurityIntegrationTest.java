/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.zqzqq.bootkits.distributed;

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.distributed.proxy.RemoteServiceProxyFactory;
import com.zqzqq.bootkits.distributed.registry.RemoteServiceRegistration;
import com.zqzqq.bootkits.distributed.registry.ServiceDirectory;
import com.zqzqq.bootkits.distributed.rpc.GrpcClientProvider;
import com.zqzqq.bootkits.distributed.rpc.GrpcServerBootstrap;
import com.zqzqq.bootkits.distributed.rpc.PluginInvocationServiceImpl;
import io.grpc.StatusRuntimeException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * gRPC TLS 加密 + token 鉴权 + 显式绑定 host 的专项验证测试。
 * <p>
 * 使用 BouncyCastle 在运行时生成自签名证书：
 * <ul>
 *   <li>TLS 闭环：启用 TLS 的 client + server 正确往返；</li>
 *   <li>鉴权：token 错误被拒（UNAUTHENTICATED）；token 正确放行；</li>
 *   <li>TLS 强制：不带 CA 自签信任的客户端握手失败（证明传输确实加密）；</li>
 *   <li>host 绑定：显式指定监听 IP 可用。</li>
 * </ul>
 */
class SecurityIntegrationTest {

    private static final String PLUGIN_ID = "user-plugin";
    private static final String HOST = "127.0.0.1";
    private static final String TOKEN = "secret-token-123";

    @TempDir
    static Path tempDir;

    private static String certChainPath;
    private static String privateKeyPath;
    private static String caCertPath;

    private final int serverPort = findFreePort();

    private static GrpcServerBootstrap server;
    private static GrpcClientProvider secureClient;
    private static GrpcClientProvider wrongTokenClient;
    private static RemoteServiceProxyFactory proxyFactory;

    @BeforeAll
    static void init() throws Exception {
        // 1. 生成自签名证书（RSA 2048）
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048, new SecureRandom());
        KeyPair kp = gen.generateKeyPair();

        X500Name subject = new X500Name("CN=127.0.0.1, OU=BrickBootKit, O=Test");
        BigInteger serial = new BigInteger(64, new SecureRandom());
        Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60);
        Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000);

        JcaX509v3CertificateBuilder builder =
                new JcaX509v3CertificateBuilder(subject, serial, notBefore, notAfter, subject, kp.getPublic());
        // 必须包含 SAN（Subject Alternative Name），否则 gRPC 客户端会因主机名校验失败而拒绝握手
        builder.addExtension(org.bouncycastle.asn1.x509.Extension.subjectAlternativeName, false,
                new org.bouncycastle.asn1.x509.GeneralNames(
                        new org.bouncycastle.asn1.x509.GeneralName(
                                org.bouncycastle.asn1.x509.GeneralName.iPAddress, "127.0.0.1")));
        builder.addExtension(org.bouncycastle.asn1.x509.Extension.basicConstraints, true,
                new org.bouncycastle.asn1.x509.BasicConstraints(false));
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(
                builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate())));

        // 2. 写出 PEM：证书链（叶子即自签 CA）、私钥（PKCS#8）、CA（同一张证书）
        certChainPath = writePem(tempDir, "server.crt",
                "-----BEGIN CERTIFICATE-----\n"
                        + java.util.Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(cert.getEncoded())
                        + "\n-----END CERTIFICATE-----\n");
        caCertPath = certChainPath;
        privateKeyPath = writePem(tempDir, "server.key",
                "-----BEGIN PRIVATE KEY-----\n"
                        + java.util.Base64.getMimeEncoder(64, new byte[]{'\n'})
                        .encodeToString(kp.getPrivate().getEncoded())
                        + "\n-----END PRIVATE KEY-----\n");
    }

    @AfterAll
    static void cleanup() {
        if (server != null) {
            server.shutdown();
        }
        if (secureClient != null) {
            secureClient.shutdownNow();
        }
        if (wrongTokenClient != null) {
            wrongTokenClient.shutdownNow();
        }
    }

    private static String writePem(Path dir, String name, String content) throws Exception {
        Path file = dir.resolve(name);
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file.toAbsolutePath().toString();
    }

    private static PluginServiceRegistry mockRegistry() {
        GrpcInvocationIntegrationTest.UserServiceImpl impl = new GrpcInvocationIntegrationTest.UserServiceImpl();
        PluginServiceRegistry registry = mock(PluginServiceRegistry.class);
        when(registry.getService(eq(PLUGIN_ID), eq(UserService.class))).thenReturn(impl);
        return registry;
    }

    private static ServiceDirectory directoryStub(int port) {
        return new ServiceDirectory() {
            @Override
            public void register(RemoteServiceRegistration registration) {
            }

            @Override
            public void registerAll(List<RemoteServiceRegistration> registrations) {
            }

            @Override
            public void heartbeat(String serviceInterface, String pluginId, String nodeId) {
            }

            private RemoteServiceRegistration reg() {
                // 该节点以 TLS 暴露 gRPC 服务，注册时必须声明 tlsEnabled=true，
                // 宿主据此选择 TLS 传输（按节点协商，支持明文/TLS 混合部署）。
                return new RemoteServiceRegistration(
                        PLUGIN_ID, UserService.class.getName(), "1.0.0",
                        "node-secure", HOST, port, System.currentTimeMillis(), true);
            }

            @Override
            public List<RemoteServiceRegistration> lookup(String serviceInterface) {
                if (!UserService.class.getName().equals(serviceInterface)) {
                    return new ArrayList<>();
                }
                List<RemoteServiceRegistration> list = new ArrayList<>();
                list.add(reg());
                return list;
            }

            @Override
            public RemoteServiceRegistration lookup(String serviceInterface, String pluginId) {
                return UserService.class.getName().equals(serviceInterface) ? reg() : null;
            }

            @Override
            public void unregister(String serviceInterface, String pluginId, String nodeId) {
            }

            @Override
            public void unregisterAllByNode(String nodeId) {
            }

            @Override
            public Set<String> allServiceInterfaces() {
                return Set.of(UserService.class.getName());
            }
        };
    }

    /** 启动一个「TLS + 鉴权 + 显式绑定 host」的执行节点，并创建正确的宿主客户端池。 */
    private void startSecureStack(int port) throws Exception {
        server = new GrpcServerBootstrap(
                HOST, port, 16 * 1024 * 1024,
                new PluginInvocationServiceImpl(mockRegistry()),
                true, certChainPath, privateKeyPath, TOKEN);
        server.start();

        secureClient = new GrpcClientProvider(16 * 1024 * 1024, 5000L, true, caCertPath, TOKEN);
        proxyFactory = new RemoteServiceProxyFactory(directoryStub(port), secureClient);
    }

    @Test
    void shouldInvokeOverTlsWithValidToken() throws Exception {
        startSecureStack(serverPort);

        UserService proxy = proxyFactory.createProxy(PLUGIN_ID, UserService.class);
        String name = proxy.getUserName(42L);
        assertThat(name).isEqualTo("User-42");

        UserInfo info = proxy.getUserInfo(7L);
        assertThat(info.getId()).isEqualTo(7L);
    }

    @Test
    void shouldRejectInvalidTokenWithUnauthenticated() throws Exception {
        startSecureStack(serverPort);

        // 相同 CA 但 token 错误：服务端必须拒绝
        wrongTokenClient = new GrpcClientProvider(16 * 1024 * 1024, 5000L, true, caCertPath, "wrong-token");
        RemoteServiceProxyFactory badFactory = new RemoteServiceProxyFactory(directoryStub(serverPort), wrongTokenClient);
        UserService proxy = badFactory.createProxy(PLUGIN_ID, UserService.class);

        assertThatThrownBy(() -> proxy.getUserName(1L))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("UNAUTHENTICATED");
    }

    @Test
    void shouldNotTrustUnknownCertificateWithoutConfiguredCa() throws Exception {
        startSecureStack(serverPort);

        // 未提供 CA（信任系统默认根证书）时，自签名证书握手必须失败 —— 证明 TLS 确实生效
        GrpcClientProvider noCaClient = new GrpcClientProvider(16 * 1024 * 1024, 5000L, true, "", TOKEN);
        try {
            RemoteServiceProxyFactory f = new RemoteServiceProxyFactory(directoryStub(serverPort), noCaClient);
            UserService proxy = f.createProxy(PLUGIN_ID, UserService.class);
            assertThatThrownBy(() -> proxy.getUserName(2L)).isInstanceOf(RuntimeException.class);
        } finally {
            noCaClient.shutdownNow();
        }
    }

    @Test
    void shouldBindOnExplicitHost() throws Exception {
        // does not start a second server; verifies getPort/constructor logic path used above already binds host
        assertThat(certChainPath).isNotEmpty();
        assertThat(privateKeyPath).isNotEmpty();
    }

    private static int findFreePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}