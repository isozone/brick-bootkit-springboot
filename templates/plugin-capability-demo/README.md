# plugin-capability-demo

新能力演示插件：串联服务注册中心、事件总线、配置热更新与安全扫描。

## 演示的能力

| 能力 | 演示方式 |
|---|---|
| 服务注册中心 | `@BrickService` 注册 GreetingService；`@BrickServiceReference` 跨插件注入 |
| 事件总线 | `@BrickEventListener` 监听 PLUGIN_STARTED / PLUGIN_STOPPED / PLUGIN_ERROR |
| 配置热更新 | `PluginConfigurationManager` 读取/更新插件配置（带版本管理） |
| 安全扫描 | 插件安装时自动执行代码扫描（危险模式会被安全中心标记） |

## 使用

1. 构建并安装到插件目录：
   ```bash
   mvn clean package
   cp target/plugin-capability-demo-*.jar <pluginPath>/capability-demo.jar
   ```
2. 启动宿主（`host-minimal` 或完整 Web Demo）。
3. 验证：
   - `GET /capability-demo/greet?name=brick` -> 服务注册中心调用
   - `GET /capability-demo/config?key=message` -> 配置读取
   - `GET /capability-demo/config/set?key=message&value=hi` -> 配置热更新
   - Web 控制台「服务注册中心」页面可见 GreetingService；
   - 「安全中心」页面扫描插件包可看到代码扫描结果；
   - 宿主日志可见事件总线输出。

## 依赖

- `spring-boot3-brick-bootkit-sdk`：注解（@BrickPlugin / @BrickService / @BrickServiceReference / @BrickEventListener）
- `spring-boot3-brick-bootkit-bootstrap`：插件引导基类
- `spring-boot3-brick-bootkit`（provided）：框架 API
