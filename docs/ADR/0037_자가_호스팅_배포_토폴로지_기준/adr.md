# ADR-0037: 자가 호스팅 배포 토폴로지 기준

**날짜**: 2026-07-18
**최종 갱신**: 2026-07-19
**상태**: Accepted

---

## 컨텍스트

기존 운영 기준은 CloudFront, S3, ALB, ECS Fargate, RDS와 ElastiCache를 조합한 AWS 배포였다. 현재는 비용과 운영 통제 범위를 다시 정리하면서 AWS 배포를 폐기하고, 소유한 단일 노트북에서 서비스를 자가 호스팅하기로 했다.

결정 당시 저장소에는 로컬 통합 실행을 위한 `docker-compose.yml`과 Nginx 설정만 있었다. 현재 Compose는 `local` 프로필, 개발용 기본값과 MySQL/Redis 호스트 포트를 사용하므로 그대로 운영 배포로 간주할 수 없다. 이 ADR은 목표 토폴로지와 운영 불변 조건을 정하며, 저장소 산출물이 실제 노트북 운영 검증까지 완료됐다는 의미는 아니다.

## 결정

### 1. 운영 목표는 단일 노트북의 k3s로 통일한다

- 운영 오케스트레이터는 단일 노트북에 설치한 단일 노드 k3s를 사용한다.
- Docker Compose는 로컬 개발, 통합 검증과 Kubernetes 장애 시 복구 진단용으로만 유지한다. Compose 구성을 운영 기준으로 승격하지 않는다.
- AWS의 CloudFront, S3, ALB, ECS Fargate, RDS, ElastiCache와 ECR은 현재 운영 토폴로지에서 제외한다.
- Kubernetes manifest와 운영 절차는 `deploy/k3s`를 기준으로 관리한다. 실제 노트북에서 외부 경로와 복구 훈련을 통과하기 전까지는 `운영 중`으로 보지 않는다.

### 2. 외부 요청은 TLS ingress 한 곳으로만 받는다

목표 요청 경로는 다음과 같다.

```text
브라우저
  -> DNS / 공유기 또는 방화벽
  -> k3s Ingress (TLS 종료, HTTP -> HTTPS)
       -> /api/* -> Spring Boot app -> cluster 내부 MySQL / Redis
       -> 그 외   -> React 정적 파일과 SPA fallback
```

- 프론트엔드와 API는 같은 origin에서 제공한다.
- 외부에는 ingress의 HTTP/HTTPS 포트만 열고 애플리케이션, MySQL, Redis와 관리·모니터링 포트는 직접 공개하지 않는다.
- 인증서 발급·갱신, DNS, 공유기 포트 전달과 방화벽 규칙은 manifest와 운영 절차에서 구체화한다.

### 3. 전달 헤더는 통제된 ingress만 신뢰한다

- ingress는 외부 요청이 임의로 보낸 `X-Forwarded-For`, `X-Real-IP`, `X-Forwarded-Proto`를 신뢰하지 않고 자신이 관리하는 값으로 덮어쓰거나 정규화한다.
- 애플리케이션의 forwarded header 처리와 `app.rate-limit.trust-forwarded-headers=true`는 애플리케이션 직접 접근이 차단되고 통제된 ingress만 앞단에 있을 때 사용한다.
- 공유기, 터널 또는 별도 프록시를 추가하면 신뢰 가능한 프록시 홉을 명시하고 실제 클라이언트 IP, HTTPS 스킴과 처리율 제한 버킷을 다시 검증한다.
- 처리율 제한 키의 원문 비노출 기준은 ADR-0036을 유지한다.

### 4. 상태 저장소는 영속 볼륨과 별도 백업을 전제로 한다

- MySQL과 Redis는 cluster 내부 Service로만 접근한다. MySQL은 명시적인 영속 볼륨을 사용하고, Redis 영속화 여부는 세션 연속성 요구에 맞춰 정한다.
- 노트북 내부 볼륨은 백업이 아니다. MySQL 데이터와 복구에 필요한 설정·암호화 키는 노트북과 물리적으로 분리된 저장소에 암호화해 백업한다.
- Redis에는 세션과 처리율 제한처럼 재생성 가능한 상태만 저장한다. Redis 데이터를 별도 백업하지 않으며, 유실 시 전체 세션 로그아웃과 처리율 버킷 초기화를 허용한다.
- 백업 주기, 보존 기간, 무결성 확인과 복원 훈련 절차를 운영 manifest와 함께 확정한다.
- 배포와 Flyway migration 전에는 복구 가능한 백업을 확인한다. 컨테이너 이미지 롤백이 데이터베이스 스키마를 되돌리지는 않는다.

### 5. secret은 저장소와 이미지 밖에서 주입한다

- DB 자격증명, `ENCRYPT_KEY`, `HMAC_KEY`, Toss, OAuth, 알림, Sentry와 관리자 초기 설정 값은 Git, 이미지와 일반 manifest에 평문으로 넣지 않는다.
- Kubernetes Secret은 저장 형식 자체가 암호화가 아니므로 노트북 파일 권한, k3s 접근 권한과 백업 접근 권한을 함께 제한한다.
- active `ENCRYPT_KEY`/`HMAC_KEY`, `PREVIOUS_ENCRYPT_KEYS`/`PREVIOUS_HMAC_KEYS`와 비회원 토큰 active/previous 서명 키는 데이터와 토큰의 전환 기간에 필요하다. MySQL 백업과 물리적으로 분리된 복구 저장소에 키 ID, 백업 시점과 함께 보관한다.
- 개발용 `.env`와 Compose 기본값을 운영 secret으로 재사용하지 않는다.

필드 암호화·HMAC과 비회원 토큰 키는 일반 Secret 교체로 바꾸지 않고 다음 유지보수 절차를 따른다.

1. 회전 입력에서 새 active 키와 target 키 ID, off-device 백업 목적지와 source 키 복구 가능성을 검증한다. 이 시점에는 runtime Secret을 바꾸지 않는다.
2. [`rotate-data-keys.sh`](../../../deploy/k3s/scripts/rotate-data-keys.sh)가 app을 0 replica로 축소하고 Pod 종료를 확인한 뒤 fresh off-device 암호화 백업을 생성하고 checksum을 검증한다.
3. 스크립트는 현재 app digest와 같은 servlet 이미지를 `SERVER_PORT=0`, `MANAGEMENT_PORT=0`으로 기동하는 임시 유지보수 Job에만 새 active·구 previous 키를 주입한다. Job은 Service가 없고 기본 deny NetworkPolicy가 적용되어 외부 ingress를 받지 않는다. 회전 runner는 `data_key_rotation_lock` 단일 행을 트랜잭션 잠금으로 선점하고 600초 제한의 단일 트랜잭션에서 AES 재암호화, HMAC 재생성과 `phone_verifications` 전량 삭제를 수행한 뒤 context를 닫는다.
4. Job이 성공한 뒤에만 runtime Secret을 새 active·구 previous 키로 전환하고 백업명·키 ID·비회원 토큰 제거 가능 시각을 annotation으로 기록한다. 이어 Redis를 비워 관리자·회원 세션과 처리율 제한 상태를 초기화하고 app을 새 keyring으로 기동한다. 진행 중이던 휴대폰 인증과 전체 로그인 세션이 무효화되는 점을 유지보수 공지에 포함한다.
5. `provider_id_enc IS NULL`인 기존 소셜 계정은 previous HMAC 후보로 로그인할 때 active AES/HMAC으로 lazy backfill한다. 이 건수가 0이 되기 전에는 previous HMAC 키를 유지한다.
6. 비회원 토큰 previous 키는 runtime 전환 시각부터 설정된 토큰 TTL에 1시간의 운영 여유를 더한 시각까지 유지한다. 기본 TTL 168시간에서는 최소 169시간이다.
7. 운영자는 새 키 기준 백업과 복원 가능성을 확인한 뒤 [`finalize-data-key-rotation.sh`](../../../deploy/k3s/scripts/finalize-data-key-rotation.sh)를 실행한다. finalizer는 비회원 토큰 유예와 `provider_id_enc IS NULL` 0건을 확인하고 app을 다시 0 replica로 축소한 뒤 소셜 조건을 재검사해 runtime previous 키를 제거한다. 보존 중인 과거 백업에 필요한 키는 분리 복구 저장소에서 해당 백업과 같은 기간 유지한다.

스크립트는 runtime Secret의 `runtime-transitioned`, `completed`, `finalized` phase annotation으로 같은 target 키에 대한 재실행과 finalization 조건을 구분한다. 회전 또는 검증이 실패하면 새·구 키를 모두 보존하고 app을 0 replica로 유지한다. 트랜잭션 롤백 여부, MySQL 상태와 백업 복원 필요성을 확인하기 전에는 정상 app을 다시 기동하거나 previous 키를 제거하지 않는다.

현재 서비스는 운영 미개시이므로 version-aware reader를 포함한 이 release를 최초 운영 기준선으로 사용한다. 접두사 없는 암호문만 지원하는 binary가 실제 운영 중인 환경에 같은 변경을 적용한다면 version-aware reader 선배포와 versioned write 전환을 분리해야 한다. `hg:<keyId>:` 쓰기 이후 구 binary 단독 rollback은 허용하지 않고 forward fix 또는 호환 키와 회전 전 DB 백업 복원을 사용한다.

### 6. 이미지는 불변 식별자로 배포하고 이전 버전을 보존한다

- 백엔드와 프론트엔드 이미지는 검증한 commit SHA 또는 digest로 식별한다. 운영 manifest에서 `latest`만 참조하지 않는다.
- 이미지는 로컬 registry를 사용하거나 k3s containerd로 명시적으로 가져오며, 선택한 방식을 배포 절차에 고정한다.
- 배포 전 build와 최소 검증을 통과시키고, 배포 후 rollout 상태와 health endpoint를 확인한다.
- 직전 이미지와 manifest를 보존해 애플리케이션을 롤백한다. Flyway가 적용된 경우에는 데이터 호환성과 복원 필요 여부를 별도로 판단한다.
- 기존 AWS 자동 배포 workflow는 제거하며, k3s 배포 자동화는 manifest와 rollback 절차가 마련된 뒤 별도로 결정한다.

### 7. probe와 종료 유예를 배포 계약에 포함한다

- manifest에는 startup/readiness/liveness probe를 정의하고 Spring Boot health endpoint를 기준으로 실제 기동 실패와 트래픽 수용 가능 상태를 구분한다.
- `terminationGracePeriodSeconds`는 애플리케이션 graceful shutdown 유예인 30초 이상으로 둔다.
- ingress timeout과 keep-alive는 ADR-0030의 외부 HTTP 연결 풀·timeout 기준과 함께 검증한다.

### 8. 단일 노드의 가용성 한계를 수용한다

- 노트북 전원, 디스크, 네트워크, 공유기 또는 k3s 장애는 곧 전체 서비스 중단으로 이어진다.
- 이 구성은 고가용성을 제공하지 않는다. 자동 재시작, 디스크 여유 공간 감시, 백업과 복원 절차로 복구 시간을 줄이는 수준을 목표로 한다.
- 무중단 운영이나 노드 장애 자동 복구가 요구되면 다중 노드 또는 관리형 인프라로의 이전을 별도 ADR로 결정한다.

## 현재 구현 상태와 남은 작업

2026-07-19 기준 `deploy/k3s`에 다음 산출물을 구현했다.

- namespace, app/frontend/MySQL/Redis/Prometheus/Alertmanager workload, ClusterIP Service, TLS Ingress와 MySQL Retain PVC
- Traefik 전달 헤더 기준, ingress·Prometheus만 허용하는 Actuator NetworkPolicy
- 저장소 밖 env와 HTTPS webhook URL 파일에서 runtime Secret을 생성·교체하는 절차
- commit SHA 이미지 build/import, server-side dry-run, rollout 검증, release manifest 보존과 수동 rollback
- `age` 암호화 off-device MySQL 백업, checksum·보존 정리, app 중지 후 복원·Redis 초기화 절차
- active/previous AES·HMAC keyring, 키 ID가 포함된 암호문, 단일 트랜잭션 회전 실행기와 소셜 provider ID lazy backfill
- app 중지·백업·Redis 초기화를 포함한 `rotate-data-keys.sh`, 유예 조건 확인 뒤 previous 키를 제거하는 `finalize-data-key-rotation.sh`

다음은 대상 노트북과 외부 환경에서만 완료할 수 있다.

- k3s와 cert-manager 설치, DNS, 공유기 포트 전달, 호스트 방화벽과 실제 TLS 발급
- 실제 외부 매체 또는 원격 mount 백업, 분리 보관한 age·필드 암호화 키로 복원 훈련
- 실제 운영 키로 필드·비회원 토큰 회전과 previous 키 제거, 회전 전후 백업 복원 훈련
- 외부 uptime 감시와 전원·디스크·네트워크 장애 알림. 애플리케이션 메트릭은 내부 Alertmanager에서 외부 HTTPS webhook으로 전달하지만 노트북 자체 중단은 감지할 수 없다.
- 실제 브라우저의 세션·CSRF·OAuth·결제·SMS 핵심 흐름 검증과 공개 운영 주소 확정

따라서 저장소 구성은 `배포 준비 완료`, 실제 서비스는 위 검증 전까지 `운영 미개시`로 표현한다.

## 결과

### 장점

- 클라우드 고정 비용을 제거하고 운영 구성과 데이터 위치를 직접 통제한다.
- k3s manifest를 기준으로 서비스, ingress, 영속 볼륨과 배포 상태를 선언적으로 관리할 수 있다.
- 개발·진단용 Compose와 운영용 Kubernetes의 역할이 분명해진다.

### 단점

- 단일 노트북과 가정용 네트워크 장애가 전체 장애가 된다.
- TLS, DNS, 보안 패치, 백업, 복원과 하드웨어 관리를 직접 책임져야 한다.
- 실제 노트북의 외부 네트워크와 복원 절차를 직접 운영·검증해야 한다.

## 참고

- [ADR-0017 Filter 처리율 제한](../0017_Filter_처리율_제한/adr.md)
- [ADR-0024 비회원 접근 토큰 강화](../0024_비회원_토큰_강화/adr.md)
- [ADR-0025 정상 종료와 Executor 정리 정책](../0025_정상_종료와_Executor_정리_정책/adr.md)
- [ADR-0028 1차 배포 준비](../0028_배포_준비_알림_연동_로그_마스킹/adr.md)
- [ADR-0030 타임아웃 계층과 ingress keep-alive 기준선](../0030_타임아웃_계층과_ingress_keep_alive_기준선/adr.md)
- [ADR-0036 개인정보 평문 제거와 블라인드 인덱스 기준](../0036_개인정보_평문_제거와_블라인드_인덱스_기준/adr.md)
- [`deploy/k3s` 운영 절차](../../../deploy/k3s/README.md)
