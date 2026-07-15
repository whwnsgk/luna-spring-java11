# CHANGELOG v3.5.6

## 라인별 25초 컷씬

자동 팀 짜기 또는 경매 종료 후 라인별 매치업 컷씬을 재생합니다.

순서:

1. TOP
2. JUNGLE
3. MID
4. ADC
5. SUPPORT

각 컷씬은 5초이며 총 약 25초입니다.

## 음성

라인별 음성 파일을 한 번만 재생합니다.

- voice-top.mp3
- voice-jungle.mp3
- voice-mid.mp3
- voice-adc.mp3
- voice-support.mp3

반복 재생하지 않으며 최대 볼륨으로 재생을 시도합니다.

## 자동 팀 짜기

자동 팀 계산 결과의 assignedPosition을 그대로 사용하여 정확한 라인 매치업을 표시합니다.

## 경매 종료

경매는 라인이 확정되지 않으므로 다음 순서로 연출용 라인을 추정합니다.

1. mostPosition 우선
2. 빈 라인 중 laneProfiles 숙련도가 가장 높은 라인
3. 마지막 남은 라인

## 스킵

화면 오른쪽 아래에 스킵 버튼을 추가했습니다.
