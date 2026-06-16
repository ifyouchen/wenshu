# wenshu Progress

最后更新：2026-06-16 Asia/Shanghai

## 当前阶段

阶段：`P5 AI 写作与润色`

整体状态：P5 进行中（3/10），P5-01/P5-02/P5-03 已完成。

## 阶段进度

| Phase | 状态 | 完成度 | 说明 |
| --- | --- | --- | --- |
| P0 后端基础设施 | DONE | 7/7 | 后端基础设施、测试 profile、OpenAPI 已完成 |
| P1 账号与用户 | DONE | 11/11 | P1 全部完成 |
| P2 作品、卷章与快照 | DONE | 7/7 | P2 全部完成 |
| P3 角色库与世界观词典 | DONE | 5/5 | P3 全部完成 |
| P4 导入、搜索替换、写作统计 | DONE | 9/9 | P4 全部完成 |
| P5 AI 写作与润色 | DOING | 3/10 | P5-01/P5-02/P5-03 已完成 |
| P6 一致性审查与锚点 | TODO | 0/7 | 未开始 |
| P7 小说转剧本 | TODO | 0/8 | 未开始 |
| P8 前端工作台 | TODO | 0/22 | 未开始 |
| P9 商业化与数据安全 | TODO | 0/9 | 未开始 |
| P10 生态与开放能力 | DEFERRED | 0/3 | V2.0/企业生态能力，已登记 |

## 已完成任务

- P0-01：项目命名、主包名、Java 21、Spring Boot 基础骨架。
- P0-02：DDD 四层包结构。
- P0-03：统一响应、错误码、全局异常处理。
- P0-04：应用配置、Actuator、MyBatis、Flyway、Redis、LLM 配置占位。
- P0-05：初始数据库 Schema。
- P0-06：本地开发 profile 和测试数据库策略。
- P0-07：OpenAPI/接口文档生成。
- P1-01：用户领域模型与仓储接口。
- P1-02：用户持久化 MyBatis Mapper。
- P1-03：邮箱注册。
- P1-04：邮箱验证与重发。
- P1-05：登录、失败锁定、登出。
- P1-06：Access Token + Refresh Token 轮换。
- P1-07：忘记密码与重置密码。
- P1-08：当前用户、资料、密码、AI 授权开关。

- P1-08：当前用户、资料、密码、AI 授权开关。
- P1-09：账号注销与 30 天撤销。
- P1-10：首次登录身份选择与入口偏好。
- P1-11：邮件模板与安全告警邮件。
- P2-01：Project/Volume/Chapter 领域模型。
- P2-02：作品 CRUD。
- P2-03：卷 CRUD。
- P2-04：章节 CRUD。
- P2-05：大纲树。
- P2-06：章节保存差量字数统计钩子。
- P2-07：版本快照创建、列表、恢复。
- P3-01：角色 CRUD（`/projects/{id}/characters`、`/characters/{id}` CRUD 接口）。
- P3-02：角色锁定/解锁（`PUT /characters/{id}/lock` 切换锁定状态）。
- P3-03：世界观要素 CRUD（`/projects/{id}/world-dict`、`/world-dict/{id}` CRUD 接口，支持 location/faction/item/rule 四类）。
- P3-04：专有名词词典别名支持（`aliases` 字段，List<String> API，JSON 字符串存储；Flyway V5 迁移；`WorldElement.syncName()` 为 P3-05 预备）。
- P3-05：角色名与词典同步策略（`updateCharacter` 检测名称变更后调用 `WorldElementApplicationService.syncCharacterName()`，词典中同名条目自动跟随更新）。

- P4-04：全书搜索（GET /projects/{id}/search，应用层内存搜索，支持大小写/整词选项，按章节分组返回匹配上下文）。
- P4-05：全书替换与快照保护（POST /projects/{id}/search/replace，替换前自动创建 auto_before_replace 快照）。
- P4-06：角色名联动替换（replace 接口加 syncCharacterName 标志，匹配角色名时同步更新角色档案）。
- P4-01：TXT/DOCX 导入解析预览（POST /import/parse，章节正则切分，24 小时解析会话）。
- P4-02：切分点调整与导入入库（PUT /import/{id}/adjust，POST /import/{id}/apply）。
- P4-03：粘贴文本导入（POST /import/paste，无预览步骤，直接写入章节）。

- P4-07：写作统计总览（GET /stats/writing，今日字数/目标/进度、7 日趋势、连续写作天数、365 日总字数）。
- P4-08：热力图、作品进度、月度摘要（GET /stats/writing/heatmap、/projects、/monthly/{yearMonth}）。
- P4-09：每日目标设置（PUT /user/writing-goal 全局目标；User 领域新增 dailyCharGoal 字段，作品级目标 P2 已支持）。

- P5-01：LLM 客户端抽象与降级策略（`LlmClient` 接口，`creativeLlmClient`/`utilityLlmClient` Bean，无 Key 时 `UnconfiguredLlmClient` 降级）。
- P5-02：Prompt 模板工具（`PromptTemplate` classpath 加载 + `{{var}}` 填充校验）、`JsonExtractor`（markdown 代码块/裸 JSON/前后文三场景健壮提取）、`prompts/skeleton.txt` 示例。
- P5-03：异步任务进度服务（`AsyncTask` 领域实体、`ai_task_progress` 表映射、`AsyncTaskService` 全生命周期管理、`GET /tasks/{taskId}/progress` 轮询端点）。

## 当前待办

P5-04（骨架生成）、P5-05（骨架应用）、P5-06（动态上下文组装）待实现，依赖 P5-01/P5-02/P5-03 已满足。

## 实现日志

### 2026-06-16 P5-01~P5-03

- **P5-01 LLM 客户端抽象与降级策略**：`LlmClient` 接口（`application/llm/`）；`UnconfiguredLlmClient` 未配置 Key 时抛 `ApiException(BAD_REQUEST)` 而非启动失败；`LangChain4jAnthropicLlmClient`（claude-sonnet-4-6）和 `LangChain4jDeepSeekLlmClient`（deepseek-chat，OpenAI 兼容接口 `https://api.deepseek.com/v1`）；`LlmConfig` `@Configuration` 根据 `WenshuProperties.llm.anthropicApiKey/deepseekApiKey` 是否为空选择真实客户端或降级客户端，分别注册 `@Qualifier("creativeLlmClient")` 和 `@Qualifier("utilityLlmClient")` Bean。
- **P5-02 Prompt 模板与 JSON 工具**：`PromptTemplate`（classpath 资源加载、`{{varName}}` 占位符填充、未填充时抛 `IllegalStateException`）；`JsonExtractor`（支持 markdown 代码块、裸 JSON、前缀文本三场景提取 JSON 对象/数组，容错解析返回 null 而非抛异常）；示例模板 `resources/prompts/skeleton.txt`（故事骨架生成提示词）。
- **P5-03 异步任务进度服务**：`AsyncTask` 聚合根（`domain/task/`，映射 `ai_task_progress` V1 已有表）、`AsyncTaskStatus` 枚举、`AsyncTaskRepository` 端口；MyBatis 持久化（`AsyncTaskMapper`/`AsyncTaskRecord`/`MyBatisAsyncTaskRepository`）；`AsyncTaskService`（创建/markRunning/updateProgress/complete/fail/getProgress 全生命周期）；`GET /tasks/{taskId}/progress` 端点（鉴权，userId 归属校验）；测试 schema 加 `ai_task_progress` 表。
- 全量回归 97 个测试通过（新增 11 个 LlmClientDegradationTests + 5 个 TaskControllerTests）。

### 2026-06-16 P4-07~P4-09

- **P4-07 写作统计总览**：扩展 `WritingDailyStatsRepository/Mapper` 增加 `findByUserIdAndStatDateBetween`；`WritingStatsQueryService.getOverview()` 汇总今日字数、用户全局日目标、7 日趋势、连续写作天数、365 日总字数。
- **P4-08 热力图/作品进度/月度摘要**：`getHeatmap()` 返回 365 天每日字数；`getProjectProgress()` 结合 `ProjectRepository.findByUserId` 和今日统计返回各作品进度；`getMonthlySummary(yearMonth)` 返回月总字数、活跃天数、日均及每日明细。
- **P4-09 全局每日目标**：`User` 领域新增 `dailyCharGoal` 字段（默认 2000，V1 schema 已有列）；更新 `UserRecord`、`UserMapper`（SELECT+UPDATE 含 `daily_char_goal`）、`UserInfo`、`MyBatisUserRepository`；`UserApplicationService.updateGlobalWritingGoal()`；`PUT /user/writing-goal` 端点。
- 新增 `StatsController`（4 个端点）；`WebMvcConfig` 增加 `/api/v1/stats/**` 鉴权。
- 测试 schema 用户表加 `daily_char_goal INT DEFAULT 2000` 列。
- 新增 `StatsControllerTests` 集成测试 8 个用例（总览、streak、401、热力图 365、作品进度、月度、修改目标、目标反映在总览）全部通过。
- 全量回归 81 个测试通过。

### 2026-06-16 P4-04~P4-06

- **P4-04 全书搜索**：`SearchApplicationService.searchChapters()` 应用层内存搜索，支持 `caseSensitive` 和 `wholeWord`，按章节分组返回命中上下文（各 30 字符前后缀）。
- **P4-05 全书替换**：`replaceInChapters()` 对每个命中章节先创建 `auto_before_replace` 快照，再执行 `Pattern.replaceAll()`，保留章节原 status。
- **P4-06 角色名联动替换**：`syncCharacterName` 参数，替换后同步匹配角色档案名称，返回 `characterNameSynced` 标志。
- 全量回归 73 个测试通过。

### 2026-06-16 P4-01~P4-03

- **P4-01 TXT/DOCX 导入解析预览**：
  - `pom.xml` 新增 `org.apache.poi:poi-ooxml:5.3.0`（DOCX 解析）。
  - Flyway V6 迁移：创建 `import_parse_sessions` 表（id, project_id, user_id, parsed_chapters TEXT, expires_at, created_at）。
  - 领域层：`ParsedChapterItem` record、`ImportParseSession` 聚合（24 小时 TTL、`isExpiredAt()`、`updateChapters()`）、`ImportParseSessionRepository` 接口。
  - 基础设施层：`ImportParseSessionRecord`、`ImportParseSessionMapper`（MyBatis）、`MyBatisImportParseSessionRepository`（用 Jackson ObjectMapper 序列化 chapters JSON）。
  - 应用层：`ImportApplicationService.parseFile()` 支持 TXT（自动识别 UTF-8/GBK 编码）和 DOCX（Apache POI `XWPFDocument`），章节切分正则支持中文 `第N章/节/回/卷` 和英文 `Chapter N / CHAPTER N`。
- **P4-02 切分点调整与导入入库**：
  - `PUT /import/{parseId}/adjust` + `AdjustSplitRequest` → `WorldElementApplicationService.adjustSplitPoints()`（覆写章节列表）。
  - `POST /import/{parseId}/apply` + `ApplyImportRequest(volumeId)` → `ImportApplicationService.applyImport()`（批量创建 Chapter，wordCount 自动计算）。
  - 解析会话应用后立即删除。
- **P4-03 粘贴文本导入**：
  - `POST /import/paste` + `PasteImportRequest(projectId, volumeId, text)` → 解析 + 直接入库，无预览会话。
- `WebMvcConfig` 新增 `/api/v1/import/**` 鉴权路径。
- 测试 schema 新增 `import_parse_sessions` 表。
- 新增 `ImportControllerTests` 集成测试 6 个用例（粘贴多章节、单章节、未鉴权、章节切分 2 个单元、paste wordCount）全部通过。
- 全量回归 66 个测试通过。

### 2026-06-16 P3-04~P3-05

- **P3-04 专有名词词典别名**：
  - Flyway V5 迁移：`world_elements` 表新增 `aliases TEXT NOT NULL DEFAULT '[]'`。
  - `WorldElement` 领域模型新增 `aliases` 字段（JSON 字符串，默认 `"[]"`），`update()` 方法接受 aliases 参数，新增 `syncName()` 方法。
  - `WorldElementRecord` 新增 `aliases` getter/setter。
  - `WorldElementMapper` SELECT/INSERT/UPDATE 全面加入 aliases，新增 `findByProjectIdAndName` 查询（供 P3-05 使用）。
  - `MyBatisWorldElementRepository` 实现 `findByProjectIdAndName`。
  - `WorldElementRepository` 接口新增 `findByProjectIdAndName`。
  - `CreateWorldElementCommand`、`UpdateWorldElementCommand`、`CreateWorldElementRequest`、`UpdateWorldElementRequest` 全部加 `List<String> aliases`。
  - `WorldElementApplicationService` 新增 `toJsonArray(List<String>)` 工具方法（无外部依赖），`syncCharacterName` 供 P3-05 调用。
  - `WorldElementInfo` 加 `List<String> aliases`，内含轻量 JSON 字符串解析器（无需引入 ObjectMapper）。
  - 禁止在代码中使用 `xx.xx.xx` 链式调用方式引用外部类，所有外部类在顶部 import 导入。
- **P3-05 角色名与词典同步**：
  - `CharacterApplicationService` 构造函数注入 `WorldElementApplicationService`，`updateCharacter` 在名称变更时调用 `syncCharacterName(projectId, oldName, newName)`。
  - 测试 schema 更新：`world_elements` 加 `aliases` 列。
  - 新增 3 个 `WorldElementControllerTests` 用例（aliases 创建/更新/默认空）+ 2 个 `CharacterControllerTests` 用例（P3-05 同步 + 无匹配条目静默成功）。
  - 全量回归 60 个测试通过。

### 2026-06-16 P3-01~P3-03

- 确认 P2 全部完成，进入 P3 阶段。
- 验证 commit 90b5618 已包含 P3-01/P3-02/P3-03 的完整实现。
- **P3-01 角色 CRUD**：`CharacterController`（GET/POST /projects/{id}/characters、GET/PUT/DELETE /characters/{id}）、`CharacterApplicationService`、`Character` 领域模型（含 `toggleLock` 方法）、`CharacterRepository`/`MyBatisCharacterRepository`/`CharacterMapper`/`CharacterRecord`。
- **P3-02 角色锁定/解锁**：`PUT /characters/{id}/lock` 调用 `CharacterApplicationService.toggleLock()`，锁定状态持久化到 `characters.is_locked` 列。
- **P3-03 世界观要素 CRUD**：`WorldElementController`（GET/POST /projects/{id}/world-dict、PUT/DELETE /world-dict/{id}）、`WorldElementApplicationService`、`WorldElement` 领域模型（type/name/description/locked 字段）、`WorldElementRepository`/`MyBatisWorldElementRepository`/`WorldElementMapper`/`WorldElementRecord`。
- AuthInterceptor 已保护 `/api/v1/characters/**` 和 `/api/v1/world-dict/**` 路径。
- **Bug 修复**：`CharacterMapper` 和 `WorldElementMapper` 的 SELECT 中 `is_locked` 列因 MyBatis `mapUnderscoreToCamelCase` 映射到 `isLocked` 属性，但 Record 的 setter 为 `setLocked()`，导致读取时 locked 始终为 false。修复：在 SELECT 中加别名 `is_locked AS locked`，确保正确映射到 `setLocked()` 方法。
- 新增 `CharacterControllerTests` 集成测试 7 个用例（P3-01: 5 个，P3-02: 2 个，全部覆盖创建、列表、详情、更新、删除、锁定/解锁、未鉴权 401）。
- 新增 `WorldElementControllerTests` 集成测试 6 个用例（P3-03: 创建、列表、更新、删除、四类 location/faction/item/rule 验证、未鉴权 401）。
- 全量回归 55 个测试通过。

### 2026-06-16

- 完成 P1-08：当前用户、资料、密码、AI 授权开关。
- 新增 Access Token 持久化和鉴权拦截器：`AccessToken` 领域对象、`AccessTokenRepository` 端口、MyBatis 持久化、Flyway V3 迁移创建 `access_tokens` 表。
- 新增 `OpaqueAuthTokenService.resolveAccessToken()` 方法，解析并验证 Access Token 对应的用户。
- 新增 `AuthInterceptor` 拦截 `/api/v1/user/**` 路径，从 `Authorization: Bearer` 头解析当前用户。
- 新增 `CurrentUserProvider` 组件，从请求上下文获取当前认证用户。
- 新增 `WebMvcConfig` 注册拦截器。
- 新增 `PUT /api/v1/user/ai-consent` 接口和 `UpdateAiConsentCommand`。
- 新增 `UserApplicationService` 处理当前用户、资料更新、密码修改、AI 授权开关。
- 新增 `UserController` 提供 4 个接口：`GET /user/me`、`PUT /user/profile`、`PUT /user/password`、`PUT /user/ai-consent`。
- `User` 领域对象新增 `updateProfile()`、`updateAiConsent()`、`changePasswordByUser()` 方法和 `avatarUrl` 字段。
- 修改密码后吊销所有 Access Token 和 Refresh Token，用户需重新登录。
- `UserRecord`、`UserMapper`、`MyBatisUserRepository` 同步增加 `avatarUrl` 字段。
- 测试 schema 同步增加 `access_tokens` 表和 `avatar_url` 字段。
- 新增 `UserControllerTests` 集成测试 9 个用例，全部通过。
- 全量回归 25 个测试通过。

### 2026-06-15

- 开始实现 P1-07：忘记密码与重置密码。
- 完成 P1-07：新增忘记密码和重置密码接口，密码重置 token 24 小时有效，重置成功后吊销该用户所有 Refresh Token，旧密码与旧 Refresh Token 均失效。
- 同步 P1-07 验收标准：忘记密码不泄露邮箱存在性，重置 token 24 小时有效且一次性使用，重置后所有设备退出。
- 完成 P1-04：注册时写入 24 小时邮箱验证 token；新增 `GET /api/v1/auth/verify-email` 与 `POST /api/v1/auth/resend-verify`；验证成功后更新 `isEmailVerified=true`，重发验证邮件 60 秒限流。
- 完成 P1-05：新增 `POST /api/v1/auth/login` 和 `POST /api/v1/auth/logout`；登录失败次数持久化，连续 5 次失败锁定 15 分钟；业务失败不回滚失败计数。
- 完成 P1-06：新增 `refresh_tokens` Flyway 迁移、RefreshToken 领域模型与仓储端口、MyBatis 持久化；注册/登录持久化 Refresh Token 哈希；新增 `POST /api/v1/auth/refresh`，刷新时旧 Refresh Token 立即吊销，新双 Token 生效。
- 完成 P1-01：新增 User 聚合、EmailAddress 值对象、身份类型枚举、UserRepository 端口和注册邮箱唯一性策略；单元测试覆盖邮箱唯一性和软删除/恢复状态。
- 完成 P1-02：新增 UserMapper、UserRecord、MyBatisUserRepository，支持按 ID/邮箱查询、邮箱存在性检查、插入和更新；测试覆盖 H2 下的持久化与领域对象还原。
- 完成 P1-03：新增 AuthApplicationService、注册命令/结果、OpaqueAuthTokenService、BCrypt 密码哈希配置、`POST /api/v1/auth/register` 接口和响应 DTO；注册返回 Access Token、Refresh Token 与未验证用户信息，重复邮箱返回统一错误响应。
- 完成 P0-06：新增 `local` profile 用于 PostgreSQL/Redis 联调，新增 `test` profile 使用 H2 内存库并关闭 Flyway/Redis 仓储，测试类统一激活 `test` profile。
- 完成 P0-07：新增 Springdoc OpenAPI WebMVC UI，配置 `/v3/api-docs` 和 `/swagger-ui.html`，补充 OpenAPI 元信息、System 接口注解和端点集成测试。
- 分析三份 HTML 原始文档，确认产品为"文枢 wenshu"。
- 将 Maven 坐标从 `novel-ai` 调整为 `wenshu`。
- 将主包名从 `com.czx.novelai` 调整为 `com.czx.wenshu`。
- 新增 DDD 四层包结构。
- 新增统一结果、错误码、业务异常、全局异常处理。
- 新增 `WenshuProperties` 配置绑定。
- 新增 `/api/v1/system/health`。
- 新增 Flyway 初始迁移，覆盖用户、项目、卷章、快照、角色、世界观、剧本、配额、任务、写作统计、邮箱验证、密码重置、pgvector 嵌入表。
- 新增 `docker-compose.yml`，描述 PostgreSQL + pgvector、Redis；对象存储后续使用腾讯云 COS。
- 新增 `README.md` 和 `docs/PROJECT_ANALYSIS.md`。
- 使用 `F:\jdk21` 执行 `mvn test`，结果通过。
- 整理本目录 AI 可执行文档和进度台账。
- 审计三份原始文档与 `ai-execution` 覆盖关系，补充缺口任务：身份选择、邮件模板、命令面板、Toast、异常 UI、渐进式引导、订阅升级、账户设置、快捷键、前端性能、团队协作、版权隐私、体验额度、生态开放能力。
- 新增覆盖矩阵 `docs/ai-execution/COVERAGE_MATRIX.md`，逐章标注产品文档、技术文档、交互设计文档的覆盖位置。
- 新增后续续跑指南 `docs/ai-execution/CONTINUATION_GUIDE.md`，固化第一轮之后的启动 prompt、阶段切换规则、每轮任务粒度和验收命令。
- 更新 `EXECUTION_RULES.md`，明确每轮只做 1 到 3 个同阶段任务，并按 P0 到 P5 的阶段顺序推进。
- 完成全局品牌名调整为 `wenshu / Wenshu / 文枢`；同步 Maven 坐标、Java 包名、配置前缀、Docker 服务名、文档和启动 prompt。

## 验证记录

| 时间 | 命令 | 结果 | 备注 |
| --- | --- | --- | --- |
| 2026-06-15 | `$env:JAVA_HOME='F:\jdk21'; mvn test` | PASS | 1 个 Spring 上下文测试通过 |
| 2026-06-15 | `$env:JAVA_HOME='F:\jdk21'; mvn test` | PASS | COS SDK、覆盖文档补充后回归通过 |
| 2026-06-15 | `$env:JAVA_HOME='F:\jdk21'; mvn test` | PASS | 后续续跑指南和执行规则更新后回归通过 |
| 2026-06-15 | `$env:JAVA_HOME='F:\jdk21'; mvn test` | PASS | 品牌名改为 wenshu/文枢 后回归通过 |
| 2026-06-15 | `$env:JAVA_HOME='F:\jdk21'; mvn test` | PASS | P0-06：test profile 使用 H2 内存库，无外部数据库依赖 |
| 2026-06-15 | `$env:JAVA_HOME='F:\jdk21'; mvn test` | PASS | P0-07：`/v3/api-docs` 集成测试通过，2 个测试通过 |
| 2026-06-15 | `$env:JAVA_HOME='F:\jdk21'; mvn test` | PASS | P1-01：邮箱唯一性与软删除状态单元测试通过，4 个测试通过 |
| 2026-06-15 | `$env:JAVA_HOME='F:\jdk21'; mvn test` | PASS | P1-02：UserMapper 按 ID/邮箱查询测试通过，6 个测试通过 |
| 2026-06-15 | `$env:JAVA_HOME='F:\jdk21'; mvn test` | PASS | P1-03：注册接口、重复邮箱、Mapper 和 OpenAPI 回归通过，8 个测试通过 |
| 2026-06-15 | `$env:JAVA_HOME='F:\jdk21'; mvn test` | PASS | P1-04：邮箱验证、重发限流、注册 token 写入与回归测试通过，10 个测试通过 |
| 2026-06-15 | `$env:JAVA_HOME='F:\jdk21'; mvn test` | PASS | P1-05：登录成功、5 次失败锁定、登出与回归测试通过，13 个测试通过 |
| 2026-06-15 | `$env:JAVA_HOME='F:\jdk21'; mvn test` | PASS | P1-06：Refresh Token 轮换、旧 token 失效和回归测试通过，14 个测试通过 |
| 2026-06-15 | `$env:JAVA_HOME='F:\jdk21'; mvn test` | PASS | P1-07：忘记密码、重置密码、重置后吊销全部 Refresh Token 与回归测试通过，16 个测试通过 |
| 2026-06-16 | `$env:JAVA_HOME="F:\jdk21"; mvn test` | PASS | P1-08：`/user/me`、`/user/profile`、`/user/password`、`/user/ai-consent` 接口与鉴权拦截器测试通过，25 个测试通过 |
| 2026-06-16 | `$env:JAVA_HOME="F:\jdk21"; mvn test` | PASS | P1-09/P1-10/P1-11：`/user` DELETE、`/user/cancel-restore`、`/user/identity-type`、邮件模板与安全告警，30 个测试通过 |
| 2026-06-16 | `$env:JAVA_HOME="F:\jdk21"; mvn test` | PASS | P2-01~P2-04：Project/Volume/Chapter 领域模型与 CRUD，39 个测试通过 |
| 2026-06-16 | `$env:JAVA_HOME="F:\jdk21"; mvn test` | PASS | P2-05~P2-07：大纲树、差量字数统计、版本快照，42 个测试通过 |
| 2026-06-16 | `JAVA_HOME=corretto-21.0.11; mvn test` | PASS | P3-01/P3-02/P3-03：角色 CRUD、锁定/解锁、世界观要素 CRUD，55 个测试通过 |
| 2026-06-16 | `JAVA_HOME=corretto-21.0.11; mvn test` | PASS | P3-04/P3-05：词典别名、角色名同步，60 个测试通过 |
| 2026-06-16 | `JAVA_HOME=corretto-21.0.11; mvn test` | PASS | P4-01/P4-02/P4-03：TXT/DOCX 导入、切分调整入库、粘贴导入，66 个测试通过 |
| 2026-06-16 | `JAVA_HOME=corretto-21.0.11; mvn test` | PASS | P4-04/P4-05/P4-06：全书搜索、替换+快照、角色名联动，73 个测试通过 |
| 2026-06-16 | `JAVA_HOME=corretto-21.0.11; mvn test` | PASS | P4-07/P4-08/P4-09：写作统计总览、热力图/月度、每日目标，81 个测试通过 |
| 2026-06-16 | `JAVA_HOME=corretto-21.0.11; mvn test` | PASS | P5-01/P5-02/P5-03：LLM 降级策略、Prompt 模板、JsonExtractor、异步任务进度，97 个测试通过 |

## 阻塞记录

- 当前系统 PATH 默认 Java 8，直接执行 `mvn test` 会失败。
  - Windows 设置：`$env:JAVA_HOME='F:\jdk21'`
  - macOS（当前机器）设置：`export JAVA_HOME=/Users/chenzhixia/Library/Java/JavaVirtualMachines/corretto-21.0.11/Contents/Home`
- 当前机器未识别 `docker` 命令，暂未进行 PostgreSQL/Redis 联调；COS 需真实腾讯云凭证后联调。

## 决策记录

- 使用 Java 21，符合原技术文档要求。
- 后端先行落地，前端作为 P8 独立阶段。
- 初始 Schema 一次性覆盖文档中的核心表，后续变化继续通过 Flyway 增量迁移。
- LLM 能力需要可降级，缺少 Key 不影响应用启动。
- 对象存储从原技术文档中的 MinIO 改为腾讯云 COS。后续实现以 `docs/ai-execution/STORAGE_COS.md` 为准，使用 `com.qcloud:cos_api` SDK，不再维护本地 MinIO 服务。
- Spring Boot 3.5.x 使用 Springdoc 2.8.x 生成 OpenAPI 文档，本项目固定 `springdoc-openapi-starter-webmvc-ui` 为 `2.8.17`。
- P1-06 已落地 Refresh Token 哈希持久化和轮换吊销；Access Token 暂为短期不透明 token，鉴权解析与当前用户上下文在 `P1-08` 继续完善。
- P1-08 已落地 Access Token 持久化（`access_tokens` 表）和 `AuthInterceptor` 鉴权拦截器；`OpaqueAuthTokenService` 同时持久化 Access Token 和 Refresh Token 哈希。
- P1-09 账号注销返回 30 天恢复令牌（`account_restore_tokens` 表），恢复端点不需要 Bearer 认证，使用独立恢复令牌。
- P1-11 邮件模板使用 Thymeleaf HTML，SMTP 发送通过 `wenshu.mail.enabled` 配置开关控制，默认日志回退。

### 2026-06-15 存储切换

- 移除 `docker-compose.yml` 中的 MinIO 服务和 volume。
- `application.yaml` 的 `wenshu.storage` 改为 COS 配置：provider、region、bucket、secretId、secretKey、customDomain、presignedUrlTtlMinutes。
- `pom.xml` 新增腾讯云 COS XML Java SDK：`com.qcloud:cos_api`。
- 为避免与 Spring `spring-jcl` 冲突，排除 COS SDK 传递依赖 `commons-logging`。
- 新增 COS 执行说明：`docs/ai-execution/STORAGE_COS.md`。

### 2026-06-16 P1-08

- Access Token 从纯内存不透明 token 改为数据库持久化吊销模型，注册和登录时写入 `access_tokens` 表。
- `OpaqueAuthTokenService.issueFor()` 现在同时持久化 Access Token 哈希和 Refresh Token 哈希。
- 新增 `AuthInterceptor` 拦截 `/api/v1/user/**`，从 `Authorization: Bearer` 头解析当前用户。
- 修改密码后吊销所有 Access Token 和 Refresh Token，强制全部设备重新登录。
- `User` 领域对象新增 `avatarUrl` 字段和 `updateProfile()`、`updateAiConsent()`、`changePasswordByUser()` 方法。

### 2026-06-16 P1-09

- 完成 P1-09：账号注销与 30 天撤销。
- 新增 `AccountRestoreToken` 领域对象与仓储端口、MyBatis 持久化、Flyway V4 迁移创建 `account_restore_tokens` 表。
- `DELETE /api/v1/user` 软删除账号并吊销所有 Token，返回 30 天有效的恢复令牌。
- `POST /api/v1/user/cancel-restore` 使用恢复令牌撤销注销并恢复账号。
- 注销后恢复令牌一次性使用，恢复成功后发送安全告警邮件。
- `AuthInterceptor` 排除 `/user/cancel-restore` 路径，因恢复端点不需要 Bearer 认证。

### 2026-06-16 P1-10

- 完成 P1-10：首次登录身份选择与入口偏好。
- 新增 `PUT /api/v1/user/identity-type` 接口，支持设置 `web_novel_author`、`short_drama_writer`、`new_author` 三种身份类型。
- IdentityType 枚举已存在于 P1-01，本任务补充独立接口和更新逻辑。

### 2026-06-16 P1-11

- 完成 P1-11：邮件模板与安全告警邮件。
- 新增三套 Thymeleaf HTML 邮件模板：`verify-email`、`reset-password`、`security-alert`。
- 新增 `EmailService` 接口和 `SmtpEmailService` 实现（异步发送，`@ConditionalOnProperty(wenshu.mail.enabled=true)`）。
- 新增 `LoggingMailConfig`（默认回退），SMTP 未启用时邮件仅记录日志。
- 新增 `SmtpMailConfig`（`wenshu.mail.enabled=true` 时激活），使用 `JavaMailSender` 真实发送。
- 新增 `SecurityAlertEmailSender` 接口和委托实现，密码修改、账号恢复时发送安全告警邮件。
- `WenshuProperties` 新增 `baseUrl` 和 `mail.from` 配置。
- 修改密码、账号恢复操作后触发安全告警邮件。
- 全量回归 30 个测试通过。

### 2026-06-16 P2-01~P2-04

- 完成 P2-01：Project/Volume/Chapter 领域模型。
  - `Project` 聚合根：id、userId、title、genre、synopsis、worldview、totalWords、dailyCharGoal、status（draft/deleted）。
  - `Volume` 实体：id、projectId、title、conflict、sortOrder。
  - `Chapter` 实体：id、volumeId、projectId、title、outline、content、wordCount、sortOrder、status（pending/draft/completed）。
  - `ChapterStatus` 和 `ProjectStatus` 枚举。
  - 领域模型含聚合规则校验：标题必填且 200 字限制、资源归属校验。
- 完成 P2-02：作品 CRUD。
  - `GET /api/v1/projects` 作品列表（按 updatedAt 降序）。
  - `POST /api/v1/projects` 创建作品。
  - `GET /api/v1/projects/{id}` 作品详情。
  - `PUT /api/v1/projects/{id}` 更新作品。
  - `DELETE /api/v1/projects/{id}?confirm=true` 删除作品。
  - 所有端口鉴权，校验 userId 归属。
- 完成 P2-03：卷 CRUD。
  - `POST /api/v1/projects/{id}/volumes` 新增卷。
  - `PUT /api/v1/volumes/{id}` 更新卷。
  - `DELETE /api/v1/volumes/{id}?confirm=true` 删除卷（级联删除章节）。
- 完成 P2-04：章节 CRUD。
  - `POST /api/v1/volumes/{id}/chapters` 新增章节。
  - `GET /api/v1/chapters/{id}` 章节详情。
  - `PUT /api/v1/chapters/{id}` 保存章节内容、标题、状态、大纲，自动计算 wordCount。
  - `DELETE /api/v1/chapters/{id}` 删除章节。
- AuthInterceptor 扩展保护 `/api/v1/projects/**`、`/api/v1/volumes/**`、`/api/v1/chapters/**`。
- 测试 schema 新增 projects、volumes、chapters 表。
- 新增 `ProjectControllerTests` 集成测试 9 个用例，全部通过。
- 全量回归 39 个测试通过。

### 2026-06-16 P4-01~P4-03 导入设计决策

- 解析会话使用 PostgreSQL 表（`import_parse_sessions`）而非 Redis 存储，原因：测试 profile 禁用 Redis，DB 方案可无缝支持 H2 测试。TTL 由应用层 `isExpiredAt()` 判断，无后台清理进程（生产环境可加定时任务清理过期会话）。
- TXT 编码自动识别：先尝试 UTF-8，若有 replacement character（`�`）则退回 GBK，覆盖大多数中文小说 TXT 文件格式。
- 章节标题正则选取行首 `第N章/节/回/卷/集/部/篇` 模式，匹配任意数字（含汉字数）；英文支持 `Chapter N`/`CHAPTER N`。无法识别章节时退回"全文一章"模式。
- `pasteImport` 不走会话存储，直接解析并入库，简化快速导入流程。

### 2026-06-16 P3-04 aliases 设计决策

- `aliases` 存储为 TEXT 列，值为 JSON 数组字符串（`"[]"` 表示无别名）。
- API 层使用 `List<String>` 方便客户端直接传递，应用层通过轻量工具方法 `toJsonArray()` 序列化；Info DTO 内置简单 JSON 字符串解析器，整体零外部依赖。
- P3-05 同步策略基于"名称精确匹配"——仅对 `name` 字段精确相等的词典条目做同步；若无匹配条目则静默跳过，不报错、不创建新条目。

### 2026-06-16 P3-01~P3-03 Mapper is_locked 映射修复

- `CharacterMapper` 和 `WorldElementMapper` 所有 SELECT 查询中，`is_locked` 列加 `AS locked` 别名。
- 根因：MyBatis `mapUnderscoreToCamelCase=true` 将 `is_locked` 映射到属性 `isLocked`，而 Record 类的 setter 命名为 `setLocked()`；MyBatis 找不到 `setIsLocked()` 时静默跳过，导致 boolean 字段保持 Java 默认值 `false`，锁定状态无法正确读取。
- 修复方式：在 SQL 层加别名，令 MyBatis 直接按列名 `locked` 查找 `setLocked()` setter。同样适用于 P2 已有的其它 `is_*` 布尔列（如 `is_email_verified`、`is_deleted` 等），已确认 UserMapper 等已通过别名方式正确处理。

### 2026-06-16 P2-05~P2-07

- 完成 P2-05：大纲树。
  - `GET /api/v1/projects/{id}/outline` 返回卷章大纲树，包含卷下所有章节的标题、大纲、字数、状态。
  - 新增 `OutlineInfo`、`VolumeNode`、`ChapterNode` DTO。
  - `ProjectApplicationService.getOutline()` 一次性查询所有卷和章节，按 volumeId 分组。
- 完成 P2-06：章节保存差量字数统计钩子。
  - 新增 `WritingDailyStats` 领域对象和 `WritingDailyStatsRepository` 端口。
  - 新增 `WritingStatsService.recordManualDelta()` 在章节保存时记录差量字数到 `writing_daily_stats` 表。
  - `ProjectApplicationService.updateChapter()` 在保存内容后调用 `writingStatsService.recordManualDelta()`。
  - `Chapter.wordCountDelta()` 计算新旧字数差值。
  - 新增 `PUT /api/v1/projects/{id}/writing-goal` 设置作品每日目标。
  - 测试 schema 新增 `writing_daily_stats` 表。
- 完成 P2-07：版本快照创建、列表、恢复。
  - 新增 `ChapterSnapshot` 领域对象和 `ChapterSnapshotRepository` 端口。
  - 新增 `ChapterSnapshotRecord`、`ChapterSnapshotMapper`、`MyBatisChapterSnapshotRepository` 持久化。
  - `GET /api/v1/chapters/{id}/snapshots` 快照列表。
  - `POST /api/v1/chapters/{id}/snapshots` 手动创建快照。
  - `POST /api/v1/snapshots/{id}/restore` 恢复快照（恢复前自动创建当前状态快照）。
  - 恢复快照时同时更新差量字数统计。
  - `ChapterInfo` 新增 `content` 字段。
  - `AuthInterceptor` 扩展保护 `/api/v1/snapshots/**`。
  - 测试 schema 新增 `chapter_snapshots` 表。
  - 新增 `SnapshotInfo` DTO 和 `CreateSnapshotRequest` 请求 DTO。
  - 新增 3 个集成测试（大纲树、写作目标、快照创建与恢复），全量 42 个测试通过。