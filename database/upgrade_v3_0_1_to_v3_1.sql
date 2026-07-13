ALTER TABLE inhouse_match ADD COLUMN IF NOT EXISTS force_yn BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE inhouse_match ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE inhouse_match ADD COLUMN IF NOT EXISTS update_reason VARCHAR(500);
CREATE TABLE IF NOT EXISTS inhouse_match_change_log(
 change_log_id BIGSERIAL PRIMARY KEY,
 match_id BIGINT NOT NULL REFERENCES inhouse_match(match_id) ON DELETE CASCADE,
 changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 changed_by VARCHAR(100),
 change_reason VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS ix_match_change_log_match ON inhouse_match_change_log(match_id,changed_at DESC);
