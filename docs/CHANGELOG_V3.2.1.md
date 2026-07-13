# CHANGELOG v3.2.1

- `AuctionService.Room`에 `Instant startDeadline` 필드가 누락되어
  방 생성 시 발생하던 컴파일 오류를 수정했습니다.
- 시작 대기 상태(`STARTING`)의 4초 카운트다운 상태값이 정상 저장됩니다.
- DB 변경은 없습니다.
