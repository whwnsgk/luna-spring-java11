# v2.2 테이블 명세 요약

## MEMBER
멤버, Riot 계정, 솔랭 최근 통계, 밸런스 코스트를 저장합니다.

## MEMBER_POSITION
멤버의 선호 포지션 1~3개를 저장합니다.

## SEASON
| 컬럼 | 설명 |
|---|---|
| season_id | 시즌 PK |
| season_name | 시즌 이름 |
| start_date / end_date | 시즌 기간 |
| active_yn | 현재 경기 등록 기본 시즌 여부 |

## INHOUSE_MATCH
경기 일시, 시즌, 승리팀, 양 팀 코스트와 메모를 저장합니다.

## INHOUSE_MATCH_PLAYER
경기별 10명의 팀·라인·챔피언·K/D/A·승패·MVP를 저장합니다.

## MEMBER_RIOT_SNAPSHOT
Riot 갱신 시점별 솔랭 통계를 보관합니다.

## MEMBER_CHAMPION_STAT
최근 솔랭 모스트 챔피언 3개를 보관합니다.
