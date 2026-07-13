# v2.2.1 변경사항

## 경기 일시 저장 오류 수정

- 브라우저의 `datetime-local` 값을 서버에서 `LocalDateTime`으로 변환합니다.
- MyBatis가 PostgreSQL `TIMESTAMP` 컬럼에 명시적으로 TIMESTAMP 타입으로 바인딩하도록 수정했습니다.
- 경기 일시가 없거나 형식이 잘못된 경우 사용자용 검증 메시지를 반환합니다.
- DB DDL 변경은 없습니다.
