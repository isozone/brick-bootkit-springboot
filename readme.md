<div align="center">

# 🚀 Spring-Boot 插件式开发框架

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-green.svg)](https://spring.io/projects/spring-boot)
[![JDK](https://img.shields.io/badge/JDK-17+-orange.svg)](https://openjdk.org/)
[![Version](https://img.shields.io/badge/version-4.0.4-brightgreen.svg)](https://github.com/v18268185209/brick-bootkit-springboot)
[![Java](https://img.shields.io/badge/Java-17-yellow.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-red.svg)](https://maven.apache.org/)

一个强大的 Spring Boot 插件化开发框架，支持插件动态加载、热插拔、类隔离等功能

[✨ 文档](https://brick-bootkit.zqzqq.com/) · [📦 示例](https://github.com/v18268185209/brick-bootkit-springboot-demo.git) · [📝 更新日志](#更新) · [💬 联系我们](#联系我们)

</div>

---

---

## 📖 目录

- [背景](#背景)
- [主要优化](#本项目二次改造主要优化的包含如下几点)
- [介绍](#介绍)
- [特性](#特性--features)
- [业务场景](#业务场景)
- [技术特性](#-技术特性)
- [开发特性](#-开发特性)
- [架构设计](#-架构设计)
- [功能模块](#-功能模块)
- [环境要求](#-环境要求)
- [快速开始](#-快速开始)
- [配置说明](#-配置说明)
- [如何引入](#-如何引入)
- [常见问题](#-常见问题-faq)
- [打包](#打包)
- [更新](#更新)
- [联系我们](#联系我们)

---

## 背景

原作者仓库地址：[https://gitee.com/starblues/springboot-plugin-framework-parent.git](https://gitee.com/starblues/springboot-plugin-framework-parent.git)

在项目开发中，使用到了原作者的框架，但升级到 Spring Boot 3.5 之后，无法使用，原作者也已经多年未更新，因此针对此项目进行二次修改后进行开源，也希望有技术能力的一起把该插件继续维护下去。

具体使用方式原作者使用方式仍然可用。【参考原作者文档 https://www.yuque.com/starblues/spring-brick-3.0.0】
在项目开发中，使用到了原作者的框架，但升级到springboot3.5之后，无法使用，原作者也已经多年未更新，因此针对此项目进行二次修改后进行开源，
也希望有技术能力的一起把该插件继续维护下去。
具体使用方式原作者使用方式仍然可用。【参考原作者文档 https://www.yuque.com/starblues/spring-brick-3.0.0】

### 本项目二次改造主要优化的包含如下几点
+ 支持springboot3.5和jdk17
+ springboot2版本仍然支持
+ 修复内存泄漏问题
+ 优化部分代码结构，日志结构
+ 调整代码包结构
+ 添加指标监控
+ 更多优化自己品味

### 💡 介绍

该框架可以在 Spring Boot 项目上开发出插件功能，在插件中可以和 Spring Boot 使用方式一模一样。使用了本框架，您可以实现以下需求：

- 🎯 **简单易用**：在插件中，您可以当成一个微型的 Spring Boot 项目来开发，简单易用
- 🔧 **灵活扩展**：在插件中扩展出系统各种功能点，用于系统灵活扩展，再也不用使用分支来交付不同需求的项目了
- 📦 **丰富集成**：在插件中可以集成各种框架及其各种 Spring Boot Starter
- 🎁 **独立依赖**：在插件中可以定义独立依赖包，再也不用在主程序中定义依赖包了
- 🛡️ **版本隔离**：完美解决插件包与插件包、插件包与主程序因为同一框架的不同版本冲突问题。各个插件可以定义同一依赖的不同版本框架
- 🔄 **动态部署**：无需重启主程序，可以自由实现插件包的动态安装部署，来动态扩展系统的功能
- 🚀 **独立服务**：插件也可以不依赖主程序独立集成微服务模块
- 🌐 **Web 管理控制台**：内置可视化插件管理和系统监控界面，支持插件上传、启动、停止、重启、卸载等操作
- 💭 **无限可能**：您可以丰富想象该框架给您带来哪些迫切的需求和扩展，以实现系统的低耦合、高内聚、可扩展的优点

### 特性 | Features

+ 简化了框架的集成步骤，更容易上手。

+ 插件开发更加贴近spring-boot原生开发。

+ 支持两种模式开发: 隔离模式、共享模式, 可自主根据需要灵活选择使用。

+ 使用maven打包插件，支持对插件的自主打包编译。目前支持: 开发打包：将插件打包成开发环境下的插件(仅需打包一次)。

+ 生产打包：将插件打包成一个jar、zip、文件夹等。

+ 自主的开发的类加载器，支持插件定义各种的依赖jar包。

+ 在插件中可以集成各种框架及其各种spring-boot-xxx-starter，比如集成mybatis、mybatis-plus、spring-jpa等。

+ 动态安装、卸载、启动、停止插件。

+ 主程序和插件类隔离, 有效避免主程序与插件、插件与插件之间的类冲突。


### 业务场景
- **To-B 系统定制化**：不同客户需求差异化，通过插件实现个性化功能扩展
- **To-C 系统功能扩展**：动态增加新功能模块，无需重启主应用
- **微服务架构演进**：从单体应用平滑过渡到插件化架构
- **依赖版本冲突**：不同插件使用不同版本依赖，完全隔离
- **团队协作开发**：不同团队独立开发插件，降低耦合度

### 🔧 技术特性
- **🏗️ 多模块架构**：8个核心模块，职责清晰，易于维护
- **🌐 Web 管理控制台**：内置可视化插件管理和系统监控界面
- **🔒 类加载隔离**：自定义类加载器，完全隔离插件依赖
- **🔄 热插拔支持**：运行时动态安装、卸载、启动、停止插件
- **🛡️ 安全管控**：完整的权限控制和代码安全扫描机制
- **📊 性能监控**：集成 Micrometer，实时监控插件性能
- **⚙️ 配置管理**：支持热更新和版本控制的配置系统
- **🧪 测试体系**：完整的单元测试、集成测试框架

### 🚀 开发特性
- **📦 Maven 集成**：原生支持 Maven 打包和依赖管理
- **🔌 两种模式**：隔离模式和共享模式，灵活选择
- **🎯 Spring 原生**：插件开发体验与 Spring Boot 完全一致
- **📝 丰富注解**：提供专用注解简化插件开发
- **🔍 智能扫描**：自动发现和注册插件组件

## 🏛️ 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    主应用程序 (Main Application)              │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │   插件 A     │  │   插件 B     │  │   插件 C     │         │
│  │ (隔离模式)    │  │ (共享模式)    │  │ (隔离模式)    │        │
│  └─────────────┘  └─────────────┘  └─────────────┘          │
├─────────────────────────────────────────────────────────────┤
│                 Web 管理控制台 (可选)                         │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  插件管理  |  系统监控  |  API 文档  |  配置管理        │    │
│  └─────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│                    插件管理层 (Plugin Management)             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │  生命周期    │  │   安全管理    │  │   性能监控   │          │
│  │   管理      │  │              │  │             │          │
│  └─────────────┘  └─────────────┘  └─────────────┘          │
├─────────────────────────────────────────────────────────────┤
│                    核心框架层 (Core Framework)                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │  类加载器     │  │   配置管理    │  │   异常处理    │        │
│  │   隔离       │  │             │  │             │          │
│  └─────────────┘  └─────────────┘  └─────────────┘          │
├─────────────────────────────────────────────────────────────┤
│                 Spring Boot 3.x + Java 17                   │
└─────────────────────────────────────────────────────────────┘
```


## 📦 功能模块

### 1. spring-boot3-brick-bootkit-core (核心模块)
**职责**：提供插件框架的核心功能和基础设施

#### 🔧 配置管理 (config)
- **PluginConfigurationManager**：配置管理器，支持热更新
- **PluginConfigurationPersister**：配置持久化，支持版本控制
- **PluginConfigurationLoader**：配置加载器，支持多种格式
- **PluginConfigurationChangeEvent**：配置变更事件机制

#### 🚨 异常处理 (exception)
- **EnhancedPluginException**：增强的插件异常基类
- **PluginErrorCode**：统一错误码定义（142个错误类型）
- **ExceptionHandlerUtils**：异常处理工具类
- **PluginExceptionFactory**：异常工厂模式

#### 🔄 生命周期管理 (lifecycle)
- **PluginLifecycleManager**：插件生命周期管理器
- **PluginLifecycleState**：生命周期状态枚举
- **PluginLifecycleListener**：生命周期监听器
- **PluginLifecycleEvent**：生命周期事件

#### 📊 性能监控 (monitoring)
- **PluginPerformanceMonitor**：性能监控器
- **PluginMetrics**：指标收集器，集成 Micrometer
- **PluginMonitoringManager**：监控管理器
- **PluginLifecycleMonitoringListener**：生命周期监控

#### 🛡️ 安全机制 (security)
- **PluginSecurityManager**：安全管理器
- **PluginCodeScanner**：代码安全扫描器
- **PluginPermissionType**：权限类型枚举（17种权限）
- **SecurityViolationType**：安全违规类型枚举（17种违规）
- **PluginSecurityAuditLogger**：安全审计日志

### 2. spring-boot3-brick-bootkit-common (通用模块)
**职责**：提供框架通用的基础工具类和常量定义

#### 🔧 核心功能
- **通用工具类**：字符串处理、文件操作、日期时间等工具方法
- **常量定义**：框架级别的常量统一管理
- **异常定义**：基础异常类和错误码
- **配置模型**：插件配置的基础数据结构

### 3. spring-boot3-brick-bootkit-loader (加载器模块)
**职责**：实现自定义类加载器，支持插件类的隔离加载

#### 🔧 核心功能
- **PluginClassLoader**：自定义插件类加载器
- **类隔离机制**：主程序与插件、插件与插件之间的类隔离
- **依赖管理**：支持插件定义独立的依赖 jar 包
- **资源加载**：插件资源的统一加载和管理

### 4. spring-boot3-brick-bootkit-bootstrap (启动引导模块)
**职责**：提供插件框架的自动配置和启动引导

#### 🔧 核心功能
- **自动配置**：基于 Spring Boot 的自动配置机制
- **AOP 支持**：提供插件级别的 AOP 拦截功能
- **启动流程管理**：框架启动和插件加载的流程控制
- **上下文集成**：与 Spring Boot 上下文的深度集成

### 5. spring-boot3-brick-bootkit-scripts (脚本管理模块)
**职责**：提供跨平台脚本执行和管理功能

#### 🔧 核心功能
- **脚本引擎**：支持 Shell、Python、Lua、Bat 等多种脚本类型
- **脚本管理**：脚本的加载、缓存和生命周期管理
- **安全执行**：沙箱环境下的脚本安全执行
- **结果处理**：脚本执行结果的统一处理和返回

### 6. spring-boot3-brick-bootkit-maven-packager (Maven 打包插件)
**职责**：提供 Maven 插件，支持插件的打包和构建

#### 🔧 核心功能
- **开发打包**：将插件打包成开发环境下的插件（仅需打包一次）
- **生产打包**：将插件打包成 jar、zip、文件夹等多种格式
- **依赖处理**：自动处理插件依赖和类路径
- **配置注入**：支持插件配置的自动注入

#### 📖 使用方式
在插件项目的 pom.xml 中添加：
```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.zqzqq</groupId>
            <artifactId>spring-boot3-brick-bootkit-maven-packager</artifactId>
            <version>4.0.4</version>
            <configuration>
                <mode>plugin</mode>
                <pluginConfig>
                    <pluginId>your-plugin-id</pluginId>
                    <pluginVersion>1.0.0</pluginVersion>
                </pluginConfig>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>repackage</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 7. spring-boot3-brick-bootkit (主模块)
**职责**：框架的核心入口，整合所有子模块提供完整的插件功能

#### 🔧 核心功能
- **插件注册**：插件的注册和管理
- **生命周期控制**：插件的生命周期管理
- **事件发布**：插件相关事件的发布和订阅
- **API 暴露**：对外提供统一的插件管理 API

### 8. spring-boot3-brick-bootkit-web (Web 管理控制台)
**职责**：提供可视化的插件管理和系统监控 Web 界面

#### 🎯 功能特性
- **插件管理**：可视化插件列表、上传、安装、启动、停止、重启、卸载
- **系统监控**：实时监控 JVM 内存、CPU 使用率、线程状态
- **API 文档**：集成 Knife4j 自动生成 REST API 文档
- **响应式设计**：支持 PC 和移动端访问

#### 📊 监控指标
- **内存监控**：堆内存、非堆内存使用情况及趋势图
- **CPU 监控**：系统负载、进程 CPU 使用率
- **线程监控**：当前线程数、守护线程、峰值线程
- **插件统计**：插件总数、运行中、已停止、异常状态

#### 🔧 技术栈
- **前端框架**：Thymeleaf + Vue 3 + Bootstrap 5
- **图表库**：ECharts 实现数据可视化
- **API 文档**：Knife4j (OpenAPI 3)
- **监控指标**：Micrometer 集成

#### 📖 使用方式
1. 在主应用中引入依赖：
```xml
<dependency>
    <groupId>com.zqzqq</groupId>
    <artifactId>spring-boot3-brick-bootkit-web</artifactId>
    <version>4.0.4</version>
</dependency>
```

2. 在启动类上添加注解：
```java
@SpringBootApplication
@EnableBrickWeb
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

3. 配置参数（可选）：
```yaml
plugin:
  web:
    enabled: true
    enable-ui: true
    api-prefix: /brick-web/api
    page-prefix: /brick-web
    monitor-refresh-interval: 5
```

4. 访问管理控制台：`http://localhost:8080/brick-web/index`



## ⚙️ 环境要求

- **JDK**: 17+
- **Spring Boot**: 3.5.5+
- **Maven**: 3.6+

## 🚀 快速开始

### 1. 创建主应用项目

#### 1.1 添加依赖

在主应用的 `pom.xml` 中添加：

```xml
<dependencies>
    <!-- 插件框架核心依赖 -->
    <dependency>
        <groupId>com.zqzqq</groupId>
        <artifactId>spring-boot3-brick-bootkit</artifactId>
        <version>4.0.4</version>
    </dependency>
    
    <!-- Web 管理控制台（可选） -->
    <dependency>
        <groupId>com.zqzqq</groupId>
        <artifactId>spring-boot3-brick-bootkit-web</artifactId>
        <version>4.0.4</version>
    </dependency>
</dependencies>
```

#### 1.2 配置启动类

在启动类上添加 `@ComponentScan` 注解，扫描插件框架包：

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.zqzqq.bootkits.*", "你的应用包路径"})
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}
```

#### 1.3 配置插件目录

在 `application.yml` 中配置插件目录：

```yaml
plugin:
  enable: true
  runMode: dev
  plugin-follow-log: true
  mainPackage: com.your.package.YourApplication
  pluginPath:
    - ./plugins
    # 可以配置多个插件目录
    # - /path/to/second/plugins
```

### 2. 创建插件项目

#### 2.1 添加依赖

在插件的 `pom.xml` 中添加：

```xml
<dependencies>
    <dependency>
        <groupId>com.zqzqq</groupId>
        <artifactId>spring-boot3-brick-bootkit-maven-packager</artifactId>
        <version>4.0.4</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### 2.2 配置 Maven 打包插件

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.zqzqq</groupId>
            <artifactId>spring-boot3-brick-bootkit-maven-packager</artifactId>
            <version>4.0.4</version>
            <configuration>
                <mode>plugin</mode>
                <pluginConfig>
                    <pluginId>my-plugin</pluginId>
                    <pluginVersion>1.0.0</pluginVersion>
                    <pluginClass>com.example.MyPluginConfig</pluginClass>
                </pluginConfig>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>repackage</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

#### 2.3 创建插件配置类

```java
@Configuration
@ComponentScan("com.example.plugin")
public class MyPluginConfig {
    // 插件配置
}
```

#### 2.4 编写插件业务代码

```java
@RestController
@RequestMapping("/plugin")
public class PluginController {
    
    @GetMapping("/hello")
    public String hello() {
        return "Hello from Plugin!";
    }
}
```

#### 2.5 打包插件

```bash
mvn clean package
```

打包完成后，将生成的 jar 文件复制到主应用的 `plugins` 目录下。

### 3. 启动主应用

```bash
mvn spring-boot:run
```

主应用启动后会自动加载 `plugins` 目录下的插件。

### 4. 访问 Web 管理控制台

如果启用了 Web 管理控制台，访问：
```
http://localhost:8080/brick-web/index
```

## 📖 配置说明

### 主应用配置

```yaml
plugin:
  # 是否启用插件
  enable: true
  # 运行模式：dev（开发模式）/ prod（生产模式）
  runMode: dev
  # 是否跟随主应用日志输出
  plugin-follow-log: true
  # 主应用启动类全限定名
  mainPackage: com.your.package.YourApplication
  # 插件路径（支持多个路径）
  pluginPath:
    - ./plugins
    - /path/to/second/plugins
  
  # Web 管理控制台配置（可选）
  web:
    enabled: true
    enable-ui: true
    api-prefix: /brick-web/api
    page-prefix: /brick-web
    monitor-refresh-interval: 5
```

**配置说明**：
- `enable`：是否启用插件功能，默认为 true
- `runMode`：运行模式
  - `dev`：开发模式，插件修改后自动重新加载
  - `prod`：生产模式，插件只加载一次
- `plugin-follow-log`：是否将插件日志输出到主应用日志
- `mainPackage`：主应用启动类的全限定名，用于类加载器隔离
- `pluginPath`：插件存储路径，支持配置多个路径，数组格式

### 插件配置

在插件项目的 `plugin.yaml` 中配置：

```yaml
plugin:
  id: my-plugin
  version: 1.0.0
  name: My Plugin
  description: This is my first plugin
  author: Your Name
  main-class: com.example.MyPluginConfig
```

## 🔧 如何引入

### 主应用

```

<dependency>
<groupId>com.zqzqq</groupId>
<artifactId>spring-boot3-brick-bootkit</artifactId>
<version>4.0.4</version>
</dependency>

```

### Web 管理控制台（可选）

```
<dependency>
<groupId>com.zqzqq</groupId>
<artifactId>spring-boot3-brick-bootkit-web</artifactId>
<version>4.0.4</version>
</dependency>

```

### 插件开发

```
<dependency>
<groupId>com.zqzqq</groupId>
<artifactId>spring-boot3-brick-bootkit-maven-packager</artifactId>
<version>4.0.4</version>
<scope>provided</scope>
</dependency>

```

**注意**：主应用启动类需要扫描以下包路径：
``` 
com.zqzqq.bootkits.*
```

### 打包
> mvn clean install -Dgpg.skip=true -Djacoco.skip=true  -DskipTests=true
> 
> 


## ❓ 常见问题 (FAQ)

### 1. 插件启动失败怎么办？

**可能原因**：
- 插件依赖与主应用冲突
- 插件配置文件格式错误
- JDK 版本不匹配

**解决方法**：
1. 检查插件日志，查看具体错误信息
2. 确认插件配置文件格式正确
3. 检查 JDK 版本是否符合要求（JDK 17+）
4. 尝试使用隔离模式加载插件

### 2. 如何实现插件与主应用之间的通信？

**方法一：使用 Spring 事件机制**
```java
// 主应用发布事件
@Component
public class MainService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public void publishEvent() {
        eventPublisher.publishEvent(new MyCustomEvent("Hello Plugin"));
    }
}

// 插件监听事件
@Component
public class PluginListener {
    @EventListener
    public void handleEvent(MyCustomEvent event) {
        System.out.println("Received: " + event.getMessage());
    }
}
```

**方法二：使用服务接口**
```java
// 定义接口
public interface DataService {
    String getData();
}

// 主应用实现
@Service
public class MainDataService implements DataService {
    @Override
    public String getData() {
        return "Data from Main";
    }
}

// 插件调用
@RestController
public class PluginController {
    @Autowired
    private DataService dataService;
    
    @GetMapping("/data")
    public String getData() {
        return dataService.getData();
    }
}
```

### 3. 插件之间如何共享数据？

**推荐方式**：使用共享模式加载插件，并通过 Spring 容器共享 Bean

```yaml
plugin:
  load-mode: shared
```

### 4. 如何热更新插件？

使用 Web 管理控制台：
1. 访问 `http://localhost:8080/brick-web/index`
2. 在插件列表中选择要更新的插件
3. 点击"卸载"按钮
4. 上传新的插件 jar 包
5. 点击"安装"和"启动"按钮

### 5. 插件依赖的第三方库冲突怎么办？

**解决方法**：
- 使用隔离模式（默认），插件依赖完全隔离
- 如果必须使用共享模式，确保依赖版本一致
- 使用 Maven 的 `dependencyManagement` 统一管理版本

### 6. 如何调试插件？

**方法一：远程调试**
```bash
# 启动主应用时添加调试参数
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar your-app.jar
```

**方法二：日志调试**
```yaml
logging:
  level:
    com.zqzqq.bootkits: DEBUG
```

### 7. 支持哪些 Spring Boot 版本？

- **Spring Boot 3.x**：完全支持（推荐 3.5.5+）
- **Spring Boot 2.x**：仍然支持，但建议升级到 3.x

### 8. 如何贡献代码？

欢迎贡献代码！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 更新
- [4.0.4](./doc/updates/4.0.4.md) - 最新版本
- [4.0.1](./doc/updates/4.0.1.md)


### 文档地址
https://brick-bootkit.zqzqq.com/

## 📄 许可证

本项目采用 Apache License 2.0 开源协议。详见 [LICENSE](LICENSE) 文件。

Copyright 2024-2025 huzhenjie

## 联系我们

[点击加我微信入群](http://wechat.zqzqq.com/)

邮箱联系我们：huzhenjie@rjnetwork.net.cn

---

## 🌟 Star 历史

如果这个项目对你有帮助，请给我们一个 ⭐️ Star！

## 🤝 贡献者

欢迎贡献代码、报告 Bug 或提出新功能建议！

## 🙏 致谢

感谢原作者 [starblues](https://gitee.com/starblues) 提供的优秀框架基础。

感谢所有贡献者和使用者的支持！
