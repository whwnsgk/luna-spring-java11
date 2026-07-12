루나 Spring Boot + PostgreSQL 프로젝트

환경
- Java 11
- Spring Boot 2.7.18
- PostgreSQL
- MyBatis
- Render Docker 배포

필수 환경변수
- DB_URL
- DB_USERNAME
- DB_PASSWORD

Render에서 사용할 DB_URL 형식
jdbc:postgresql://호스트:5432/DB명

주의
- postgresql://사용자:비밀번호@호스트/DB명 형식을 그대로 넣지 말고
  jdbc:postgresql://호스트:5432/DB명 형식으로 변환하세요.
- 비밀번호는 GitHub나 application.properties에 직접 넣지 마세요.
- schema.sql이 앱 시작 시 테이블과 초기 데이터를 자동 생성합니다.
