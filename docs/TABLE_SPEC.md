# 2차 추가 테이블/컬럼

## MEMBER 추가 컬럼
- `summoner_id`, `summoner_level`: KR 플랫폼 API 식별정보
- `recent_win_rate`, `recent_avg_kills/deaths/assists`: 최근 솔랭 20경기 집계
- `most_position`: 최근 솔랭 최다 포지션

## MEMBER_RIOT_SNAPSHOT
Riot 정보 갱신 시점별 티어와 최근 경기 요약을 보존합니다.

## MEMBER_CHAMPION_STAT
현재는 `SOLO_RECENT` 범위의 모스트 챔피언 3개를 저장합니다. 내전 모스트는 경기 원장에서 실시간 집계합니다.
