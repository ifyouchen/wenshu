# AI 执行规则

## 每轮实现流程

AI 每次实现必须遵循：

1. 读取 `PROGRESS.md`，确认当前阶段和正在进行的任务。
2. 从 `MASTER_BACKLOG.md` 选取一个未完成任务，优先选择依赖已满足且优先级最高的任务。
3. 每轮只做 1 到 3 个任务，且默认只做同一 Phase 内的任务。
4. 实现前将任务状态改为 `DOING`，并在 `PROGRESS.md` 增加开始记录。
5. 实现代码、迁移、测试和必要文档。
6. 运行对应验证命令。
7. 验证通过后将任务状态改为 `DONE`，登记完成记录。
8. 验证失败且无法立即修复时标记 `BLOCKED`，写明阻塞原因和下一步。

## 阶段切换规则

- P0 全部 `DONE` 后，进入 P1 账号与用户。
- P1 全部 `DONE` 后，进入 P2 作品、卷章与快照。
- P2 全部 `DONE` 后，进入 P3 角色库与世界观词典。
- P3 全部 `DONE` 后，进入 P4 导入、搜索替换、写作统计。
- P4 全部 `DONE` 后，再进入 P5 AI 写作与润色。
- P8 前端工作台建议等 P1-P4 后端主链路稳定后启动。
- P10 为后续生态能力，默认不进入 V1 实现。

若当前 Phase 中存在 `BLOCKED` 任务，AI 只能在同 Phase 内选择不依赖该阻塞项的任务；否则停止并说明阻塞。

## 状态约定

- `TODO`：尚未开始。
- `DOING`：当前正在实现。
- `DONE`：已实现并通过最低验证。
- `BLOCKED`：存在外部依赖或技术阻塞。
- `DEFERRED`：主动推迟，不属于当前阶段。

## 代码约束

- 后端遵循 DDD 四层：`interfaces`、`application`、`domain`、`infrastructure`。
- `interfaces` 只做 HTTP、DTO、校验和响应转换。
- `application` 负责编排、事务边界、事件发布。
- `domain` 不依赖 Spring，不直接访问数据库。
- `infrastructure` 负责 MyBatis、Redis、腾讯云 COS、LLM、邮件、支付等适配。
- 所有 HTTP 返回统一使用 `Result<T>`。
- 所有接口统一前缀 `/api/v1`。
- 所有业务异常使用 `ApiException` 或其后续子类。
- 数据库变更必须走 Flyway，禁止手动改库后不留迁移。
- 涉及 LLM 的功能必须可配置降级，缺少 API Key 时不能导致应用启动失败。
- 涉及对象存储的功能必须使用腾讯云 COS。禁止新增 MinIO 依赖、配置或本地 MinIO 服务。

## 前端约束

- 前端技术栈：Vue 3 + Vite + TypeScript + Naive UI + Pinia + TipTap。
- 编辑器任意时刻只加载当前章节，不加载全卷。
- SSE token 写入 TipTap 必须 RAF 批量处理。
- 配额 Store 是全局单例，AI 操作完成后刷新。
- Token 刷新必须有队列锁，避免并发刷新风暴。
- 移动端保留核心写作，复杂剧本工作台提示 PC 使用。

## 进度记录要求

每次修改代码后，必须同步更新：

- `MASTER_BACKLOG.md`：任务状态。
- `PROGRESS.md`：实现日志、验证结果、阻塞或决策。

若新增重要技术决策，必须写入 `PROGRESS.md` 的“决策记录”。

## 后续启动方式

第一轮之后继续启动 AI 实现时，使用 `CONTINUATION_GUIDE.md` 中的固定 prompt，避免重新解释项目或跨阶段自由挑选任务。
