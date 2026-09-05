# 노트북에서 여러 프로젝트를 함께 운영하는 검토안

2026-09-05 기준 검토안이다. 사용자가 제공한 [포트폴리오](https://ljkportfolio.netlify.app/)의 개인 프로젝트와 이 저장소의 k3s 설정을 확인했다. 다른 프로젝트의 최신 배포 설정·실측 사용량까지 확인한 것은 아니다. **현재 공동 운영이나 아래 자원 한도는 적용하지 않았다.** 실행 순서는 루트 `plan.md`에서 관리한다.

## 프로젝트별 후보

| 프로젝트 | 확인한 실행 구성 | 공동 운영 검토 |
| --- | --- | --- |
| happyGallery | Spring Boot·Node SSR·MySQL·Redis·모니터링 | 실제 주문·예약을 받는 우선 서비스. 단독 기동·부하·복원부터 검증 |
| BATON Core와 GO·WATCH·RELAY·BRIEF·CAL | Core와 6개 서비스 중 상태를 저장하는 서비스들. 독립 저장소, 전체 기술 목록에 MySQL·PostgreSQL·RabbitMQ·SQS 포함 | 전체를 한 앱으로 계산하지 않음. Core와 필요한 서비스부터 추가하고 각 저장소·메시지 전달 의존성을 확인 |
| BATON ROUND | Spring WebSocket 시그널링, 메모리 방 상태, 브라우저 간 미디어 또는 외부 TURN | 메모리는 별도 측정. 영상 변환 서버로 계산하지 않음. 현재 단일 인스턴스 조건과 외부망·TURN 검증 필요 |
| IntentTrace | Kotlin/Spring 서버·PostgreSQL, REST/MCP·IntelliJ 클라이언트 연동 | 서버와 DB는 추가 후보. IntelliJ 플러그인 자체는 개발 PC에 설치. 메모리 세션을 쓰는 단일 인스턴스 조건 유지 |
| 청년정책메이트 | Spring Boot·Next.js·PostgreSQL | 개발·시연 후보. 포트폴리오 기준 실제 정책 수집·외부 AI·알림은 미연결이므로 완성된 운영 서비스로 취급하지 않음 |
| WebRTC/HLS 현장강의 보조 서비스 | mediasoup·FFmpeg·GStreamer·영상 세그먼트 저장 | CPU·디스크·업로드 대역폭을 따로 측정할 시연 후보. 주문 서비스와 상시 동시 운영은 초기 범위에서 제외하는 안을 권고 |
| Hope Commit | 로컬 Git을 읽는 Node.js 스킬·HTML 생성 도구 | 별도 상시 웹 서버를 올릴 필요 없음 |

근거: [BATON 전체 구성](https://ljkportfolio.netlify.app/projects/baton/), [ROUND](https://ljkportfolio.netlify.app/projects/baton/round/), [IntentTrace](https://ljkportfolio.netlify.app/projects/intent-trace/), [청년정책메이트](https://ljkportfolio.netlify.app/projects/youth-policy-mate/), [HLS 프로젝트](https://ljkportfolio.netlify.app/projects/webrtc/), [Hope Commit](https://ljkportfolio.netlify.app/projects/hope-commit/). 포트폴리오의 테스트 통과 설명을 노트북에서 측정한 성능으로 해석하지 않는다.

## 배포 방식 권고

기존 happyGallery 배포·복원 절차를 재사용할 수 있도록 **단일 k3s에서 프로젝트별 namespace를 나누는 방식**을 권고한다. 노트북 한 대에 프로젝트마다 k3s를 따로 설치하지 않는다. Docker Compose로도 자원 제한은 가능하지만, 선택한다면 happyGallery의 개발용 Compose와 별도로 운영 배포·TLS·백업·복구를 구현해야 한다. k3s 밖의 Docker 컨테이너는 Kubernetes 자원 할당량의 적용 대상이 아니므로 별도 제한 없이 혼합하지 않는다.

공유 범위는 k3s·Traefik·인증서 관리부터 시작한다. 프로젝트마다 도메인·Ingress·Secret·ServiceAccount·DB 프로세스·계정·볼륨·백업 경로를 구분한다. BATON처럼 서비스 간 연동이 필요한 경우에는 그 경로만 허용한다. 서버 메모리를 줄이려고 happyGallery와 다른 프로젝트의 DB를 한 프로세스나 계정으로 바로 합치지 않는다.

기존 Prometheus·Alertmanager·Grafana는 happyGallery namespace와 복구 이미지 목록에 속한다. 공유 모니터링으로 옮기려면 경보·접근 정책·배포 및 복원 검증을 함께 변경한다. 현재 상태에서 모든 프로젝트를 이미 감시한다고 설명하지 않는다. 노트북 전체 장애 감시는 계속 외부에 둔다.

## 필요한 격리와 현재 상태

| 구분 | 현재 happyGallery | 공동 운영 전에 필요한 일 |
| --- | --- | --- |
| CPU·메모리 | 상주 컨테이너별 `requests`·`limits` 있음. 메모리 한도 합계 약 4.75GiB | 프로젝트 전체 `ResourceQuota`와 기본값 `LimitRange` 추가. 재배포·백업·복원·키 회전·인증서 발급 임시 Pod까지 동작 확인 |
| 네트워크 | namespace의 기본 ingress 차단과 필요한 경로 허용. egress 정책은 없음 | 다른 namespace도 기본 차단 후 필요한 통신만 허용. DNS·결제·OAuth·알림 등 실제 외부 연동을 보존 |
| 권한 | 운영 Secret과 Pod 보안 설정 있음 | 프로젝트별 운영 계정·RBAC 범위를 제한하고 다른 프로젝트 Secret이나 클러스터 관리자 권한을 주지 않음 |
| 디스크 | local-path 볼륨과 사용량 경보 | 프로젝트별 디렉터리·백업을 분리하고 실제 파일시스템 용량 제한 또는 별도 볼륨 방식을 검토. 이미지·로그·임시 저장소도 제한 |

Namespace만 나눠서는 자원과 통신이 자동 격리되지 않는다. [Kubernetes 공동 사용 안내](https://kubernetes.io/docs/concepts/security/multi-tenancy/), [ResourceQuota](https://kubernetes.io/docs/concepts/policy/resource-quotas/)와 [LimitRange](https://kubernetes.io/docs/concepts/policy/limit-range/)를 기준으로 구현한다. CPU 제한은 전용 코어 배정을 뜻하지 않고, 메모리 한도 초과는 해당 컨테이너 종료로 이어질 수 있다. CPU·메모리 설정만으로 디스크 I/O와 회선 대역폭까지 보장하지는 않는다.

현재 사용하는 [local-path-provisioner는 볼륨 용량 상한을 강제하지 않는다](https://github.com/rancher/local-path-provisioner#cons). PVC의 `5Gi`·`20Gi` 또는 `requests.storage` 할당량만으로 다른 프로젝트의 디스크 고갈을 막았다고 판단하지 않는다.

컨테이너는 같은 OS 커널과 호스트를 공유한다. 이 안은 같은 운영자가 관리하는 프로젝트끼리 사용량과 접근 범위를 나누는 목적이며 VM 수준의 완전한 격리는 아니다. 노트북·공유기·무선 연결·디스크 장애는 모든 프로젝트에 동시에 영향을 준다.

## 초기 메모리 계획 예시

아래 값은 실측 요구량·예약된 자원·적용된 quota가 아닌 **검증을 시작할 때의 예산안**이다. Ubuntu에서 실제 사용 가능한 메모리를 확인하고 합계를 조정한다.

| 용도 | 검토할 메모리 예산 |
| --- | --- |
| Ubuntu·k3s·기본 공통 구성 | 약 3GiB |
| happyGallery와 기존 모니터링 | 약 6GiB 범위에서 재배포·유지보수 여유 검증 |
| 먼저 추가할 프로젝트와 해당 DB | 합계 약 3GiB부터 검증. BATON 전체가 이 안에 들어간다는 뜻은 아님 |
| 나머지 | 운영 여유로 유지 |

상시 서비스를 늘릴 때는 CPU 사용량·디스크 지연·무선 업로드 속도도 함께 본다. 운영 중인 노트북에서 여러 Gradle·Docker 빌드나 Testcontainers 테스트를 동시에 실행하지 않는다. 현재 happyGallery의 첫 빌드·반입은 공개 전에 같은 호스트에서 진행하고, 이후 운영 배포는 별도 빌드·이미지 전달 절차를 먼저 마련한다.

먼저 happyGallery 단독 상태에서 기준값을 기록한 뒤 **BATON Core와 필요한 연동 서비스 또는 IntentTrace 중 한 구성을 추가**해 비교한다. 청년정책메이트와 나머지 BATON 서비스는 남은 자원과 실제 사용 목적에 따라 순차 추가한다. 전체 프로젝트를 항상 켜둘 필요는 없으며, 개발·시연 서비스는 필요할 때 켜는 방식을 함께 사용한다.
