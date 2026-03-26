# host-broken-plugin-path

这是一个**故意配置错误**的宿主模板，用来演示 `plugin.pluginPath` 指向错误目录时的表现。

## 预期现象

- doctor 会提示插件目录不存在
- doctor 会提示当前未发现任何插件
- Web 首页 checklist 会一直提示“把插件包放到 plugin.pluginPath 指向的目录”

## 修复方式

1. 把 `application.yml` 中的 `pluginPath` 改成真实存在的目录
2. 或直接改用 `templates/host-minimal`
