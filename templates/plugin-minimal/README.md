# plugin-minimal

最小插件模板，用于快速生成一个可以被宿主加载的插件工程。

## 你需要改的地方

1. 把包名 `com.example.plugin` 改成你的插件包名
2. 修改 `pluginInfo.id`
3. 修改 `pluginInfo.bootstrapClass`
4. 修改插件版本和描述

## 打包

```bash
mvn clean package
```

打包后把生成的插件包放到宿主的 `plugin.pluginPath` 指向目录即可。
