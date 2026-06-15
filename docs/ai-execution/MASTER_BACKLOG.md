# wenshu Master Backlog

状态枚举：`TODO`、`DOING`、`DONE`、`BLOCKED`、`DEFERRED`。

## P0 后端基础设施

| ID | 状态 | 任务 | 依赖 | 验收 |
| --- | --- | --- | --- | --- |
| P0-01 | DONE | 项目命名、主包名、Java 21、Spring Boot 基础骨架 | 无 | `mvn test` 通过 |
| P0-02 | DONE | DDD 四层包结构 | P0-01 | 存在 `interfaces/application/domain/infrastructure` |
| P0-03 | DONE | 统一响应、错误码、全局异常处理 | P0-01 | Controller 可返回 `Result<T>` |
| P0-04 | DONE | 应用配置、Actuator、MyBatis、Flyway、Redis、LLM 配置占位 | P0-01 | 配置可绑定，测试上下文启动 |
| P0-05 | DONE | 初始数据库 Schema | P0-04 | Flyway 脚本覆盖核心表 |
| P0-06 | DONE | 本地开发 profile 和测试数据库策略 | P0-04 | 无外部数据库也能运行单元测试 |
| P0-07 | DONE | OpenAPI/接口文档生成 | P0-03 | `/v3/api-docs` 或等价接口可访问 |

## P1 账号与用户

| ID | 状态 | 任务 | 依赖 | 验收 |
| --- | --- | --- | --- | --- |
| P1-01 | DONE | 用户领域模型与仓储接口 | P0 | 单元测试覆盖邮箱唯一性与软删除状态 |
| P1-02 | DONE | 用户持久化 MyBatis Mapper | P1-01 | 可按邮箱、ID 查询用户 |
| P1-03 | DONE | 邮箱注册 | P1-02 | `POST /api/v1/auth/register` 返回双 Token 和用户信息 |
| P1-04 | DONE | 邮箱验证与重发 | P1-03 | token 24 小时有效，验证后 AI 功能解锁 |
| P1-05 | DONE | 登录、失败锁定、登出 | P1-03 | 连续 5 次失败锁定 15 分钟 |
| P1-06 | DONE | Access Token + Refresh Token 轮换 | P1-05 | 旧 Refresh Token 失效，新 Token 生效 |
| P1-07 | DONE | 忘记密码与重置密码 | P1-03 | 重置后吊销所有 Refresh Token |
| P1-08 | DONE | 当前用户、资料、密码、AI 授权开关 | P1-06 | `/user/me` 和设置接口可用 |
| P1-09 | DONE | 账号注销与 30 天撤销 | P1-06 | 注销为软删除，撤销可恢复 |
| P1-10 | DONE | 首次登录身份选择与入口偏好 | P1-08 | 网文作者/短剧编剧/新人作者影响入口排序和引导文案 |
| P1-11 | DONE | 邮件模板与安全告警邮件 | P1-03 | verify/reset-password/security-alert 三套 HTML 模板可发送 |

## P2 作品、卷章与快照

| ID | 状态 | 任务 | 依赖 | 验收 |
| --- | --- | --- | --- | --- |
| P2-01 | DONE | Project/Volume/Chapter 领域模型 | P1 | 聚合规则清晰，章节归属校验 |
| P2-02 | DONE | 作品 CRUD | P2-01 | `/projects` 列表、详情、创建、更新、删除 |
| P2-03 | DONE | 卷 CRUD | P2-02 | `/projects/{id}/volumes`、`/volumes/{id}` |
| P2-04 | DONE | 章节 CRUD | P2-03 | `/volumes/{id}/chapters`、`/chapters/{id}` |
| P2-05 | TODO | 大纲树 | P2-04 | `/projects/{id}/outline` 返回卷章树 |
| P2-06 | TODO | 章节保存差量字数统计钩子 | P2-04 | 保存章节时计算字数变化 |
| P2-07 | TODO | 版本快照创建、列表、恢复 | P2-04 | AI 大改/替换前可自动快照 |

## P3 角色库与世界观词典

| ID | 状态 | 任务 | 依赖 | 验收 |
| --- | --- | --- | --- | --- |
| P3-01 | TODO | 角色 CRUD | P2 | `/projects/{id}/characters` 可用 |
| P3-02 | TODO | 角色锁定/解锁 | P3-01 | 锁定角色优先进入上下文 |
| P3-03 | TODO | 世界观要素 CRUD | P2 | 可管理地点、势力、道具、规则 |
| P3-04 | TODO | 专有名词词典 CRUD | P2 | 支持别名、类型、定义、锁定 |
| P3-05 | TODO | 角色名与词典同步策略 | P3-01,P3-04 | 角色名更新时词典一致 |

## P4 导入、搜索替换、写作统计

| ID | 状态 | 任务 | 依赖 | 验收 |
| --- | --- | --- | --- | --- |
| P4-01 | TODO | TXT/DOCX 导入解析预览 | P2 | `/import/parse` 返回章节切分预览 |
| P4-02 | TODO | 切分点调整与导入入库 | P4-01 | `/import/{parseId}/apply` 写入章节 |
| P4-03 | TODO | 粘贴文本导入 | P2 | `/import/paste` 可直接导入 |
| P4-04 | TODO | 全书搜索 | P2 | `/projects/{id}/search` 分章节返回结果 |
| P4-05 | TODO | 全书替换与快照保护 | P4-04,P2-07 | 替换前自动快照 |
| P4-06 | TODO | 角色名联动替换 | P4-05,P3 | 替换角色名时可同步档案 |
| P4-07 | TODO | 写作统计总览 | P2-06 | `/stats/writing` 返回今日、趋势、连续天数 |
| P4-08 | TODO | 热力图、作品进度、月度摘要 | P4-07 | 对应统计接口可用 |
| P4-09 | TODO | 每日目标设置 | P4-07 | 全局和作品目标可更新 |

## P5 AI 写作与润色

| ID | 状态 | 任务 | 依赖 | 验收 |
| --- | --- | --- | --- | --- |
| P5-01 | TODO | LLM 客户端抽象与降级策略 | P0 | 无 Key 时应用可启动并返回明确错误 |
| P5-02 | TODO | Prompt 模板目录、填充工具与 JSON 健壮解析 | P5-01 | 模板变量校验，LLM JSON 输出可容错解析 |
| P5-03 | TODO | 异步任务进度服务 | P0 | `/tasks/{taskId}/progress` 可轮询 |
| P5-04 | TODO | 骨架生成 | P5-02,P5-03,P2,P3 | `/novel/skeleton` 返回 taskId |
| P5-05 | TODO | 骨架应用入库 | P5-04 | `/skeleton/{taskId}/apply` 生成卷章角色 |
| P5-06 | TODO | 动态上下文组装 | P2,P3 | 锁定角色、摘要、近期原文按预算组装 |
| P5-07 | TODO | SSE 流式续写 | P5-01,P5-06 | `/novel/continue` 支持首字超时事件 |
| P5-08 | TODO | 卡点分支建议 | P5-01,P5-06 | `/novel/branch` 返回 3 个方向 |
| P5-09 | TODO | 基础校正、进阶润色、风格重塑 | P5-01 | polish 接口返回批注或改写文本 |
| P5-10 | TODO | 文风档案与即时预览 | P5-09 | 样本保存后异步分析标签 |

## P6 一致性审查与锚点

| ID | 状态 | 任务 | 依赖 | 验收 |
| --- | --- | --- | --- | --- |
| P6-01 | TODO | 章节摘要生成 | P2,P5 | 章节完成后可异步生成摘要 |
| P6-02 | TODO | 角色锚点自动提取 | P3,P5 | 章节保存/完成触发档案更新 |
| P6-03 | TODO | 关键事件时间线 | P2,P5 | 生成 chapter_key_events |
| P6-04 | TODO | 向量嵌入写入与检索 | P6-03 | pgvector 检索可用 |
| P6-05 | TODO | 一致性审查配额检查 | P1,P4 | 按套餐限制字数和次数 |
| P6-06 | TODO | 一致性审查任务 | P5,P6-05 | 返回报告和条目 |
| P6-07 | TODO | 审查条目处理状态 | P6-06 | open/handled/ignored 可更新 |

## P7 小说转剧本

| ID | 状态 | 任务 | 依赖 | 验收 |
| --- | --- | --- | --- | --- |
| P7-01 | TODO | 剧本草稿与场景模型 | P2 | draft/scenes 数据可读写 |
| P7-02 | TODO | 场景分割 | P5 | 原文切为场景列表 |
| P7-03 | TODO | 心理外化策略 | P5 | ACTION/DIALOGUE/VOICEOVER/SKIP |
| P7-04 | TODO | 异步剧本改编 | P7-01,P7-02,P7-03 | `/script/convert` 返回 taskId/draftId |
| P7-05 | TODO | 剧本工作台数据接口 | P7-04 | 草稿详情、场景分页 |
| P7-06 | TODO | 场景乐观锁 | P7-05 | 版本冲突返回 409 |
| P7-07 | TODO | 分集管理 | P7-05 | episode CRUD 和场景归属 |
| P7-08 | TODO | DOCX/FDX/分镜导出 | P7-05 | 导出任务返回预签名 URL |

## P8 前端工作台

| ID | 状态 | 任务 | 依赖 | 验收 |
| --- | --- | --- | --- | --- |
| P8-01 | TODO | Vue 3 + Vite + TypeScript + Naive UI 初始化 | P0 | 前端能启动 |
| P8-02 | TODO | API 客户端、Token 刷新队列锁 | P1 | 401 可静默刷新 |
| P8-03 | TODO | Pinia auth/quota/task/scriptDraft stores | P1,P5 | 状态和轮询可复用 |
| P8-04 | TODO | 登录/注册/邮箱验证/忘记密码 UI | P1 | 对齐 IDD §0 |
| P8-05 | TODO | 作品首页、卡片、空状态、配额 Tooltip | P2,P4 | 对齐 IDD §1 |
| P8-06 | TODO | TipTap 章节编辑器 | P2 | 只加载当前章节 |
| P8-07 | TODO | 大纲、角色库、词典侧栏 | P2,P3 | 左侧图标面板 |
| P8-08 | TODO | AI 浮窗、SSE 插入、AI 内容标识 | P5 | RAF 批量写入 |
| P8-09 | TODO | 搜索替换横条 | P4 | 300ms debounce |
| P8-10 | TODO | 写作统计看板 | P4 | 趋势、热力图、作品进度 |
| P8-11 | TODO | 版本快照和 diff | P2 | 支持只看差异 |
| P8-12 | TODO | 一致性审查报告 | P6 | 可跳转章节和处理条目 |
| P8-13 | TODO | 剧本四栏工作台 | P7 | 场景目录、原文、剧本、AI 建议 |
| P8-14 | TODO | 移动端响应式 | P8 | 核心写作可用，复杂功能限制提示 |
| P8-15 | TODO | 命令面板与键盘导航 | P8-06 | Cmd/Ctrl+K 打开，支持功能搜索、键盘导航、底部快捷提示 |
| P8-16 | TODO | Toast 通知系统 | P8-01 | 右下角堆叠、成功/警告/错误/信息四类、移动端顶部居中 |
| P8-17 | TODO | 异常状态 UI 与内容安全说明 | P8-08,P9-05 | A/B/C/D 级异常展示、内容替换 Tooltip、申诉入口 |
| P8-18 | TODO | 渐进式用户引导 | P8-05,P8-06 | 首次进入、首次选中文字、首次接受 AI、写够 3000 字等触发 |
| P8-19 | TODO | 订阅升级引导 UI | P9 | 专业版功能、配额用尽、改编次数用尽弹窗分场景展示 |
| P8-20 | TODO | 账户设置 UI | P1,P9 | 个人资料、订阅用量、创作偏好、AI 内容设置、数据与隐私 |
| P8-21 | TODO | 快捷键参考面板 | P8-15 | `?` 或命令面板打开，覆盖导航、写作、AI 操作、视图切换 |
| P8-22 | TODO | 前端性能边界与代码分割 | P8-01 | TipTap/剧本工作台/diff 独立分包，移动端不下载重组件 |

## P9 商业化与数据安全

| ID | 状态 | 任务 | 依赖 | 验收 |
| --- | --- | --- | --- | --- |
| P9-01 | TODO | 配额模型与扣减 | P1,P5 | 操作前预估，完成后扣减 |
| P9-02 | TODO | 订阅方案查询 | P1 | `/subscriptions/plans` |
| P9-03 | TODO | 支付订单与回调验签 | P9-02 | 微信/支付宝回调可验签 |
| P9-04 | TODO | 数据导出 ZIP | P2,P3,P7 | 异步导出用户全部数据 |
| P9-05 | TODO | 内容安全过滤和申诉入口 | P5 | 输出替换透明展示 |
| P9-06 | TODO | 版权免责、AI 辅助标识与隐私策略 | P1,P5,P7 | 作品默认私有、导出标注 AI 辅助、隐私和授权可配置 |
| P9-07 | TODO | 团队版成员与共享配额 | P1,P9-01 | 团队管理员邀请成员，成员共享团队配额池 |
| P9-08 | TODO | 团队版作品协作与统一账单 | P9-07,P2 | 项目协作者权限、按人/作品统计用量 |
| P9-09 | TODO | 体验额度与字数包 | P9-01,P9-03 | 免费一次性体验额度、月度配额、不过期字数包可叠加 |

## P10 生态与开放能力

| ID | 状态 | 任务 | 依赖 | 验收 |
| --- | --- | --- | --- | --- |
| P10-01 | DEFERRED | 文风市场 | P5,P9 | V2.0 生态能力，暂不进入 V1 |
| P10-02 | DEFERRED | 多人实时协作增强 | P9-08 | V2.0 生态能力，团队基础协作先在 P9 覆盖 |
| P10-03 | DEFERRED | 企业 API 开放平台 | P9 | 企业专属 REST API、Key 管理、调用审计 |
