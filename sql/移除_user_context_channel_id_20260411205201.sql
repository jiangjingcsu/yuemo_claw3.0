-- 迁移: 移除 user_context 表的 channel_id 字段，修改主键
-- 时间: 20260411205201

BEGIN TRANSACTION;

ALTER TABLE user_context RENAME TO user_context_old;

CREATE TABLE user_context (
    user_id TEXT NOT NULL,
    current_session_id TEXT,
    last_active_time TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    metadata TEXT,
    PRIMARY KEY (user_id)
);

INSERT INTO user_context (user_id, current_session_id, last_active_time, metadata)
SELECT user_id, current_session_id, last_active_time, metadata FROM user_context_old;

DROP TABLE user_context_old;

COMMIT;
