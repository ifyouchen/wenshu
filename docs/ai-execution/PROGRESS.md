# wenshu Progress

最后更新：2026-06-16 Asia/Shanghai

## 当前阶段

阶段：`P1 账号与用户`

整体状态：P1-08 已完成，下一步实现 `P1-09 账号注销与 30 天撤销`。

## 阶段进度

| Phase | 状态 | 完成度 | 说明 |
| --- | --- | --- | --- |
| P0 后端基础设施 | DONE | 7/7 | 后端基础设施、测试 profile、OpenAPI 已完成 |
| P1 账号与用户 | DOING | 8/11 | P1-08 已完成，下一步 P1-09 账号注销与 30 天撤销 |
| P2 作品、卷章与快照 | TODO | 0/7 | 未开始 |
| P3 角色库与世界观词典 | TODO | 0/5 | 未开始 |
| P4 导入、搜索替换、写作统计 | TODO | 0/9 | 未开始 |
| P5 AI 写作与润色 | TODO | 0/10 | 未开始 |
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

## 当前待办

下一步领取 `P1-09 账号注销与 30 天撤销`。

## 实现日志

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
| 2026-06-16 | `$env:JAVA_HOME='F:\jdk21'; mvn test` | PASS | P1-08：`/user/me`、`/user/profile`、`/user/password`、`/user/ai-consent` 接口与鉴权拦截器测试通过，25 个测试通过 |

## 阻塞记录

- 当前系统 PATH 默认 Java 8，直接执行 `mvn test` 会失败。需先设置 `JAVA_HOME=F:\jdk21`。
- 当前机器未识别 `docker` 命令，暂未进行 PostgreSQL/Redis 联调；COS 需真实腾讯云凭证后联调。

## 决策记录

- 使用 Java 21，符合原技术文档要求。
- 后端先行落地，前端作为 P8 独立阶段。
- 初始 Schema 一次性覆盖文档中的核心表，后续变化继续通过 Flyway 增量迁移。
- LLM 能力需要可降级，缺少 Key 不影响应用启动。
- 对象存储从原技术文档中的 MinIO 改为腾讯云 COS。后续实现以 `docs/ai-execution/STORAGE_COS.md` 为准，使用 `com.qcloud:cos_api` SDK，不再维护本地 MinIO 服务。
- Spring Boot 3.5.x 使用 Springdoc 2.8.x 生成 OpenAPI 文档，本项目固定 `springdoc-openapi-starter-webmvc-ui` 为 `2.8.17`。
- P1-06 已落地 Refresh Token 哈希持久化和轮换吊销；Access Token 暂为短期不透明 token，鉴权解析与当前用户上下文在 `P1-08` 继续完善。

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