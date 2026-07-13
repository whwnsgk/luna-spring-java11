-- 멤버와 Riot 정보는 유지하고 시즌/경기 데이터만 초기화
BEGIN;

TRUNCATE TABLE
    inhouse_match_change_log,
    inhouse_match_player,
    inhouse_match,
    season
RESTART IDENTITY CASCADE;

COMMIT;
