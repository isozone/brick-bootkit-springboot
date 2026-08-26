package demo.host.client;

import demo.api.UserService;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Feign 客户端：典型写法——@FeignClient 接口 extends 真正的服务接口 UserService。
 * <p>
 * 调用方代码完全不变。brick 的 Feign 桥（启用 plugin.distributed 时默认开启）会识别
 * 该接口并将其父接口 UserService 命中为插件能力，从而把调用透明路由到跨容器的 Worker
 * （经 LOCATOR → gRPC），而非发起 HTTP。
 * <p>
 * url 仅为让 OpenFeign 成功创建客户端占位；实际流量由桥接拦截，不会走到该 HTTP 地址。
 */
@FeignClient(name = "worker", url = "http://localhost:9090")
public interface UserClient extends UserService {
}
