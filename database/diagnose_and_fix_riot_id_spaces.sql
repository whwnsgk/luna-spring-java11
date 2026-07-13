-- 특정 멤버 Riot ID 확인 및 공백 문자 진단
SELECT
    member_id,
    real_name,
    game_name,
    tag_line,
    LENGTH(game_name) AS game_name_length,
    LENGTH(tag_line) AS tag_line_length,
    ENCODE(CONVERT_TO(game_name, 'UTF8'), 'hex') AS game_name_utf8_hex,
    ENCODE(CONVERT_TO(tag_line, 'UTF8'), 'hex') AS tag_line_utf8_hex
FROM member
ORDER BY member_id;

-- 앞뒤 특수 공백만 정리합니다.
-- game_name 가운데 일반 공백은 Riot ID의 일부이므로 제거하지 않습니다.
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
        '\s+', '', 'g')
    ),
    updated_at = CURRENT_TIMESTAMP;
