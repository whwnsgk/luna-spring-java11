ALTER TABLE member
  ADD COLUMN IF NOT EXISTS external_yn BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE member
  ADD COLUMN IF NOT EXISTS manual_tier_yn BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS member_lane_profile(
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

-- 기존 선호 포지션은 1순위=2점, 2순위=1점으로 이관합니다.
-- 과거 3순위/FILL은 새 규칙상 자동 이관하지 않고 0점으로 둡니다.
INSERT INTO member_lane_profile(member_id,position_code,preference_score,champion_count)
SELECT m.member_id,
       lane.position_code,
       COALESCE(
         MAX(
           CASE
             WHEN mp.priority_no=1 AND mp.position_code=lane.position_code THEN 2
             WHEN mp.priority_no=2 AND mp.position_code=lane.position_code THEN 1
             ELSE 0
           END
         ),0
       )::SMALLINT,
       0
  FROM member m
 CROSS JOIN (VALUES('TOP'),('JUNGLE'),('MID'),('ADC'),('SUPPORT')) lane(position_code)
  LEFT JOIN member_position mp
    ON mp.member_id=m.member_id
   AND mp.position_code=lane.position_code
 GROUP BY m.member_id,lane.position_code
ON CONFLICT(member_id,position_code) DO NOTHING;

-- 실행 후 멤버 화면에서 각 라인별 기용 가능한 챔피언 수를 입력하고 저장하세요.
-- 저장 즉시 전체 레이팅이 재계산됩니다.
