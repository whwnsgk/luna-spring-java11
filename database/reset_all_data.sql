-- ============================================================
-- Rift Arena 전체 데이터 초기화
-- PostgreSQL
--
-- 경고: 모든 멤버, Riot 통계, 시즌, 경기, 수정 이력이 삭제됩니다.
-- 테이블 구조는 유지되고 ID 시퀀스는 1부터 다시 시작합니다.
-- ============================================================

BEGIN;

TRUNCATE TABLE
    inhouse_match_change_log,
    inhouse_match_player,
    inhouse_match,
    member_champion_stat,
    member_riot_snapshot,
    member_position,
    season,
    member
RESTART IDENTITY CASCADE;

COMMIT;

-- 결과 확인
SELECT 'member' AS table_name, COUNT(*) AS row_count FROM member
UNION ALL SELECT 'member_position', COUNT(*) FROM member_position
UNION ALL SELECT 'member_riot_snapshot', COUNT(*) FROM member_riot_snapshot
UNION ALL SELECT 'member_champion_stat', COUNT(*) FROM member_champion_stat
UNION ALL SELECT 'season', COUNT(*) FROM season
UNION ALL SELECT 'inhouse_match', COUNT(*) FROM inhouse_match
UNION ALL SELECT 'inhouse_match_player', COUNT(*) FROM inhouse_match_player
UNION ALL SELECT 'inhouse_match_change_log', COUNT(*) FROM inhouse_match_change_log
ORDER BY table_name;
