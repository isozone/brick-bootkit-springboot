package com.zqzqq.bootkits.distributed;

/**
 * 测试用共享服务契约，模拟「宿主与执行节点双方 classpath 都可见的接口」。
 * <p>包含一对同名重载方法（{@code getUserInfo}），用于验证执行节点的签名精确匹配，
 * 避免仅按方法名匹配时选错重载。</p>
 */
public interface UserService {

    String getUserName(Long userId);

    UserInfo getUserInfo(Long userId);

    UserInfo getUserInfo(String name);
}