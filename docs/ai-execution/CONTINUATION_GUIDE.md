# AI Continuation Guide

本文件用于第一轮之后继续启动 AI 实现任务。目标是让后续 AI 不重新解释项目、不自由挑功能，而是基于进度台账连续推进。

## 后续启动 Prompt

每次新开一轮实现时，将下面提示词交给 AI：

```text
继续实现 F:\software\IdeaProject\novel-ai 项目。

请先读取：
1. docs/ai-execution/README.md
2. docs/ai-execution/EXECUTION_RULES.md
3. docs/ai-execution/MASTER_BACKLOG.md
4. docs/ai-execution/API_CONTRACT.md
5. docs/ai-execution/ACCEPTANCE_CRITERIA.md
6. docs/ai-execution/PROGRESS.md
7. docs/ai-execution/STORAGE_COS.md
8. docs/ai-execution/COVERAGE_MATRIX.md
9. docs/ai-execution/CONTINUATION_GUIDE.md

然后按规则继续：
- 查看 PROGRESS.md 当前阶段和阻塞记录。
- 从 MASTER_BACKLOG.md 选择下一个依赖已满足的 TODO 任务。
- 优先完成当前 Phase，不要跨阶段跳功能。
- 开始前把任务状态改为 DOING。
- 实现后运行验证。
- 验证通过后把任务状态改为 DONE。
- 更新 PROGRESS.md 的实现日志和验证记录。
- 若失败无法解决，标记 BLOCKED 并写明阻塞原因。
- 对象存储必须使用腾讯云 COS，禁止恢复 MinIO。
- Maven 使用 JDK 21：F:\jdk21。

如果 P0 已全部 DONE，请从 P1-01 开始，按 P1-01、P1-02、P1-03 的顺序推进。
```

## 推荐续跑节奏

- 每轮只做 1 到 3 个 backlog 任务。
- 同一轮只做同一阶段任务，不要跨阶段混做。
- 每轮结束必须留下干净状态：测试通过、backlog 状态更新、progress 更新。
- 如果一个 Phase 全部 `DONE`，下一轮在 prompt 中明确要求进入下一个 Phase。

## 阶段切换规则

| 当前完成情况 | 下一阶段 |
| --- | --- |
| P0 全部 DONE | 进入 P1 账号与用户 |
| P1 全部 DONE | 进入 P2 作品、卷章与快照 |
| P2 全部 DONE | 进入 P3 角色库与世界观词典 |
| P3 全部 DONE | 进入 P4 导入、搜索替换、写作统计 |
| P4 全部 DONE | 进入 P5 AI 写作与润色 |
| P1-P4 后端主链路稳定 | 可启动 P8 前端工作台 |

P10 为已登记的后续生态能力，默认不进入 V1 实现。

## 每轮验收命令

```powershell
$env:JAVA_HOME='F:\jdk21'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn test
```

若本轮实现了接口，还必须自查：

- 是否符合 `API_CONTRACT.md`。
- 是否满足 `ACCEPTANCE_CRITERIA.md`。
- 是否更新 `MASTER_BACKLOG.md` 和 `PROGRESS.md`。

