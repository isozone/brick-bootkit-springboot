# host-broken-main-package

这是一个**故意会失败**的宿主模板，用来演示 `plugin.mainPackage` 缺失时的表现。

## 预期现象

- 启动期会报 `plugin.mainPackage` 相关错误
- doctor 会提示主应用包名未检测到

## 用途

- 给新同学演示“为什么标准 `@SpringBootApplication` + 自动推断更重要”
- 用于测试错误提示和排障链路

## 修复方式

1. 删除这个模板里的自定义 `BrokenIntegrationConfiguration`
2. 回到 `templates/host-minimal`
3. 或显式配置正确的 `plugin.mainPackage`
