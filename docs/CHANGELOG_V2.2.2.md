# CHANGELOG v2.2.2

## Riot 전적 갱신

- 솔로랭크 정보가 있는 멤버: 최근 솔로랭크(queue 420) 최대 20경기 집계
- 솔로랭크 정보가 없는 멤버: 일반 소환사의 협곡 전적을 자동 대체
  - queue 400: Draft Pick
  - queue 430: Blind Pick
  - queue 480: Swiftplay
  - queue 490: Normal Quickplay
- 여러 일반 큐의 Match ID를 합치고 중복 제거 후 최신 20경기를 집계

## PostgreSQL 오류 수정

- 시즌 어워드 쿼리의 `ORDER BY games is ambiguous` 오류 수정
- 모든 정렬 컬럼에 CTE 별칭을 명시하고 어워드별 CTE로 분리

## 테스트 데이터

- `database/sample_inhouse_matches_v2_2_2.sql` 추가
- 활성 멤버 10명을 이용해 내전 8경기 생성
- 궁합, 상대 전적, 명예의 전당, MVP, 모스트 챔피언 테스트 가능
