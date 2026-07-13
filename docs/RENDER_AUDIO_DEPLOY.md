# Render 배포용 오디오 배치

## 파일 위치

프로젝트 내부에 다음 두 파일을 넣습니다.

```text
src/main/resources/static/audio/draft-wait.mp3
src/main/resources/static/audio/draft-go.mp3
```

Spring Boot가 `src/main/resources/static` 아래 파일을 정적 리소스로 자동 제공하므로,
브라우저에서는 다음 주소로 접근합니다.

```text
/audio/draft-wait.mp3
/audio/draft-go.mp3
```

## Git 반영 확인

```bash
git status
git add src/main/resources/static/audio/draft-wait.mp3
git add src/main/resources/static/audio/draft-go.mp3
git commit -m "Add auction background music"
git push
```

Render가 Git 저장소를 다시 빌드하면 MP3 파일도 JAR에 포함되어 배포됩니다.

## 확인 주소

로컬:

```text
http://localhost:8081/audio/draft-wait.mp3
http://localhost:8081/audio/draft-go.mp3
```

Render:

```text
https://배포주소/audio/draft-wait.mp3
https://배포주소/audio/draft-go.mp3
```
