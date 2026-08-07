# host-cluster

集群模式宿主模板：演示 `plugin.clusterEnabled` 下的节点注册、插件状态同步与分布式锁。

## 这个模板包含

- 共享目录节点注册 + 心跳（`ClusterNodeRegistry`）
- 插件生命周期状态同步（`ClusterLifecycleExtension`）
- Redis 分布式锁（引入 `spring-boot-starter-data-redis` 后自动启用，否则回退文件锁）

## 运行

```bash
# 实例 1
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"

# 实例 2（另开终端，验证多节点）
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

启动后访问 `http://localhost:8080/plugins-web/`，在「集群管理」页面查看在线节点与插件状态。

## 关键配置

| 配置 | 说明 |
|---|---|
| `plugin.clusterEnabled` | 是否启用集群模式 |
| `plugin.clusterSharedPath` | 多实例共享目录（节点注册/状态同步/文件锁） |
| `plugin.clusterLockTimeoutMs` | 集群锁超时（毫秒） |
| `spring.data.redis.*` | 可选：Redis 连接（启用 Redis 分布式锁） |
