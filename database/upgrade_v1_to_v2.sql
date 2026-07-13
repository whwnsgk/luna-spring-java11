-- Rift Arena 1차 DB -> 2차 DB 무중단 확장 DDL
-- 기존 member/inhouse_match 데이터는 유지됩니다.
ALTER TABLE member ADD COLUMN IF NOT EXISTS summoner_id VARCHAR(100);
ALTER TABLE member ADD COLUMN IF NOT EXISTS summoner_level BIGINT;
ALTER TABLE member ADD COLUMN IF NOT EXISTS recent_win_rate NUMERIC(5,1) NOT NULL DEFAULT 0;
ALTER TABLE member ADD COLUMN IF NOT EXISTS recent_avg_kills NUMERIC(6,1) NOT NULL DEFAULT 0;
ALTER TABLE member ADD COLUMN IF NOT EXISTS recent_avg_deaths NUMERIC(6,1) NOT NULL DEFAULT 0;
ALTER TABLE member ADD COLUMN IF NOT EXISTS recent_avg_assists NUMERIC(6,1) NOT NULL DEFAULT 0;
ALTER TABLE member ADD COLUMN IF NOT EXISTS most_position VARCHAR(10);

CREATE TABLE IF NOT EXISTS member_riot_snapshot (
 snapshot_id BIGSERIAL PRIMARY KEY, member_id BIGINT NOT NULL REFERENCES member(member_id),
 solo_tier VARCHAR(20) NOT NULL, solo_rank VARCHAR(5), solo_lp INTEGER NOT NULL DEFAULT 0,
 recent_wins INTEGER NOT NULL DEFAULT 0, recent_losses INTEGER NOT NULL DEFAULT 0,
 recent_win_rate NUMERIC(5,1) NOT NULL DEFAULT 0,
 recent_avg_kills NUMERIC(6,1) NOT NULL DEFAULT 0, recent_avg_deaths NUMERIC(6,1) NOT NULL DEFAULT 0,
 recent_avg_assists NUMERIC(6,1) NOT NULL DEFAULT 0, most_position VARCHAR(10), collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS member_champion_stat (
 member_champion_stat_id BIGSERIAL PRIMARY KEY, member_id BIGINT NOT NULL REFERENCES member(member_id) ON DELETE CASCADE,
 stat_scope VARCHAR(20) NOT NULL, champion_name VARCHAR(50) NOT NULL, games INTEGER NOT NULL DEFAULT 0,
 wins INTEGER NOT NULL DEFAULT 0, rank_no SMALLINT NOT NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uk_member_champion_scope UNIQUE(member_id,stat_scope,champion_name)
);
CREATE INDEX IF NOT EXISTS ix_riot_snapshot_member_date ON member_riot_snapshot(member_id,collected_at DESC);
CREATE INDEX IF NOT EXISTS ix_champion_stat_member_scope ON member_champion_stat(member_id,stat_scope,rank_no);
