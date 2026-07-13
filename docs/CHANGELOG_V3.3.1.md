# CHANGELOG v3.3.1

## 이 팀으로 기록 오류 수정

`openMatch()`가 호출하던 `teamRows()` 함수가 누락되어 발생하던
`ReferenceError: teamRows is not defined` 오류를 수정했습니다.

블루/레드 각 5명의 경기 입력 행을 생성하며 다음을 입력할 수 있습니다.

- 포지션
- 챔피언
- K / D / A
- MVP

## 외부 경매 BGM

기본 폴더:

`C:/luna-spring/audio`

필수 파일:

- `draft-wait.mp3`
- `draft-go.mp3`

상태별 동작:

- WAITING: draft-wait.mp3 반복
- STARTING / RUNNING / SOLD: draft-go.mp3 반복
- COMPLETE 또는 WebSocket 연결 종료: 정지

다른 폴더를 사용하려면 환경변수 `AUDIO_DIR`을 설정합니다.
