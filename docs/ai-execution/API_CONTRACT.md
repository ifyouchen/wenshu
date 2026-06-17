# API Contract

所有接口统一前缀：`/api/v1`。所有成功/失败响应统一：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "2026-06-15T12:00:00Z"
}
```

需要鉴权的接口使用 `Authorization: Bearer {accessToken}`。

## System

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | `/system/health` | No | 系统探针 |
| GET | `/tasks/{taskId}/progress` | Yes | 异步任务进度 |

## Auth & User

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/auth/register` | No | 邮箱密码注册，发送验证邮件，返回双 Token |
| GET | `/auth/verify-email` | No | 邮箱验证，query: `token` |
| POST | `/auth/resend-verify` | No | 重新发送验证邮件，60 秒限流 |
| POST | `/auth/login` | No | 邮箱密码登录，失败 5 次锁定 |
| POST | `/auth/password/forgot` | No | 发起密码重置 |
| POST | `/auth/password/reset` | No | 重置密码并吊销所有 Refresh Token |
| POST | `/auth/refresh` | No | Refresh Token 轮换 |
| POST | `/auth/logout` | Yes | 登出当前设备 |
| POST | `/auth/logout-all` | Yes | 登出全部设备 |
| GET | `/user/me` | Yes | 当前用户和配额摘要 |
| PUT | `/user/profile` | Yes | 更新昵称、头像、身份类型 |
| PUT | `/user/password` | Yes | 修改密码 |
| GET | `/user/quota` | Yes | 配额详情 |
| PUT | `/user/ai-consent` | Yes | AI 训练授权开关 |
| PUT | `/user/identity-type` | Yes | 设置首次登录身份类型和入口偏好 |
| POST | `/user/data/export` | Yes | 异步导出用户数据 |
| DELETE | `/user` | Yes | 注销账号 |
| POST | `/user/cancel-restore` | Yes | 30 天内撤销注销 |
| GET | `/user/style-profile` | Yes | 获取文风档案 |
| PUT | `/user/style-profile` | Yes | 保存文风样本并异步分析 |
| DELETE | `/user/style-profile` | Yes | 删除文风档案 |
| PUT | `/user/writing-goal` | Yes | 设置全局每日目标 |
| GET | `/style-templates` | Yes | 风格模板列表，query: `type=writing/polish` |
| POST | `/style-templates` | Yes | 创建写作/润色风格模板 |
| PUT | `/style-templates/{id}` | Yes | 更新风格模板 |
| PUT | `/style-templates/{id}/activate` | Yes | 激活风格模板，同类型唯一激活 |
| DELETE | `/style-templates/{id}` | Yes | 删除风格模板 |

## Projects, Volumes, Chapters

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | `/projects` | Yes | 作品列表，sort: `updated_at/created_at/word_count` |
| POST | `/projects` | Yes | 创建作品 |
| GET | `/projects/{id}` | Yes | 作品详情 |
| PUT | `/projects/{id}` | Yes | 更新作品 |
| DELETE | `/projects/{id}` | Yes | 删除作品，query: `confirm=true` |
| GET | `/projects/{id}/outline` | Yes | 卷章大纲树 |
| PUT | `/projects/{id}/writing-goal` | Yes | 设置作品每日目标 |
| POST | `/projects/{id}/volumes` | Yes | 新增卷 |
| PUT | `/volumes/{id}` | Yes | 更新卷 |
| DELETE | `/volumes/{id}` | Yes | 删除卷，query: `confirm=true` |
| POST | `/volumes/{id}/chapters` | Yes | 新增章节 |
| GET | `/chapters/{id}` | Yes | 章节详情 |
| PUT | `/chapters/{id}` | Yes | 保存章节内容、标题、状态、大纲 |
| DELETE | `/chapters/{id}` | Yes | 删除章节 |
| GET | `/chapters/{id}/context` | Yes | 编辑器章节上下文，聚合章节、角色、词典和关键事件 |
| GET | `/chapters/{id}/snapshots` | Yes | 快照列表 |
| POST | `/chapters/{id}/snapshots` | Yes | 手动创建快照 |
| POST | `/snapshots/{id}/restore` | Yes | 恢复快照 |

## Characters & World

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | `/projects/{id}/characters` | Yes | 角色列表 |
| POST | `/projects/{id}/characters` | Yes | 创建角色 |
| GET | `/characters/{id}` | Yes | 角色详情 |
| PUT | `/characters/{id}` | Yes | 更新角色 |
| DELETE | `/characters/{id}` | Yes | 删除角色 |
| PUT | `/characters/{id}/lock` | Yes | 锁定/解锁角色 |
| GET | `/projects/{id}/world-dict` | Yes | 词典列表 |
| POST | `/projects/{id}/world-dict` | Yes | 创建词条 |
| PUT | `/world-dict/{id}` | Yes | 更新词条 |
| DELETE | `/world-dict/{id}` | Yes | 删除词条 |

## Import, Search, Stats

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/import/parse` | Yes | 文件上传并解析章节预览 |
| PUT | `/import/{parseId}/adjust` | Yes | 调整切分点 |
| POST | `/import/{parseId}/apply` | Yes | 确认导入 |
| POST | `/import/paste` | Yes | 粘贴文本导入 |
| GET | `/projects/{id}/search` | Yes | 全书搜索 |
| POST | `/projects/{id}/search/replace` | Yes | 全书替换 |
| GET | `/stats/writing` | Yes | 写作统计总览 |
| GET | `/stats/writing/heatmap` | Yes | 写作热力图 |
| GET | `/stats/writing/projects` | Yes | 各作品进度 |
| GET | `/stats/writing/monthly/{yearMonth}` | Yes | 月度摘要 |

## AI Writing

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/novel/skeleton` | Yes | 异步生成故事骨架 |
| POST | `/skeleton/{taskId}/apply` | Yes | 应用骨架 |
| GET SSE | `/novel/continue` | Yes | 流式续写，query: `chapterId`, `instruction` |
| POST | `/novel/branch` | Yes | 剧情分支建议 |
| POST | `/polish/basic` | Yes | 基础校正 |
| POST | `/polish/advanced` | Yes | 进阶润色 |
| POST | `/polish/style` | Yes | 风格重塑 |
| POST | `/polish/custom` | Yes | 专属文风润色 |
| POST | `/consistency/check` | Yes | 异步一致性审查 |
| GET | `/consistency/reports/{id}` | Yes | 审查报告 |
| PATCH | `/consistency/items/{id}` | Yes | 更新审查条目状态 |

## Script

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/script/convert` | Yes | 异步小说转剧本 |
| GET | `/script/drafts/{id}` | Yes | 草稿详情 |
| GET | `/script/drafts/{id}/scenes` | Yes | 场景分页 |
| PUT | `/script/scenes/{id}` | Yes | 编辑场景，带 version |
| POST | `/script/drafts/{id}/export` | Yes | 导出剧本 |
| GET | `/script/projects/{projectId}/drafts` | Yes | 作品下剧本草稿 |

## Subscription

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | `/subscriptions/plans` | No | 订阅方案 |
| POST | `/subscriptions/checkout` | Yes | 创建支付订单 |
| POST | `/subscriptions/topup` | Yes | 购买字数包 |
| GET | `/subscriptions/current` | Yes | 当前订阅 |
| POST | `/subscriptions/cancel` | Yes | 取消自动续费 |
| POST | `/webhook/payment` | No | 支付回调，需要验签 |

## Content Safety & Feedback

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| GET | `/content-safety/policy` | No | 内容安全说明 |
| POST | `/content-safety/appeals` | Yes | 提交内容安全误报反馈 |

## Team

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/teams` | Yes | 创建团队 |
| GET | `/teams/current` | Yes | 当前团队与成员信息 |
| POST | `/teams/{id}/invites` | Yes | 邀请成员 |
| POST | `/teams/invites/{token}/accept` | Yes | 接受邀请 |
| DELETE | `/teams/{id}/members/{userId}` | Yes | 移除成员 |
| PUT | `/teams/{id}/members/{userId}/role` | Yes | 调整成员角色 |
| GET | `/teams/{id}/usage` | Yes | 团队统一账单与配额消耗 |
| POST | `/projects/{id}/collaborators` | Yes | 添加项目协作者 |
| DELETE | `/projects/{id}/collaborators/{userId}` | Yes | 移除项目协作者 |
