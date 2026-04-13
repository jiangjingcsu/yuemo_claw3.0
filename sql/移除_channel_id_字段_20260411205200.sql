-- 迁移: 移除 chat_session 表的 channel_id 字段
-- 时间: 20260411205200

BEGIN TRANSACTION;

ALTER TABLE chat_session RENAME TO chat_session_old;

CREATE TABLE chat_session (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    session_title TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    is_active INTEGER NOT NULL DEFAULT 1,
    metadata TEXT
);

INSERT INTO chat_session (id, user_id, session_title, created_at, updated_at, is_active, metadata)
SELECT id, user_id, session_title, created_at, updated_at, is_active, metadata FROM chat_session_old;

DROP TABLE chat_session_old;

COMMIT;
