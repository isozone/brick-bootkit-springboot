package com.example.cluster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 集群模式宿主模板。
 * <p>
 * 启动后：
 * 1. 在共享目录注册节点并启动心跳（ClusterNodeRegistry）
 * 2. 插件生命周期变化时同步状态到集群（ClusterLifecycleExtension）
 * 3. 引入 Redis 后使用 Redis 分布式锁，否则回退文件锁
 * <p>
 * 验证：启动两个实例（不同端口、同一 clusterSharedPath），
 * 在 Web 控制台「集群管理」页面可看到两个在线节点。
 */
@SpringBootApplication
public class TemplateClusterHostApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemplateClusterHostApplication.class, args);
    }
}
