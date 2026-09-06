---
name: happygallery-observability-flows
description: happyGallery의 requestId·로그 마스킹·Prometheus·Grafana·Alertmanager·Sentry·프론트엔드 측정 지표를 변경할 때 사용한다.
---

# happyGallery 로그·모니터링

## 규칙

- `monitoring/`, 웹 `RequestIdFilter`·`GlobalExceptionHandler`, `frontend/src/features/monitoring/`, ADR-0015를 확인한다.
- 로그·오류 응답·배치의 requestId 전달과 Sentry tag를 유지한다. `[client-monitoring]` 이벤트, `happygallery.funnel.*`, `/actuator/prometheus`, dashboard·alert label은 사용처와 함께 변경한다.
- alert 원본은 `monitoring/alerts.yml`이다. `deploy/k3s/scripts/sync-prometheus-alerts.sh`로 생성 파일을 갱신한다. Grafana도 원본을 바꾸고 해당 k3s 생성물을 갱신한다.
- 운영 장애 counter를 추가하면 실제로 export된 metric 이름으로 alert를 연결하고 진단에 필요한 panel을 함께 검토한다.
- Sentry PII 비활성과 개인정보·비밀값 마스킹을 유지한다. 메시지와 예외 stack trace에 같은 마스킹을 적용하고 실제 encoder의 token으로 검증한다.
- Actuator 포트는 현재 프로필에서 확인한다. 기본 management는 8081, local은 8080이다.

## 검증

- 요청·오류·지표는 `RequestIdFilterUseCaseIT`, `ClientMonitoringUseCaseIT`, `GlobalExceptionHandlerTest` 중 관련 테스트를 선택한다.
- alert 변경은 `deploy/k3s/scripts/sync-prometheus-alerts.sh --check`와 `deploy/k3s/scripts/validate.sh`, Compose 변경은 `docker compose config`를 실행한다.
- 프론트엔드 측정 코드가 바뀌면 `frontend`에서 `npm run build`를 실행한다.
