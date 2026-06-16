-- P0-1 修复：为 writing_daily_stats 表添加 peak_hour 字段，记录当日写作高峰小时（-1 表示未记录）
ALTER TABLE writing_daily_stats ADD COLUMN IF NOT EXISTS peak_hour INT DEFAULT -1;
