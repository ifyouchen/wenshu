-- P3-04: 专有名词词典别名支持
-- world_elements 表新增 aliases 列，存储 JSON 字符串数组，例如 ["别名一","别名二"]
ALTER TABLE world_elements ADD COLUMN aliases TEXT NOT NULL DEFAULT '[]';
