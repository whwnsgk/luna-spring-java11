# Rift Arena 1차 MVP
Java 11 / Spring Boot 2.7.18 / MyBatis / PostgreSQL / Vanilla JS

1. DBeaver에서 `database/ddl.sql` 실행
2. 필요 시 `database/sample_data.sql` 실행
3. STS Run Configurations에 DB_URL, DB_USERNAME, DB_PASSWORD 등록
4. 선택적으로 RIOT_API_KEY 등록
5. `RiftArenaApplication` 실행 후 http://localhost:8081

`schema.sql`은 없으며 DDL은 클라이언트에서 직접 관리합니다.
Render에서는 Internal DB 호스트를 사용하세요.
