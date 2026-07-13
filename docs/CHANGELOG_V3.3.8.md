# CHANGELOG v3.3.8

## Render 정적 파일 캐시 문제 대응

Render 배포 후 `index.html`, `style.css`, `app.js` 버전이 서로 달라
UI가 깨지거나 JavaScript 동작이 맞지 않는 문제를 방지합니다.

- `style.css?v=3.3.8`
- `app.js?v=3.3.8`
- HTML/CSS/JS 응답에 no-cache 헤더 적용
- 브라우저 콘솔에 `RIFT ARENA build v3.3.8 loaded` 출력
