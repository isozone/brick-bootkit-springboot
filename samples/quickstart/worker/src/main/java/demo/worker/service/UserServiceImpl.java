package demo.worker.service;

import com.zqzqq.bootkits.core.communication.annotation.PluginService;
import demo.api.UserService;
import org.springframework.stereotype.Service;

/**
 * 业务实现：只比普通微服务多一个 {@link PluginService} 注解（接口 UserService 完全不变）。
 * <p>
 * brick 的「宿主级自动注册」会在 Worker 启动后扫描主上下文，把该 bean 注册进本地
 * 注册中心；因本节点 role=WORKER，ServiceRegistrationScheduler 会把它作为跨容器能力
 * 发布到 Nacos（含 gRPC 地址）。Host 即可经 LOCATOR/gRPC 调用。
 */
@Service
@PluginService(version = "1.0.0")
public class UserServiceImpl implements UserService {

    @Override
    public String getUserName(Long userId) {
        return "worker-User-" + userId;
    }
}
