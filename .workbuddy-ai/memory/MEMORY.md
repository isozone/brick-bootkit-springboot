# 项目长期记忆

## 项目定位

`brick-bootkit-springboot`：Spring Boot 3 插件化开发框架（groupId `com.zqzqq`，包根 `com.zqzqq.bootkits`）。
源自 starblues 的 springboot-plugin-framework，原作者停更后在 Spring Boot 3.5 + JDK 17 上二次改造并开源。
当前版本 **4.0.11**，主分支 `main`，构建 `mvn clean install -Dgpg.skip=true -Djacoco.skip=true -DskipTests=true`。

## 模块（12 个，源码规模 main/test）

| 模块 | main | test | 职责 |
|---|---|---|---|
| spring-boot3-brick-bootkit | 196 | 18 | 主模块/编排入口（annotation、core、integration、spring、utils） |
| spring-boot3-brick-bootkit-core | 126 | 20 | 生命周期、config、dependency、isolation、security、monitoring、eventbus、sandbox |
| spring-boot3-brick-bootkit-web | 107 | 27 | 控制台：Thymeleaf + Vue3 + ECharts，15 个 api controller |
| spring-boot3-brick-bootkit-loader | 82 | 7 | PluginClassLoader 类隔离 |
| spring-boot3-brick-bootkit-bootstrap | 59 | 2 | 插件独立 Spring 容器引导 |
| spring-boot3-brick-bootkit-scripts | 46 | 16 | Shell/Python/Lua/Bat 脚本执行 |
| spring-boot3-brick-bootkit-maven-packager | 43 | 2 | 构建期打包（dev/prod），`prepare-meta` + `repackage` |
| spring-boot3-brick-bootkit-distributed | 25 | 19 | gRPC + Redis 远程插件 |
| spring-boot3-brick-bootkit-common | 26 | 3 | 常量/工具/插件描述 |
| spring-boot3-brick-bootkit-sdk | 5 | 1 | 对外开发包 |
| spring-boot3-brick-bootkit-spring-boot-starter | 4 | 2 | 统一 Starter |
| spring-boot3-brick-bootkit-archetype | 0 | 0 | 脚手架 |

依赖方向：web → 主模块 → {bootstrap, core, loader} → common；maven-packager/archetype 仅构建期。

## 关键约定

- Java 17 / Spring Boot 3.5.5 / Maven 3.6+；dev profile 默认激活（跳过 javadoc/source/gpg）。
- 质量门禁：Checkstyle（根 `checkstyle.xml` + suppressions）、JaCoCo（指令 80% / 分支 40%，haltOnFailure=false）、PMD、SpotBugs。
- 配置前缀 `plugin.*`（enable / runMode / pluginPath / mainPackage / clusterSharedPath / admissionMode / rolloutMode 等）。
- 主包 `plugin.mainPackage` 已支持自动推断，非标准结构才需显式配置。
- 自检接口 `GET /plugins-web/api/doctor`；控制台 `/plugins-web/index`（支持 `?embedded=true`）。
- 发布脚本：`release-precheck.sh` / `.ps1`，`update-version.sh|bat <version>`（会生成 pom 备份）。
- 模板目录 `templates/`（host-minimal、plugin-minimal、3 个 broken 排障模板）；文档站 `docs-website/`。

## 存量项目接入能力（2026-09 新增）

三件套按顺序使用：**preflight → 切片分析器 → L0/L1/L2 接入 → doctor**。

- **离线体检器**：`tools/preflight/preflight.sh <项目路径> [--json]`。零依赖单文件 Java，
  JDK 单文件源码模式运行，不编译/不修改目标项目。退出码 0/1/2（无阻断/有阻断/参数错），可进 CI。
  检查项前缀 `PFxxx`，输出建议 mainPackage 与推荐接入级别。文档 `doc/8.旧项目渐进式接入指南.md`。
- **业务切片分析器**：`tools/slicer/slice.sh <项目路径> [--json] [--top N] [--depth N]`。
  同为单文件源码模式；源码级包依赖图（非字节码），换取"项目能编译之前就能跑"。
  输出候选切片排序 + fanIn/fanOut + 对外契约类 + 循环依赖双向伙伴 + 需要主程序提供/切出后需改哪些包。
  退出码恒 0（建议性工具，不可当 CI 门禁）。文档 `doc/9.业务切片分析指南.md`。
- **渐进式接入级别**：由 `plugin.autoLoadPlugins` + `plugin.autoStartPlugins` 组合而成
  （默认均 true = 全量模式 / AdoptionLevel.ACTIVE）：
  - L0 影子：`autoLoadPlugins=false`，框架装配但不读插件目录
  - L1 观察：`autoStartPlugins=false`，解析校验插件但不启动
  - L2 全量：两者皆 true
  拦截实现在 `DefaultPluginOperator#initPlugins`；doctor 会报 `ADOPTION_LEVEL_*` 警告（预期行为）。

## 近期演进重点

2026-02：准入管线 admission、集群锁可插拔、迁移校验和、灰度回滚、SPI 生命周期扩展、Web 鉴权 delegate。
2026-08：安全中心、服务注册中心、配置热更新、性能与配额、集群管理、依赖分析、灰度 UI、事件总线可视化、统一 Starter。
2026-09：存量项目接入三件套（preflight / 渐进式接入级别 / 业务切片分析器）+ 配套文档 doc/8、doc/9。
ROADMAP 当前阶段主题：**降低第一次接入门槛**（模板 + doctor + checklist + 文档站同步）。

## 已踩过的坑

- `plugin.mainPackage` 自动推断依赖 `AutoConfigurationPackages.get()` 的**第一个**包，
  推断过宽会导致插件类被主加载器抢走，表现为极难排查的 `ClassCastException`。
- Mockito mock 接口时**默认方法返回 null**，doctor 等消费默认方法处必须判空。
- 静态扫描工具（preflight / slicer）都要按根 pom 的 `<modules>` 收敛扫描范围，
  否则仓库里的 `templates/`、`*-demo` 会被当成业务代码。
- 解析源码时"启动类识别"等必须用行首锚定正则，否则注释和字符串里的文本会误报。
- **同一文件上并行发起多个 Edit 调用会静默丢失**（本次丢过 4 处）：
  同一文件务必逐个编辑，改完立刻读回校验。
- 沙箱内 bash 的 `grep` 偶发返回空结果，改用 Grep 工具。
