-- P5-04: 异步任务结果存储（骨架 JSON 等 LLM 输出）
ALTER TABLE ai_task_progress ADD COLUMN result_json TEXT;
