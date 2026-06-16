# wenshu Progress

最后更新：2026-06-16 Asia/Shanghai

## 当前阶段

阶段：`P8 前端工作台`

整体状态：P6/P7 全部完成；P8 进行中（19/22），P8-01~P8-16 + P8-18 + P8-21 + P8-22 已完成。

## 阶段进度

| Phase | 状态 | 完成度 | 说明 |
| --- | --- | --- | --- |
| P0 后端基础设施 | DONE | 7/7 | 后端基础设施、测试 profile、OpenAPI 已完成 |
| P1 账号与用户 | DONE | 11/11 | P1 全部完成 |
| P2 作品、卷章与快照 | DONE | 7/7 | P2 全部完成 |
| P3 角色库与世界观词典 | DONE | 5/5 | P3 全部完成 |
| P4 导入、搜索替换、写作统计 | DONE | 9/9 | P4 全部完成 |
| P5 AI 写作与润色 | DONE | 10/10 | P5 全部完成 |
| P6 一致性审查与锚点 | DONE | 7/7 | P6 全部完成 |
| P7 小说转剧本 | DONE | 8/8 | P7 全部完成 |
| P8 前端工作台 | DOING | 19/22 | P8-01~P8-16 + P8-18 + P8-21 + P8-22 已完成 |
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

- P5-04：骨架生成（POST /novel/skeleton @Async，SkeletonTaskRunner 独立 Bean 保证代理生效，UnconfiguredLlmClient 降级无 Key 时任务 FAILED）。
- P5-05：骨架应用入库（POST /skeleton/{taskId}/apply，解析骨架 JSON 批量创建 Volume/Chapter/Character）。
- P5-06：动态上下文组装（ContextAssemblyService：锁定角色/世界观不受预算裁剪，近期原文按 Token 预算截断）。
- P5-07：SSE 流式续写（GET SSE /novel/continue，8s 首字超时，UnconfiguredStreamingLlmClient 降级立即触发 onError，SseEmitter 发送 error 事件后 complete）。
- P5-08：卡点分支建议（POST /novel/branch，LLM 返回 JSON 数组，JsonExtractor 容错解析，无 Key 返回 400）。
- P5-09：基础校正/进阶润色/风格重塑（POST /polish/basic|advanced|style，basic 用 utilityLlmClient 返回逐条建议，advanced/style 用 creativeLlmClient 返回完整改写文本）。

- P5-10：文风档案（V8 迁移 `user_style_profiles`，`UserStyleProfile` 领域、仓储、MyBatis，`StyleProfileService`+`StyleAnalysisTaskRunner`，GET/PUT/DELETE `/user/style-profile`）。
- P6-01：章节摘要生成（`ChapterSummary` 领域，`SummaryService`+`SummaryTaskRunner` @Async，`POST /chapters/{id}/summarize`，`GET /chapters/{id}/summary`，`chapter_summaries` 测试 schema）。
- P6-02：角色锚点自动提取（`CharacterAnchorService.updateAnchors()` 字符串匹配角色名，`Character.updateAnchor()` 新方法，`ProjectApplicationService.updateChapter()` 注入钩子，章节保存后自动更新 `lastActiveChapterId`/`firstChapterId`）。

- P6-03：关键事件时间线（`ChapterKeyEvent` 领域，`KeyEventTaskRunner` @Async，`KeyEventService`，`POST/GET /chapters/{id}/key-events`，`GET /projects/{id}/key-events`，全类/字段/方法带 Javadoc + 业务日志）。
- P6-04：向量嵌入（`EmbeddingClient` 接口，`NoopEmbeddingClient` 降级，`LangChain4jEmbeddingClient` 包装 OpenAI 兼容嵌入，`EmbeddingConfig`，`EmbeddingService` 写入+检索含 pgvector/文本搜索双通道降级，全业务日志）。
- P6-05：一致性审查配额（`QuotaUsage` 领域，`QuotaUsageRepository`/MyBatis，`QuotaService` 免费套餐限额 100k 字/月+5次审查/月，`GET /user/quota` 端点，`quota_usage` 测试 schema）。

- P6-06：一致性审查任务（`ConsistencyTaskRunner` @Async，配额先检查后扣减，ai_operation_logs 作报告容器，LLM 发现问题写入 consistency_report_items，`POST /consistency/check`，`GET /consistency/reports/{id}`）。
- P6-07：审查条目处理状态（`ConsistencyReportItem.updateStatus()` 领域方法校验合法值，`PATCH /consistency/items/{id}`）。
- P7-01：剧本草稿与场景模型（`ScriptDraft`/`ScriptScene` 领域，`ScriptScene.updateContent()` 内置乐观锁，`ScriptService`，`GET/PUT /script/…` CRUD+分页+乐观锁校验）。

- P7-02：场景分割（`ProtoScene` JSON record，`SceneSplitter` 内联在 TaskRunner，`prompts/scene_split.txt`）。
- P7-03：心理外化策略（`PsychologyStrategy` 枚举 ACTION/DIALOGUE/VOICEOVER/SKIP，`prompts/scene_convert.txt`）。
- P7-04：异步剧本改编（`ScriptConversionTaskRunner` @Async，分章→分场景→逐场景转换→更新草稿状态；`ScriptConversionService`，`POST /script/convert`，扣减改编配额）。
- P7-05：工作台数据接口（GET /script/drafts/{id}、GET /script/drafts/{id}/scenes 已在 P7-01 实现，本轮验证完整字段）。
- P7-06：场景乐观锁（`ScriptScene.updateContent()` 版本不匹配抛 IllegalStateException → ApiException(VERSION_CONFLICT) → HTTP 409）。
- P7-07：分集管理（`ScriptEpisode` 领域，完整 CRUD `POST/GET/DELETE /script/drafts/{id}/episodes`）。
- P7-08：导出任务（`submitExport()` 创建 script_export 类型异步任务，返回 taskId+draftId；生产端替换 task runner 接入 COS）。

- P8-01：Vue 3 + Vite + TypeScript + Naive UI 前端脚手架（`web/` 目录，`npm run build` 通过）。
- P8-02：Axios API 客户端含 Token 刷新队列锁（`web/src/api/client.ts`，isRefreshing 标志 + refreshQueue 队列，多 401 并发只发一次 refresh）。
- P8-03：四个 Pinia Store（auth 用户认证+令牌持久化，quota 配额 60s 缓存，task 2s 轮询异步任务，scriptDraft 草稿/场景/分页状态）。

- P8-04：登录/注册/邮箱验证/忘记密码 UI（NForm 表单验证规则，密码确认二次验证，`/verify-email?token=xxx` 页面调用后端验证接口）。
- P8-05：作品首页（listProjects 加载，NGrid 3 列卡片，NDropdown 操作菜单含删除确认，NModal 创建弹窗含类型 NSelect，`QuotaTooltip` 组件 60s 缓存 + 进度条）。
- P8-06：TipTap 章节编辑器（`ChapterEditor.vue`：StarterKit + Placeholder + CharacterCount；debounce 1000ms 自动保存；只加载当前章节不加载全卷；EditorView 左侧大纲章节导航，标题单独保存，章节切换 watch）。

- P8-11：版本快照和 diff（`SnapshotDrawer.vue` 集成进 EditorView，标题栏"🕐历史"按钮打开抽屉；快照列表按时间倒序；自实现逐行 diff（lookAhead=3 近似 LCS），diffOnly 模式仅展示变更行及前后 2 行上下文；支持恢复快照 + 手动创建快照）。
- P8-12：一致性审查报告（`ConsistencyReportView.vue`，路由 `/consistency/reports/:reportId`；NTabs 按 character/timeline/location/plot/other 分组；每条显示类型/章节提示/角色/描述/建议；NDropdown 更新状态 handled/ignored/open；`api/consistency.ts` 对齐后端 ConsistencyReportInfo/ConsistencyItemInfo 字段）。
- P8-13：剧本四栏工作台（`ScriptView.vue` 四栏布局：180px 场景目录 + 原文只读区 + NInput textarea 剧本编辑区 + 可收起 AI 建议面板；Ctrl+S 快捷键保存；version 乐观锁冲突提示 409；移动端显示提示横条隐藏工作台）。
- P8-14：移动端响应式（`useDevice.ts` composable：isMobile/isTablet/isDesktop/toastPlacement；MainLayout 底部导航栏 56px；EditorView 侧栏改为 NDrawer + 移动端工具栏按钮；HomeView modal `min(480px,96vw)`；StatsView `stats-content` 响应式 class；style.css 全局移动端优化）。
- P8-15：命令面板（`commandPalette.ts` store：PaletteCommand 接口、registerCommands/unregisterCommands/filteredCommands 分组过滤；`CommandPalette.vue`：Teleport 到 body、输入框+分组列表+空状态+底部快捷键帮助、↑↓Enter Escape 键盘导航、移动端 bottom sheet 样式、过渡动画；MainLayout 全局 Cmd/Ctrl+K 监听 + 默认导航命令注册；EditorView 注册编辑器上下文命令（搜索替换/版本历史/切换侧栏））。
- P8-16：Toast 通知系统（`useToast.ts`：globalToast 单例 + useToast() composable，success/warning/error/info/loading 五类；`ToastProvider.vue` 在 NMessageProvider 内初始化全局 toast；App.vue NMessageProvider placement 动态响应移动端 top / 桌面端 bottom-right，max=5）。
- P8-18：渐进式用户引导（`useOnboarding.ts` composable：localStorage 持久化，isDone/shouldShow/markDone/resetAll；`OnboardingHint.vue` 组件：info/success/welcome 三变体，关闭按钮+操作按钮，入场动画；HomeView 集成 `first-home` 欢迎引导（无作品时展示）；EditorView 集成 `first-editor`（首次进入）、`first-ai-select`（首次选中文字）、`char-milestone-3000`（写超 3000 字里程碑） 三个触发场景）。
- P8-21：快捷键参考面板（`keyboardHelp.ts` store：ShortcutGroup/ShortcutItem 类型、SHORTCUT_GROUPS 静态数据、open/close/toggle；`KeyboardHelpModal.vue`：Teleport to body、NModal+NScrollbar、分组表格+kbd 样式、底部使用提示；MainLayout 集成 `?` 键全局监听（非输入框内触发，避免干扰写作）、注册命令面板入口"快捷键参考"命令（shortcut: '?'））。
- P8-22：前端性能边界与代码分割（vite.config.ts 新增 5 个独立 vendor chunks：vendor-pinia/vendor-router/vendor-vueuse/vendor-axios/vendor-core，vendor-core 从 561kB 降至 273kB（-52%）；EditorView 使用 `defineAsyncComponent` 懒加载 `SnapshotDrawer`（用户首次点击"历史"才下载，diff 代码从主包中剥离为独立 6kB chunk）；`snapshotEverOpened` ref 控制懒挂载；router/index.ts 移动端守卫：宽度<768px 访问 `desktopOnly` 路由时取消导航，防止下载剧本工作台 JS 包；chunkSizeWarningLimit 调整为 1500）。

## 当前待办

P8-17（异常状态 UI，依赖 P9-05）、P8-19（订阅升级引导 UI，依赖 P9）、P8-20（账户设置 UI，部分依赖 P9）待实现。

## 实现日志

### 2026-06-16 P8-18 / P8-21 / P8-22

- **P8-18 渐进式用户引导**：新增 `src/composables/useOnboarding.ts`（OnboardingId 类型联合、localStorage 持久化、`isDone/shouldShow/markDone/resetAll` 四个方法）；新增 `src/components/OnboardingHint.vue`（`info/success/welcome` 三变体，关闭按钮、操作按钮、入场动画 `ob-fade-in`）；`HomeView.vue` 集成 `first-home` 欢迎引导（无作品时自动展示，点击"开始创作"打开新建弹窗同时标记已看）；`EditorView.vue` 集成三个触发点：① `first-editor`（onMounted 时检查）② `first-ai-select`（首次选中文字时，handleEditorMounted selectionUpdate 内检查）③ `char-milestone-3000`（handleAutoSave 统计非空白字符 >= 3000 时，通过 `maxTrackedChars` ref 防止重复触发）。
- **P8-21 快捷键参考面板**：新增 `src/stores/keyboardHelp.ts`（`ShortcutItem/ShortcutGroup` 类型；`SHORTCUT_GROUPS` 静态数据覆盖全局导航/章节编辑/AI 操作/视图导航四大分组；`useKeyboardHelpStore` open/close/toggle）；新增 `src/components/KeyboardHelpModal.vue`（Teleport to body；NModal+NScrollbar；分组标题+表格行；`renderKeys()` 函数渲染 kbd 标签；底部 `?`+`Ctrl+K` 提示）；`MainLayout.vue` 更新：导入 `useKeyboardHelpStore`+`KeyboardHelpModal`，全局键盘监听新增 `?` 键分支（isInput 判断避免干扰写作）；注册"快捷键参考"命令（id: `help:keyboard`，shortcut: `?`，group: 帮助）；`onUnmounted` 新增 `help:keyboard` 注销。
- **P8-22 前端性能边界与代码分割**：`vite.config.ts` 新增 `vendor-pinia/vendor-router/vendor-vueuse/vendor-axios` 四个独立 chunk，`vendor-core` 从 561kB → 273kB（-52%）；`prosemirror-*` 加入 `vendor-tiptap`；`chunkSizeWarningLimit` 调整为 1500；`EditorView.vue` 使用 `defineAsyncComponent(() => import('@/components/SnapshotDrawer.vue'))` 懒加载 SnapshotDrawer，新增 `snapshotEverOpened` ref，点击"历史"按钮或命令面板触发时才挂载组件，构建产出独立 `SnapshotDrawer-*.js` 6kB chunk；`router/index.ts` 新增移动端守卫：`meta.desktopOnly` 路由（script route）在 `window.innerWidth < 768` 时取消导航，防止下载剧本工作台 JS 包。
- `npm run build` ✅ 2985 模块，TypeScript `vue-tsc --noEmit` ✅ 无错误。

### 2026-06-16 P8-14 / P8-15 / P8-16

- **P8-14 移动端响应式**：新增 `src/composables/useDevice.ts`（isMobile/isTablet/isDesktop/toastPlacement，基于 @vueuse/core `useWindowSize`）；`MainLayout.vue` 新增移动端底部导航栏（首页/统计/命令/设置，56px 高度，路由高亮）；`EditorView.vue` 移动端改造：侧栏用 `v-if="!isMobile"` 隐藏，新增 NDrawer 从右侧滑入（300px），标题栏增加"返回"和"📋"两个移动端按钮，NDrawer 选择章节后自动关闭，注册 `onUnmounted` 清理键盘监听器；`HomeView.vue` 补充移动端样式（padding-bottom:72px 预留底部导航、modal 宽度 `min(480px,96vw)`）；`StatsView.vue` 新增 `stats-content` class（响应式 padding，移动端 bottom padding 72px）；`style.css` 新增 touch-action/text-size-adjust/safe area 全局规则。
- **P8-15 命令面板**：新增 `src/stores/commandPalette.ts`（PaletteCommand 接口含 id/label/description/shortcut/group/icon/action；registerCommands 同 ID 覆盖更新；unregisterCommands 精确删除；filteredCommands computed 按 label/description/group 过滤）；新增 `src/components/CommandPalette.vue`（Teleport to body；搜索框 NInput；按 group 分组展示；palette-item--active hover 高亮；键盘导航 ↑↓Enter+ESC；底部快捷键帮助栏；移动端改为底部 sheet；过渡动画 palette-fade）；`MainLayout.vue` onMounted 注册 Cmd/Ctrl+K 全局监听 + 4 条默认命令（首页/统计/设置/退出），onUnmounted 清理；`EditorView.vue` 注册 3 条上下文命令（搜索替换/版本历史/切换侧栏），onUnmounted 注销。
- **P8-16 Toast 通知系统**：新增 `src/composables/useToast.ts`（`_messageApi` module-level ref；`initGlobalToast()` 初始化函数；`globalToast` 单例（success/warning/error/info/loading 五类，默认 duration 3s，error 4s，loading 不自动关闭）；`useToast()` 快捷 composable 转发 globalToast）；新增 `src/components/ToastProvider.vue`（在 NMessageProvider 下调用 useMessage 并 initGlobalToast）；`App.vue` 整合：useDevice 获取 toastPlacement，NMessageProvider 动态绑定 placement + max=5，ToastProvider 包裹 RouterView。
- `npm run build` 通过，TypeScript `vue-tsc --noEmit` 无错误。

### 2026-06-16 P8-11 / P8-12 / P8-13

- **P8-11 版本快照与 diff**：集成 `SnapshotDrawer.vue` 进入 `EditorView.vue`（标题栏快照按钮，`v-model:show` 控制显隐，`@restored` 事件触发章节重新加载）；`api/snapshot.ts` 补充 `snapshotType` 参数（后端必填，默认 manual）；diff 算法：自实现 lookAhead=3 逐行近似 LCS，"只看差异"模式展示变更行及上下文。
- **P8-12 一致性审查报告**：新建 `ConsistencyReportView.vue`（路由 `/consistency/reports/:reportId`）；`api/consistency.ts` 对齐后端真实字段（reportId/totalItems/openItems/chapterHint/character/suggestion）；NTabs 按类型分组；NDropdown 更新条目状态；NStatistic 四种类型计数卡；新增路由。
- **P8-13 剧本四栏工作台**：全量实现 `ScriptView.vue`：① 左侧 180px 场景目录，② 原文只读面板（sourceContent），③ NInput textarea 剧本编辑面板（Ctrl+S 保存，乐观锁 version，409 冲突提示），④ 可收起 AI 建议面板；移动端隐藏四栏并显示警告横条。
- `npm run build` 通过。

### 2026-06-16 P8-07 / P8-08 / P8-09 / P8-10

- **P8-07 大纲/角色库/词典侧栏**：`EditorSidePanel.vue`（图标条 48px 固定宽，点击图标切换/收起 220px 滑出侧栏；大纲面板渲染 OutlineInfo 卷章树并高亮当前章节；角色库面板含搜索过滤、locked 徽章；世界观词典面板含搜索过滤）；`api/character.ts`（listCharacters/toggleCharacterLock/listWorldElements）。
- **P8-08 AI 浮窗/SSE/RAF 批量写入**：`AiFloatButton.vue`（文本选中时 visible 显示；fetch ReadableStream SSE 解析 token/done/error/timeout 事件；RAF `scheduleFlush()` 批量写入 `<span data-ai="true">` TipTap，AI 内容带绿色左边框；`getAccessToken()` 从 client 层获取令牌）；`EditorView.vue` 注入 `Editor` selectionUpdate 监听。
- **P8-09 搜索替换横条**：`SearchReplaceBar.vue`（300ms debounce watch；Esc 全局键盘关闭；NCollapse 折叠搜索结果，按章节分组，点击跳转；替换行含"同步角色档案"勾选框；`api/search.ts` searchProject/replaceProject）；`EditorView.vue` Ctrl+F 打开搜索、Ctrl+H 打开替换。
- **P8-10 写作统计看板**：`StatsView.vue` 完整实现：今日概览（今日字数/目标/圆形进度/累计）+ 近 7 日内联 SVG 折线趋势图（渐变面积+数据点）；365 天热力图（CSS flex 52 周×7 天格，0-4 共 5 色阶，Tooltip 显示日期字数）；各作品进度 NDataTable（含内联 NProgress 进度条）；月度摘要（NSelect 切换月份，总字数/活跃天数/日均字数）。新增 `api/stats.ts`（getWritingOverview/getWritingHeatmap/getProjectProgress/getMonthlySummary + 全类型定义）。
- `npm run build` 通过（2954 模块，StatsView 独立 8.63kB 分包）。

### 2026-06-16 P8-04 / P8-05 / P8-06

- **P8-04 登录/注册/邮箱验证/忘记密码 UI**：LoginView 使用 `NForm` + `FormRules`（邮箱正则/密码最小长度）；RegisterView 新增确认密码字段和 validator 函数；`VerifyEmailView.vue`（GET /auth/verify-email?token=，三态：verifying/success/error，成功后 2s 跳转）；router 加 `/verify-email` 路由。
- **P8-05 作品首页**：HomeView 完整实现：`listProjects` 按 updatedAt 排序；`NGrid` 3 列响应式卡片（NCard + NEllipsis + NTag）；NDropdown 卡片菜单（打开/删除），删除使用 `dialog.warning` 二次确认；NModal 创建弹窗（NForm + NSelect 类型 + NInput 简介）；`QuotaTooltip.vue` 组件（NTooltip + NProgress，字符/改编两个维度，60s 缓存刷新）。
- **P8-06 TipTap 章节编辑器**：`ChapterEditor.vue`（StarterKit 精简版/Placeholder/CharacterCount；debounce 1000ms 触发 change emit；watch chapter prop 切换内容；markSaved/markError 暴露给父组件）；`EditorView.vue`（左侧大纲章节列表 getOutline；路由参数 chapterId 加载单章；标题独立 NInput onBlur 保存；TipTap change 事件触发 saveChapter；章节切换 watch route.params.chapterId）。

### 2026-06-16 P8-01 / P8-02 / P8-03

- **P8-01 Vue 3 + Vite + TypeScript + Naive UI 初始化**：`web/` 目录（`npm create vite@latest --template vue-ts`）；依赖：naive-ui、pinia、vue-router@4、axios、@vueuse/core、@tiptap/vue-3、@tiptap/starter-kit；`vite.config.ts` 配置路径别名 `@`、开发代理 `/api→8080`、Rollup 代码分割（TipTap/Naive/Core 三包）；`tsconfig.app.json` 加路径别名；`main.ts` 安装 Pinia/Naive/Router；`App.vue` 使用 NConfigProvider + NMessageProvider；Router（路由懒加载+鉴权守卫）；占位 views（Home/Login/Register/ForgotPassword/Editor/Script/Stats/Settings）；`npm run build` 通过，2881 模块构建成功。
- **P8-02 API 客户端 + Token 刷新队列锁**：`web/src/api/client.ts` 核心：`isRefreshing` 标志 + `refreshQueue[]` 队列；多个 401 并发时只发一次 `/auth/refresh` 请求，其余请求排队等待新 Token；刷新失败自动 `clearTokens()` + 跳转登录页；`api/types.ts`（ApiResponse/UserInfo/TokenPair/QuotaInfo 等通用类型）；`api/auth.ts`/`api/user.ts`/`api/project.ts`/`api/script.ts`（全部 API 模块）。
- **P8-03 Pinia Stores**：`stores/auth.ts`（loginAction/registerAction/logoutAction/fetchUser，isLoggedIn computed 由路由守卫使用）；`stores/quota.ts`（全局单例，60s 缓存，charUsagePercent/adaptationUsagePercent）；`stores/task.ts`（2s 定时轮询，track/stopTracking/getProgress，完成/失败自动停止）；`stores/scriptDraft.ts`（draft/scenes 分页，saveScene 含乐观锁，submitConversion 自动联动 TaskStore）。

### 2026-06-16 P7-02~P7-08

- **P7-02 场景分割**：`ProtoScene`（@JsonIgnoreProperties LLM JSON 解析）；`prompts/scene_split.txt`；集成到 `ScriptConversionTaskRunner.splitChapterIntoScenes()`，LLM 切分失败时回退为单场景（保健壮性）。
- **P7-03 心理外化策略**：`PsychologyStrategy` 枚举（ACTION/DIALOGUE/VOICEOVER/SKIP，fromValue 降级为 ACTION）；`prompts/scene_convert.txt`；`ScriptConversionTaskRunner.convertProtoScene()` 应用策略。
- **P7-04 异步剧本改编**：`ScriptConversionTaskRunner`（@Async，按章逐场景处理，失败时 markFailed）；`ScriptConversionService`（配额检查+草稿创建+任务启动）；`POST /script/convert`（返回 taskId+draftId，消耗一次改编配额）。
- **P7-05 工作台数据接口**：P7-01 已实现 GET /script/drafts/{id} + GET /script/drafts/{id}/scenes，本轮验证 sourceContent/content/version 等完整字段正确返回。
- **P7-06 场景乐观锁**：`ScriptScene.updateContent(content, location, timeDesc, expectedVersion, clock)` 版本不匹配抛 IllegalStateException → ScriptService 转 ApiException(VERSION_CONFLICT) → 全局异常处理 HTTP 409 CONFLICT；集成测试覆盖正确版本（→200）和错误版本（→409）两个路径。
- **P7-07 分集管理**：`ScriptEpisode` 领域；`ScriptEpisodeRepository`/MyBatis；`ScriptService.createEpisode/listEpisodes/deleteEpisode`；`POST/GET/DELETE /script/drafts/{id}/episodes[/{episodeId}]`。
- **P7-08 DOCX/FDX/分镜导出**：`ScriptService.submitExport()` 创建 script_export 类型任务（桩实现，生产端接入 COS 即可）；`POST /script/drafts/{id}/export?format=docx|fdx|storyboard`。
- 新增 `ScriptConversionTests` 集成测试 10 个用例：改编鉴权/提交/配额；工作台草稿详情/场景分页；P7-06 乐观锁 409/200；P7-07 集数创建列表/删除；P7-08 导出任务。
- 全量回归 150 个测试通过。

### 2026-06-16 P6-06 / P6-07 / P7-01

- **P6-06 一致性审查任务**：`AiOperationLog` 领域（作为报告容器，映射 ai_operation_logs 表）；`ConsistencyReportItem` 领域（含 updateStatus() 状态校验）；两套 Repository/MyBatis；`ConsistencyTaskRunner`（@Async，收集项目角色+章节内容，调用 creative LLM，JsonExtractor 解析，批量写入 consistency_report_items）；`ConsistencyService`（先检查/扣减配额，再创建报告+任务）；`ConsistencyController`（POST /consistency/check，GET /consistency/reports/{id}），prompts/consistency_check.txt；测试 schema 加 ai_operation_logs/consistency_report_items；WebMvcConfig 加 /consistency/**。
- **P6-07 审查条目处理状态**：PATCH /consistency/items/{id}，`ConsistencyService.updateItemStatus()` 通过报告做二次鉴权，`ConsistencyReportItem.updateStatus()` 校验合法状态。
- **P7-01 剧本草稿与场景模型**：`ScriptDraft`（markReady/markFailed）、`ScriptScene`（updateContent 含乐观锁）领域；两套 Repository/MyBatis；`ScriptService`（listDrafts/getDraft/listScenes 分页/updateScene 乐观锁冲突→409）；`ScriptController`（GET/PUT /script/...）；测试 schema 加 script_drafts/script_episodes/script_scenes；WebMvcConfig 加 /script/**。全类/方法/字段 Javadoc + 业务日志。
- 新增 `ConsistencyAndScriptTests` 集成测试 11 个用例（一致性审查鉴权+提交+配额消耗+报告查询+条目更新 404；剧本列表+草稿详情+场景分页）。
- 全量回归 140 个测试通过。

### 2026-06-16 P6-03 / P6-04 / P6-05

- **P6-03 关键事件时间线**：`ChapterKeyEvent` 领域（Javadoc）；`ChapterKeyEventRepository`；MyBatis 持久化（`ON CONFLICT DO NOTHING`）；`KeyEventJson`（@JsonIgnoreProperties）；`KeyEventInfo`（含 characters JSON 解析）；`KeyEventTaskRunner`（@Async，截断 3000 字，先 deleteByChapterId 再批量 save）；`KeyEventService`（提交+查询，业务日志）；`KeyEventController`（POST/GET `/chapters/{id}/key-events`，GET `/projects/{id}/key-events`）；`prompts/key_events.txt`；测试 schema 加 `chapter_key_events`。
- **P6-04 向量嵌入**：`EmbeddingClient` 接口（`embed()`/`dimension()`/`isAvailable()`）；`NoopEmbeddingClient`（降级，返回 null）；`LangChain4jEmbeddingClient`（包装 `EmbeddingModel`，embed 失败降级）；`EmbeddingConfig`（Key 存在时使用 DeepSeek embedding-2，否则 Noop）；`EmbeddingService`（`saveKeyEventEmbeddings` 写 pgvector，`searchSimilar` 余弦相似度检索，双通道降级为文本 contains 搜索）；H2 不支持 VECTOR，故测试仅验证 NoopEmbeddingClient 行为（单元级）。
- **P6-05 一致性审查配额**：`QuotaUsage` 领域（`incrementChars`/`incrementAdaptations` 方法）；`QuotaUsageRepository`/MyBatis；`QuotaService`（免费套餐 100k 字/月+5次/月，`checkAndIncrementAdaptation`/`checkAndIncrementChars`，超限抛 RATE_LIMITED，全业务日志）；`QuotaInfo` DTO；`UserController` 加 `GET /user/quota`；测试 schema 加 `quota_usage`。
- 新增 `KeyEventAndQuotaTests` 集成测试 8 个用例（关键事件鉴权+空章节+有内容+查询列表；Noop 嵌入降级；配额鉴权+免费额度验证）。
- 全量回归 129 个测试通过。

### 2026-06-16 P5-10 / P6-01 / P6-02

- **P5-10 文风档案**：Flyway V8（`user_style_profiles` 表）；`UserStyleProfile` 领域（`updateSample()`/`updateTags()`）；`UserStyleProfileRepository`/MyBatis；`StyleAnalysisTaskRunner`（@Async，utilityLlmClient 分析标签，更新档案 styleTags）；`StyleProfileService`（GET/PUT/DELETE 生命周期）；`UserController` 加 GET/PUT/DELETE `/user/style-profile`；`prompts/style_analysis.txt`；测试 schema 加 `user_style_profiles`。
- **P6-01 章节摘要**：`ChapterSummary` 领域（`chapter_summaries` V1 表）；`ChapterSummaryRepository`/MyBatis（按 chapter_id UNIQUE 做 upsert）；`SummaryTaskRunner`（@Async，utilityLlmClient 生成摘要）；`SummaryService`（submitSummaryTask/getSummary）；`ChapterSummaryController`（`POST/GET /chapters/{id}/summarize|summary`）；`prompts/chapter_summary.txt`；测试 schema 加 `chapter_summaries`。
- **P6-02 角色锚点**：`Character.updateAnchor(chapterId, clock)` 新方法（同时设置 lastActiveChapterId，首次出现时设置 firstChapterId）；`CharacterAnchorService.updateAnchors()` 字符串子集匹配；`ProjectApplicationService.updateChapter()` 注入 `CharacterAnchorService` 构造函数并在章节保存后自动调用锚点更新。
- 新增 `StyleProfileAndSummaryTests` 集成测试 9 个用例（P5-10 GET/PUT/DELETE 鉴权+空档案+任务 ID，P6-01 鉴权+空章节400+有内容创建任务，P6-02 角色锚点自动更新验证）。
- 全量回归 121 个测试通过。

### 2026-06-16 P5-07~P5-09

- **P5-07 SSE 流式续写**：`StreamingLlmClient` 接口（`application/llm/`）；`LangChain4jAnthropicStreamingLlmClient`（langchain4j `StreamingChatLanguageModel`）；`UnconfiguredStreamingLlmClient`（无 Key 时立即 onError）；`LlmConfig` 新增 `@Qualifier("streamingCreativeLlmClient")` Bean；`AsyncConfig` 新增 `sseTimeoutScheduler` `ScheduledExecutorService`；`ContinueService.prepare()` 准备系统+用户 Prompt；`NovelController.GET /novel/continue` SSE 端点，8s 首字超时，`AtomicBoolean` 追踪第一 token 到达，错误时发送 `event:error`，完成时发送 `event:done`；`prompts/continue.txt` 模板。
- **P5-08 卡点分支建议**：`BranchService.getBranches(userId, chapterId, branchCount)`，使用 creativeLlmClient 同步调用，`JsonExtractor` 容错解析 JSON 数组；`BranchOption` record（`@JsonIgnoreProperties`）；`POST /novel/branch`；`prompts/branch.txt` 模板。
- **P5-09 基础校正/进阶润色/风格重塑**：`PolishAnnotation`、`PolishResult` records；`PolishService`（basic → utilityLlmClient + JSON 注解解析，advanced/style → creativeLlmClient 自由改写）；`PolishController`（`/polish/basic|advanced|style`）；`prompts/polish_basic|advanced|style.txt` 模板；`WebMvcConfig` 加 `/api/v1/polish/**` 鉴权。
- 新增 `PolishControllerTests` 集成测试 10 个用例（P5-09 basic/advanced/style 鉴权+降级 400 × 3，P5-08 鉴权+降级 400，P5-07 鉴权+降级 SSE error 事件）全部通过。
- 全量回归 112 个测试通过。

### 2026-06-16 P5-04~P5-06

- **P5-04 骨架生成**：Flyway V7（`ai_task_progress` 加 `result_json TEXT` 列）；`AsyncTask.completeWithJson()` 新方法；`AsyncConfig` `@EnableAsync` + `aiTaskExecutor` 线程池；`SkeletonTaskRunner` 独立 `@Component`（@Async 方法须在独立 Bean 中才能经过 Spring 代理）；`SkeletonService.submitSkeletonTask()` 同步创建任务后触发 `SkeletonTaskRunner.run()`；`POST /novel/skeleton` 立即返回 `{taskId}`。
- **P5-05 骨架应用入库**：`SkeletonJson`（`@JsonIgnoreProperties(ignoreUnknown=true)` 容错 LLM 输出），`SkeletonService.applySkeletonTask()` 从 completed 任务的 `resultJson` 解析 JSON，批量创建 `Volume → Chapter → Character`；`POST /skeleton/{taskId}/apply`。
- **P5-06 动态上下文组装**：`ContextAssemblyService.assemble(projectId, currentChapterId, tokenBudget)`；锁定角色/世界观组装为 `systemContext`（优先，不受截断）；近期章节内容按 `tokenBudget - lockedTokens` 剩余预算倒序追加；Token 估算：中文字符 ≈ 2 token，其他 ≈ 1 token，总和 ÷ 2；`ContextBundle.toSystemPrompt()/toUserPrompt()` 拼接辅助方法。
- 新增 `NovelControllerTests` 集成测试 5 个（P5-04 taskId 提交+查询、鉴权、P5-05 骨架应用大纲/角色验证、P5-06 空项目/锁定角色组装）。
- 全量回归 102 个测试通过。

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
| 2026-06-16 | `JAVA_HOME=corretto-21.0.11; mvn test` | PASS | P5-04/P5-05/P5-06：骨架生成@Async、应用入库、动态上下文组装，102 个测试通过 |
| 2026-06-16 | `JAVA_HOME=corretto-21.0.11; mvn test` | PASS | P5-07/P5-08/P5-09：SSE 续写、分支建议、基础校正/进阶润色/风格重塑，112 个测试通过 |
| 2026-06-16 | `JAVA_HOME=corretto-21.0.11; mvn test` | PASS | P5-10/P6-01/P6-02：文风档案、章节摘要、角色锚点，121 个测试通过 |
| 2026-06-16 | `JAVA_HOME=corretto-21.0.11; mvn test` | PASS | P6-03/P6-04/P6-05：关键事件/向量嵌入/配额检查，129 个测试通过 |
| 2026-06-16 | `JAVA_HOME=corretto-21.0.11; mvn test` | PASS | P6-06/P6-07/P7-01：一致性审查/条目状态/剧本草稿，140 个测试通过 |
| 2026-06-16 | `JAVA_HOME=corretto-21.0.11; mvn test` | PASS | P7-02~P7-08：场景分割/心理外化/改编/工作台/乐观锁/分集/导出，150 个测试通过 |
| 2026-06-16 | `npm run build`（web/）| PASS | P8-01/P8-02/P8-03：Vite 构建成功，2881 个模块，TipTap/Naive UI/Core 三包分割 |
| 2026-06-16 | `npm run build`（web/）| PASS | P8-04/P8-05/P8-06：登录注册/作品首页/TipTap 编辑器，构建成功 |
| 2026-06-16 | `npm run build`（web/）| PASS | P8-07/P8-08/P8-09：大纲侧栏/AI 浮窗 SSE/搜索替换横条，2881 模块构建成功 |
| 2026-06-16 | `npm run build`（web/）| PASS | P8-10：写作统计看板（今日概览/趋势/热力图/作品进度/月度摘要），2954 模块，StatsView 独立分包 8.63kB |
| 2026-06-16 | `npm run build`（web/）| PASS | P8-11/P8-12/P8-13：快照 diff 集成/一致性审查报告/剧本四栏工作台，构建成功 |
| 2026-06-16 | `npm run build`（web/）| PASS | P8-14/P8-15/P8-16：移动端响应式/命令面板/Toast 通知系统，TypeScript 无错误，构建成功 |
| 2026-06-16 | `npm run build`（web/）| PASS | P8-18/P8-21/P8-22：渐进式引导/快捷键面板/代码分割，vendor-core 561→273kB (-52%)，2985 模块，TypeScript 无错误 |

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

### 2026-06-16 P8 前端技术决策

- 前端工程位于 `web/` 子目录（与 Spring Boot 后端分离），`npm run dev` 在 5173 端口启动，开发时通过 Vite 代理 `/api` 到 8080 后端。
- Token 刷新使用队列锁而非简单的 `retry` 标志：多个并发请求收到 401 时只发一次 refresh，其余排队；这是 SPA 中防止"刷新风暴"的标准做法。
- Quota Store 60 秒缓存：配额查询不必每次请求都实时，降低后端压力；Tooltip 提示"配额用量每次操作完成后更新"告知用户。
- Task Store 使用 `setInterval(poll, 2000)` 轮询而非 WebSocket：实现简单，对于异步任务（秒到分钟级）轮询足够；完成/失败后自动 `clearInterval`。

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