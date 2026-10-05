CREATE TABLE IF NOT EXISTS posts (
    chat_id TEXT NOT NULL,
    message_id INTEGER NOT NULL,
    posted_at INTEGER NOT NULL,
    event_at INTEGER NOT NULL,
    update_id INTEGER NOT NULL,
    matches INTEGER NOT NULL,
    excerpt TEXT NOT NULL,
    telegram_url TEXT NOT NULL,
    PRIMARY KEY (chat_id, message_id)
);

CREATE INDEX IF NOT EXISTS posts_feed_order
    ON posts (matches, posted_at DESC, message_id DESC);
