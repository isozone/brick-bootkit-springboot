# plugin-with-dependency

带插件间依赖的插件模板：演示 `pluginDependencies` 声明与依赖分析能力。

## 这个模板包含

- 标准插件引导类 + REST 控制器
- `pluginDependencies` 声明：安装本插件前需先安装 `minimal-plugin`（非可选依赖）

## 使用

1. 先构建并安装 `minimal-plugin` 到插件目录：
   ```bash
   cd ../plugin-minimal && mvn clean package
   cp target/plugin-minimal-*.jar <pluginPath>/minimal-plugin.jar
   ```
2. 再构建本插件并放入插件目录：
   ```bash
   mvn clean package
   cp target/plugin-with-dependency-*.jar <pluginPath>/
   ```
3. 启动宿主，Web 控制台「依赖分析」页面可看到：
   - `plugin-with-dependency -> minimal-plugin` 的依赖边
   - 升级影响面分析结果

## 关键配置

| 配置 | 说明 |
|---|---|
| `pluginDependencies.dependency.pluginId` | 依赖的宿主插件 ID |
| `pluginDependencies.dependency.version` | 依赖插件版本 |
| `pluginDependencies.dependency.optional` | 是否可选（true 时缺失不阻断安装） |
