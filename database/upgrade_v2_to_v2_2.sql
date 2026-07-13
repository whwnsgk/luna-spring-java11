-- =========================================================
-- Rift Arena v2.1 -> v2.2 업그레이드 DDL
-- 기존 멤버/경기/Riot 데이터는 유지됩니다.
-- DBeaver에서 현재 Render PostgreSQL DB에 전체 실행하세요.
-- =========================================================

-- 과거 버전별 차이까지 함께 보정
ALTER TABLE member ADD COLUMN IF NOT EXISTS puuid VARCHAR(100);
ALTER TABLE member ADD COLUMN IF NOT EXISTS summoner_id VARCHAR(100);
ALTER TABLE member ADD COLUMN IF NOT EXISTS profile_icon_id INTEGER;
ALTER TABLE member ADD COLUMN IF NOT EXISTS summoner_level BIGINT;
ALTER TABLE member ADD COLUMN IF NOT EXISTS recent_wins INTEGER NOT NULL DEFAULT 0;
ALTER TABLE member ADD COLUMN IF NOT EXISTS recent_losses INTEGER NOT NULL DEFAULT 0;
ALTER TABLE member ADD COLUMN IF NOT EXISTS recent_win_rate NUMERIC(5,1) NOT NULL DEFAULT 0;
ALTER TABLE member ADD COLUMN IF NOT EXISTS recent_avg_kills NUMERIC(6,1) NOT NULL DEFAULT 0;
ALTER TABLE member ADD COLUMN IF NOT EXISTS recent_avg_deaths NUMERIC(6,1) NOT NULL DEFAULT 0;
ALTER TABLE member ADD COLUMN IF NOT EXISTS recent_avg_assists NUMERIC(6,1) NOT NULL DEFAULT 0;
ALTER TABLE member ADD COLUMN IF NOT EXISTS most_position VARCHAR(10);
ALTER TABLE member ADD COLUMN IF NOT EXISTS riot_updated_at TIMESTAMP;
ALTER TABLE member ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS member_riot_snapshot (
 snapshot_id BIGSERIAL PRIMARY KEY,
 member_id BIGINT NOT NULL REFERENCES member(member_id),
 solo_tier VARCHAR(20) NOT NULL,
 solo_rank VARCHAR(5),
 solo_lp INTEGER NOT NULL DEFAULT 0,
 recent_wins INTEGER NOT NULL DEFAULT 0,
 recent_losses INTEGER NOT NULL DEFAULT 0,
 recent_win_rate NUMERIC(5,1) NOT NULL DEFAULT 0,
 recent_avg_kills NUMERIC(6,1) NOT NULL DEFAULT 0,
 recent_avg_deaths NUMERIC(6,1) NOT NULL DEFAULT 0,
 recent_avg_assists NUMERIC(6,1) NOT NULL DEFAULT 0,
 most_position VARCHAR(10),
 collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS member_champion_stat (
 member_champion_stat_id BIGSERIAL PRIMARY KEY,
 member_id BIGINT NOT NULL REFERENCES member(member_id) ON DELETE CASCADE,
 stat_scope VARCHAR(20) NOT NULL,
 champion_name VARCHAR(50) NOT NULL,
 games INTEGER NOT NULL DEFAULT 0,
 wins INTEGER NOT NULL DEFAULT 0,
 rank_no SMALLINT NOT NULL,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uk_member_champion_scope UNIQUE(member_id,stat_scope,champion_name)
);

-- 시즌
CREATE TABLE IF NOT EXISTS season (
 season_id BIGSERIAL PRIMARY KEY,
 season_name VARCHAR(100) NOT NULL,
 start_date DATE NOT NULL,
 end_date DATE,
 active_yn BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT ck_season_period CHECK(end_date IS NULL OR end_date >= start_date)
);

INSERT INTO season(season_name,start_date,end_date,active_yn)
SELECT '통산 시즌', CURRENT_DATE, NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM season);

ALTER TABLE inhouse_match ADD COLUMN IF NOT EXISTS season_id BIGINT;

UPDATE inhouse_match
   SET season_id=(SELECT season_id FROM season ORDER BY active_yn DESC,season_id LIMIT 1)
 WHERE season_id IS NULL;

ALTER TABLE inhouse_match
    ALTER COLUMN season_id SET NOT NULL;

DO $$
BEGIN
 IF NOT EXISTS (
   SELECT 1 FROM pg_constraint WHERE conname='fk_inhouse_match_season'
 ) THEN
   ALTER TABLE inhouse_match
     ADD CONSTRAINT fk_inhouse_match_season
     FOREIGN KEY(season_id) REFERENCES season(season_id);
 END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_season_single_active
    ON season ((active_yn)) WHERE active_yn=TRUE;
CREATE INDEX IF NOT EXISTS ix_match_season_date
    ON inhouse_match(season_id,played_at DESC);
CREATE INDEX IF NOT EXISTS ix_riot_snapshot_member_date
    ON member_riot_snapshot(member_id,collected_at DESC);
CREATE INDEX IF NOT EXISTS ix_champion_stat_member_scope
    ON member_champion_stat(member_id,stat_scope,rank_no);

-- 확인
SELECT table_name
  FROM information_schema.tables
 WHERE table_schema='public'
   AND table_name IN ('season','member_riot_snapshot','member_champion_stat')
 ORDER BY table_name;
