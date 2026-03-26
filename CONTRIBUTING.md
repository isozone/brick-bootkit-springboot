# Contributing

感谢你为 `brick-bootkit-springboot` 做贡献。

这份指南的目标很简单：

1. 让第一次提 issue / PR 的人知道该怎么做
2. 让问题复现和代码评审的成本更低
3. 尽量避免“修了 A 又把 B 弄坏”

## 提 issue 之前

请先完成这几步：

1. 确认当前使用的版本号
2. 记录运行环境：JDK、Spring Boot、操作系统
3. 如果是宿主接入问题，先执行 `/plugins-web/api/doctor`
4. 如果是插件包问题，尽量附上最小可复现插件或关键打包配置
5. 检查文档站里的“排障与自检”页面

## issue 建议包含

- 当前版本号
- 宿主项目最小配置
- 插件项目最小配置
- 关键日志或异常堆栈
- `doctor` 摘要或导出结果
- 期望行为与实际行为

## 提交 PR 之前

请尽量保证：

1. 改动范围尽量聚焦，不混入无关重构
2. 新增功能或修复要补充测试
3. 如果改了对外行为，请同步 README / 文档站 / 模板
4. 不要依赖内部实现包作为公共 API

## 本地建议检查

Java 模块：

```bash
./mvnw -pl spring-boot3-brick-bootkit-loader,spring-boot3-brick-bootkit-core,spring-boot3-brick-bootkit -am test
```

文档站：

```bash
cd docs-website
npm ci
npm run verify-docs
```

前端源码：

```bash
cd spring-boot3-brick-bootkit-web/vue3
npm install
npm run build
```

如果你想在提交前做一次汇总检查，也可以直接运行：

```bash
./release-precheck.sh
```

Windows：

```powershell
.\release-precheck.ps1
```

## 变更分类建议

- `fix`: 缺陷修复
- `feat`: 新功能
- `docs`: 文档更新
- `refactor`: 重构
- `test`: 测试补充
- `chore`: 杂项维护

## 对公共行为的改动

以下改动请特别谨慎：

- `plugin.*` 配置项语义
- `PluginManager` 对外接口
- Web API 路径和返回结构
- 插件打包元数据结构
- 类加载隔离规则

如果必须修改，请同时：

1. 补充兼容性说明
2. 更新文档
3. 增加回归测试

## 文档约定

如果新增：

- 配置项
- 错误码
- doctor 检查项
- 模板

请同时更新：

- `readme.md`
- `docs-website/src/content/docs.js`
- 必要时补充 `doc/updates/*.md`
