# templates

这个目录提供多套模板：

**正向模板（可直接复制使用）**
- `host-minimal`：标准 Spring Boot 宿主最小接入模板
- `plugin-minimal`：最小插件工程模板
- `host-cluster`：集群模式宿主模板（Redis 分布式锁 + 节点注册 + 插件状态同步）
- `plugin-with-dependency`：带插件间依赖声明的插件模板（配合依赖分析）
- `plugin-capability-demo`：新能力演示插件（服务注册中心 + 事件总线 + 配置热更新 + 安全扫描串联）

**故障模板（用于培训/演示/排障验证）**
- `host-broken-main-package`：故意演示 `plugin.mainPackage` 缺失时的错误模板
- `host-broken-plugin-path`：故意演示插件目录配置错误时的模板
- `plugin-broken-packaging`：故意演示插件未按框架要求打包时的模板

建议用法：

1. 复制模板目录到你自己的工作区
2. 改包名、artifactId、插件 ID
3. 宿主先启动并执行 `/plugins-web/api/doctor`
4. 再打包插件并放入 `plugin.pluginPath`

如果你要做培训、演示或验证排障链路，可以直接运行这些故障模板，对照 doctor、自检页和错误码表查看行为。
