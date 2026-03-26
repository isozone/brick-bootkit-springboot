# host-minimal

最小宿主模板，用于把插件框架快速接入一个标准 Spring Boot 3.x 项目。

## 你需要改的地方

1. 把包名 `com.example.host` 改成你的宿主项目包名
2. 按需修改 `artifactId`
3. 确认 `plugin.pluginPath` 指向你希望放插件包的目录

## 这个模板默认包含

- `spring-boot3-brick-bootkit-web`
- Web 管理台入口
- 最小 `application.yml`
- 标准 `@SpringBootApplication`

## 首次启动后建议执行

```text
GET /plugins-web/api/doctor
```

先看环境自检结果，再放入插件包。
