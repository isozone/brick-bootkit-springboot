# docs-website

`docs-website` 是 `brick-bootkit-springboot` 仓库中的文档前端工程，基于 Vue 3 + Vite 构建。

## 目标

- 以源码事实为基准生成文档内容，避免文案与代码脱节。
- 统一页面结构，减少重复页面维护成本。
- 提供现代化、顺滑且移动端可用的文档浏览体验。

## 关键特性

- 单页面模板渲染：所有文档页由 `DocPage.vue` + `docs.js` 数据驱动。
- 全站快捷搜索：支持 `Ctrl/Cmd + K` 搜索页面标题、章节和参数关键字。
- 源码依据可跳转：每个 `sources` 条目可直接跳转到 GitHub 对应文件。
- 上下页导航：按侧栏顺序自动生成上一篇/下一篇链接。

## 技术栈

- Vue 3
- Vue Router
- Vite

## 本地开发

```bash
npm install
npm run dev
```

默认开发地址：`http://localhost:5173`

## 构建

```bash
npm run build
```

`build` 会先执行 `verify-docs`，确保文档中的源码路径真实存在，再输出到仓库根目录 `docs/`。

## 文档验真

先刷新自动索引（当配置源码有变更时）：

```bash
npm run generate-config-index
```

再执行校验：

```bash
npm run verify-docs
```

该命令包含两部分：

- `verify-docs:sources`：检查 `src/content/docs.js` 里所有 `sources` 路径是否真实存在。
- `verify-docs:coverage`：从源码配置类与条件开关注解自动提取配置键，并检查文档覆盖完整性（当前自动识别 88 项）。

任一校验失败都会直接退出并输出缺失项。

## 文档内容来源

页面内容通过 `src/content/docs.js` 统一维护，所有关键章节都附带源码依据路径。

## 目录

```txt
src/
  content/docs.js      # 文档数据源
  content/generated-config-keys.json  # 自动提取的配置键索引
  content/generated-config-keys.js    # 前端消费的配置键索引模块
  views/DocPage.vue    # 通用文档渲染页面
  App.vue              # 站点布局（侧栏、TOC、导航）
  main.js              # 路由和应用入口
  style.css            # 全局视觉样式
scripts/
  config-key-extractor.mjs  # 配置键提取器（源码解析）
  generate-config-index.mjs  # 生成配置键索引文件
  generate-config-summary.mjs  # 生成配置键汇总（artifact 用）
  verify-doc-sources.mjs  # sources 路径存在性校验
  verify-config-coverage.mjs  # 配置键覆盖校验
```
