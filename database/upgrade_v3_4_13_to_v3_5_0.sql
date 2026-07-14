ALTER TABLE member_lane_profile
  DROP CONSTRAINT IF EXISTS ck_lane_preference_score;

ALTER TABLE member_lane_profile
  ADD CONSTRAINT ck_lane_preference_score
  CHECK(preference_score IN(0,1,2,3));

CREATE TABLE IF NOT EXISTS balance_weight_setting(
 setting_id SMALLINT PRIMARY KEY DEFAULT 1,
 matchup_diff_weight NUMERIC(10,3) NOT NULL DEFAULT 1.000,
 team_diff_weight NUMERIC(10,3) NOT NULL DEFAULT 0.350,
 skill1_penalty NUMERIC(10,3) NOT NULL DEFAULT 120.000,
 skill3_penalty NUMERIC(10,3) NOT NULL DEFAULT 300.000,
 crowding_penalty NUMERIC(10,3) NOT NULL DEFAULT 50.000,
 top_tier_support_penalty NUMERIC(10,3) NOT NULL DEFAULT 1000000.000,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT ck_balance_setting_single CHECK(setting_id=1)
);
INSERT INTO balance_weight_setting(setting_id) VALUES(1)
ON CONFLICT(setting_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS match_balance_feature(
 match_id BIGINT PRIMARY KEY REFERENCES inhouse_match(match_id) ON DELETE CASCADE,
 matchup_diff NUMERIC(12,2) NOT NULL DEFAULT 0,
 team_diff NUMERIC(12,2) NOT NULL DEFAULT 0,
 skill1_count INTEGER NOT NULL DEFAULT 0,
 skill2_count INTEGER NOT NULL DEFAULT 0,
 skill3_count INTEGER NOT NULL DEFAULT 0,
 crowding_count INTEGER NOT NULL DEFAULT 0,
 top_tier_support_count INTEGER NOT NULL DEFAULT 0,
 algorithm_score NUMERIC(14,2) NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS match_balance_vote(
 vote_id BIGSERIAL PRIMARY KEY,
 match_id BIGINT NOT NULL REFERENCES inhouse_match(match_id) ON DELETE CASCADE,
 voter_key VARCHAR(100) NOT NULL,
 vote_value VARCHAR(10) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT ck_balance_vote CHECK(vote_value IN('PERFECT','NORMAL','BAD')),
 CONSTRAINT uk_match_balance_voter UNIQUE(match_id,voter_key)
);
CREATE INDEX IF NOT EXISTS ix_balance_vote_match ON match_balance_vote(match_id,vote_value);
