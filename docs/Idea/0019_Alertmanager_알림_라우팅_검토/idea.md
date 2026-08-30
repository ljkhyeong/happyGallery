# Alertmanager 알림 라우팅 검토

**날짜**: 2026-03-20
**상태**: k3s generic webhook 1차 구현 완료, 채널별 확장은 후속

---

## 현재 상태

현재 저장소에는 다음 운영 관측성 자산이 이미 있다.

- `monitoring/prometheus.yml`
- `monitoring/alerts.yml`
- `docker-compose.yml`의 Prometheus / Alertmanager / Grafana 서비스
- `monitoring/alertmanager.yml`

Docker Compose의 로컬 Alertmanager는 no-op receiver를 유지한다. k3s 운영 구성에는 private Alertmanager를 추가하고 저장소 밖 Kubernetes Secret의 HTTPS generic webhook URL로 모든 critical/warning alert를 전달한다. Prometheus도 `alertmanager:9093`을 활성 대상으로 사용하며 rollout 검증에서 연결을 확인한다.

---

## 왜 검토할 가치가 있는가

현재 `alerts.yml`에는 다음처럼 운영 의미가 있는 rule이 이미 있다.

- `HighErrorRate`
- `HighResponseLatency`
- `HikariPoolExhaustion`
- `JvmHeapHigh`
- `AppDown`

이 정도면 대시보드 관찰만으로 끝내기보다,
critical / warning을 운영 채널로 분기하는 구성이 장기적으로 더 실용적일 수 있다.

특히 `AppDown`, `HighErrorRate`는 사람이 Prometheus UI를 보고 있을 때만 알 수 있으면
실제 대응 가치가 떨어진다.

---

## 아이디어

Alertmanager 기본 구성은 이미 들어갔고, 이제는 실제 receiver와 라우팅 정책을 어디까지 확장할지 검토한다.

최소 범위:

1. `severity=critical` / `severity=warning` route 분리
2. receiver는 처음엔 하나만 붙인다
   - Slack webhook 또는 generic webhook
3. 로컬 기본값은 계속 no-op receiver를 유지한다
4. 실제 채널 연결은 비밀값 주입 방식과 함께 문서화한다

---

## 기대 효과

- Prometheus rule이 로컬 Alertmanager까지만 머무르지 않고 실제 운영 알림으로 이어진다.
- critical / warning 분기 기준을 코드/설정으로 명시할 수 있다.
- 로컬에서도 알림 라우팅 설정을 재현하고 문서화할 수 있다.

---

## 주의점

- 로컬 compose에 외부 알림 채널을 직접 묶으면 비밀값 관리가 따라온다.
- 알림 노이즈가 많으면 rule tuning 없이 receiver만 붙여서는 실효성이 낮다.
- 현재는 Sentry가 애플리케이션 예외를 잡고 있으므로, Alertmanager는 인프라/메트릭 성격 알림에 집중하는 편이 낫다.

---

## 나중에 할 일

- 실제 운영 채널에 맞춰 Slack 전용 포맷이나 별도 critical/warning endpoint가 필요하면 receiver를 분리한다.
- 외부 uptime 감시는 같은 노트북 밖에서 별도로 구성한다.
