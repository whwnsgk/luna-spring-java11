# CHANGELOG v3.3.4

- gameName 내부 공백을 삭제하지 않고 그대로 Riot API에 전달
- gameName 앞뒤 공백과 NBSP/zero-width/BOM만 정리
- tagLine은 모든 공백 제거 및 대문자 변환
- MyBatis Map이 camelCase 또는 snake_case 키를 반환해도 Riot ID를 읽도록 보강
- 400 오류 메시지에 gameName/tagLine 누락 여부 표시
- Riot API Bad Request 안내 문구 개선
- `database/diagnose_and_fix_riot_id_spaces.sql` 추가
