# Rift Arena v2.2

친구들과 진행하는 리그 오브 레전드 내전의 멤버, 팀 편성, 수동 경기 기록, Riot 솔랭 정보와 시즌별 통계를 관리하는 Spring Boot 프로젝트입니다.

## 환경
- Java 11
- Spring Boot 2.7.18
- MyBatis / PostgreSQL
- HTML / CSS / JavaScript
- Docker / Render

## 기존 v2.1에서 적용
1. 소스를 덮어쓰되 `.git` 폴더는 유지합니다.
2. DBeaver에서 `database/upgrade_v2_to_v2_2.sql`을 전체 실행합니다.
3. STS: Refresh → Maven Update → Project Clean → 재실행합니다.
4. 브라우저는 `Ctrl + F5`로 강력 새로고침합니다.

## 환경변수
필수:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

선택:
- `RIOT_API_KEY`
- `DISCORD_WEBHOOK_URL`

API 키와 Webhook URL은 소스나 GitHub에 직접 적지 않습니다.

## v2.2 주요 기능
- 시즌 생성·활성화·필터
- 내전 상세 통계, 최고 연승, 최근 5경기
- 최고의/최악의 궁합과 상대 전적
- 시즌 어워드
- Discord 텍스트 복사 및 Webhook 전송

## DB 파일
- 신규 설치: `database/ddl.sql`
- v1 → v2: `database/upgrade_v1_to_v2.sql`
- v2.1 → v2.2: `database/upgrade_v2_to_v2_2.sql`


## v2.2.1 적용 메모

경기 등록 시 `played_at` 타입 오류를 수정한 버전입니다. DB 변경은 없으며 소스 덮어쓰기 후 STS Refresh, Maven Update, Project Clean, 서버 재시작만 진행하면 됩니다.


## v2.2.2 추가 테스트

랭크가 없는 계정은 일반 소환사의 협곡 전적을 자동으로 집계합니다.

내전 분석용 샘플 8경기를 생성하려면 DBeaver에서 실행:

```text
database/sample_inhouse_matches_v2_2_2.sql
```

샘플 데이터는 메모가 `[RIFT_SAMPLE_V222_`로 시작하므로 실제 데이터와 구분할 수 있습니다.
