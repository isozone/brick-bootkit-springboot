package demo.api;

/**
 * 共享服务契约（*-contract）。
 * <p>
 * 该接口由 Worker 实现并提供为分布式能力（@PluginService），由 Host 通过 Feign 引用。
 * 接口本身保持不变——这是「零改接口」的目标。
 */
public interface UserService {

    String getUserName(Long userId);
}
