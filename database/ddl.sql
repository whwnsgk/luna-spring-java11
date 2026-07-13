CREATE TABLE member (
 member_id BIGSERIAL PRIMARY KEY,
 real_name VARCHAR(50) NOT NULL,
 game_name VARCHAR(100) NOT NULL,
 tag_line VARCHAR(20) NOT NULL,
 puuid VARCHAR(100),
 solo_tier VARCHAR(20) NOT NULL DEFAULT 'UNRANKED',
 solo_rank VARCHAR(5),
 solo_lp INTEGER NOT NULL DEFAULT 0,
 balance_score INTEGER NOT NULL DEFAULT 1000,
 active_yn BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uk_member_riot_id UNIQUE(game_name,tag_line),
 CONSTRAINT ck_member_lp CHECK(solo_lp BETWEEN 0 AND 100)
);
COMMENT ON TABLE member IS '내전 멤버';
CREATE TABLE member_position(
 member_id BIGINT NOT NULL REFERENCES member(member_id) ON DELETE CASCADE,
 position_code VARCHAR(10) NOT NULL,
 priority_no SMALLINT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(member_id,position_code),
 CONSTRAINT ck_position CHECK(position_code IN('TOP','JUNGLE','MID','ADC','SUPPORT','FILL'))
);
CREATE TABLE inhouse_match(
 match_id BIGSERIAL PRIMARY KEY,
 played_at TIMESTAMP NOT NULL,
 winner_team VARCHAR(4) NOT NULL CHECK(winner_team IN('BLUE','RED')),
 memo VARCHAR(1000),
 blue_cost INTEGER NOT NULL,
 red_cost INTEGER NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE inhouse_match_player(
 match_player_id BIGSERIAL PRIMARY KEY,
 match_id BIGINT NOT NULL REFERENCES inhouse_match(match_id) ON DELETE CASCADE,
 member_id BIGINT NOT NULL REFERENCES member(member_id),
 team_code VARCHAR(4) NOT NULL CHECK(team_code IN('BLUE','RED')),
 position_code VARCHAR(10),
 champion_name VARCHAR(50),
 kills INTEGER NOT NULL DEFAULT 0,
 deaths INTEGER NOT NULL DEFAULT 0,
 assists INTEGER NOT NULL DEFAULT 0,
 win_yn BOOLEAN NOT NULL,
 mvp_yn BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(match_id,member_id),
 CHECK(kills>=0 AND deaths>=0 AND assists>=0)
);
CREATE INDEX ix_member_active ON member(active_yn,real_name);
CREATE INDEX ix_match_date ON inhouse_match(played_at DESC);
CREATE INDEX ix_match_player_member ON inhouse_match_player(member_id,match_id);


-- 2차 Riot API 확장
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
