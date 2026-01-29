## Spring-Boot插件式开发框架
+ 原作者仓库地址[https://gitee.com/starblues/springboot-plugin-framework-parent.git]
#### 背景
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

### 介绍
该框架可以在spring-boot项目上开发出插件功能，在插件中可以和spring-boot使用方式一模一样。使用了本框架您可以实现如下需求：

+ 在插件中，您可以当成一个微型的spring-boot项目来开发，简单易用。
+ 在插件中扩展出系统各种功能点，用于系统灵活扩展，再也不用使用分支来交付不同需求的项目了。
+ 在插件中可以集成各种框架及其各种spring-boot-xxx-starter。
+ 在插件中可以定义独立依赖包了，再也不用在主程序中定义依赖包了。
+ 可以完美解决插件包与插件包、插件包与主程序因为同一框架的不同版本冲突问题了。各个插件可以定义同一依赖的不同版本框架。
+ 无需重启主程序，可以自由实现插件包的动态安装部署，来动态扩展系统的功能。
+ 插件也可以不依赖主程序独立集成微服务模块。
+ **内置 Web 管理控制台**：可视化插件管理和系统监控界面，支持插件上传、启动、停止、重启、卸载等操作。
+ 您可以丰富想象该框架给您带来哪些迫切的需求和扩展，以实现系统的低耦合、高内聚、可扩展的优点。

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

### 2. spring-boot3-brick-bootkit-web (Web 管理控制台)
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
    <version>4.0.3</version>
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



## 如何引入

主应用

```

<dependency>
<groupId>com.zqzqq</groupId>
<artifactId>spring-boot3-brick-bootkit</artifactId>
<version>4.0.3</version>
</dependency>

```

Web 管理控制台（可选）

```
<dependency>
<groupId>com.zqzqq</groupId>
<artifactId>spring-boot3-brick-bootkit-web</artifactId>
<version>4.0.3</version>
</dependency>

```

插件

```
<dependency>
<groupId>com.zqzqq</groupId>
<artifactId>spring-boot3-brick-bootkit-maven-packager</artifactId>
<version>4.0.3</version>
</dependency>

```

具体使用方法可参考原作者文档，然后把对应的groupId和articleId和version进行替换即可使用。
主应用启动类应该把此包路径进行扫描：
``` 
com.zqzqq.bootkits.*
```

### 打包
> mvn clean install -Dgpg.skip=true -Djacoco.skip=true  -DskipTests=true
> 
> 


### 更新
[4.0.3](./doc/updates/4.0.3.md)


### 文档地址
https://brick-bootkit.zqzqq.com/

## 联系我们

[点击加我微信入群](http://wechat.zqzqq.com/)

邮箱联系我们 huzhenjie@rjnetwork.net.cn
