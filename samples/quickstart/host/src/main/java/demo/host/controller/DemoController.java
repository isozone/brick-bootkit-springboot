package demo.host.controller;

import demo.host.client.UserClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    private final UserClient userClient;

    public DemoController(UserClient userClient) {
        this.userClient = userClient;
    }

    @GetMapping("/user/{id}")
    public String user(@PathVariable Long id) {
        // 看起来是普通 Feign 调用，实际经 brick 桥跨容器落到 Worker 的 @PluginService 实现
        return userClient.getUserName(id);
    }
}
