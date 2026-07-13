# CHANGELOG v3.3.3

## Riot ID 공백 처리

- Riot 갱신 전 gameName 앞뒤 공백 제거
- tagLine의 일반 공백, NBSP, zero-width space, BOM 제거
- 멤버 저장 및 Riot ID 확인에서도 동일한 정규화 적용
- 기존 DB 정리용 `database/cleanup_riot_id_whitespace.sql` 추가

## 멤버 일괄 업데이트

멤버 관리 화면에 `멤버 일괄 업데이트` 버튼을 추가했습니다.

- 활성 멤버를 한 명씩 순서대로 Riot 갱신
- 일부 멤버 실패 시 다음 멤버 계속 처리
- 성공/실패/전체 건수 표시
- 실패한 멤버 이름과 오류 사유 표시
