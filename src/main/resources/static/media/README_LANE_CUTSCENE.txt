라인 컷씬 음성 파일
===================

가장 권장하는 위치와 파일명:

src/main/resources/static/media/
- voice-top.mp3
- voice-jungle.mp3
- voice-mid.mp3
- voice-adc.mp3
- voice-support.mp3

지원 확장자:
- .mp3
- .wav
- .m4a
- .ogg

프로그램은 다음 위치를 순서대로 찾습니다.

1. /media/voice-라인명.확장자
2. /voice-라인명.확장자

주의:
- 파일 이름은 영문 소문자로 정확히 맞춰주세요.
- Windows 탐색기에서 확장자가 숨겨져 `voice-top.mp3.mp3`가 되지 않았는지 확인하세요.
- 파일 추가 후 서버를 완전히 재시작해야 `target/classes/static`에 복사됩니다.
- 브라우저에서 `http://localhost:8081/media/voice-top.mp3`를 직접 열어 404가 아닌지 확인하세요.
- 각 음성은 컷씬 시작 시 1회만 재생하며 반복하지 않습니다.
