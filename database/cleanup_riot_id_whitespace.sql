-- 기존 member 테이블에 저장된 Riot ID 앞뒤 공백/태그 공백 정리
UPDATE member
   SET game_name = BTRIM(
           REPLACE(
             REPLACE(
               REPLACE(game_name, CHR(160), ' '),
             CHR(8203), ''),
           CHR(65279), '')
       ),
       tag_line = UPPER(
           REGEXP_REPLACE(
             REPLACE(
               REPLACE(
                 REPLACE(tag_line, CHR(160), ''),
               CHR(8203), ''),
             CHR(65279), ''),
           '\s+', '', 'g'
           )
       ),
       updated_at = CURRENT_TIMESTAMP
 WHERE game_name IS NOT NULL
    OR tag_line IS NOT NULL;

SELECT member_id, real_name, game_name, tag_line
  FROM member
 ORDER BY member_id;
