# host-grayscale

灰度发布宿主模板：演示 `plugin.rolloutMode=gray` 下的升级探针校验与失败自动回滚。

## 这个模板包含

- 灰度发布配置（`rolloutMode=gray` + 自动启动 + 失败回滚）
- 冒烟探针示例（`SmokeRolloutProbe`，检查插件描述符完整性）
- 完整 Web 控制台（灰度发布页面可查看配置/探针/模拟决策）

## 运行

```bash
mvn spring-boot:run
```

启动后访问 `http://localhost:8080/plugins-web/`，在「灰度发布」页面可看到：

- 发布模式为 `GRAY`，已注册 1 个探针（smoke-probe）
- 选择插件后可模拟灰度决策，查看每个探针的通过/拒绝结果

## 灰度流程说明

升级插件时（`plugin.rolloutMode=gray`）：

1. 安装新版本插件
2. 依次运行容器中所有 `PluginRolloutProbe` 探针
3. 任一探针未通过 -> 升级失败 -> 卸载新版本 -> 回滚备份版本 -> 按需恢复启动

## 自定义探针

实现 `com.zqzqq.bootkits.integration.rollout.PluginRolloutProbe` 并以 Spring Bean 注入即可，
框架自动收集（可参考 `SmokeRolloutProbe`）。

## 关键配置

| 配置 | 说明 |
|---|---|
| `plugin.rolloutMode` | `gray` 开启灰度，`direct` 直接发布 |
| `plugin.rolloutAutoStart` | 安装/升级后是否自动启动 |
| `plugin.rolloutRollbackOnFailure` | 升级失败时是否自动回滚备份版本 |
