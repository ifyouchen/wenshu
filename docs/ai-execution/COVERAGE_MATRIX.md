# Original Documents Coverage Matrix

本文件用于审计 `ai-execution` 是否覆盖三份原始文档的功能范围。

状态：

- `Covered`：已进入 backlog/API/验收。
- `Deferred`：原文定义为后续版本或生态能力，已登记但不进入 V1。
- `Reference`：市场、竞品、路线图等背景信息，不形成直接实现任务。

## 产品文档覆盖

| 原始章节 | 覆盖状态 | AI 执行位置 |
| --- | --- | --- |
| 1 产品概述、定位、差异化 | Covered | README 项目目标 |
| 2 市场分析与竞品 | Reference | 不形成实现任务 |
| 3 用户画像与场景 | Covered | README 项目目标、P8 用户引导 |
| 4.0 功能矩阵总览 | Covered | P1-P9 全部主任务 |
| 4.1 账户体系 | Covered | P1、API Auth & User、账号验收 |
| 4.1 项目/作品管理 | Covered | P2、API Projects |
| 4.1 大纲面板 | Covered | P2-05、P8-07 |
| 4.1 角色库面板 | Covered | P3-01/P3-02、P8-07 |
| 4.1 全文搜索与替换 | Covered | P4-04 到 P4-06、P8-09 |
| 4.1 写作统计看板 | Covered | P4-07 到 P4-09、P8-10 |
| 4.1 内容导入 | Covered | P4-01 到 P4-03 |
| 4.1 专有名词/世界观词典 | Covered | P3-03 到 P3-05 |
| 4.1 数据管理与账户安全 | Covered | P1、P9-04、P9-06 |
| 4.2 AI 写小说 | Covered | P5-04 到 P5-08、P8-08 |
| 4.3 AI 改小说 | Covered | P5-09/P5-10、P6-05 到 P6-07 |
| 4.4 小说转剧本 | Covered | P7、P8-13 |
| 5.1 商业模式 | Covered | P9-01 到 P9-04、P9-09 |
| 5.2 用户配额设计 | Covered | P9-01、P9-09、P8-19 |
| 5.2 团队版协作能力 | Covered | P9-07、P9-08、Team API |
| 5.3 种子用户获取 | Reference | 运营事项，不形成产品实现任务 |
| 5.4 关键指标 | Reference | 可后续转分析埋点任务 |
| 6 路线图 | Covered/Deferred | V1 主链路在 P1-P9；V2 生态在 P10 |
| 7 合规与伦理 | Covered | P9-05、P9-06、内容安全/合规验收 |

## 技术文档覆盖

| 原始章节 | 覆盖状态 | AI 执行位置 |
| --- | --- | --- |
| 0 工程规范与 DDD | Covered | EXECUTION_RULES、P0 |
| 0 编码/日志/设计模式/命名 | Covered | EXECUTION_RULES、ACCEPTANCE 全局 |
| 1 技术架构 | Covered | P0、README、STORAGE_COS |
| 1 模型选用策略 | Covered | P5-01、P6-06、执行规则 |
| 2 Prompt 链 | Covered | P5-02、P5-04 到 P5-10、P7-02/P7-03 |
| 3 上下文锚点系统 | Covered | P5-06、P6-01 到 P6-04 |
| 4 超长文本处理 | Covered | P5-06、P6-01、上下文验收 |
| 5 三功能数据流 | Covered | P5、P6、P7 |
| 6 异常处理策略 | Covered | P5-01、P8-17、异常验收 |
| 7 数据模型与存储 | Covered | P0-05、Flyway 迁移、P1-P9 |
| 8 LangChain4j 实现 | Covered | P5-01、P5-07 |
| 9 基础设施与部署 | Covered | P0-06、docker-compose、STORAGE_COS |
| 10 REST API 接口设计 | Covered | API_CONTRACT |
| 11 异步任务进度 | Covered | P5-03、System API |
| 12 认证 API/邮件模板 | Covered | P1、API Auth、P1-11 |
| 13 支付系统集成 | Covered | P9-03、Subscription API |
| 14 内容安全过滤 | Covered | P9-05、P8-17、Content Safety API |
| 15 数据导出 ZIP | Covered | P9-04 |
| 16/17 导出格式与文件处理 | Covered | P7-08、STORAGE_COS |
| 18 前端 SSE 断连重连 | Covered | P8-08、前端验收 |
| 19 TipTap 深度集成 | Covered | P8-06、P8-08、P8-11 |
| 20 内容导入实现 | Covered | P4-01 到 P4-03 |
| 21 搜索替换实现 | Covered | P4-04 到 P4-06 |
| 22 写作统计实现 | Covered | P4-07 到 P4-09 |
| 前端工程化规范 | Covered | P8-01 到 P8-03、P8-22 |

## 交互设计文档覆盖

| 原始章节 | 覆盖状态 | AI 执行位置 |
| --- | --- | --- |
| 0 注册与登录 | Covered | P1、P8-04 |
| 0 邮件模板 | Covered | P1-11 |
| 0 首次登录身份选择 | Covered | P1-10、P8-18 |
| 1 作品列表首页 | Covered | P8-05 |
| 1 配额 Tooltip/操作前预估 | Covered | P9-01、P8-05、P8-19 |
| 2 编辑器布局 | Covered | P8-06 到 P8-10 |
| 2 新建作品骨架流程 | Covered | P5-04/P5-05、P8-08 |
| 2 大纲/角色/导入/词典/搜索/统计 | Covered | P3/P4/P8 |
| 3 选中即操作/润色/分支 | Covered | P5-08/P5-09、P8-08 |
| 4 AI 内容标识/加载/SSE 首字超时 | Covered | P5-07、P8-08 |
| 5 版本快照/diff/一致性报告 | Covered | P2-07、P8-11/P8-12 |
| 5 一致性审查配额超限 UI | Covered | P6-05、P8-19 |
| 6 渐进式用户引导/文风入口 | Covered | P8-18、P5-10 |
| 7 剧本改编工作台/导出预览/分集/冲突 | Covered | P7、P8-13 |
| 8 命令面板 | Covered | P8-15 |
| 9 异常状态 UI/内容安全申诉 | Covered | P8-17、P9-05 |
| 10 移动端响应式 | Covered | P8-14、P8-22 |
| 11 Toast 通知 | Covered | P8-16 |
| 12 订阅升级引导 | Covered | P8-19、P9 |
| 13 账户设置/注销撤销 | Covered | P1-09、P8-20 |
| 14 快捷键参考 | Covered | P8-21 |
| 15 前端性能约束与交互边界 | Covered | P8-22、EXECUTION_RULES |

## Deliberate Overrides

| 原始内容 | 新执行决策 |
| --- | --- |
| 技术文档中的 MinIO 对象存储 | 已改为腾讯云 COS，见 `STORAGE_COS.md` |

