-- Rift Arena v2.2 신규 설치용 전체 DDL
CREATE TABLE member (
 member_id BIGSERIAL PRIMARY KEY,
 real_name VARCHAR(50) NOT NULL,
 game_name VARCHAR(100) NOT NULL,
 tag_line VARCHAR(20) NOT NULL,
 puuid VARCHAR(100),
 summoner_id VARCHAR(100),
 profile_icon_id INTEGER,
 summoner_level BIGINT,
 solo_tier VARCHAR(20) NOT NULL DEFAULT 'UNRANKED',
 solo_rank VARCHAR(5),
 solo_lp INTEGER NOT NULL DEFAULT 0,
 recent_wins INTEGER NOT NULL DEFAULT 0,
 recent_losses INTEGER NOT NULL DEFAULT 0,
 recent_win_rate NUMERIC(5,1) NOT NULL DEFAULT 0,
 recent_avg_kills NUMERIC(6,1) NOT NULL DEFAULT 0,
 recent_avg_deaths NUMERIC(6,1) NOT NULL DEFAULT 0,
 recent_avg_assists NUMERIC(6,1) NOT NULL DEFAULT 0,
 most_position VARCHAR(10),
 balance_score INTEGER NOT NULL DEFAULT 1000,
 external_yn BOOLEAN NOT NULL DEFAULT FALSE,
 manual_tier_yn BOOLEAN NOT NULL DEFAULT FALSE,
 active_yn BOOLEAN NOT NULL DEFAULT TRUE,
 riot_updated_at TIMESTAMP,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uk_member_riot_id UNIQUE(game_name,tag_line),
 CONSTRAINT ck_member_lp CHECK(solo_lp BETWEEN 0 AND 100)
);
CREATE TABLE member_position(
 member_id BIGINT NOT NULL REFERENCES member(member_id) ON DELETE CASCADE,
 position_code VARCHAR(10) NOT NULL,
 priority_no SMALLINT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(member_id,position_code),
 CONSTRAINT ck_position CHECK(position_code IN('TOP','JUNGLE','MID','ADC','SUPPORT','FILL'))
);


CREATE TABLE member_lane_profile(
 member_id BIGINT NOT NULL REFERENCES member(member_id) ON DELETE CASCADE,
 position_code VARCHAR(10) NOT NULL,
 preference_score SMALLINT NOT NULL DEFAULT 0,
 champion_count INTEGER NOT NULL DEFAULT 0,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(member_id,position_code),
 CONSTRAINT ck_lane_profile_position CHECK(position_code IN('TOP','JUNGLE','MID','ADC','SUPPORT')),
 CONSTRAINT ck_lane_preference_score CHECK(preference_score IN(0,1,2)),
 CONSTRAINT ck_lane_champion_count CHECK(champion_count>=0)
);

CREATE TABLE season(
 season_id BIGSERIAL PRIMARY KEY,
 season_name VARCHAR(100) NOT NULL,
 start_date DATE NOT NULL,
 end_date DATE,
 active_yn BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT ck_season_period CHECK(end_date IS NULL OR end_date>=start_date)
);
CREATE UNIQUE INDEX ux_season_single_active ON season((active_yn)) WHERE active_yn=TRUE;
INSERT INTO season(season_name,start_date,active_yn) VALUES('통산 시즌',CURRENT_DATE,TRUE);
CREATE TABLE inhouse_match(
 match_id BIGSERIAL PRIMARY KEY,
 season_id BIGINT NOT NULL REFERENCES season(season_id),
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
 position_code VARCHAR(10),champion_name VARCHAR(50),
 kills INTEGER NOT NULL DEFAULT 0,deaths INTEGER NOT NULL DEFAULT 0,assists INTEGER NOT NULL DEFAULT 0,
 win_yn BOOLEAN NOT NULL,mvp_yn BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(match_id,member_id),CHECK(kills>=0 AND deaths>=0 AND assists>=0)
);
CREATE TABLE member_riot_snapshot(
 snapshot_id BIGSERIAL PRIMARY KEY,member_id BIGINT NOT NULL REFERENCES member(member_id),
 solo_tier VARCHAR(20) NOT NULL,solo_rank VARCHAR(5),solo_lp INTEGER NOT NULL DEFAULT 0,
 recent_wins INTEGER NOT NULL DEFAULT 0,recent_losses INTEGER NOT NULL DEFAULT 0,recent_win_rate NUMERIC(5,1) NOT NULL DEFAULT 0,
 recent_avg_kills NUMERIC(6,1) NOT NULL DEFAULT 0,recent_avg_deaths NUMERIC(6,1) NOT NULL DEFAULT 0,recent_avg_assists NUMERIC(6,1) NOT NULL DEFAULT 0,
 most_position VARCHAR(10),collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE member_champion_stat(
 member_champion_stat_id BIGSERIAL PRIMARY KEY,member_id BIGINT NOT NULL REFERENCES member(member_id) ON DELETE CASCADE,
 stat_scope VARCHAR(20) NOT NULL,champion_name VARCHAR(50) NOT NULL,games INTEGER NOT NULL DEFAULT 0,wins INTEGER NOT NULL DEFAULT 0,
 rank_no SMALLINT NOT NULL,updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uk_member_champion_scope UNIQUE(member_id,stat_scope,champion_name)
);
CREATE INDEX ix_member_active ON member(active_yn,real_name);
CREATE INDEX ix_match_season_date ON inhouse_match(season_id,played_at DESC);
CREATE INDEX ix_match_player_member ON inhouse_match_player(member_id,match_id);
CREATE INDEX ix_riot_snapshot_member_date ON member_riot_snapshot(member_id,collected_at DESC);
CREATE INDEX ix_champion_stat_member_scope ON member_champion_stat(member_id,stat_scope,rank_no);
