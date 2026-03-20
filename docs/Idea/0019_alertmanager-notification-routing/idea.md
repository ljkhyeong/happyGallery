# Alertmanager 알림 라우팅 검토

**날짜**: 2026-03-20
**상태**: 아이디어

---

## 현재 상태

현재 저장소에는 다음 운영 관측성 자산이 이미 있다.

- `monitoring/prometheus.yml`
- `monitoring/alerts.yml`
- `docker-compose.yml`의 Prometheus / Grafana 서비스

즉, Prometheus는 alert rule을 평가할 수 있지만, 현재 로컬 구성에는 다음이 없다.

- Alertmanager 서비스
- `alerting.alertmanagers` 설정
- Slack / webhook / email 같은 receiver 정의

결과적으로 현재 alert는 Prometheus UI에서 상태를 확인하는 수준에 머물고,
운영 채널로 push되지는 않는다.

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

Alertmanager를 로컬 운영 스택과 문서에 추가하는 방향을 검토한다.

최소 범위:

1. `docker-compose.yml`에 `alertmanager` 서비스 추가
2. `monitoring/alertmanager.yml` 추가
3. `monitoring/prometheus.yml`에 `alerting.alertmanagers` 연결
4. `severity=critical` / `severity=warning` route 분리
5. receiver는 처음엔 하나만 붙인다
   - Slack webhook 또는 generic webhook

---

## 기대 효과

- Prometheus rule이 "UI에서만 보이는 상태"가 아니라 실제 알림으로 이어진다.
- critical / warning 분기 기준을 코드/설정으로 명시할 수 있다.
- 로컬에서도 알림 라우팅 설정을 재현하고 문서화할 수 있다.

---

## 주의점

- 로컬 compose에 외부 알림 채널을 직접 묶으면 비밀값 관리가 따라온다.
- 알림 노이즈가 많으면 rule tuning 없이 receiver만 붙여서는 실효성이 낮다.
- 현재는 Sentry가 애플리케이션 예외를 잡고 있으므로, Alertmanager는 인프라/메트릭 성격 알림에 집중하는 편이 낫다.

---

## 나중에 할 일

- Alertmanager 도입 시 `README.md`, `HANDOFF.md`, 운영 문서의 observability 섹션도 같이 갱신
- Slack/webhook receiver 선택 기준 정리
- `critical` rule부터 먼저 연결하고 `warning`은 점진적으로 추가
