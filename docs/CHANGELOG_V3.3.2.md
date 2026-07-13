# CHANGELOG v3.3.2

## 로딩 애니메이션

API 요청이 350ms 이상 걸리면 전체 화면 로딩 애니메이션을 표시합니다.

- 무료 서버 초기 기동 안내
- 동시에 여러 API 요청이 실행돼도 전부 끝난 뒤 닫힘
- 빠른 요청에서는 화면 깜빡임 방지

## 데이터 초기화 SQL

- `database/reset_all_data.sql`: 전체 데이터 삭제
- `database/reset_match_data_only.sql`: 멤버는 유지하고 경기/시즌만 삭제
