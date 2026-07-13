-- ============================================================
-- Rift Arena v2.2.2 내전 테스트 전적 생성
-- 전제: active_yn=true 멤버가 최소 10명 존재해야 합니다.
--
-- 특징
-- 1) 현재 활성 시즌에 8경기를 생성합니다.
-- 2) 매 경기 팀 구성을 회전시켜 궁합/상대전적 검증이 가능합니다.
-- 3) 메모가 [RIFT_SAMPLE_V222_%]인 데이터가 이미 있으면 중복 생성하지 않습니다.
-- 4) 실제 데이터 삭제 없이 샘플만 별도로 식별할 수 있습니다.
-- ============================================================

DO $$
DECLARE
    v_member_ids BIGINT[];
    v_season_id BIGINT;
    v_match_id BIGINT;
    v_game INTEGER;
    v_slot INTEGER;
    v_idx INTEGER;
    v_member_id BIGINT;
    v_team VARCHAR(4);
    v_position VARCHAR(10);
    v_champion VARCHAR(50);
    v_winner VARCHAR(4);
    v_is_win BOOLEAN;
    v_kills INTEGER;
    v_deaths INTEGER;
    v_assists INTEGER;
    v_balance_score INTEGER;
    v_blue_cost INTEGER;
    v_red_cost INTEGER;
BEGIN
    IF EXISTS (
        SELECT 1
          FROM inhouse_match
         WHERE memo LIKE '[RIFT_SAMPLE_V222_%'
    ) THEN
        RAISE NOTICE 'v2.2.2 샘플 전적이 이미 존재하여 생성을 건너뜁니다.';
        RETURN;
    END IF;

    SELECT ARRAY_AGG(member_id ORDER BY member_id)
      INTO v_member_ids
      FROM (
          SELECT member_id
            FROM member
           WHERE active_yn = TRUE
           ORDER BY member_id
           LIMIT 10
      ) x;

    IF COALESCE(array_length(v_member_ids, 1), 0) < 10 THEN
        RAISE EXCEPTION '활성 멤버가 10명 이상 필요합니다. 현재: %명',
            COALESCE(array_length(v_member_ids, 1), 0);
    END IF;

    SELECT season_id
      INTO v_season_id
      FROM season
     WHERE active_yn = TRUE
     ORDER BY season_id DESC
     LIMIT 1;

    IF v_season_id IS NULL THEN
        INSERT INTO season (season_name, start_date, active_yn)
        VALUES ('테스트 시즌', CURRENT_DATE, TRUE)
        RETURNING season_id INTO v_season_id;
    END IF;

    FOR v_game IN 1..8 LOOP
        v_winner := CASE WHEN v_game IN (1, 3, 4, 7, 8) THEN 'BLUE' ELSE 'RED' END;

        v_blue_cost := 0;
        v_red_cost := 0;

        -- 회전된 팀 편성 기준으로 당시 코스트를 먼저 계산합니다.
        FOR v_slot IN 1..10 LOOP
            v_idx := ((v_slot + v_game - 2) % 10) + 1;
            v_member_id := v_member_ids[v_idx];

            SELECT balance_score
              INTO v_balance_score
              FROM member
             WHERE member_id = v_member_id;

            IF v_slot <= 5 THEN
                v_blue_cost := v_blue_cost + COALESCE(v_balance_score, 1000);
            ELSE
                v_red_cost := v_red_cost + COALESCE(v_balance_score, 1000);
            END IF;
        END LOOP;

        INSERT INTO inhouse_match (
            season_id,
            played_at,
            winner_team,
            memo,
            blue_cost,
            red_cost
        ) VALUES (
            v_season_id,
            CURRENT_TIMESTAMP - ((9 - v_game) * INTERVAL '1 day'),
            v_winner,
            '[RIFT_SAMPLE_V222_' || LPAD(v_game::TEXT, 2, '0') || '] 궁합/통계 테스트 경기',
            v_blue_cost,
            v_red_cost
        )
        RETURNING match_id INTO v_match_id;

        FOR v_slot IN 1..10 LOOP
            -- 경기마다 멤버 순서를 회전시켜 팀 조합을 변화시킵니다.
            v_idx := ((v_slot + v_game - 2) % 10) + 1;
            v_member_id := v_member_ids[v_idx];
            v_team := CASE WHEN v_slot <= 5 THEN 'BLUE' ELSE 'RED' END;
            v_position := CASE ((v_slot - 1) % 5)
                WHEN 0 THEN 'TOP'
                WHEN 1 THEN 'JUNGLE'
                WHEN 2 THEN 'MID'
                WHEN 3 THEN 'ADC'
                ELSE 'SUPPORT'
            END;

            v_champion := CASE v_position
                WHEN 'TOP' THEN CASE WHEN (v_game + v_slot) % 3 = 0 THEN 'Ornn'
                                     WHEN (v_game + v_slot) % 3 = 1 THEN 'Garen'
                                     ELSE 'Aatrox' END
                WHEN 'JUNGLE' THEN CASE WHEN (v_game + v_slot) % 3 = 0 THEN 'LeeSin'
                                        WHEN (v_game + v_slot) % 3 = 1 THEN 'Vi'
                                        ELSE 'Nocturne' END
                WHEN 'MID' THEN CASE WHEN (v_game + v_slot) % 3 = 0 THEN 'Ahri'
                                     WHEN (v_game + v_slot) % 3 = 1 THEN 'Syndra'
                                     ELSE 'Orianna' END
                WHEN 'ADC' THEN CASE WHEN (v_game + v_slot) % 3 = 0 THEN 'Jinx'
                                     WHEN (v_game + v_slot) % 3 = 1 THEN 'KaiSa'
                                     ELSE 'Ezreal' END
                ELSE CASE WHEN (v_game + v_slot) % 3 = 0 THEN 'Nautilus'
                          WHEN (v_game + v_slot) % 3 = 1 THEN 'Lulu'
                          ELSE 'Thresh' END
            END;

            v_is_win := (v_team = v_winner);
            v_kills := CASE WHEN v_is_win THEN ((v_game * 3 + v_slot) % 9) + 2
                            ELSE ((v_game + v_slot) % 5) END;
            v_deaths := CASE WHEN v_is_win THEN ((v_game + v_slot) % 4)
                             ELSE ((v_game * 2 + v_slot) % 6) + 2 END;
            v_assists := CASE WHEN v_is_win THEN ((v_game * 4 + v_slot) % 13) + 5
                              ELSE ((v_game * 2 + v_slot) % 8) + 1 END;

            INSERT INTO inhouse_match_player (
                match_id,
                member_id,
                team_code,
                position_code,
                champion_name,
                kills,
                deaths,
                assists,
                win_yn,
                mvp_yn
            ) VALUES (
                v_match_id,
                v_member_id,
                v_team,
                v_position,
                v_champion,
                v_kills,
                v_deaths,
                v_assists,
                v_is_win,
                (v_is_win AND v_slot = CASE WHEN v_winner='BLUE' THEN 3 ELSE 8 END)
            );
        END LOOP;
    END LOOP;

    RAISE NOTICE '내전 테스트 전적 8경기 생성 완료';
END $$;

-- 생성 결과 확인
SELECT im.match_id,
       s.season_name,
       im.played_at,
       im.winner_team,
       im.memo,
       COUNT(mp.match_player_id) AS player_count
  FROM inhouse_match im
  JOIN season s
    ON s.season_id = im.season_id
  JOIN inhouse_match_player mp
    ON mp.match_id = im.match_id
 WHERE im.memo LIKE '[RIFT_SAMPLE_V222_%'
 GROUP BY im.match_id, s.season_name
 ORDER BY im.played_at;

-- 샘플 전적만 제거할 때 사용하는 쿼리
-- DELETE FROM inhouse_match
--  WHERE memo LIKE '[RIFT_SAMPLE_V222_%';
