<div align="center">

# 🚀 Spring-Boot 插件式开发框架

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-green.svg)](https://spring.io/projects/spring-boot)
[![JDK](https://img.shields.io/badge/JDK-17+-orange.svg)](https://openjdk.org/)
[![Version](https://img.shields.io/badge/4.0.9-4.0.9-brightgreen.svg)](https://github.com/v18268185209/brick-bootkit-springboot)
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
- [插件服务通信](#-插件服务通信)
- [环境要求](#-环境要求)
- [快速开始](#-快速开始)
- [Spring Boot 快速接入](#-spring-boot-快速接入)
- [最小模板](#-最小模板)
- [支持矩阵](#-支持矩阵)
- [参与贡献](#-参与贡献)
- [路线图](#-路线图)
- [发布与验收](#-发布与验收)
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
+ 基于此插件开发了一个通用的业务系统具体演示地址可参考这个[演示demo.md](doc/%E6%BC%94%E7%A4%BAdemo.md)

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

Brick BootKit 采用分层架构设计，从上到下分为：应用层、扩展层、管理层、核心层、基础层。

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              应用层 (Application Layer)                        │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐            │
│  │   主应用程序      │  │   插件应用 A      │  │   插件应用 B      │            │
│  │  (Main App)      │  │  (Plugin App A)   │  │  (Plugin App B)   │            │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘            │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              扩展层 (Extension Layer)                         │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                    Web 管理控制台 (Web Console)                        │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐               │   │
│  │  │ 插件管理  │  │ 系统监控  │  │ API 文档  │  │ 脚本管理  │               │   │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘               │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              管理层 (Management Layer)                        │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        PluginManager (插件管理器)                       │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │   │
│  │  │  生命周期管理  │  │   依赖管理    │  │   配置管理    │                 │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘                 │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │   │
│  │  │   安全管理    │  │   监控管理    │  │   日志管理    │                 │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘                 │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              核心层 (Core Layer)                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  ┌──────────────────────┐  ┌──────────────────────┐                   │   │
│  │  │   Bootstrap 模块     │  │    Loader 模块       │                   │   │
│  │  │  (插件启动引导)       │  │   (类加载器隔离)      │                   │   │
│  │  │                      │  │                      │                   │   │
│  │  │ - SpringPluginBootstrap│ - PluginClassLoader  │                   │   │
│  │  │ - PluginApplicationContext │ - DevelopmentMode │                   │   │
│  │  │ - PluginSpringApplication │ - PluginResourceStorage│             │   │
│  │  └──────────────────────┘  └──────────────────────┘                   │   │
│  │                                                                       │   │
│  │  ┌──────────────────────┐  ┌──────────────────────┐                   │   │
│  │  │     Core 模块        │  │    Scripts 模块      │                   │   │
│  │  │  (核心功能实现)       │  │    (脚本管理)        │                   │   │
│  │  │                      │  │                      │                   │   │
│  │  │ - PluginInfo/PluginState │ - ScriptExecutor   │                   │   │
│  │  │ - PluginLauncherManager  │ - ScriptScheduler  │                   │   │
│  │  │ - RuntimeMode           │ - ScriptTemplate   │                   │   │
│  │  └──────────────────────┘  └──────────────────────┘                   │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              基础层 (Foundation Layer)                       │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                         Common 模块 (通用基础)                         │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐               │   │
│  │  │ 常量定义  │  │ 异常定义  │  │ 加密工具  │  │ 插件描述  │               │   │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘               │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                      Spring Boot 3.x + Java 17                       │   │
│  │           (Spring IoC / Spring AOP / Spring MVC / etc.)              │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 🚀 Spring Boot 快速接入

如果你是把这个项目接到一个现有的 Spring Boot 3.x 系统里，而不是先研究完整 Demo，建议直接看接入指南：

- [doc/2.SpringBoot项目快速接入指南.md](doc/2.SpringBoot项目快速接入指南.md)

最小接入通常只需要两步：

1. 引入 `spring-boot3-brick-bootkit`
2. 配置 `plugin.enable=true` 和 `plugin.pluginPath`

对于标准 Spring Boot 启动项目，`plugin.mainPackage` 现在会优先自动推断；只有在非标准包结构或特殊启动方式下，才建议显式配置。

接入后如果你不确定环境是否配置正确，可以直接访问：

```text
GET /plugins-web/api/doctor
```

它会返回主包推断、插件目录、上传临时目录、已发现插件数量等自检结果，适合第一次接入时排查问题。

## 📦 最小模板

仓库里已经提供可直接复制的最小模板：

- `templates/host-minimal`
- `templates/plugin-minimal`
- `templates/host-broken-main-package`
- `templates/host-broken-plugin-path`
- `templates/plugin-broken-packaging`

如果你是第一次接入，建议不要直接从大 Demo 拆代码，先从模板开始：

1. 复制 `host-minimal`
2. 复制 `plugin-minimal`
3. 改包名、artifactId、插件 ID
4. 先启动宿主并执行 `/plugins-web/api/doctor`
5. 再打包插件放入 `plugin.pluginPath`

如果你要验证排障流程，也可以直接用故障模板：

- `host-broken-main-package`：演示主包配置缺失
- `host-broken-plugin-path`：演示插件目录错误
- `plugin-broken-packaging`：演示插件包未按框架要求打包

## 🧩 支持矩阵

支持矩阵见：

- [doc/3.兼容与支持矩阵.md](doc/3.兼容与支持矩阵.md)

当前开源版明确的官方基线是：

- JDK 17
- Spring Boot 3.5.x
- Maven 3.6+

## 🤝 参与贡献

协作规范与提交流程见：

- [CONTRIBUTING.md](CONTRIBUTING.md)
- [SECURITY.md](SECURITY.md)
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)

## 🗺️ 路线图

项目路线图见：

- [ROADMAP.md](ROADMAP.md)

## ✅ 发布与验收

发布前建议检查清单见：

- [doc/4.发布与验收清单.md](doc/4.发布与验收清单.md)

仓库根目录还提供了自动化预检脚本：

- `release-precheck.sh`
- `release-precheck.ps1`

### 模块依赖关系图

```
                    ┌─────────────────────────────────────┐
                    │  spring-boot3-brick-bootkit-web     │
                    │        (Web 管理控制台)              │
                    └──────────────┬──────────────────────┘
                                   │ depends on
                    ┌──────────────▼──────────────────────┐
                    │  spring-boot3-brick-bootkit          │
                    │           (主模块)                    │
                    └──────────────┬──────────────────────┘
                                   │ depends on
         ┌─────────────────────────┼─────────────────────────┐
         │                         │                         │
┌────────▼────────┐     ┌─────────▼──────────┐   ┌────────▼─────────┐
│   bootstrap     │     │       core          │   │     loader       │
│  (启动引导模块)   │     │    (核心模块)       │   │   (加载器模块)    │
└────────┬────────┘     └─────────┬──────────┘   └────────┬─────────┘
         │                        │                        │
         └────────────────────────┼────────────────────────┘
                                  │ depends on
                         ┌────────▼──────────┐
                         │     common         │
                         │   (通用基础模块)    │
                         └───────────────────┘
```

### 核心组件说明

#### 1. Common 模块（通用基础层）
**职责**：提供框架通用的基础工具类、常量定义和基础数据结构

**核心包**：
- `cipher`：加密解密工具
- `Constants`：框架常量定义
- `ManifestKey`：插件 Manifest 文件键值
- `PluginDescriptorKey`：插件描述符键值
- `PackageStructure`：包结构定义
- `PackageType`：包类型枚举

#### 2. Loader 模块（类加载器层）
**职责**：实现自定义类加载器，支持插件类的隔离加载和资源管理

**核心组件**：
- `PluginClassLoader`：自定义插件类加载器
- `DevelopmentMode`：开发模式支持
- `PluginResourceStorage`：插件资源存储
- `archive`：插件归档文件处理
- `classloader`：类加载器实现
- `jar`：JAR 文件处理
- `launcher`：插件启动器

**关键特性**：
- 双亲委派模型改造
- 插件间类隔离
- 插件与主应用类隔离
- 资源加载隔离

#### 3. Bootstrap 模块（启动引导层）
**职责**：提供插件框架的自动配置和启动引导，集成 Spring 生态

**核心组件**：
- `SpringPluginBootstrap`：插件启动引导核心类
- `PluginApplicationContext`：插件应用上下文
- `PluginWebApplicationContext`：插件 Web 应用上下文
- `PluginSpringApplication`：插件 Spring 应用
- `AutowiredTypeResolver`：依赖注入类型解析
- `ConfigurePluginEnvironment`：插件环境配置

**关键特性**：
- 与 Spring Boot 深度集成
- 插件独立 Spring 容器
- 依赖注入支持
- 自动配置支持

#### 4. Core 模块（核心功能层）
**职责**：提供插件框架的核心功能和基础设施

**核心包**：
- `config`：配置管理
  - 配置管理器、持久化、加载器
  - 配置变更事件机制
- `lifecycle`：生命周期管理
  - 插件生命周期管理器
  - 生命周期状态枚举
  - 生命周期监听器
- `state`：状态管理
  - 插件状态定义
  - 状态转换管理
- `version`：版本管理
  - 版本号解析和比较
  - 版本兼容性检查
- `dependency`：依赖管理
  - 插件依赖解析
  - 依赖冲突检测
- `isolation`：隔离管理
  - 类隔离策略
  - 资源隔离策略
- `exception`：异常处理
  - 统一异常定义
  - 错误码管理
  - 异常工厂
- `health`：健康检查
  - 插件健康状态检测
  - 健康指标收集
- `logging`：日志管理
  - 插件日志隔离
  - 日志配置管理
- `monitoring`：监控管理
  - 性能监控器
  - 指标收集器
  - 监控管理器
- `performance`：性能优化
  - 性能分析工具
  - 性能指标统计
- `security`：安全管理
  - 代码安全扫描
  - 权限控制
  - 安全审计日志
- `plugin`：插件管理
  - 插件信息定义
  - 插件管理器
  - 插件启动管理

#### 5. Scripts 模块（脚本管理层）
**职责**：提供跨平台脚本执行和管理功能

**核心包**：
- `core`：脚本核心功能
- `executor`：脚本执行器
- `scheduler`：脚本调度器
- `template`：脚本模板
- `cache`：脚本缓存
- `security`：脚本安全
- `monitor`：脚本监控
- `batch`：批处理脚本
- `importexport`：导入导出
- `env`：环境变量
- `utils`：工具类

**支持的脚本类型**：
- Shell 脚本
- Python 脚本
- Lua 脚本
- Batch 脚本

#### 6. Web 模块（Web 管理层）
**职责**：提供可视化的插件管理和系统监控 Web 界面

**核心组件**：
- `controller`：控制器层
  - `api`：REST API 接口
  - `page`：页面控制器
- `service`：服务层
  - 插件管理服务
  - 监控服务
  - 上传历史服务
- `dto`：数据传输对象
- `config`：配置类
- `exception`：异常处理
- `ws`：WebSocket 支持
- `scripts`：脚本管理接口

**功能特性**：
- 插件上传、安装、卸载
- 插件启动、停止、重启
- 系统监控（内存、CPU、线程）
- API 文档（Knife4j）
- 插件日志查看

#### 7. Maven Packager 模块（打包工具层）
**职责**：提供 Maven 插件，支持插件的打包和构建

**核心功能**：
- 开发环境打包
- 生产环境打包
- 依赖处理
- 配置注入
- 版本管理

### 数据流图

```
┌──────────────┐
│  主应用启动   │
└──────┬───────┘
       │
       ▼
┌─────────────────────────────┐
│  Bootstrap 初始化          │
│  - 加载配置                 │
│  - 扫描插件目录             │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│  Loader 加载插件            │
│  - 创建 PluginClassLoader   │
│  - 加载插件 JAR             │
│  - 解析插件 Manifest        │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│  Core 初始化插件            │
│  - 创建 PluginInfo          │
│  - 解析依赖                 │
│  - 验证版本兼容性           │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│  Bootstrap 启动插件         │
│  - 创建 PluginApplicationContext │
│  - 注入依赖                 │
│  - 执行初始化逻辑           │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│  插件运行中                 │
│  - 处理业务请求             │
│  - 监控性能指标             │
│  - 记录日志                 │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│  插件停止/卸载              │
│  - 执行清理逻辑             │
│  - 释放资源                 │
│  - 关闭类加载器             │
└─────────────────────────────┘
```

### 关键设计模式

1. **工厂模式**：PluginExceptionFactory、PluginLauncherManager
2. **单例模式**：PluginManager、PluginContextHolder
3. **观察者模式**：生命周期监听器、配置变更事件
4. **策略模式**：类加载隔离策略、运行模式策略
5. **模板方法模式**：插件启动流程
6. **代理模式**：插件 Bean 代理
7. **建造者模式**：PluginInfo 构建


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
            <version>4.0.9</version>
            <configuration>
                <mode>prod</mode>
                <pluginInfo>
                    <id>your-plugin-id</id>
                    <bootstrapClass>com.example.plugin.YourPluginBootstrap</bootstrapClass>
                    <version>1.0.0</version>
                </pluginInfo>
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
    <version>4.0.9</version>
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
    api-prefix: /plugins-web/api
    page-prefix: /plugins-web
    monitor-refresh-interval: 5
```

4. 访问管理控制台：`http://localhost:8080/plugins-web/index`

#### 🔗 嵌入模式
Web 管理控制台支持通过 URL 参数 `embedded=true` 以嵌入模式加载，此时会隐藏侧边栏、头部和底部，适合 iframe 嵌入到其他页面中使用。

```
# 嵌入模式访问地址
http://localhost:8080/plugins-web/index?embedded=true
http://localhost:8080/plugins-web/scripts?embedded=true
http://localhost:8080/plugins-web/monitor?embedded=true
```




## 🔗 插件服务通信

本框架提供插件间服务通信能力，支持插件之间相互调用方法或接口。

### 核心概念

- **Service Registry (服务注册中心)**: 管理插件服务的注册、发现和生命周期
- **Service Descriptor (服务描述符)**: 存储服务的元信息（版本、优先级、健康状态等）
- **Version Range (版本范围)**: 支持语义化版本控制，如 `[1.0,2.0)`
- **Cross-ClassLoader Proxy (跨类加载器代理)**: 解决插件间类隔离问题

### 快速开始

#### 1. 定义服务接口

```java
package com.example.plugin.service;

public interface UserService {
    String getUserName(Long userId);
    UserInfo getUserInfo(Long userId);
}
```

#### 2. 在插件中实现服务

```java
package com.example.plugin.service.impl;

@PluginService(
    interfaceClass = UserService.class,
    version = "1.0.0",
    name = "user-service",
    priority = 100
)
public class UserServiceImpl implements UserService {
    
    @Override
    public String getUserName(Long userId) {
        return "User-" + userId;
    }
    
    @Override
    public UserInfo getUserInfo(Long userId) {
        return new UserInfo(userId, "User-" + userId);
    }
}
```

#### 3. 在其他插件中调用服务

```java
package com.example.consumer.plugin;

@ServiceDependency(
    pluginId = "user-plugin",
    serviceInterface = UserService.class,
    versionRange = "[1.0,2.0)"
)
public class UserConsumer {
    
    @ServiceDependency
    private UserService userService;
    
    public void printUserName(Long userId) {
        String name = userService.getUserName(userId);
        System.out.println("User name: " + name);
    }
}
```

### 编程式使用

#### 注册服务

```java
@Autowired
private PluginServiceRegistry pluginRegistry;

// 方式1: 自动扫描（推荐）
// 在插件启动时使用 PluginServiceRegistryManager 自动扫描 @PluginService 注解

// 方式2: 手动注册
public void registerUserService() {
    UserServiceImpl userService = new UserServiceImpl();
    
    pluginRegistry.registerService(
        "user-plugin",                    // pluginId
        UserService.class,               // service interface
        userService,                      // service instance
        ServiceMetadata.builder()
            .version("1.0.0")
            .priority(100)
            .healthCheckEnabled(true)
            .build()
    );
}
```

#### 发现服务

```java
@Autowired
private PluginServiceRegistry pluginRegistry;

// 根据插件ID获取服务
public UserService getUserService() {
    return pluginRegistry.getService("user-plugin", UserService.class);
}

// 获取所有实现某接口的服务
public List<UserService> getAllUserServices() {
    return pluginRegistry.getServices(UserService.class);
}

// 根据版本范围获取服务
public List<UserService> getUserServicesByVersion() {
    return pluginRegistry.getServicesByVersion(UserService.class, "[1.0,2.0)");
}
```

#### 依赖检查

```java
public boolean checkPluginDependencies(String pluginId) {
    ServiceDependencyCheckResult result = pluginRegistry.checkDependencies(pluginId);
    
    if (!result.isSatisfied()) {
        result.getUnsatisfiedDependencies().forEach(dep -> {
            System.err.println("Missing dependency: " + dep.getRequiredService());
        });
    }
    
    return result.isSatisfied();
}
```

### 事件监听

```java
public class ServiceChangeListenerImpl implements ServiceChangeListener {
    
    @Override
    public void onServiceChange(ServiceEvent event) {
        if (event instanceof ServiceRegisteredEvent) {
            ServiceDescriptor descriptor = event.getServiceDescriptor();
            System.out.println("Service registered: " + descriptor.getServiceId());
        } else if (event instanceof ServiceUnregisteredEvent) {
            ServiceDescriptor descriptor = event.getServiceDescriptor();
            System.out.println("Service unregistered: " + descriptor.getServiceId());
        }
    }
}

// 订阅服务变化
pluginRegistry.subscribe(UserService.class, new ServiceChangeListenerImpl());
```

### 版本兼容

框架支持多种版本范围表达式：

| 表达式 | 说明 | 示例 |
|--------|------|------|
| `[1.0.0]` | 精确版本 | 只匹配 1.0.0 |
| `[1.0,2.0)` | 范围版本 | >=1.0.0 且 <2.0.0 |
| `[1.0,)` | 最低版本 | >=1.0.0 |
| `(,2.0)` | 最高版本 | <2.0.0 |
| `*` | 任意版本 | 匹配所有版本 |

### API 参考

| 方法 | 说明 |
|------|------|
| `registerService(pluginId, interface, instance, metadata)` | 注册服务 |
| `getService(pluginId, interface)` | 获取服务 |
| `getServices(interface)` | 获取所有实现 |
| `getServicesByVersion(interface, versionRange)` | 按版本获取 |
| `checkDependencies(pluginId)` | 检查依赖 |
| `subscribe(interface, listener)` | 订阅变化 |
| `unregisterService(pluginId, interface)` | 注销服务 |

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
        <version>4.0.9</version>
    </dependency>
    
    <!-- Web 管理控制台（可选） -->
    <dependency>
        <groupId>com.zqzqq</groupId>
        <artifactId>spring-boot3-brick-bootkit-web</artifactId>
        <version>4.0.9</version>
    </dependency>
</dependencies>
```

#### 1.2 配置启动类

保持标准 Spring Boot 启动类即可，通常**不需要**额外手动扫描 `com.zqzqq.bootkits`：

```java
@SpringBootApplication
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
  pluginPath:
    - ./plugins
    # 可以配置多个插件目录
    # - /path/to/second/plugins
```

说明：

- 标准 Spring Boot 项目会优先自动推断 `plugin.mainPackage`
- 只有在非标准启动类结构、多模块特殊扫描或自动推断失败时，才建议显式配置 `plugin.mainPackage`

### 2. 创建插件项目

#### 2.1 添加依赖

按插件业务需要引入运行时依赖即可。  
`spring-boot3-brick-bootkit-maven-packager` 是 **Maven 打包插件**，应配置在 `<build><plugins>` 中（见下节），不要放在 `<dependencies>` 中。

#### 2.2 配置 Maven 打包插件

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.zqzqq</groupId>
            <artifactId>spring-boot3-brick-bootkit-maven-packager</artifactId>
            <version>4.0.9</version>
            <configuration>
                <mode>prod</mode>
                <pluginInfo>
                    <id>my-plugin</id>
                    <bootstrapClass>com.example.MyPluginConfig</bootstrapClass>
                    <version>1.0.0</version>
                </pluginInfo>
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
http://localhost:8080/plugins-web/index
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

  # 是否启用集群协同锁（多实例部署建议开启）
  clusterEnabled: false
  # 集群共享路径（锁文件和迁移状态文件）
  # 开启 clusterEnabled 后建议配置为所有实例都可读写的共享存储（如 NFS/NAS）
  clusterSharedPath: /data/shared/plugins
  # 集群锁获取超时（毫秒）
  clusterLockTimeoutMs: 30000
  
  # Web 管理控制台配置（可选）
  web:
    enabled: true
    enable-ui: true
    api-prefix: /plugins-web/api
    page-prefix: /plugins-web
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
- `clusterEnabled`：是否启用跨实例插件生命周期互斥锁，默认 `false`
- `clusterSharedPath`：集群共享目录。用于保存锁文件（`.plugin-locks`）和插件迁移状态（`.plugin-state/migrations`）
- `clusterLockTimeoutMs`：集群锁超时时间（毫秒），默认 `30000`

#### 主应用依赖隔离说明

- 框架默认会屏蔽主应用依赖中的 Spring 自动装配元数据文件（如 `META-INF/spring.factories`、`META-INF/spring/*.imports`），避免主应用引入 Liquibase 等依赖时干扰插件启动。
- 若插件确实需要放开部分主应用依赖资源，可在主应用中自定义 `MainResourcePatternDefiner` 扩展 `includePatterns`，按包路径精确放开。

### 插件配置

在插件包的 `META-INF/PLUGIN.META` 中配置插件元信息（`properties` 格式）：

```properties
plugin.id=my-plugin
plugin.version=1.0.0
plugin.bootstrapClass=com.example.MyPluginBootstrap
plugin.description=This is my first plugin
plugin.provider=Your Name
```

#### 插件数据库迁移脚本（可选）

在同一个 `META-INF/PLUGIN.META` 中追加以下配置：

```properties
# 安装时按顺序执行（逗号分隔）
plugin.migration.up=db/migration/V1__init.sql,db/migration/V2__seed.sql
# 卸载时按逆序执行，按索引与 up 一一对应
plugin.migration.down=db/migration/U1__init.sql,db/migration/U2__seed.sql
# 数据源：main（默认）或 bean:<beanName>
plugin.migration.datasource=main
```

- 安装插件时会执行 `plugin.migration.up`，已执行过的脚本会被跳过（幂等）。
- 卸载插件时会按逆序执行 `plugin.migration.down`，且仅回滚已执行过的 up 脚本。
- up/down 必须按索引一一对应；缺失 down 脚本会直接报错并终止卸载流程。
- 脚本路径支持目录插件和 jar 插件。推荐将 SQL 放在 `classes/db/migration` 下，并在配置中使用 `db/migration/*.sql`。
- 迁移执行记录保存在 `${clusterSharedPath}/.plugin-state/migrations`；未配置 `clusterSharedPath` 时，默认使用首个 `pluginPath`（若为空则退回主进程工作目录）。
- 多实例场景下，建议开启 `clusterEnabled=true`，插件安装/卸载及迁移会通过 `${clusterSharedPath}/.plugin-locks` 进行跨实例互斥。

### 单插件启动测试（独立/共享）

框架已支持单插件直接启动测试，不依赖主程序先启动。

#### 1. 插件入口示例

```java
import com.zqzqq.bootkits.bootstrap.SpringPluginBootstrap;
import com.zqzqq.bootkits.bootstrap.annotation.OneselfConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OneselfConfig(
        developmentMode = "coexist", // isolation / coexist
        mainConfigFileName = {"application.yml"}
)
public class DemoPluginBootstrap extends SpringPluginBootstrap {
    public static void main(String[] args) {
        new DemoPluginBootstrap().run(args);
    }
}
```

#### 2. 模式选择方式

- 代码方式：`@OneselfConfig(developmentMode = "isolation|coexist")`
- JVM 参数方式：`-Dplugin.developmentMode=isolation|coexist`
- 兼容参数：`-Dspring-boot3-brick-bootkit.developmentMode=...`、`-DdevelopmentMode=...`
- 环境变量：`PLUGIN_DEVELOPMENT_MODE=isolation|coexist`
- `prod` 包启动（`ProdLauncher`）优先读取 `MANIFEST.MF` 中的 `Main-Development-Mode`。
- 当 `Main-Development-Mode` 未配置时，会回退到上述 JVM 参数和环境变量链路。

#### 3. 无 `PLUGIN.META` 的本地开发支持

当 IDE 直接运行、尚未生成 `META-INF/PLUGIN.META` 时，框架会自动使用当前启动类构建临时插件描述信息，不再阻塞启动。

可选覆盖参数：

- `-Dplugin.standalone.id=xxx`
- `-Dplugin.standalone.version=1.0.0-SNAPSHOT`
- `-Dplugin.standalone.bootstrapClass=com.example.DemoPluginBootstrap`

#### 4. 推荐：编译期预生成 `PLUGIN.META`

为避免本地直接启动时读取到临时描述信息，建议在插件工程中增加 `prepare-meta` 目标，让 `process-classes` 阶段就生成 `target/META-INF/PLUGIN.META`：

```xml
<plugin>
    <groupId>com.zqzqq</groupId>
    <artifactId>spring-boot3-brick-bootkit-maven-packager</artifactId>
    <version>4.0.9</version>
    <configuration>
        <mode>prod</mode>
        <pluginInfo>
            <id>demoTestUploadPlus</id>
            <bootstrapClass>com.example.DemoPluginBootstrap</bootstrapClass>
            <version>0.0.3</version>
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
</plugin>
```

## 🔧 如何引入

### 主应用

```xml

<dependency>
<groupId>com.zqzqq</groupId>
<artifactId>spring-boot3-brick-bootkit</artifactId>
<version>4.0.9</version>
</dependency>

```

### Web 管理控制台（可选）

```xml
<dependency>
<groupId>com.zqzqq</groupId>
<artifactId>spring-boot3-brick-bootkit-web</artifactId>
<version>4.0.9</version>
</dependency>

```

### 插件开发

```xml
<plugin>
    <groupId>com.zqzqq</groupId>
    <artifactId>spring-boot3-brick-bootkit-maven-packager</artifactId>
    <version>4.0.9</version>
    <configuration>
        <mode>prod</mode>
        <pluginInfo>
            <id>my-plugin</id>
            <bootstrapClass>com.example.MyPluginConfig</bootstrapClass>
            <version>1.0.0</version>
        </pluginInfo>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>repackage</goal>
            </goals>
        </execution>
    </executions>
</plugin>

```

**注意**：标准 Spring Boot 项目通常不需要额外手工扫描 `com.zqzqq.bootkits`。
如果你的启动结构比较特殊，或者自动推断失败，再考虑显式补充扫描与 `plugin.mainPackage` 配置。

### 打包
> mvn clean install -Dgpg.skip=true -Djacoco.skip=true  -DskipTests=true
> 
> 

### 版本号更新脚本

项目提供跨平台版本更新脚本，会同步更新各模块 `pom.xml` 及相关文件中的版本号。

Windows：

```bash
update-version.bat 4.1.0
```

Linux / macOS：

```bash
./update-version.sh 4.1.0
```

参数规则：
- 仅接受一个版本参数。
- 支持格式：`x.y.z` 或 `x.y.z-suffix`（例如：`4.0.1-beta`）。

建议在项目任意子目录执行，脚本会自动探测 Git 根目录，并在执行前生成 `pom.xml.backup.*` 备份文件。

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

**推荐方式**：优先使用“插件服务通信”能力共享数据（`@PluginService` + `@ServiceDependency`），
避免通过共享类加载器直接共享实现类。

### 4. 如何热更新插件？

使用 Web 管理控制台：
1. 访问 `http://localhost:8080/plugins-web/index`
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
- [4.0.9](./doc/updates/4.0.9.md) - 最新版本
- [4.0.1](./doc/updates/4.0.1.md)


### 文档地址
https://brick-bootkit.zqzqq.com/

## 📄 许可证

本项目采用 Apache License 2.0 开源协议。详见 [LICENSE](LICENSE) 文件。

Copyright 2024-2025 huzhenjie

## 联系我们

**我唯一官方微信号为 worker_680**
**邮箱联系我们：huzhenjie@rjnetwork.net.cn**

---

## 🌟 Star 历史

如果这个项目对你有帮助，请给我们一个 ⭐️ Star！

## 🤝 贡献者

欢迎贡献代码、报告 Bug 或提出新功能建议！

## 🙏 致谢

感谢原作者 [starblues](https://gitee.com/starblues) 提供的优秀框架基础。

感谢所有贡献者和使用者的支持！


### 视频教程
微信视频号 搜索 用户  jove.hu 点击关注 点进去即可观看视频教程。唯一视频课程地址。



---


<div align="center">

**🏢 商业合作**

本公司专业解决各种系统疑难杂症，如有商业合作需求，可直接添加下述微信详谈。

【软件开发业务 | 系统二开业务 | ...】

遇财（杭州）科技有限公司为您提供技术支撑。
<div style="width: 200px">

![微信二维码](./docs/me.jpg)

</div>
</div>

## 2026-02 增强能力（新增）

以下能力已经落地到主链路，默认保持向后兼容。

### 1) 安全准入管线（Admission Pipeline）
- 新增配置 `plugin.admissionMode=off|warn|enforce`（默认 `warn`）。
- 新增 SPI：`com.zqzqq.bootkits.core.admission.PluginAdmissionCheck`。
- 安装、启动阶段会进入准入管线；`enforce` 模式下命中拒绝项会直接阻断操作。
- 默认内置 `PluginDescriptorAdmissionCheck`（检查 `pluginId/version/mainClass`）。

示例：
```yaml
plugin:
  admissionMode: enforce
```

### 2) 集群锁可插拔
- 新增配置 `plugin.clusterLockProviderBeanName`，可显式指定主应用中的锁实现 Bean。
- 如果未指定且容器中只有一个 `ClusterLockProvider`，框架会自动使用该实现。
- 未提供自定义实现时，仍保持原有策略：
  - `clusterEnabled=false` -> NoOp 锁
  - `clusterEnabled=true` -> 文件锁（`clusterSharedPath/.plugin-locks`）

示例：
```yaml
plugin:
  clusterEnabled: true
  clusterSharedPath: /data/shared/plugins
  clusterLockProviderBeanName: redisClusterLockProvider
```

### 3) 迁移引擎增强
- 新增配置：
  - `plugin.migrationValidateChecksum=true|false`（默认 `true`）
  - `plugin.migrationContinueOnError=true|false`（默认 `false`）
- 每个已执行的 up 脚本会持久化 checksum；重复安装时可校验脚本是否被篡改。
- 迁移状态文件继续保存在：`${clusterSharedPath}/.plugin-state/migrations`。

示例：
```yaml
plugin:
  migrationValidateChecksum: true
  migrationContinueOnError: false
```

### 4) 灰度与回滚
- 新增配置：
  - `plugin.rolloutMode=direct|gray`（默认 `direct`）
  - `plugin.rolloutAutoStart=true|false`（默认 `true`）
  - `plugin.rolloutRollbackOnFailure=true|false`（默认 `true`）
- 升级失败时可自动回滚：卸载失败版本 -> 重新安装备份版本 -> 按需恢复启动。
- `gray` 模式下支持主应用注入探针：`PluginRolloutProbe`。

示例：
```yaml
plugin:
  rolloutMode: gray
  rolloutAutoStart: true
  rolloutRollbackOnFailure: true
```

### 5) SPI 扩展管理器
- 新增配置 `plugin.lifecycleExtensionsEnabled=true|false`（默认 `true`）。
- 新增 SPI：`com.zqzqq.bootkits.integration.spi.PluginLifecycleExtension`。
- 支持两种扩展来源：
  - Spring Bean
  - Java SPI（`META-INF/services/...PluginLifecycleExtension`）
- 生命周期钩子已接入安装/启动/停止/卸载流程。

示例：
```yaml
plugin:
  lifecycleExtensionsEnabled: true
```

### 6) Web 鉴权委托模式（已落地）
- 新增配置 `plugin.web.auth.mode=disabled|delegate|strict`（默认 `delegate`）。
- `strict` 模式要求主应用提供 `PluginWebAuthorizer`，否则启动失败。
- 新增能力查询接口：`GET /plugins/auth/capabilities`。
