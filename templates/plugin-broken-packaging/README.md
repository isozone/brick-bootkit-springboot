# plugin-broken-packaging

这是一个**故意缺少打包插件配置**的插件模板，用来演示插件包未按框架要求打包时的表现。

## 预期现象

- 宿主上传或安装时可能提示插件文件无效
- doctor 环境正常，但插件校验或安装失败

## 这个模板故意省略了什么

- `spring-boot3-brick-bootkit-maven-packager`
- `prepare-meta`
- `repackage`

## 修复方式

直接对照 `templates/plugin-minimal/pom.xml`，把 Maven Packager 的配置补回来。
