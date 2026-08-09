import generatedConfigKeys from './generated-config-keys.js';

export const siteMeta = {
  product: 'Brick BootKit SpringBoot',
  version: '4.0.6',
  checkedAt: '2026-03-26',
  repo: 'https://github.com/v18268185209/brick-bootkit-springboot',
  docs: 'http://brickbootkit.aius.autos/',
  sourceBase: 'https://github.com/v18268185209/brick-bootkit-springboot/blob/HEAD'
};

const homeModulesTable = {
  columns: ['模块', '职责', '源码位置'],
  rows: [
    ['spring-boot3-brick-bootkit', '主集成模块，管理插件生命周期与集成配置', 'spring-boot3-brick-bootkit/pom.xml'],
    ['spring-boot3-brick-bootkit-core', '核心能力：生命周期、通信、安全、监控等', 'spring-boot3-brick-bootkit-core/src/main/java'],
    ['spring-boot3-brick-bootkit-loader', '主程序引导与类加载启动链路', 'spring-boot3-brick-bootkit-loader/src/main/java'],
    ['spring-boot3-brick-bootkit-bootstrap', '插件侧引导基类与注解支持', 'spring-boot3-brick-bootkit-bootstrap/src/main/java'],
    ['spring-boot3-brick-bootkit-maven-packager', 'Maven 打包插件：repackage / prepare-meta', 'spring-boot3-brick-bootkit-maven-packager/src/main/java'],
    ['spring-boot3-brick-bootkit-scripts', '脚本执行与调度能力', 'spring-boot3-brick-bootkit-scripts/src/main/java'],
    ['spring-boot3-brick-bootkit-web', '插件管理与监控 Web 控制台', 'spring-boot3-brick-bootkit-web/src/main/java'],
    ['spring-boot3-brick-bootkit-common', '公共常量和工具类', 'spring-boot3-brick-bootkit-common/src/main/java']
  ]
};

const autoConfigIndexTable = {
  columns: ['配置键', '来源分组'],
  rows: (generatedConfigKeys.keys || []).map((item) => [item.key, item.group])
};

export const docPages = [
  {
    id: 'home',
    path: '/',
    title: '源码驱动的文档站',
    lead: '这版 docs-website 以仓库源码为唯一事实来源，先保证“真”，再追求“好看”。',
    badges: ['Version 4.0.6', 'Java 17+', 'Spring Boot 3.5.5', 'Checked 2026-03-26'],
    sections: [
      {
        id: 'facts',
        title: '事实快照',
        paragraphs: [
          '父工程版本在 `pom.xml` 中定义为 `4.0.6`，并包含 8 个核心模块。',
          '默认 Java 版本为 17，`spring-boot.version` 在父工程中固定为 `3.5.5`。',
          '文档中涉及的配置项来自 `AutoIntegrationConfiguration` 与 Web 模块配置类，不使用猜测字段。'
        ],
        bullets: [
          '核心运行模式：`plugin.runMode=dev|prod`（源码枚举 `RuntimeMode`）',
          '插件准入管线：`plugin.admissionMode=off|warn|enforce`',
          '升级发布策略：`plugin.rolloutMode=direct|gray`',
          'Web 鉴权模式：`plugin.web.auth.mode=disabled|delegate|strict`'
        ],
        sources: [
          'pom.xml',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/AutoIntegrationConfiguration.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/core/admission/PluginAdmissionMode.java',
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/auth/PluginWebAuthMode.java'
        ]
      },
      {
        id: 'modules',
        title: '模块地图',
        lead: '仓库当前结构是“核心 + 集成 + 打包 + Web 控制台 + 脚本能力”的组合，而不是单体 SDK。',
        table: homeModulesTable,
        sources: ['pom.xml']
      },
      {
        id: 'principles',
        title: '文档改造原则',
        callout: {
          tone: 'info',
          title: '为什么重构这套站点',
          body: '旧站内容存在编码异常、路由页面重复、文案可信度难验证。新站统一改为结构化数据驱动，页面视觉与内容一致升级。'
        },
        bullets: [
          '每页都给出源码依据路径，便于开发者二次核对。',
          '默认优先展示可直接落地的配置和接口。',
          '对“仍在演进中的能力”会明确标注，不误导生产使用。'
        ]
      }
    ]
  },
  {
    id: 'introduction',
    path: '/introduction',
    title: '框架定位',
    lead: 'Brick BootKit SpringBoot 的核心价值是：在 Spring Boot 主程序中，以插件方式扩展能力并支持运行时生命周期管理。',
    badges: ['Plugin Runtime', 'Hot Install', 'ClassLoader Isolation'],
    sections: [
      {
        id: 'positioning',
        title: '能力边界',
        paragraphs: [
          '从接口层看，`PluginManager` 提供加载、安装、启动、停止、卸载、验证、重载等完整操作集合。',
          '从集成层看，`IntegrationConfiguration` 把运行模式、插件路径、版本校验、集群锁、准入和灰度发布放在一套配置入口。'
        ],
        bullets: [
          '不是纯注解库，而是带生命周期和状态管理的运行框架。',
          '不是一次性编译插件，而是支持上传安装、在线启停和回滚流程。',
          '支持插件服务通信注解模型（`@PluginService` / `@ServiceDependency`）。'
        ],
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/core/PluginManager.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/IntegrationConfiguration.java',
          'spring-boot3-brick-bootkit-core/src/main/java/com/zqzqq/bootkits/core/communication/annotation/PluginService.java'
        ]
      },
      {
        id: 'architecture',
        title: '运行架构',
        paragraphs: [
          '主程序侧通过 `SpringMainBootstrap` 引导，插件侧通过 `SpringPluginBootstrap` 启动。',
          '默认插件操作实现为 `DefaultPluginOperator`，内部组合了插件管理器、准入管线、集群锁、生命周期扩展管理器。'
        ],
        code: {
          language: 'text',
          filename: 'runtime-chain.txt',
          content: String.raw`Host App -> SpringMainBootstrap -> DefaultPluginOperator
               -> PluginLauncherManager / PluginManager
               -> Admission Pipeline + Cluster Lock + Lifecycle Extensions`
        },
        sources: [
          'spring-boot3-brick-bootkit-loader/src/main/java/com/zqzqq/bootkits/loader/launcher/SpringMainBootstrap.java',
          'spring-boot3-brick-bootkit-bootstrap/src/main/java/com/zqzqq/bootkits/bootstrap/SpringPluginBootstrap.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/operator/DefaultPluginOperator.java'
        ]
      }
    ]
  },
  {
    id: 'quickstart',
    path: '/quickstart',
    title: '快速开始',
    lead: '下面流程是“最短可运行路径”，全部字段均可在源码中找到。',
    badges: ['10 min', 'Host + Plugin', 'Maven'],
    sections: [
      {
        id: 'deps',
        title: '1) 主程序引入依赖',
        code: {
          language: 'xml',
          filename: 'pom.xml',
          content: String.raw`<dependency>
  <groupId>com.zqzqq</groupId>
  <artifactId>spring-boot3-brick-bootkit</artifactId>
  <version>4.0.6</version>
</dependency>

<!-- 可选：Web 管理控制台 -->
<dependency>
  <groupId>com.zqzqq</groupId>
  <artifactId>spring-boot3-brick-bootkit-web</artifactId>
  <version>4.0.6</version>
</dependency>`
        },
        sources: ['spring-boot3-brick-bootkit/pom.xml', 'spring-boot3-brick-bootkit-web/pom.xml']
      },
      {
        id: 'yaml',
        title: '2) 最小配置',
        code: {
          language: 'yaml',
          filename: 'application.yml',
          content: String.raw`plugin:
  enable: true
  runMode: dev
  pluginPath:
    - ./plugins
  pluginRestPathPrefix: /plugins
  enablePluginIdRestPathPrefix: true`
        },
        callout: {
          tone: 'info',
          title: '关键点',
          body: '标准 Spring Boot 项目现在会优先自动推断 `plugin.mainPackage`。只有在非标准启动结构、多模块特殊扫描或自动推断失败时，才建议显式配置。'
        },
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/AutoIntegrationConfiguration.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/DefaultIntegrationConfiguration.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/ExtendPointConfiguration.java'
        ]
      },
      {
        id: 'bootstrap',
        title: '3) 主程序启动类',
        code: {
          language: 'java',
          filename: 'Application.java',
          content: String.raw`import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}`
        },
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/SpringBootPluginStarter.java',
          'spring-boot3-brick-bootkit/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports'
        ]
      },
      {
        id: 'doctor',
        title: '4) 先跑自检',
        code: {
          language: 'text',
          filename: 'doctor.txt',
          content: String.raw`GET /plugins-web/api/doctor`
        },
        bullets: [
          '先看主应用包名是否已自动推断成功',
          '再看插件目录是否存在且可读',
          '再看上传临时目录是否可写',
          '最后确认当前发现了多少个插件'
        ],
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/doctor/PluginDoctorService.java',
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/DoctorController.java'
        ]
      }
    ]
  },
  {
    id: 'project-structure',
    path: '/project-structure',
    title: '项目结构',
    lead: '多模块拆分是这个仓库的核心设计，建议按“职责”而不是“目录深度”理解。',
    sections: [
      {
        id: 'parent',
        title: '父工程与统一版本',
        paragraphs: [
          '父工程统一管理 Java、Spring Boot、测试和发布插件版本。',
          '发布 profile 与开发 profile 在父 POM 中分离，便于本地开发和中央仓库发布切换。'
        ],
        bullets: [
          '父工程 artifactId: `spring-boot3-brick-bootkit-parent`',
          'groupId: `com.zqzqq`',
          'version: `4.0.6`'
        ],
        sources: ['pom.xml']
      },
      {
        id: 'modules-table',
        title: '模块职责表',
        table: homeModulesTable,
        sources: ['pom.xml']
      }
    ]
  },
  {
    id: 'templates',
    path: '/templates',
    title: '最小模板',
    lead: '如果你不想从大 Demo 里删代码，直接从模板复制通常是最快的接入方式。',
    badges: ['Host Template', 'Plugin Template', 'Beginner Friendly'],
    sections: [
      {
        id: 'template-list',
        title: '仓库内置模板',
        bullets: [
          '`templates/host-minimal`：标准 Spring Boot 宿主最小接入模板',
          '`templates/plugin-minimal`：最小插件工程模板',
          '`templates/host-broken-main-package`：演示主包配置缺失',
          '`templates/host-broken-plugin-path`：演示插件目录错误',
          '`templates/plugin-broken-packaging`：演示插件未按框架要求打包'
        ],
        sources: [
          'templates/README.md',
          'templates/host-minimal/pom.xml',
          'templates/plugin-minimal/pom.xml',
          'templates/host-broken-main-package/pom.xml',
          'templates/host-broken-plugin-path/pom.xml',
          'templates/plugin-broken-packaging/pom.xml'
        ]
      },
      {
        id: 'host-template',
        title: '宿主模板包含什么',
        bullets: [
          '标准 `@SpringBootApplication` 启动类',
          '最小 `application.yml`',
          '`spring-boot3-brick-bootkit-web` 依赖',
          '可直接执行 `/plugins-web/api/doctor` 的基础环境'
        ],
        sources: [
          'templates/host-minimal/src/main/java/com/example/host/TemplateHostApplication.java',
          'templates/host-minimal/src/main/resources/application.yml'
        ]
      },
      {
        id: 'plugin-template',
        title: '插件模板包含什么',
        bullets: [
          '最小 `SpringPluginBootstrap` 插件入口',
          '一个示例 REST Controller',
          'Maven Packager 的 `prepare-meta + repackage` 配置'
        ],
        sources: [
          'templates/plugin-minimal/src/main/java/com/example/plugin/TemplatePluginBootstrap.java',
          'templates/plugin-minimal/src/main/java/com/example/plugin/controller/TemplatePluginController.java',
          'templates/plugin-minimal/pom.xml'
        ]
      },
      {
        id: 'template-flow',
        title: '推荐使用顺序',
        code: {
          language: 'text',
          filename: 'template-flow.txt',
          content: String.raw`1. 复制 host-minimal
2. 复制 plugin-minimal
3. 改包名 / artifactId / pluginInfo.id
4. 启动宿主并执行 GET /plugins-web/api/doctor
5. 打包插件并放入 plugin.pluginPath`
        },
        sources: ['templates/README.md']
      },
      {
        id: 'broken-templates',
        title: '故障示例模板',
        table: {
          columns: ['模板', '故意制造的问题', '适合演示什么'],
          rows: [
            ['host-broken-main-package', '主包配置缺失', 'doctor 如何提示主包问题、启动期如何报错'],
            ['host-broken-plugin-path', '插件目录不存在', 'doctor 和首页 checklist 如何提示“未发现插件”'],
            ['plugin-broken-packaging', '插件未按框架要求打包', '上传校验、错误码和排障文档如何联动']
          ]
        },
        sources: [
          'templates/host-broken-main-package/README.md',
          'templates/host-broken-plugin-path/README.md',
          'templates/plugin-broken-packaging/README.md'
        ]
      }
    ]
  },
  {
    id: 'support-matrix',
    path: '/support-matrix',
    title: '支持矩阵',
    lead: '开源版当前明确承诺和 CI 覆盖到的运行组合，以“说清楚”为第一原则。',
    sections: [
      {
        id: 'official-baseline',
        title: '官方基线',
        table: {
          columns: ['项目', '当前基线', '说明'],
          rows: [
            ['Java', 'JDK 17', '父工程 `java.version=17`'],
            ['Spring Boot', '3.5.5', '父工程 `spring-boot.version=3.5.5`'],
            ['Maven', '3.6+', '建议最低版本'],
            ['Docs Node', '20', 'docs-website CI 使用 Node 20']
          ]
        },
        sources: [
          'pom.xml',
          '.github/workflows/ci.yml',
          'doc/3.兼容与支持矩阵.md'
        ]
      },
      {
        id: 'ci-scope',
        title: 'CI 当前覆盖',
        bullets: [
          'GitHub Actions 当前基线为 `ubuntu-latest + Java 17`',
          '稳定测试模块当前覆盖 `loader / core / spring-boot3-brick-bootkit`',
          'docs-website 使用 `Node 20` 执行校验和构建'
        ],
        sources: ['.github/workflows/ci.yml']
      }
    ]
  },
  {
    id: 'contributing',
    path: '/contributing',
    title: '参与贡献',
    lead: '把 issue、PR、文档和模板更新都做成可协作的流程，是开源版长期可维护的前提。',
    sections: [
      {
        id: 'contributing-files',
        title: '贡献入口',
        bullets: [
          '`CONTRIBUTING.md`：贡献指南',
          '`SECURITY.md`：安全问题反馈约定',
          '`.github/ISSUE_TEMPLATE/bug_report.yml`：缺陷反馈模板',
          '`.github/ISSUE_TEMPLATE/feature_request.yml`：需求建议模板',
          '`.github/pull_request_template.md`：PR 模板'
        ],
        sources: [
          'CONTRIBUTING.md',
          'SECURITY.md',
          '.github/ISSUE_TEMPLATE/bug_report.yml',
          '.github/ISSUE_TEMPLATE/feature_request.yml',
          '.github/pull_request_template.md'
        ]
      },
      {
        id: 'contribution-basics',
        title: '贡献建议',
        bullets: [
          '提 issue 前先附上版本号、运行环境和 doctor 摘要',
          '改了公开行为时，记得同步 README、文档站和模板',
          '尽量避免让外部用户依赖内部实现包'
        ],
        sources: ['CONTRIBUTING.md']
      }
    ]
  },
  {
    id: 'roadmap',
    path: '/roadmap',
    title: '路线图',
    lead: '开源版路线图的核心不是“功能越多越好”，而是“第一次接入成功率更高、排障成本更低、协作体验更稳定”。',
    sections: [
      {
        id: 'current-focus',
        title: '当前重点',
        bullets: [
          '降低第一次接入门槛',
          '打通 doctor、错误码、文档和模板',
          '已落地 9 项增强能力（安全/注册/配置/性能/集群/依赖/灰度/事件总线/日志/插件市场）',
          '统一 Starter 真实运行时，补充 Controller/Service/组件/集成测试与前端国际化'
        ],
        sources: ['ROADMAP.md']
      },
      {
        id: 'next-focus',
        title: '下一阶段',
        bullets: [
          '完成前端静态资源构建和验收',
          '扩展更多场景模板',
          '继续补宿主接入与排障测试'
        ],
        sources: ['ROADMAP.md']
      }
    ]
  },
  {
    id: 'release-checklist',
    path: '/release-checklist',
    title: '发布与验收',
    lead: '这页不是“发布流程图”，而是每次发版前最小必须过一遍的检查清单。',
    sections: [
      {
        id: 'release-items',
        title: '最小发布检查',
        bullets: [
          '`git diff --check` 通过',
          '关键模块测试通过',
          'README / docs-website / 更新日志同步',
          '最小模板和故障模板内容已对齐当前版本',
          '首页 doctor 摘要和上传向导可正常工作'
        ],
        sources: ['doc/4.发布与验收清单.md']
      },
      {
        id: 'release-script',
        title: '自动化预检脚本',
        bullets: [
          '`release-precheck.sh`：Linux / macOS 下的汇总预检脚本',
          '`release-precheck.ps1`：Windows 下的汇总预检脚本'
        ],
        sources: [
          'release-precheck.sh',
          'release-precheck.ps1',
          'doc/4.发布与验收清单.md'
        ]
      }
    ]
  },
  {
    id: 'configuration',
    path: '/configuration',
    title: '配置说明',
    lead: '核心配置入口是 `plugin.*`，此外启动器还支持 developmentMode 的配置化解析。以下内容均对应源码可验证路径。',
    sections: [
      {
        id: 'core-keys',
        title: '核心配置键（plugin.*）',
        table: {
          columns: ['配置键', '默认值', '说明'],
          rows: [
            ['plugin.enable', 'true', '总开关'],
            ['plugin.runMode', 'dev', '运行模式：dev / prod'],
            ['plugin.mainPackage', '空字符串', '主程序包名（标准 Spring Boot 项目优先自动推断）'],
            ['plugin.pluginPath', '["~/plugins/"]', '插件目录列表'],
            ['plugin.uploadTempPath', '系统临时目录/spring-boot3-brick-bootkit-temp', '插件上传临时目录'],
            ['plugin.backupPath', 'backupPlugin', '插件备份目录'],
            ['plugin.pluginRestPathPrefix', '/plugins', '插件 REST 前缀'],
            ['plugin.enablePluginIdRestPathPrefix', 'true', '是否拼接 pluginId 前缀'],
            ['plugin.enablePluginIds', '空', '仅启用指定 pluginId 列表'],
            ['plugin.disablePluginIds', '空', '禁用指定 pluginId 列表；支持 `*`'],
            ['plugin.sortInitPluginIds', '空', '初始化启动顺序'],
            ['plugin.version', '0.0.0', '主程序版本（用于 requires 校验）'],
            ['plugin.exactVersion', 'false', '是否要求完全版本匹配'],
            ['plugin.pluginSwaggerScan', 'true', '是否扫描插件 Swagger 接口'],
            ['plugin.pluginFollowProfile', 'false', '插件 profile 是否跟随主程序'],
            ['plugin.pluginFollowLog', 'false', '插件日志是否跟随主程序'],
            ['plugin.lifecycleExtensionsEnabled', 'true', '是否启用生命周期扩展管理器']
          ]
        },
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/AutoIntegrationConfiguration.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/DefaultIntegrationConfiguration.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/IntegrationConfiguration.java'
        ]
      },
      {
        id: 'launcher-development-mode',
        title: '开发模式配置（启动器）',
        paragraphs: [
          '宿主通过 `SpringMainBootstrap.launch(...)` 启动时，开发模式现在支持配置化，不再必须在宿主类里覆写 `developmentMode()`。',
          '若宿主类覆写了 `developmentMode()`，启动器会优先使用代码返回值；未覆写时才按顺序读取系统属性和环境变量。',
          '`ProdLauncher` 启动主程序包时会先读取 Manifest 的 `Main-Development-Mode`，若未提供再回退到相同的系统属性/环境变量链路。'
        ],
        table: {
          columns: ['键/变量', '默认值', '生效规则（按优先级）'],
          rows: [
            ['Main-Development-Mode（Manifest）', '未配置', '优先级 0；仅 `ProdLauncher` 路径优先读取'],
            ['plugin.developmentMode', '未配置', '优先级 1；仅在未覆写 `developmentMode()` 时读取'],
            ['spring-boot3-brick-bootkit.developmentMode', '未配置', '优先级 2；前项为空时读取'],
            ['developmentMode', '未配置', '优先级 3；前两项为空时读取'],
            ['PLUGIN_DEVELOPMENT_MODE', '未配置', '优先级 4（环境变量）；系统属性都为空时读取'],
            ['SpringBootstrap#developmentMode()', 'isolation', '覆写时直接生效；未覆写且无配置时回落到接口默认值']
          ]
        },
        bullets: [
          '取值仅支持 `isolation` / `coexist`（大小写不敏感）。',
          '该解析链路由 `SpringMainBootstrap` 和 `DevelopmentModeSetting` 协同实现。'
        ],
        code: {
          language: 'bash',
          filename: '启动参数示例',
          content: String.raw`# JVM system property（推荐优先用这一项）
java -Dplugin.developmentMode=coexist -jar app.jar

# 兼容历史键
java -Dspring-boot3-brick-bootkit.developmentMode=coexist -jar app.jar
java -DdevelopmentMode=coexist -jar app.jar

# 或使用环境变量
export PLUGIN_DEVELOPMENT_MODE=coexist`
        },
        sources: [
          'spring-boot3-brick-bootkit-loader/src/main/java/com/zqzqq/bootkits/loader/launcher/SpringMainBootstrap.java',
          'spring-boot3-brick-bootkit-loader/src/main/java/com/zqzqq/bootkits/loader/launcher/ProdLauncher.java',
          'spring-boot3-brick-bootkit-loader/src/main/java/com/zqzqq/bootkits/loader/launcher/DevelopmentModeSetting.java',
          'spring-boot3-brick-bootkit-loader/src/main/java/com/zqzqq/bootkits/loader/launcher/SpringBootstrap.java'
        ]
      },
      {
        id: 'cluster-and-rollout',
        title: '集群、准入、迁移与发布',
        table: {
          columns: ['配置键', '默认值', '说明'],
          rows: [
            ['plugin.clusterEnabled', 'false', '是否启用跨实例集群锁协同'],
            ['plugin.clusterSharedPath', '空字符串', '集群共享目录'],
            ['plugin.clusterLockTimeoutMs', '30000', '获取集群锁超时（毫秒）'],
            ['plugin.clusterLockProviderBeanName', '空字符串', '自定义集群锁 Bean 名称'],
            ['plugin.admissionMode', 'warn', '准入模式：off / warn / enforce'],
            ['plugin.migrationValidateChecksum', 'true', '是否校验迁移脚本校验和'],
            ['plugin.migrationContinueOnError', 'false', '迁移失败是否继续执行'],
            ['plugin.rolloutMode', 'direct', '发布模式：direct / gray'],
            ['plugin.rolloutAutoStart', 'true', '安装/升级后自动启动'],
            ['plugin.rolloutRollbackOnFailure', 'true', '升级失败时自动回滚']
          ]
        },
        code: {
          language: 'yaml',
          filename: 'application.yml',
          content: String.raw`plugin:
  clusterEnabled: true
  clusterSharedPath: /data/shared/plugins
  clusterLockTimeoutMs: 30000
  admissionMode: warn

  migrationValidateChecksum: true
  migrationContinueOnError: false

  rolloutMode: gray
  rolloutAutoStart: true
  rolloutRollbackOnFailure: true`
        },
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/AutoIntegrationConfiguration.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/core/admission/PluginAdmissionPipeline.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/rollout/PluginRolloutMode.java'
        ]
      },
      {
        id: 'decrypt-keys',
        title: '插件解密配置（plugin.decrypt.*）',
        table: {
          columns: ['配置键', '默认值', '说明'],
          rows: [
            ['plugin.decrypt.enable', 'false（由默认配置覆盖）', '是否启用插件解密校验'],
            ['plugin.decrypt.className', 'AesPluginCipher', '解密实现类名（支持自定义）'],
            ['plugin.decrypt.props', '{}', '解密实现参数'],
            ['plugin.decrypt.plugins', '全部插件生效', '按插件粒度覆盖解密配置'],
            ['plugin.decrypt.plugins.pluginId', '空', '指定生效插件 ID'],
            ['plugin.decrypt.plugins.props', '{}', '该插件专属解密参数（覆盖全局 props）']
          ]
        },
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/decrypt/DecryptConfiguration.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/decrypt/DecryptPluginConfiguration.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/DefaultIntegrationConfiguration.java',
          'spring-boot3-brick-bootkit/src/main/resources/META-INF/spring-configuration-metadata.json'
        ]
      },
      {
        id: 'web-keys',
        title: 'Web 配置（plugin.web.*）',
        table: {
          columns: ['配置键', '默认值', '说明'],
          rows: [
            ['plugin.web.enabled', 'true', '是否启用 Web 模块自动配置'],
            ['plugin.web.enableUI', 'true', '是否启用 UI 页面（也可写 enable-ui）'],
            ['plugin.web.apiPrefix', '/plugins-web/api', 'Web API 前缀'],
            ['plugin.web.pagePrefix', '/plugins-web', 'Web 页面前缀'],
            ['plugin.web.monitorRefreshInterval', '5', '监控刷新间隔（秒）'],
            ['plugin.web.apiTitle', 'Brick BootKit Web API', 'API 文档标题'],
            ['plugin.web.authMode', 'delegate', '鉴权模式：disabled / delegate / strict'],
            ['plugin.web.pluginPaths', '由 IntegrationConfiguration 注入', '插件路径（通常无需手动设置）'],
            ['plugin.web.uploadTempPath', '由 IntegrationConfiguration 注入', '上传临时目录（通常无需手动设置）'],
            ['plugin.web.backupPath', '由 IntegrationConfiguration 注入', '备份目录（通常无需手动设置）'],
            ['plugin.web.pluginRestPathPrefix', '由 IntegrationConfiguration 注入', '插件 REST 前缀（通常无需手动设置）'],
            ['plugin.web.cors.enabled', 'false', '是否开启跨域配置']
          ]
        },
        sources: [
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/config/BrickWebProperties.java',
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/config/BrickWebAutoConfiguration.java',
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/config/BrickWebCorsConfiguration.java'
        ]
      },
      {
        id: 'monitoring-keys',
        title: '监控配置（plugin.monitoring.*）',
        table: {
          columns: ['配置键', '默认值', '说明'],
          rows: [
            ['plugin.monitoring.enabled', 'true', '监控总开关'],
            ['plugin.monitoring.collectionInterval', '30', '采集间隔（秒）'],
            ['plugin.monitoring.memoryMonitoringEnabled', 'true', '内存监控开关'],
            ['plugin.monitoring.cpuMonitoringEnabled', 'true', 'CPU 监控开关'],
            ['plugin.monitoring.threadMonitoringEnabled', 'true', '线程监控开关'],
            ['plugin.monitoring.classLoadingMonitoringEnabled', 'true', '类加载监控开关'],
            ['plugin.monitoring.gcMonitoringEnabled', 'true', 'GC 监控开关'],
            ['plugin.monitoring.memoryWarningThreshold', '80.0', '内存告警阈值（%）'],
            ['plugin.monitoring.memoryCriticalThreshold', '95.0', '内存严重阈值（%）'],
            ['plugin.monitoring.cpuWarningThreshold', '80.0', 'CPU 告警阈值（%）'],
            ['plugin.monitoring.cpuCriticalThreshold', '95.0', 'CPU 严重阈值（%）'],
            ['plugin.monitoring.threadWarningThreshold', '100', '线程数告警阈值'],
            ['plugin.monitoring.threadCriticalThreshold', '200', '线程数严重阈值'],
            ['plugin.monitoring.performanceReportEnabled', 'true', '性能报告开关'],
            ['plugin.monitoring.performanceReportInterval', '60', '性能报告间隔（分钟）'],
            ['plugin.monitoring.slowOperationDetectionEnabled', 'true', '慢操作检测开关'],
            ['plugin.monitoring.slowOperationThreshold', '5000', '慢操作阈值（毫秒）'],
            ['plugin.monitoring.historyRetentionDays', '7', '历史数据保留天数'],
            ['plugin.monitoring.realTimeMonitoringEnabled', 'false', '实时监控开关'],
            ['plugin.monitoring.realTimeUpdateInterval', '5', '实时监控刷新间隔（秒）']
          ]
        },
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/monitoring/PluginMonitoringProperties.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/monitoring/PluginMonitoringAutoConfiguration.java'
        ]
      },
      {
        id: 'scripts-storage-keys',
        title: '脚本存储配置（plugin.scripts.*）',
        table: {
          columns: ['配置键', '默认值', '说明'],
          rows: [
            ['plugin.scripts.enabled', 'true', '脚本存储自动配置开关'],
            ['plugin.scripts.storage.type', 'file', '存储类型：file / jdbc / custom'],
            ['plugin.scripts.storage.customBeanName', '空', 'type=custom 时的 Bean 名称'],
            ['plugin.scripts.storage.file.dataPath', './brick-scripts-data', '文件存储目录'],
            ['plugin.scripts.storage.file.logRetentionDays', '30', '日志保留天数'],
            ['plugin.scripts.storage.file.maxLogSizeMb', '100', '最大日志文件大小（MB）'],
            ['plugin.scripts.storage.file.autoCleanup', 'true', '是否自动清理过期数据'],
            ['plugin.scripts.storage.jdbc.dataPath', './brick-scripts-data', 'JDBC 模式附加文件目录'],
            ['plugin.scripts.storage.jdbc.tablePrefix', 'brick_', 'JDBC 表前缀'],
            ['plugin.scripts.storage.jdbc.autoCreateTables', 'false', '是否自动建表']
          ]
        },
        sources: [
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/scripts/config/ScriptStorageProperties.java',
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/scripts/config/ScriptStorageAutoConfiguration.java'
        ]
      },
      {
        id: 'metadata-note',
        title: '元数据与源码差异说明',
        callout: {
          tone: 'warn',
          title: '注意',
          body: '`spring-configuration-metadata.json` 中存在 `plugin.integration.enabled` 等历史项；当前源码自动配置未发现对应开关。以 `@ConfigurationProperties` 与 `@ConditionalOnProperty` 代码路径为准。'
        },
        sources: [
          'spring-boot3-brick-bootkit/src/main/resources/META-INF/spring-configuration-metadata.json',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/AutoIntegrationConfiguration.java'
        ]
      },
      {
        id: 'auto-generated-config-index',
        title: '自动索引（源码提取）',
        lead: `该索引由脚本自动从配置类和条件开关注解中提取，当前共 ${generatedConfigKeys.keyCount || 0} 项。`,
        table: autoConfigIndexTable,
        sources: [
          'docs-website/src/content/generated-config-keys.json',
          'docs-website/src/content/generated-config-keys.js',
          'docs-website/scripts/config-key-extractor.mjs',
          'docs-website/scripts/generate-config-index.mjs'
        ]
      }
    ]
  },
  {
    id: 'plugins',
    path: '/plugins',
    title: '插件生命周期',
    lead: '生命周期能力由 `PluginManager` 接口定义，对应安装、启动、停止、卸载、校验、重载等动作。',
    sections: [
      {
        id: 'manager-api',
        title: 'PluginManager 关键 API',
        bullets: [
          '`loadPlugins()` 扫描并加载插件描述信息。',
          '`install(Path)` 安装插件包。',
          '`start(pluginId)` / `stop(pluginId)` 启停插件。',
          '`uninstall(pluginId)` 卸载插件。',
          '`verify(Path)` 校验插件包。',
          '`reload(pluginId)` 执行重载。'
        ],
        sources: ['spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/core/PluginManager.java']
      },
      {
        id: 'operator-behavior',
        title: 'DefaultPluginOperator 行为要点',
        paragraphs: [
          '`uploadPlugin` 路径支持上传并安装；如存在旧版本，可结合回滚参数在失败时恢复。',
          '当 `rolloutMode=gray` 时，会调用宿主提供的 `PluginRolloutProbe` 列表进行探针决策。',
          '准入检查在安装/启动阶段触发，`ENFORCE` 模式会抛异常阻断。'
        ],
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/operator/DefaultPluginOperator.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/rollout/PluginRolloutProbe.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/core/admission/PluginAdmissionPipeline.java'
        ]
      }
    ]
  },
  {
    id: 'plugins-packaging',
    path: '/plugins-packaging',
    title: '插件打包',
    lead: '打包能力由 Maven 插件 `spring-boot3-brick-bootkit-maven-packager` 提供。',
    sections: [
      {
        id: 'goals',
        title: '目标（Goals）',
        table: {
          columns: ['Goal', 'Phase', '说明'],
          rows: [
            ['repackage', 'package', '按 mode 执行 dev/prod/main 打包流程'],
            ['prepare-meta', 'process-classes', '仅生成本地运行所需元数据']
          ]
        },
        sources: [
          'spring-boot3-brick-bootkit-maven-packager/src/main/java/com/zqzqq/bootkits/plugin/pack/RepackageMojo.java',
          'spring-boot3-brick-bootkit-maven-packager/src/main/java/com/zqzqq/bootkits/plugin/pack/PrepareMetaMojo.java',
          'spring-boot3-brick-bootkit-maven-packager/src/main/resources/META-INF/maven/plugin.xml'
        ]
      },
      {
        id: 'packager-keys',
        title: '配置项总览（repackage / prepare-meta）',
        table: {
          columns: ['配置项', '默认值', '说明'],
          rows: [
            ['mode', 'dev', '打包模式：dev / prod / main'],
            ['skip', 'false', '是否跳过执行'],
            ['outputDirectory', '${project.build.directory}', '构建输出目录'],
            ['includes', '空', '包含依赖列表（groupId/artifactId）'],
            ['excludes', '空', '排除依赖列表（groupId/artifactId）'],
            ['includeSystemScope', 'true', '是否包含 scope=system 的依赖'],
            ['pluginInfo.id', '必填', '插件唯一 ID'],
            ['pluginInfo.bootstrapClass', '必填', '插件启动类'],
            ['pluginInfo.version', '必填', '插件版本'],
            ['pluginInfo.configFileName', '空', '插件配置文件名'],
            ['pluginInfo.configFileLocation', 'target/classes（默认读取路径）', '插件配置文件目录'],
            ['pluginInfo.args', '空', '插件启动参数'],
            ['pluginInfo.description/provider/requires/license', '空', '插件元信息字段'],
            ['pluginInfo.dependencyPlugins', '空', '插件依赖插件声明'],
            ['loadMainResourcePattern.includes', '空', '从主程序加载资源白名单'],
            ['loadMainResourcePattern.excludes', '空', '从主程序加载资源黑名单'],
            ['devConfig.moduleDependencies', '空', '开发态模块依赖（target/classes）'],
            ['devConfig.localJars', '空', '开发态本地 jar 依赖'],
            ['prodConfig.packageType', 'jar', '生产包类型：jar / jar-outer / zip / zip-outer / dir'],
            ['prodConfig.fileName', 'pluginId-version-repackage', '生产包输出文件名'],
            ['prodConfig.outputDirectory', 'target', '生产包输出目录'],
            ['prodConfig.libDir', '空', '外置依赖目录（outer/dir 类型常用）'],
            ['mainConfig.mainClass', '必填', 'main 模式主启动类'],
            ['mainConfig.packageType', 'jar', '主程序包类型：jar / jar-outer'],
            ['mainConfig.fileName', 'artifactId-version-repackage', '主程序包输出文件名'],
            ['mainConfig.outputDirectory', 'target', '主程序包输出目录'],
            ['mainConfig.libDir', '空', '主程序依赖目录'],
            ['mainConfig.developmentMode', '未配置', '可直接配置为 isolation/coexist/simple；未配置时尝试反射调用主类 `developmentMode()`'],
            ['loadToMain.dependencies', '空', '需要标记为加载到主程序的依赖'],
            ['encryptConfig.rsa / encryptConfig.aes', '空', '打包加密配置']
          ]
        },
        bullets: [
          '`prepare-meta` 复用同一套配置模型，但执行逻辑只生成本地运行元文件（不产出完整发布包）。',
          '如果你只需要避免覆写 `developmentMode()`，优先配置 `mainConfig.developmentMode`。'
        ],
        sources: [
          'spring-boot3-brick-bootkit-maven-packager/src/main/resources/META-INF/maven/plugin.xml',
          'spring-boot3-brick-bootkit-maven-packager/src/main/java/com/zqzqq/bootkits/plugin/pack/AbstractPackagerMojo.java',
          'spring-boot3-brick-bootkit-maven-packager/src/main/java/com/zqzqq/bootkits/plugin/pack/RepackageMojo.java',
          'spring-boot3-brick-bootkit-maven-packager/src/main/java/com/zqzqq/bootkits/plugin/pack/PrepareMetaMojo.java',
          'spring-boot3-brick-bootkit-maven-packager/src/main/java/com/zqzqq/bootkits/plugin/pack/PluginInfo.java',
          'spring-boot3-brick-bootkit-maven-packager/src/main/java/com/zqzqq/bootkits/plugin/pack/LoadMainResourcePattern.java',
          'spring-boot3-brick-bootkit-maven-packager/src/main/java/com/zqzqq/bootkits/plugin/pack/dev/DevConfig.java',
          'spring-boot3-brick-bootkit-maven-packager/src/main/java/com/zqzqq/bootkits/plugin/pack/prod/ProdConfig.java',
          'spring-boot3-brick-bootkit-maven-packager/src/main/java/com/zqzqq/bootkits/plugin/pack/main/MainConfig.java',
          'spring-boot3-brick-bootkit-maven-packager/src/main/java/com/zqzqq/bootkits/plugin/pack/LoadToMain.java',
          'spring-boot3-brick-bootkit-maven-packager/src/main/java/com/zqzqq/bootkits/plugin/pack/encrypt/EncryptConfig.java'
        ]
      },
      {
        id: 'packager-config',
        title: '推荐配置',
        code: {
          language: 'xml',
          filename: 'pom.xml',
          content: String.raw`<plugin>
  <groupId>com.zqzqq</groupId>
  <artifactId>spring-boot3-brick-bootkit-maven-packager</artifactId>
  <version>4.0.6</version>
  <configuration>
    <mode>prod</mode>
    <pluginInfo>
      <id>my-plugin</id>
      <bootstrapClass>com.example.plugin.MyPluginBootstrap</bootstrapClass>
      <version>1.0.0</version>
    </pluginInfo>
  </configuration>
  <executions>
    <execution>
      <goals>
        <goal>prepare-meta</goal>
        <goal>repackage</goal>
      </goals>
    </execution>
  </executions>
</plugin>`
        },
        callout: {
          tone: 'success',
          title: '实践建议',
          body: '本地单插件直接启动场景建议加 `prepare-meta`，避免还没 package 就缺少 `PLUGIN.META`。'
        },
        sources: [
          'spring-boot3-brick-bootkit-maven-packager/src/main/java/com/zqzqq/bootkits/plugin/pack/PrepareMetaMojo.java'
        ]
      }
    ]
  },
  {
    id: 'dynamic-deployment',
    path: '/dynamic-deployment',
    title: '动态部署与控制台 API',
    lead: 'Web 模块提供插件上传、安装、启停、重启、卸载、校验和历史记录管理接口。',
    sections: [
      {
        id: 'plugin-api',
        title: '插件管理接口（节选）',
        code: {
          language: 'text',
          filename: 'PluginController routes',
          content: String.raw`GET    /plugins-web/api/plugins
GET    /plugins-web/api/plugins/all
GET    /plugins-web/api/plugins/{pluginId}
POST   /plugins-web/api/plugins/upload/temp
POST   /plugins-web/api/plugins/install/temp
POST   /plugins-web/api/plugins/upload
POST   /plugins-web/api/plugins/install
POST   /plugins-web/api/plugins/{pluginId}/start
POST   /plugins-web/api/plugins/{pluginId}/stop
POST   /plugins-web/api/plugins/{pluginId}/restart
DELETE /plugins-web/api/plugins/{pluginId}
POST   /plugins-web/api/plugins/verify
GET    /plugins-web/api/plugins/auth/capabilities`
        },
        sources: ['spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/PluginController.java']
      },
      {
        id: 'monitor-api',
        title: '监控接口（节选）',
        code: {
          language: 'text',
          filename: 'MonitorController routes',
          content: String.raw`GET /plugins-web/api/monitor/overview
GET /plugins-web/api/monitor/memory
GET /plugins-web/api/monitor/cpu
GET /plugins-web/api/monitor/threads
GET /plugins-web/api/monitor/threads/detail
GET /plugins-web/api/monitor/system
GET /plugins-web/api/monitor/history
GET /plugins-web/api/monitor/thread-pools`
        },
        sources: ['spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/MonitorController.java']
      },
      {
        id: 'web-prefix',
        title: 'Web 前缀配置',
        code: {
          language: 'yaml',
          filename: 'application.yml',
          content: String.raw`plugin:
  web:
    enabled: true
    enable-ui: true
    api-prefix: /plugins-web/api
    page-prefix: /plugins-web
    monitor-refresh-interval: 5
    auth-mode: delegate`
        },
        sources: ['spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/config/BrickWebProperties.java']
      }
    ]
  },
  {
    id: 'capability-apis',
    path: '/capability-apis',
    title: '增强能力 API（2026-08）',
    lead: '安全中心、服务注册中心、配置热更新、性能分析、集群管理、依赖分析、灰度发布、事件总线等能力已接入 Web 控制台。',
    code: {
      language: 'text',
      filename: 'Capability controllers routes',
      content: String.raw`# 安全中心
GET    /plugins-web/api/security/scan/{pluginId}
GET    /plugins-web/api/security/scan?path=
GET    /plugins-web/api/security/policy/{pluginId}
POST   /plugins-web/api/security/policy
POST   /plugins-web/api/security/permissions/grant
POST   /plugins-web/api/security/permissions/revoke
GET    /plugins-web/api/security/permissions/{pluginId}

# 服务注册中心
GET    /plugins-web/api/registry/statistics
GET    /plugins-web/api/registry/services
GET    /plugins-web/api/registry/services/{pluginId}
GET    /plugins-web/api/registry/plugins

# 配置热更新
GET    /plugins-web/api/configurations
GET    /plugins-web/api/configurations/{pluginId}
PUT    /plugins-web/api/configurations/{pluginId}
GET    /plugins-web/api/configurations/{pluginId}/versions
POST   /plugins-web/api/configurations/{pluginId}/rollback
DELETE /plugins-web/api/configurations/{pluginId}

# 性能分析与资源隔离
GET    /plugins-web/api/performance/analyze/{pluginId}
GET    /plugins-web/api/performance/usage
GET    /plugins-web/api/performance/summary
GET    /plugins-web/api/performance/scores
GET    /plugins-web/api/performance/quota/{pluginId}
POST   /plugins-web/api/performance/quota/{pluginId}

# 集群管理
GET    /plugins-web/api/cluster/overview
GET    /plugins-web/api/cluster/nodes
GET    /plugins-web/api/cluster/nodes/current
GET    /plugins-web/api/cluster/plugins/states
POST   /plugins-web/api/cluster/plugins/sync

# 依赖分析
GET    /plugins-web/api/dependency/graph
GET    /plugins-web/api/dependency/{pluginId}
GET    /plugins-web/api/dependency/{pluginId}/resolve
GET    /plugins-web/api/dependency/{pluginId}/compatibility
GET    /plugins-web/api/dependency/{pluginId}/impact
GET    /plugins-web/api/dependency/matrix

# 灰度发布
GET    /plugins-web/api/rollout/config
GET    /plugins-web/api/rollout/probes
POST   /plugins-web/api/rollout/check/{pluginId}

# 事件总线
GET    /plugins-web/api/eventbus/stats
GET    /plugins-web/api/eventbus/types
GET    /plugins-web/api/eventbus/recent?limit=50`
    },
    sources: [
      'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/SecurityController.java',
      'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/RegistryController.java',
      'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/ConfigurationController.java',
      'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/PerformanceController.java',
      'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/ClusterController.java',
      'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/DependencyController.java',
      'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/RolloutController.java',
      'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/EventBusController.java'
    ]
  },
  {
    id: 'plugin-lifecycle',
    path: '/plugin-lifecycle',
    title: '生命周期扩展',
    lead: '生命周期扩展能力允许宿主以 SPI 或 Spring Bean 的方式插入插件安装/启动/停止/卸载链路。',
    sections: [
      {
        id: 'extension-interface',
        title: '扩展点接口',
        code: {
          language: 'java',
          filename: 'PluginLifecycleExtension.java',
          content: String.raw`public interface PluginLifecycleExtension {
  default void beforeInstall(PluginInsideInfo info) {}
  default void afterInstall(PluginInsideInfo info) {}
  default void beforeStart(PluginInsideInfo info) {}
  default void afterStart(PluginInsideInfo info) {}
  default void beforeStop(PluginInsideInfo info) {}
  default void afterStop(PluginInsideInfo info) {}
  default void beforeUninstall(PluginInsideInfo info) {}
  default void afterUninstall(PluginInsideInfo info) {}
}`
        },
        sources: ['spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/spi/PluginLifecycleExtension.java']
      },
      {
        id: 'extension-loading',
        title: '加载策略',
        bullets: [
          '支持 Java SPI（`META-INF/services/...`）和 Spring Bean 两种来源。',
          '按 `getOrder()` 排序执行；同序按 `getExtensionId()` 排序。',
          '扩展抛错不会中断主流程，管理器内部做了安全调用包装。'
        ],
        sources: ['spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/spi/PluginLifecycleExtensionManager.java']
      }
    ]
  },
  {
    id: 'configuration-management',
    path: '/configuration-management',
    title: '配置管理（扩展能力）',
    lead: '源码中存在独立的 `plugin.configuration.*` 配置管理模型（类注释标注 `since 4.1.0`），用于插件配置版本化与热加载。',
    sections: [
      {
        id: 'config-keys',
        title: '可用字段',
        table: {
          columns: ['键', '默认值', '用途'],
          rows: [
            ['plugin.configuration.enabled', 'true', '配置管理开关'],
            ['plugin.configuration.configDirectory', 'config/plugins', '配置目录'],
            ['plugin.configuration.hotReloadEnabled', 'true', '是否热重载'],
            ['plugin.configuration.hotReloadDelay', '1000', '热重载防抖延迟（毫秒）'],
            ['plugin.configuration.persistenceEnabled', 'true', '是否持久化'],
            ['plugin.configuration.maxVersionsPerPlugin', '10', '每插件最大版本数'],
            ['plugin.configuration.cacheSize', '100', '配置缓存大小'],
            ['plugin.configuration.validationEnabled', 'true', '配置校验'],
            ['plugin.configuration.encryptionEnabled', 'false', '是否启用配置加密'],
            ['plugin.configuration.encryptionKey', '空', '配置加密密钥'],
            ['plugin.configuration.backupEnabled', 'true', '配置备份'],
            ['plugin.configuration.backupDirectory', 'backup/plugins', '备份目录'],
            ['plugin.configuration.backupRetentionDays', '30', '备份保留天数']
          ]
        },
        callout: {
          tone: 'warn',
          title: '注意',
          body: '该能力在源码中已具备配置对象，但你应在目标分支中确认其自动装配链路再用于生产。'
        },
        sources: ['spring-boot3-brick-bootkit-core/src/main/java/com/zqzqq/bootkits/core/config/PluginConfigurationProperties.java']
      }
    ]
  },
  {
    id: 'performance-monitoring',
    path: '/performance-monitoring',
    title: '性能监控',
    lead: '监控包含两部分：Web API 输出 + Core 监控配置模型。',
    sections: [
      {
        id: 'monitor-endpoints',
        title: 'Web 监控 API',
        bullets: [
          '系统概览、内存、CPU、线程、线程池、历史趋势均有独立接口。',
          '默认 API 前缀来自 `plugin.web.api-prefix`，默认 `/plugins-web/api`。'
        ],
        sources: [
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/MonitorController.java',
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/config/BrickWebProperties.java'
        ]
      },
      {
        id: 'monitor-config-model',
        title: '监控配置模型（全字段）',
        paragraphs: [
          '`PluginMonitoringConfiguration` 提供采集周期、阈值、慢操作检测、实时监控与历史保留等字段，并带参数合法性校验。'
        ],
        table: {
          columns: ['配置键', '默认值', '说明'],
          rows: [
            ['plugin.monitoring.enabled', 'true', '监控总开关'],
            ['plugin.monitoring.collectionInterval', '30', '采集间隔（秒）'],
            ['plugin.monitoring.memoryMonitoringEnabled', 'true', '内存监控开关'],
            ['plugin.monitoring.cpuMonitoringEnabled', 'true', 'CPU 监控开关'],
            ['plugin.monitoring.threadMonitoringEnabled', 'true', '线程监控开关'],
            ['plugin.monitoring.classLoadingMonitoringEnabled', 'true', '类加载监控开关'],
            ['plugin.monitoring.gcMonitoringEnabled', 'true', 'GC 监控开关'],
            ['plugin.monitoring.memoryWarningThreshold', '80.0', '内存告警阈值（%）'],
            ['plugin.monitoring.memoryCriticalThreshold', '95.0', '内存严重阈值（%）'],
            ['plugin.monitoring.cpuWarningThreshold', '80.0', 'CPU 告警阈值（%）'],
            ['plugin.monitoring.cpuCriticalThreshold', '95.0', 'CPU 严重阈值（%）'],
            ['plugin.monitoring.threadWarningThreshold', '100', '线程数告警阈值'],
            ['plugin.monitoring.threadCriticalThreshold', '200', '线程数严重阈值'],
            ['plugin.monitoring.performanceReportEnabled', 'true', '性能报告开关'],
            ['plugin.monitoring.performanceReportInterval', '60', '性能报告间隔（分钟）'],
            ['plugin.monitoring.slowOperationDetectionEnabled', 'true', '慢操作检测开关'],
            ['plugin.monitoring.slowOperationThreshold', '5000', '慢操作阈值（毫秒）'],
            ['plugin.monitoring.historyRetentionDays', '7', '历史数据保留天数'],
            ['plugin.monitoring.realTimeMonitoringEnabled', 'false', '实时监控开关'],
            ['plugin.monitoring.realTimeUpdateInterval', '5', '实时监控刷新间隔（秒）']
          ]
        },
        code: {
          language: 'yaml',
          filename: 'monitoring-sample.yml',
          content: String.raw`plugin:
  monitoring:
    enabled: true
    collectionInterval: 30
    memoryWarningThreshold: 80
    memoryCriticalThreshold: 95
    cpuWarningThreshold: 80
    cpuCriticalThreshold: 95
    slowOperationDetectionEnabled: true
    slowOperationThreshold: 5000`
        },
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/monitoring/PluginMonitoringProperties.java',
          'spring-boot3-brick-bootkit-core/src/main/java/com/zqzqq/bootkits/core/monitoring/PluginMonitoringConfiguration.java'
        ]
      }
    ]
  },
  {
    id: 'security',
    path: '/security',
    title: '安全能力',
    lead: '安全链路由插件安全管理、准入管线和 Web 鉴权三部分组成。',
    sections: [
      {
        id: 'plugin-security-manager',
        title: 'PluginSecurityManager',
        bullets: [
          '支持代码扫描、权限校验、策略校验、文件系统与网络访问校验。',
          '支持授权、撤权、监听器和审计日志。',
          '支持按插件粒度维护权限与策略。'
        ],
        sources: ['spring-boot3-brick-bootkit-core/src/main/java/com/zqzqq/bootkits/core/security/PluginSecurityManager.java']
      },
      {
        id: 'admission-security',
        title: '准入管线（Admission Pipeline）',
        paragraphs: [
          '准入模式 `OFF/WARN/ENFORCE` 由 `PluginAdmissionMode` 控制。',
          '`WARN` 记录告警但不中断；`ENFORCE` 在拒绝时抛出 `PluginException`。'
        ],
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/core/admission/PluginAdmissionMode.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/core/admission/PluginAdmissionPipeline.java'
        ]
      },
      {
        id: 'web-auth-mode',
        title: 'Web 鉴权模式',
        table: {
          columns: ['模式', '行为'],
          rows: [
            ['disabled', '关闭插件管理 API 鉴权检查'],
            ['delegate', '委托宿主鉴权；若无宿主实现则回退 allow-all 并告警'],
            ['strict', '必须由宿主提供 `PluginWebAuthorizer`，否则启动失败']
          ]
        },
        sources: [
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/auth/PluginWebAuthMode.java',
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/auth/PluginWebAuthorizationService.java',
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/auth/PluginWebAuthorizer.java'
        ]
      }
    ]
  },
  {
    id: 'api',
    path: '/api',
    title: '核心 API',
    lead: '这里不是“营销式 API 列表”，而是你在二次开发里最常碰到的几个正式入口。',
    sections: [
      {
        id: 'manager-interface',
        title: '插件管理接口',
        code: {
          language: 'java',
          filename: 'PluginManager.java',
          content: String.raw`public interface PluginManager {
  List<PluginInfo> loadPlugins();
  PluginInfo install(Path path);
  void start(String pluginId);
  void stop(String pluginId);
  void uninstall(String pluginId);
  void reload(String pluginId);
  boolean verify(Path jarPath);
  PluginInfo parse(Path pluginPath);
}`
        },
        sources: ['spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/core/PluginManager.java']
      },
      {
        id: 'service-registry',
        title: '插件服务通信',
        bullets: [
          '`PluginServiceRegistry` 是服务注册中心抽象，支持注册、发现、依赖检查。',
          '`DefaultPluginServiceRegistry` 提供并发安全实现和版本范围检查。',
          '`PluginServiceRegistryManager` 负责扫描注解并注册服务。'
        ],
        sources: [
          'spring-boot3-brick-bootkit-core/src/main/java/com/zqzqq/bootkits/core/communication/PluginServiceRegistry.java',
          'spring-boot3-brick-bootkit-core/src/main/java/com/zqzqq/bootkits/core/communication/DefaultPluginServiceRegistry.java',
          'spring-boot3-brick-bootkit-core/src/main/java/com/zqzqq/bootkits/core/communication/PluginServiceRegistryManager.java'
        ]
      }
    ]
  },
  {
    id: 'annotations',
    path: '/annotations',
    title: '注解模型',
    lead: '插件服务通信注解可以直接用于插件间接口声明与依赖约束。',
    sections: [
      {
        id: 'plugin-service-annotation',
        title: '@PluginService',
        bullets: [
          '可指定 `interfaceClass`、`version`、`priority`、`singleton`、`enabled`、`tags`。',
          '可内嵌 `dependencies` 声明服务依赖。'
        ],
        code: {
          language: 'java',
          filename: 'ExampleService.java',
          content: String.raw`@PluginService(
  interfaceClass = UserService.class,
  version = "1.0.0",
  priority = 0,
  dependencies = {
    @ServiceDependency(interfaceClass = AuditService.class, versionRange = "[1.0,2.0)")
  }
)
public class UserServiceImpl implements UserService {
}`
        },
        sources: ['spring-boot3-brick-bootkit-core/src/main/java/com/zqzqq/bootkits/core/communication/annotation/PluginService.java']
      },
      {
        id: 'service-dependency-annotation',
        title: '@ServiceDependency',
        paragraphs: [
          '支持 `versionRange` 和 `optional`。当 optional=false 且依赖不满足时，插件可被阻止启动。'
        ],
        sources: ['spring-boot3-brick-bootkit-core/src/main/java/com/zqzqq/bootkits/core/communication/annotation/ServiceDependency.java']
      }
    ]
  },
  {
    id: 'config-parameters',
    path: '/config-parameters',
    title: '参数速查表',
    lead: '快速查常用字段，不需要每次翻源码。',
    sections: [
      {
        id: 'runtime-parameters',
        title: '运行与发布参数',
        table: {
          columns: ['参数', '默认值', '建议'],
          rows: [
            ['plugin.runMode', 'dev', '生产必须改成 prod'],
            ['plugin.clusterEnabled', 'false', '多实例部署建议开启'],
            ['plugin.clusterSharedPath', '空', '集群模式必须配置共享存储'],
            ['plugin.admissionMode', 'warn', '高风险场景用 enforce'],
            ['plugin.rolloutMode', 'direct', '逐步放量场景改 gray'],
            ['plugin.rolloutRollbackOnFailure', 'true', '建议保持 true']
          ]
        },
        sources: ['spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/AutoIntegrationConfiguration.java']
      },
      {
        id: 'development-mode-parameters',
        title: '开发模式参数（启动器）',
        table: {
          columns: ['参数', '默认值', '建议'],
          rows: [
            ['Main-Development-Mode（Manifest）', '未配置', 'prod 包启动可直接在 Manifest 固定模式'],
            ['plugin.developmentMode', '未配置', '优先使用此键，取值 isolation/coexist'],
            ['spring-boot3-brick-bootkit.developmentMode', '未配置', '兼容历史项目时使用'],
            ['developmentMode', '未配置', '仅用于兼容旧键；避免与其他框架同名冲突'],
            ['PLUGIN_DEVELOPMENT_MODE', '未配置', '容器环境可用；优先级低于系统属性'],
            ['SpringBootstrap#developmentMode()', 'isolation', '需要强制代码优先时再覆写']
          ]
        },
        sources: [
          'spring-boot3-brick-bootkit-loader/src/main/java/com/zqzqq/bootkits/loader/launcher/SpringMainBootstrap.java',
          'spring-boot3-brick-bootkit-loader/src/main/java/com/zqzqq/bootkits/loader/launcher/ProdLauncher.java',
          'spring-boot3-brick-bootkit-loader/src/main/java/com/zqzqq/bootkits/loader/launcher/DevelopmentModeSetting.java',
          'spring-boot3-brick-bootkit-loader/src/main/java/com/zqzqq/bootkits/loader/launcher/SpringBootstrap.java'
        ]
      },
      {
        id: 'web-parameters',
        title: 'Web 参数',
        table: {
          columns: ['参数', '默认值', '说明'],
          rows: [
            ['plugin.web.enabled', 'true', '是否启用 Web 模块'],
            ['plugin.web.enable-ui', 'true', '是否暴露前端页面'],
            ['plugin.web.api-prefix', '/plugins-web/api', 'API 前缀'],
            ['plugin.web.page-prefix', '/plugins-web', '页面前缀'],
            ['plugin.web.monitor-refresh-interval', '5', '监控刷新间隔（秒）'],
            ['plugin.web.auth-mode', 'delegate', '鉴权模式']
          ]
        },
        sources: ['spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/config/BrickWebProperties.java']
      }
    ]
  },
  {
    id: 'examples',
    path: '/examples',
    title: '实战示例',
    lead: '以下示例用于快速验证链路是否打通。',
    sections: [
      {
        id: 'standalone-plugin',
        title: '单插件独立启动',
        code: {
          language: 'java',
          filename: 'DemoPluginBootstrap.java',
          content: String.raw`import com.zqzqq.bootkits.bootstrap.SpringPluginBootstrap;
import com.zqzqq.bootkits.bootstrap.annotation.OneselfConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OneselfConfig(developmentMode = "coexist", mainConfigFileName = {"application.yml"})
public class DemoPluginBootstrap extends SpringPluginBootstrap {
  public static void main(String[] args) {
    new DemoPluginBootstrap().run(args);
  }
}`
        },
        sources: [
          'spring-boot3-brick-bootkit-bootstrap/src/main/java/com/zqzqq/bootkits/bootstrap/SpringPluginBootstrap.java',
          'spring-boot3-brick-bootkit-bootstrap/src/main/java/com/zqzqq/bootkits/bootstrap/annotation/OneselfConfig.java'
        ]
      },
      {
        id: 'script-module',
        title: '脚本执行能力',
        bullets: [
          '脚本类型枚举覆盖 Shell/Batch/PowerShell/Lua/Python/Ruby/Perl/JavaScript/NodeJS/Groovy/Executable。',
          '脚本执行配置支持超时、工作目录、环境变量、输出大小、编码和调试模式。'
        ],
        sources: [
          'spring-boot3-brick-bootkit-scripts/src/main/java/com/zqzqq/bootkits/scripts/core/ScriptType.java',
          'spring-boot3-brick-bootkit-scripts/src/main/java/com/zqzqq/bootkits/scripts/core/ScriptConfiguration.java'
        ]
      }
    ]
  },
  {
    id: 'enterprise-users',
    path: '/enterprise-users',
    title: '企业落地建议',
    lead: '本页不展示“未经验证的企业名单”，只给可执行的落地模式。',
    sections: [
      {
        id: 'scenario-a',
        title: '多租户定制化',
        bullets: [
          '通过插件隔离租户差异化能力，核心主程序保持稳定。',
          '使用 `enablePluginIds/disablePluginIds` 做租户启用策略。',
          '发布时结合 `rolloutMode=gray` + 探针实现灰度校验。'
        ],
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/AutoIntegrationConfiguration.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/rollout/PluginRolloutProbe.java'
        ]
      },
      {
        id: 'scenario-b',
        title: '多实例部署',
        bullets: [
          '开启 `clusterEnabled` 并配置共享路径，避免多实例并发安装冲突。',
          '必要时通过 `clusterLockProviderBeanName` 接入自定义分布式锁实现。'
        ],
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/IntegrationConfiguration.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/operator/DefaultPluginOperator.java'
        ]
      }
    ]
  },
  {
    id: 'troubleshooting',
    path: '/troubleshooting',
    title: '排障与自检',
    lead: '小白接入最容易卡在“配置没生效、目录不对、鉴权没配”。这页只讲最常见的坑和最快的排查顺序。',
    badges: ['Doctor', 'Troubleshooting', 'Beginner First'],
    sections: [
      {
        id: 'doctor-first',
        title: '第一步：先跑 doctor',
        paragraphs: [
          '接入后第一件事，不是盯着堆栈看，而是先调 `/plugins-web/api/doctor`。它会把主包推断、插件目录、上传临时目录和当前插件发现情况一次性告诉你。',
          '启动完成后，框架也会自动打印一条 `Plugin doctor summary` 日志，适合在宿主应用日志里快速定位问题。'
        ],
        code: {
          language: 'text',
          filename: 'doctor.txt',
          content: String.raw`GET /plugins-web/api/doctor`
        },
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/doctor/PluginDoctorService.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/doctor/PluginDoctorStartupReporter.java',
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/DoctorController.java'
        ]
      },
      {
        id: 'common-errors',
        title: '常见问题与建议',
        table: {
          columns: ['问题信号', '通常原因', '建议动作'],
          rows: [
            ['`plugin.mainPackage` 相关报错', '主应用包未推断成功或启动结构非标准', '保持标准 `@SpringBootApplication` 启动结构，或显式配置 `plugin.mainPackage`'],
            ['未发现任何插件', '插件目录不存在、路径错误、目录为空', '先看 doctor 报告中的插件目录检查结果'],
            ['Web strict/delegate 鉴权失败', '宿主没有提供 `PluginWebAuthorizer`', '生产环境注入自定义 `PluginWebAuthorizer`，不要依赖 fallback'],
            ['上传/安装时报路径不合法', '访问了上传目录和插件根目录之外的路径', '通过上传接口生成临时文件，再安装，不要手填任意系统路径'],
            ['启动异常看不懂', '底层异常信息太偏实现细节', '先看 Web 错误返回和启动期的友好提示，再看原始堆栈']
          ]
        },
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/doctor/PluginFriendlyMessageResolver.java',
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/exception/GlobalExceptionHandler.java',
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/service/PluginWebFileSupport.java'
        ]
      },
      {
        id: 'error-codes',
        title: '常见错误码',
        table: {
          columns: ['错误码', '含义', '建议'],
          rows: [
            ['1012', '主应用包名未配置', '优先保持标准 `@SpringBootApplication` 启动结构，自动推断失败时再显式配置 `plugin.mainPackage`'],
            ['1013', '插件目录配置无效', '检查 `plugin.pluginPath` 是否为空、是否误指向文件'],
            ['1014', '插件目录不存在', '先创建目录，再把插件包放进去'],
            ['1017', '插件上传临时目录不可写', '检查 `plugin.uploadTempPath` 和目录权限'],
            ['1018', '未配置插件 Web 鉴权器', '生产环境注入 `PluginWebAuthorizer`，不要依赖 fallback'],
            ['1020', '当前未发现任何插件', '先看 doctor 报告中的插件目录检查结果']
          ]
        },
        sources: [
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/dto/ErrorCode.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/doctor/PluginIssueDefinition.java'
        ]
      },
      {
        id: 'monitor-overview',
        title: '监控首页也能看到 doctor 摘要',
        paragraphs: [
          '监控概览接口现在会附带 doctor summary，前端首页可以直接显示“当前环境 OK / WARN / ERROR”，不用再跳两次页面。'
        ],
        sources: [
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/dto/MonitorOverviewDTO.java',
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/service/MonitorWebService.java'
        ]
      }
    ]
  },
  {
    id: 'changelog',
    path: '/changelog',
    title: '版本变化（源码可验证）',
    lead: '这里只记录在仓库中有明确代码落点的变化。',
    sections: [
      {
        id: 'v406-highlights',
        title: '4.0.6 可见变更',
        bullets: [
          '标准 Spring Boot 宿主会优先自动推断 `plugin.mainPackage`。',
          '新增 doctor 自检与文本 / JSON 导出能力。',
          'Web 首屏增加 doctor 摘要、首次接入 checklist 和统一错误面板。',
          '仓库新增最小模板与故障示例模板。',
          '错误码、排障文档锚点与 Web 返回结构打通。'
        ],
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/ExtendPointConfiguration.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/doctor/PluginDoctorService.java',
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/controller/api/DoctorController.java',
          'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/exception/GlobalExceptionHandler.java',
          'templates/README.md'
        ]
      }
    ]
  },
  {
    id: 'faq',
    path: '/faq',
    title: 'FAQ',
    lead: '针对项目接入时最常见的落地问题。',
    sections: [
      {
        id: 'faq-main-package',
        title: 'Q1: 插件不加载，第一步查什么？',
        paragraphs: [
          '先调 `/plugins-web/api/doctor` 看主包推断和插件目录检查结果，再回头看启动日志。',
          '其次检查 `runMode` 与插件包形态是否匹配（dev/prod）。'
        ],
        sources: [
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/doctor/PluginDoctorService.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/DefaultIntegrationConfiguration.java',
          'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/IntegrationConfiguration.java'
        ]
      },
      {
        id: 'faq-auth',
        title: 'Q2: 为什么 strict 模式启动失败？',
        paragraphs: [
          '`plugin.web.auth.mode=strict` 要求宿主必须提供 `PluginWebAuthorizer` Bean。',
          '如果只用了默认回退 authorizer，授权服务会直接抛异常阻止启动。'
        ],
        sources: ['spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/auth/PluginWebAuthorizationService.java']
      },
      {
        id: 'faq-gray',
        title: 'Q3: 灰度发布如何生效？',
        paragraphs: [
          '设置 `plugin.rolloutMode=gray` 后，升级流程会执行宿主注入的 `PluginRolloutProbe`。',
          '任一探针返回 reject，会触发失败并走回滚逻辑（取决于 `plugin.rolloutRollbackOnFailure`）。'
        ],
        sources: ['spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/operator/DefaultPluginOperator.java']
      }
    ]
  },
  {
    id: 'contact',
    path: '/contact',
    title: '项目地址与反馈',
    lead: '优先通过仓库 Issue / PR 协作，保证讨论可追溯。',
    sections: [
      {
        id: 'links',
        title: '官方链接',
        bullets: [`仓库: ${siteMeta.repo}`, `文档站点: ${siteMeta.docs}`]
      },
      {
        id: 'feedback',
        title: '建议反馈内容',
        bullets: [
          '当前版本号与运行环境（JDK、Spring Boot）。',
          '最小复现工程或关键日志片段。',
          '期望行为与实际行为对比。'
        ]
      }
    ]
  }
];

export const pageByPath = Object.fromEntries(docPages.map((page) => [page.path, page]));
export const pageById = Object.fromEntries(docPages.map((page) => [page.id, page]));

export const sidebarGroups = [
  {
    title: '开始',
    items: [
      { label: '首页', path: '/' },
      { label: '框架定位', path: '/introduction' },
      { label: '快速开始', path: '/quickstart' },
      { label: '项目结构', path: '/project-structure' },
      { label: '最小模板', path: '/templates' },
      { label: '支持矩阵', path: '/support-matrix' }
    ]
  },
  {
    title: '核心能力',
    items: [
      { label: '配置说明', path: '/configuration' },
      { label: '插件生命周期', path: '/plugins' },
      { label: '生命周期扩展', path: '/plugin-lifecycle' },
      { label: '配置管理扩展', path: '/configuration-management' },
      { label: '插件打包', path: '/plugins-packaging' },
      { label: '动态部署', path: '/dynamic-deployment' },
      { label: '安全能力', path: '/security' },
      { label: '性能监控', path: '/performance-monitoring' }
    ]
  },
  {
    title: '开发参考',
    items: [
      { label: '核心 API', path: '/api' },
      { label: '注解模型', path: '/annotations' },
      { label: '参数速查', path: '/config-parameters' },
      { label: '实战示例', path: '/examples' },
      { label: '排障与自检', path: '/troubleshooting' }
    ]
  },
  {
    title: '运营与协作',
    items: [
      { label: '企业落地建议', path: '/enterprise-users' },
      { label: '版本变化', path: '/changelog' },
      { label: '路线图', path: '/roadmap' },
      { label: '发布与验收', path: '/release-checklist' },
      { label: '参与贡献', path: '/contributing' },
      { label: 'FAQ', path: '/faq' },
      { label: '联系与反馈', path: '/contact' }
    ]
  }
];

export const topNav = [
  { label: '快速开始', path: '/quickstart' },
  { label: '配置', path: '/configuration' },
  { label: 'API', path: '/api' },
  { label: '示例', path: '/examples' },
  { label: 'FAQ', path: '/faq' }
];

export const routeAliases = {
  '/home': '/',
  '/index': '/',
  '/guide': '/quickstart',
  '/lifecycle-extension': '/plugin-lifecycle',
  '/monitoring': '/performance-monitoring',
  '/config-management': '/configuration-management'
};

export const orderedDocPages = (() => {
  const orderPaths = sidebarGroups
    .flatMap((group) => group.items.map((item) => item.path))
    .filter((path, index, arr) => arr.indexOf(path) === index);

  const pageMap = new Map(docPages.map((page) => [page.path, page]));
  const ordered = [];

  orderPaths.forEach((path) => {
    if (pageMap.has(path)) {
      ordered.push(pageMap.get(path));
      pageMap.delete(path);
    }
  });

  for (const page of pageMap.values()) {
    ordered.push(page);
  }

  return ordered;
})();

export function getAdjacentPages(path) {
  const index = orderedDocPages.findIndex((page) => page.path === path);
  if (index < 0) {
    return { previous: null, next: null };
  }

  return {
    previous: index > 0 ? orderedDocPages[index - 1] : null,
    next: index < orderedDocPages.length - 1 ? orderedDocPages[index + 1] : null
  };
}

function compactText(value) {
  return String(value || '').replace(/\s+/g, ' ').trim();
}

export function searchDocs(keyword) {
  const q = compactText(keyword).toLowerCase();
  if (!q) {
    return [];
  }

  const results = [];

  docPages.forEach((page) => {
    const baseText = [page.title, page.lead, ...(page.badges || [])].join(' ').toLowerCase();
    if (baseText.includes(q)) {
      results.push({
        key: `${page.path}-page`,
        type: 'page',
        path: page.path,
        title: page.title,
        snippet: compactText(page.lead)
      });
    }

    (page.sections || []).forEach((section) => {
      const sectionText = [
        section.title,
        section.lead,
        ...(section.paragraphs || []),
        ...(section.bullets || [])
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      if (sectionText.includes(q)) {
        const snippetSource = section.paragraphs?.[0] || section.bullets?.[0] || section.lead || '';
        results.push({
          key: `${page.path}-${section.id}`,
          type: 'section',
          path: page.path,
          hash: section.id,
          title: `${page.title} / ${section.title}`,
          snippet: compactText(snippetSource)
        });
      }
    });
  });

  return results.slice(0, 18);
}

export function sourceToUrl(source) {
  if (!source || typeof source !== 'string') {
    return '#';
  }

  const [rawPath, lineText] = source.split(':');
  const relPath = rawPath.trim().replace(/^\/+/, '');
  const encodedPath = relPath
    .split('/')
    .map((part) => encodeURIComponent(part))
    .join('/');
  const line = Number.parseInt(lineText, 10);
  const lineHash = Number.isInteger(line) && line > 0 ? `#L${line}` : '';
  return `${siteMeta.sourceBase}/${encodedPath}${lineHash}`;
}

export function getPageByPath(path) {
  return pageByPath[path] ?? null;
}

export function getPageById(id) {
  return pageById[id] ?? null;
}
