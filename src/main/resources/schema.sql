CREATE TABLE IF NOT EXISTS luna_call_count (
    id BIGINT PRIMARY KEY,
    call_count BIGINT NOT NULL DEFAULT 0,
    last_call_dtm TIMESTAMP
);

INSERT INTO luna_call_count (
    id,
    call_count,
    last_call_dtm
)
VALUES (
    1,
    0,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;
