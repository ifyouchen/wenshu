# 文枢 wenshu 文档分析与开项记录

## 文档结论

三份文档定义的是同一个产品：面向长篇网文作者的一站式 AI 创作平台。产品差异化不是单纯续写，而是“写、改、编”闭环：可控长篇续写、分级润色和小说转剧本。

## 产品范围

V1 应优先形成创作闭环：

- 账号与项目管理：邮箱注册登录、作品列表、配额展示、数据安全。
- 小说创作：骨架生成、章节编辑、分段续写、卡点分支、上下文锚点。
- 小说修改：三级润色、文风定制、一致性审查。
- 剧本改编：场景切分、心理外化策略、剧本工作台、导出。
- 辅助能力：内容导入、角色库、世界观词典、全文搜索替换、写作统计。

## 技术路线

技术文档要求后端采用 Java 生态和 DDD 分层：

- 接口层 `interfaces`：Controller、DTO、参数校验、统一响应。
- 应用层 `application`：用例编排、事务边界、事件发布。
- 领域层 `domain`：核心业务规则和仓储接口，不依赖 Spring。
- 基础设施层 `infrastructure`：数据库、Redis、腾讯云 COS、LLM 适配器。

基础设施选型：

- Spring Boot + Spring MVC
- PostgreSQL 16 + pgvector
- Redis 7
- 腾讯云 COS
- Flyway
- MyBatis
- LangChain4j
- Micrometer + Actuator + Prometheus

## 交互约束

交互文档强调编辑器体验：

- 编辑器按章节加载，不能一次加载全卷。
- SSE token 需要前端 RAF 批量写入，避免频繁 ProseMirror transaction。
- AI 生成内容必须有独立标识、接受、重写、编辑和快照。
- 移动端只保留核心写作能力，复杂剧本工作台提示 PC 使用。
- 配额消耗必须在操作前预估、操作后刷新。

## 本次开项落地

本次先搭后端项目底座，避免在没有基础设施的情况下直接铺业务代码：

- Maven 坐标切换为 `wenshu`。
- 主包名切换为 `com.czx.wenshu`。
- 新增 DDD 四层包结构。
- 新增统一 `Result`、`ErrorCode`、`ApiException`、`GlobalExceptionHandler`。
- 新增 `WenshuProperties` 配置绑定。
- 新增 `/api/v1/system/health` 系统探针。
- 新增 Flyway 初始 Schema，覆盖用户、作品、卷章、角色、世界观、剧本、配额、任务、写作统计、向量嵌入等核心表。
- 新增 `docker-compose.yml`，提供 PostgreSQL + pgvector、Redis；对象存储使用腾讯云 COS。

## 建议开发顺序

1. 账号体系：邮箱注册、登录、刷新 Token、邮箱验证、密码重置。
2. 项目与章节：作品 CRUD、卷章 CRUD、大纲树、章节保存。
3. 角色库与世界观词典：为后续上下文锚点打底。
4. AI 续写 SSE：先打通最小可用链路，再接配额和审计。
5. 写作统计与全文搜索：与章节保存同步落地。
6. 剧本改编工作台：在章节/角色/LLM 链路稳定后实现。
