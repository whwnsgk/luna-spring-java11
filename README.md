# Rift Arena v2.1

> v2의 Riot 랭크 조회 오류 수정 및 UI 개선판입니다. 기존 v2 DB 구조를 그대로 사용합니다.

# Rift Arena v2

1차 MVP에 Riot API 자동 갱신, 최근 20판 통계, 모스트 챔피언/라인, 멤버 상세, 궁합, Discord 복사를 추가한 버전입니다.

## 기존 v1 DB 사용자는 먼저 실행
`database/upgrade_v1_to_v2.sql`

신규 DB는 `database/ddl.sql` 전체를 실행합니다. `schema.sql` 자동 실행은 없습니다.

## 필수 환경변수
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- Riot 기능 사용 시 `RIOT_API_KEY`

로컬 STS와 Render 양쪽에 `RIOT_API_KEY`를 각각 등록해야 합니다. 개발 키는 만료될 수 있으므로 갱신 후 환경변수 값도 교체하세요.

## 2차 Riot 수집 흐름
Riot ID → ACCOUNT-V1(PUUID) → SUMMONER-V4 → LEAGUE-V4 → MATCH-V5 최근 솔랭 20경기 → DB 저장

## 적용 순서
1. 기존 프로젝트 백업
2. v2 파일 전체 덮어쓰기
3. DBeaver에서 `upgrade_v1_to_v2.sql` 실행
4. STS Refresh → Maven Update → Project Clean
5. `RIOT_API_KEY` 환경변수 추가
6. 재실행 후 멤버 카드의 `Riot 갱신` 클릭
