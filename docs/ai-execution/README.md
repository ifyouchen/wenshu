# wenshu AI Execution Pack

本目录把 `docs/产品文档.html`、`docs/技术文档.html`、`docs/交互设计文档.html` 整理为 AI 可连续执行的施工文档。

## 使用方式

任何 AI 接手实现时，必须按以下顺序读取：

1. `docs/ai-execution/README.md`
2. `docs/ai-execution/EXECUTION_RULES.md`
3. `docs/ai-execution/MASTER_BACKLOG.md`
4. `docs/ai-execution/API_CONTRACT.md`
5. `docs/ai-execution/ACCEPTANCE_CRITERIA.md`
6. `docs/ai-execution/PROGRESS.md`
7. `docs/ai-execution/STORAGE_COS.md`
8. `docs/ai-execution/COVERAGE_MATRIX.md`
9. `docs/ai-execution/CONTINUATION_GUIDE.md`

原始 HTML 文档作为需求源，执行时以本目录为任务拆解源。若本目录和原始文档冲突，默认以原始文档为准，并在 `PROGRESS.md` 的“决策记录”中登记修正。

例外：对象存储已经由 MinIO 调整为腾讯云 COS，后续实现必须以 `STORAGE_COS.md` 为准，覆盖原始技术文档中的 MinIO 表述。

## 项目目标

文枢 wenshu 是面向长篇创作者的 AI 写作工作台，目标是打通“写、改、编”闭环：

- 写：骨架生成、章节编辑、分段续写、卡点分支、上下文锚点。
- 改：基础校正、进阶润色、风格重塑、文风定制、一致性审查。
- 编：小说转剧本、心理外化策略、四栏剧本工作台、分集管理、导出。
- 管：账户、作品、卷章、角色库、世界观词典、导入、搜索替换、写作统计、配额。

## 当前代码基线

当前仓库已经具备后端基础骨架：

- Maven 项目名：`wenshu`
- 主包名：`com.czx.wenshu`
- Spring Boot 3 + Java 21
- DDD 四层包结构
- 统一 `Result`、`ErrorCode`、`ApiException`、`GlobalExceptionHandler`
- Flyway 初始 Schema
- `GET /api/v1/system/health`
- `docker-compose.yml` 描述 PostgreSQL + pgvector、Redis；对象存储使用腾讯云 COS

## 执行优先级

先做能闭环的数据和写作链路，再做高级 AI 和商业化：

1. 账号与认证
2. 作品、卷、章节
3. 角色库、世界观词典、快照
4. 内容导入、全文搜索替换、写作统计
5. AI 骨架、续写、润色、分支
6. 一致性审查、上下文锚点
7. 小说转剧本、分集、导出
8. 前端工程与编辑器
9. 配额、订阅、支付、数据导出

## 后续续跑

第一轮之后继续启动 AI 时，使用 `docs/ai-execution/CONTINUATION_GUIDE.md` 中的固定 prompt。每轮只做 1 到 3 个依赖已满足的任务，优先完成当前 Phase，不跨阶段混做。
