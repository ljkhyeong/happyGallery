# happyGallery 단일 노드 k3s 운영

이 디렉터리는 단일 노드 k3s의 서비스 배포·백업·복원 절차를 관리한다. 현재는 [ADR-0037](../../docs/ADR/0037_자가_호스팅_배포_토폴로지_기준/adr.md)에 따라 보유 노트북에 먼저 설치해 검증한다. OS·전원·회선·원격 백업 준비는 [노트북 설치 준비](../laptop/README.md)를 따른다. [클라우드 운영 준비](../cloud/README.md)는 대체 후보이며 VM 구매는 보류다. Docker Compose는 계속 로컬 개발·복구 진단용이며 이 manifest의 대체물이 아니다.

```text
Internet -> 공유기/호스트 방화벽 :80/:443 -> k3s Traefik
                                   -> /api/* -> app:8080
                                   -> 그 외   -> frontend:8080 (React Router Node SSR)
app -> mysql:3306 (Retain PVC)
app -> redis:6379 (비영속 세션/처리율 상태)
prometheus -> app-management:8081/actuator/prometheus (cluster 내부 전용)
           -> alertmanager:9093 -> 외부 HTTPS webhook
grafana -> prometheus:9090 (cluster 내부 전용)
```

단일 운영 호스트, 디스크, 전원, 네트워크 또는 k3s 장애는 전체 서비스 중단으로 이어진다. 이 구성은 고가용성을 제공하지 않는다.
Prometheus는 애플리케이션 내부 지표와 alert rule을 평가하고 내부 Alertmanager가 저장소 밖 Secret의 HTTPS webhook으로 전달한다. 다만 둘 다 같은 운영 호스트에 있으므로 운영 호스트 자체 장애는 알릴 수 없다. 외부 uptime 감시와 webhook 수신자는 운영 호스트 밖에 별도로 둔다.
경보 규칙의 단일 원본은 저장소 루트의 `monitoring/alerts.yml`이다. 변경 후 `./deploy/k3s/scripts/sync-prometheus-alerts.sh`를 실행하면 kustomize가 읽는 `base/prometheus-alerts.generated.yml`이 갱신된다. 생성 파일을 직접 편집하지 않으며 `validate.sh`는 원본, 생성 파일과 최종 ConfigMap 중 하나라도 달라지면 실패한다.

## 디렉터리

| 경로 | 역할 |
| --- | --- |
| `base/` | Namespace, Deployment/StatefulSet, ClusterIP Service, Ingress/TLS, PVC, NetworkPolicy |
| `cluster/` | k3s 기본 Traefik의 전달 헤더 신뢰 경계 설정 |
| `images/` | app·React Router Node SSR 컨테이너 빌드 |
| `examples/` | 저장소 밖에 만들 운영 env 파일의 키 목록 |
| `scripts/` | secret 생성, 이미지 import, rollout/rollback, 검증, 백업/복원 |
| `systemd/` | 운영 호스트에서 6시간 백업과 heartbeat 감시를 실행하는 unit 예시 |

## 1. 외부 전제

- Linux 호스트 한 대에 단일 노드 k3s, Docker, Git, Java 25, Ruby, Trivy, `age`, `curl`을 설치한다. 현재 이미지 스크립트는 빌드와 반입을 같은 호스트에서 수행하므로 첫 배포는 서비스 기동 전에 진행한다. 운영 중 갱신을 시작하기 전에는 빌드 호스트 분리 절차를 마련한다.
- k3s는 `secrets-encryption: true`로 설치하고 `/etc/rancher/k3s/k3s.yaml`을 root 또는 지정 운영자만 읽게 한다.
- k3s 기본 Traefik과 local-path provisioner를 사용한다. 다른 Ingress/StorageClass를 쓰려면 manifest와 검증 스크립트를 함께 변경한다.
- cert-manager `v1.20.2` 정적 manifest를 공식 release에서 받아 출처와 checksum/signature를 검증해 운영 호스트에 보관한다.
- 직접 공개 시 DNS A 레코드는 실제 공인 IPv4를 가리킨다. 노트북은 공유기에서 TCP 80/443만 예약된 내부 IP로 전달하고, 유동 공인 IP는 갱신 방법을 마련한다. AAAA는 IPv6 연결을 검증한 뒤에만 추가한다. SSH는 내부 관리망으로 제한한다. 클라우드 전환 시에는 해당 방화벽에서 운영자 IP만 허용한다.
- 노트북 회선의 인바운드 접근 가능 여부를 먼저 확인한다. 별도 터널이나 프록시를 추가하면 전달 헤더 신뢰 경계와 실제 IP 기반 처리율 제한을 다시 검증한다.
- 운영 백업 대상은 호스트 장애·전원·회선과 분리된 원격 mount다. 클라우드 VM을 선택하면 다른 업체를 사용한다. 로컬 복원 훈련에서는 분리된 USB 디스크·NAS도 사용할 수 있지만 운영 호스트 내부 디스크나 같은 집의 사본만으로 운영 백업을 대체하지 않는다.
- 공개 결제 운영 전 기준 프로필의 대표자명, 전자우편주소, 통신판매업 신고번호와 `/terms`, `/privacy`, `/business-info`, footer 표시를 실제 사업자 정보와 다시 대조한다. `prod` 프로필은 필수 온라인 판매 고지가 완성될 때까지 결제 prepare를 `503`으로 차단한다.

k3s 설정 예시:

```yaml
# /etc/rancher/k3s/config.yaml
secrets-encryption: true
write-kubeconfig-mode: "0600"
```

설치 후 공통 구성:

```bash
sudo k3s secrets-encrypt status
./deploy/k3s/scripts/bootstrap-cluster.sh /opt/install/cert-manager-v1.20.2.yaml
```

`bootstrap-cluster.sh`는 노드가 정확히 한 개인지 확인하고 Traefik이 외부 요청의 임의 `X-Forwarded-*` 헤더를 무조건 신뢰하지 않도록 설정한다. NetworkPolicy는 `kube-system` 전체가 아니라 `app.kubernetes.io/name=traefik` Pod만 app/frontend/ACME solver에 접근하게 하므로 설치 후 이 label도 검사한다. 별도 프록시나 터널을 앞에 추가하면 해당 프록시 IP만 Traefik trusted IP로 지정하고 실제 IP, HTTPS scheme, rate-limit 버킷을 다시 검증해야 한다.

### 프런트엔드 CSP 기준선

브라우저 자원 정책은 HTML을 반환하는 `frontend/src/entry.server.tsx`가 소유한다. SSR 서버는 요청마다 새 nonce를 만들어 CSP 헤더와 React Router·stream renderer에 같은 값을 전달한다. Traefik Ingress에는 CSP를 중복 설정하지 않는다. 현재 정책은 다음 자원을 명시적으로 반영한다.

- same-origin API·정적 자원과 `data:` 이미지·폰트
- Toss Payments SDK와 결제 도메인
- jsDelivr Pretendard, Google Fonts
- Sentry ingest 도메인
- SSR inline script와 JSON-LD의 요청별 nonce

현재는 `Content-Security-Policy-Report-Only`이므로 위반을 차단하지 않는다. `report-uri`나 `report-to`도 아직 없어서 중앙 수집되는 것이 아니라 브라우저 개발자 도구 콘솔에서만 확인할 수 있다. 공개 전 k3s 프런트 이미지를 실제 HTTPS 경로로 열고 홈, 로그인, 결제창 호출과 Sentry 이벤트 전송을 확인한다. 예상하지 않은 출처가 없다는 것을 확인하고 중앙 수집 경로와 개인정보 처리 기준을 정한 뒤 enforced CSP 전환을 별도 변경으로 수행한다.

운영 Node 서버는 React Router Express adapter를 사용하고 request access log를 남기지 않는다. Toss 성공 callback의 `paymentKey`와 `orderId`를 포함한 query가 Pod 로그에 들어가지 않게 하는 경계이며, 애플리케이션 오류 로그는 기존 마스킹·안전 메시지 기준을 유지한다.

`validate.sh`는 Ingress가 CSP를 중복 소유하지 않는지 검사한다. `verify.sh`는 실제 HTTPS 응답의 CSP nonce와 SSR inline script nonce가 같은지 확인한다. 허용 출처는 배포 전 실제 브라우저 콘솔에서 확인하며, 구조화 데이터 본문은 정적 hash 대신 같은 요청 nonce를 사용한다.

frontend Deployment는 `INTERNAL_API_ORIGIN=http://app:8080`으로 공개 loader의 내부 API 대상을 고정한다. canonical·robots·sitemap의 대표 origin은 `frontend/src/shared/seo/metadata.ts`의 `https://happy-gallery.com` 한 곳에서 소유한다. manifest 렌더러는 `PUBLIC_HOST=happy-gallery.com`만 받아 Ingress·OAuth callback·SEO origin이 모두 같은 주소를 사용하게 한다. app ingress NetworkPolicy는 frontend Pod의 TCP 8080 접근만 명시적으로 허용한다.

## 2. Secret 준비

예제 파일을 `/etc/happygallery` 같은 저장소 밖 경로에 만든 뒤 권한을 제한한다. 값에는 shell 따옴표를 넣지 않는다.

```bash
sudo install -d -m 700 -o "$USER" -g "$(id -gn)" /etc/happygallery
sudo install -m 600 -o "$USER" -g "$(id -gn)" deploy/k3s/examples/mysql.env.example /etc/happygallery/mysql.env
sudo install -m 600 -o "$USER" -g "$(id -gn)" deploy/k3s/examples/redis.env.example /etc/happygallery/redis.env
sudo install -m 600 -o "$USER" -g "$(id -gn)" deploy/k3s/examples/app.env.example /etc/happygallery/app.env
sudo install -m 600 -o "$USER" -g "$(id -gn)" deploy/k3s/examples/alert-webhook-url.example /etc/happygallery/alert-webhook-url
```

- `FIELD_ENCRYPTION_KEY_ID`: `v1`처럼 배포 세대가 식별되는 1~32자 ID
- `ENCRYPT_KEY`, `HMAC_KEY`: 각각 별도로 `openssl rand -hex 32`
- DB/Redis/guest token 비밀값: 서로 재사용하지 않은 충분히 긴 암호학적 난수
- Toss, Google, Naver, Kakao, NHN Cloud Alimtalk·SMS: 각 제공자 운영 자격증명
- 도로명주소·공휴일: 공식 승인키를 설정한 뒤에만 각각 `ROAD_ADDRESS_ENABLED=true`, `PUBLIC_HOLIDAY_ENABLED=true`로 전환
- Alimtalk: NHN Cloud에 카카오 발신 프로필을 연결하고 `KakaoTemplateCatalog`의 모든 `HG_*` 템플릿을 승인받은 뒤 `ALIMTALK_SENDER_KEY`를 설정
- 알림 timeout: 예제의 `NOTIFICATION_TIMEOUT_MILLIS=5000`은 NHN transport 단계 합(`acquire 500 + connect 1000 + response 2000`)보다 크게 유지한다. 역전된 값은 애플리케이션 기동 시 거부한다.
- 결제 timeout: 애플리케이션 기본 `PAYMENT_TIMEOUT_MILLIS=5000`은 Toss transport 단계 합(`acquire 500 + connect 1000 + response 3000`)보다 크게 유지한다. 역전된 값은 애플리케이션 기동 시 거부한다.
- 이메일 인증 SMTP: STARTTLS와 직접 SSL 중 정확히 하나를 사용하고 서버 인증서 호스트명을 검증한다. `EMAIL_VERIFICATION_TIMEOUT_MILLIS`는 연결·읽기·쓰기 timeout 합보다 크게 유지한다.
- active/previous AES·HMAC·guest token 키: DB 백업과 물리적으로 분리된 복구 저장소에도 보관

```bash
./deploy/k3s/scripts/create-secrets.sh \
  /etc/happygallery/mysql.env \
  /etc/happygallery/redis.env \
  /etc/happygallery/app.env \
  /etc/happygallery/alert-webhook-url
```

`alert-webhook-url`에는 Alertmanager JSON을 받을 외부 HTTPS endpoint 한 줄만 둔다. URL은 `happygallery-alertmanager` Secret의 파일로 mount되며 manifest, release metadata와 로그에는 기록하지 않는다. critical은 1시간, warning은 4시간 반복 간격으로 같은 receiver에 전달하고 해결 알림도 보낸다. 최초 rollout 전에 수신 서비스에서 테스트 alert가 실제 도착하는지 확인한다.

Kubernetes Secret은 base64 인코딩일 뿐 자체 암호화가 아니다. k3s datastore 암호화, kubeconfig/host 접근 제한, off-device 복구 키 보관을 함께 적용한다. `create-secrets.sh`는 세 env 파일에서 예제와 일치하는 허용 키만 받으며, `SPRING_PROFILES_ACTIVE` 같은 운영 불변식 우회 키를 거부한다. 운영 모드·`prod` 단일 프로필·처리율 제한·Secure cookie·management port·전달 헤더·Actuator 상세 노출은 Secret보다 우선하는 Deployment `env`로 고정된다. 애플리케이션은 환경 후처리 단계에서 관리자 API key 비활성화와 MFA 등록 강제를 포함한 핵심 보안값을 검증해 Spring context, DataSource와 Flyway가 만들어지기 전에 잘못된 운영 설정을 거부한다. 또한 기존 MySQL PVC가 있으면 DB 비밀번호를 Secret에서만 바꾸는 동작을 거부한다. MySQL 공식 이미지의 초기화 환경 변수는 기존 데이터 디렉터리의 계정 비밀번호를 바꾸지 않기 때문이다.

기존 `happygallery-app` Secret의 active/previous AES·HMAC·guest token 키와 key ID는 일반 교체를 거부한다. 기존 MySQL PVC에서 이 Secret이 유실됐다면 새 키를 만들지 말고 분리 보관한 기존 키링을 먼저 복구한다. Toss/OAuth/알림 같은 일반 app Secret을 바꾸면 `kubectl -n happygallery rollout restart deployment/app`으로 새 Pod에 반영한다.

데이터 키 회전은 keyring과 `provider_id_enc`를 지원하는 app 이미지를 먼저 일반 rollout한 뒤 유지보수 창에서 실행한다. 이 저장소는 운영 미개시이므로 해당 이미지를 최초 운영 기준선으로 삼는다. 접두사 없는 암호문만 읽는 구 binary가 이미 운영 중인 별도 환경에서는 트래픽을 받기 전에 version-aware reader를 먼저 배포해야 하며, `hg:<keyId>:` 쓰기가 시작된 뒤에는 구 binary만 되돌리지 않고 forward fix 또는 회전 전 백업 복원을 선택한다. 새 키 파일과 기존 `backup.env`를 모두 600 권한으로 준비하고, 구키와 신키의 recovery copy가 DB 백업과 분리돼 있는지 먼저 확인한다. key ID는 retained backup의 키를 식별하므로 과거 ID를 다른 키에 재사용하지 않는다.

```bash
sudo install -m 600 -o "$USER" -g "$(id -gn)" \
  deploy/k3s/examples/data-key-rotation.env.example \
  /etc/happygallery/data-key-rotation.env

CONFIRM_DATA_KEY_ROTATION=rotate-happygallery-data-keys \
  ./deploy/k3s/scripts/rotate-data-keys.sh \
  /etc/happygallery/data-key-rotation.env \
  /etc/happygallery/backup.env
```

전용 스크립트는 다음 순서를 강제한다.

1. 단일 app replica, digest 고정 app 이미지, MySQL PVC·Secret과 외부 백업 mount marker를 확인한다.
2. app을 0 replica로 줄이고 실제 Pod가 모두 사라진 뒤 fresh `age` 암호화 백업과 checksum을 만든다.
3. 현재 app과 같은 digest의 임시 Job을 Service 없이 임의 servlet/management port로 실행한다. Job만 새 AES/HMAC을 active로, 구 AES/HMAC을 previous로 읽고 `KEY_ROTATION_ENABLED=true`로 개인정보와 관리자 TOTP 비밀키를 재암호화·재색인한다. 휴대폰·이메일 인증 행은 전량 제거되어 사용자가 다시 인증해야 한다.
4. Job 성공 후에만 runtime Secret을 새 active/구 previous로 전환하고, Redis 세션·rate-limit 상태를 `FLUSHALL`해 전체 로그아웃시킨 뒤 app을 재기동한다.
5. 오류가 나면 임시 Job/Secret을 정리하고 app을 0 replica로 유지한다. 시작 단계와 원래 replica 수를 Secret annotation에 기록하므로 같은 env로 다시 실행하면 fresh 백업부터 또는 runtime Secret 전환 이후 단계부터 재개한다. 다른 target key ID로는 덮어쓸 수 없다.

성공 직후 `/etc/happygallery/app.env`도 현재 runtime Secret과 같은 active/previous 상태로 갱신한다. 값은 로그에 출력되지 않으므로 새 키 파일과 분리 보관한 구키를 사용한다. `PREVIOUS_ENCRYPT_KEYS`와 `PREVIOUS_HMAC_KEYS` 형식은 `sourceKeyId=64자리hex`이며, `GUEST_TOKEN_PREVIOUS_HMAC_SECRET`에는 구 guest key를 둔다. 이전 키가 남아 있는 동안 새 회전을 시작할 수 없다.

기존 소셜 계정은 다음 OAuth 로그인 때 `provider_id_enc`와 새 HMAC을 lazy backfill한다. 비회원 결제 휴대폰 HMAC은 암호화 payload가 남아 있으면 Job이 새 키로 재생성한다. 만료 결제처럼 payload가 이미 제거된 행은 구 HMAC과 키 ID를 30일 보존 정리 때까지 유지한다. 구 guest token verifier는 일반·복구·결제 상태 조회 토큰 TTL 중 최댓값(기본 720시간)에 1시간 안전 여유를 더한 시각까지 유지한다. 같은 키로 서명한 비회원 결제 인증 증거도 있으므로, 회전 경계 전에 준비되어 아직 fulfillment 가능한 결제가 모두 완료·보상·대사 종결된 뒤 previous AES/HMAC/guest 키를 한 번에 제거한다.

```bash
CONFIRM_DATA_KEY_FINALIZATION=finalize-happygallery-data-keys \
  ./deploy/k3s/scripts/finalize-data-key-rotation.sh
```

finalize는 `user_social_accounts.provider_id_enc IS NULL`, active 키 ID가 아닌 `payment_attempt.owner_phone_hmac`과 `admin_user.totp_secret_enc`, 회전 경계 전에 준비된 fulfillment 가능 비회원 결제가 모두 0건이고 guest 보존기한이 지났는지 확인한다. app을 0 replica로 만든 뒤 같은 조건을 다시 확인하고 previous 키를 제거해 원래 replica를 복구하며, 실패 시 app을 0으로 유지한다. `finalizing` 단계에서 중단되면 같은 명령으로 재개한다. 성공 후 `/etc/happygallery/app.env`의 세 previous 값도 비운다. runtime에서 제거한 구키도 해당 키에 결합된 보존 백업이 남아 있는 동안 off-device recovery bundle에서는 폐기하지 않는다.

기존 MySQL 자격증명은 유지보수 창에서 DB 계정과 Kubernetes Secret을 함께 회전한다. 새 비밀번호는 저장소 밖 600 권한 파일로 준비하고, 성공 후 `/etc/happygallery/mysql.env`의 `MYSQL_ROOT_PASSWORD`/`MYSQL_PASSWORD`와 `/etc/happygallery/app.env`의 `DB_PASSWORD`도 같은 값으로 갱신한다. 스크립트는 app Pod가 실제로 모두 종료된 뒤 두 계정을 한 SQL 문장으로 바꾸고, Secret 갱신, MySQL 재시작, app 재기동 순서로 처리한다. `started`, `db-updated`, `secrets-updated`, `completed` 단계를 Secret annotation에 기록해 같은 회전 파일로 재개하며, 중간 실패 시 app은 중지 상태로 남긴다. `completed` 기록이 있어도 DB root/app 접속, MySQL·app Secret 값, 원래 app replica 복구 여부를 다시 확인하고 drift가 있으면 같은 목표로 복구한다. 부분 실패와 완료 응답 유실 재개 경로는 `scripts/validate.sh`의 fake kubectl 실행 테스트로 검증한다.

```bash
sudo install -m 600 -o "$USER" -g "$(id -gn)" \
  deploy/k3s/examples/mysql-rotation.env.example \
  /etc/happygallery/mysql-rotation.env
CONFIRM_MYSQL_CREDENTIAL_ROTATION=rotate-happygallery-mysql \
  ./deploy/k3s/scripts/rotate-mysql-credentials.sh \
  /etc/happygallery/mysql-rotation.env
```

Redis 비밀번호도 `create-secrets.sh`에서 직접 바꿀 수 없다. app과 Redis가 서로 다른 비밀번호를 읽는 구간을 없애기 위해 전용 절차가 app Pod를 모두 종료하고 Secret 교체와 Redis 재시작을 마친 뒤 기존 app replica 수를 복구한다. Redis Secret이 유실된 경우에도 같은 절차가 app을 먼저 중지하고 Secret을 재생성한다. 원래 app이 0 replica였다면 중지 상태를 유지한다. Redis 재시작으로 세션과 처리율 제한 상태가 초기화되므로 전체 회원 로그아웃을 허용하는 유지보수 창에서 실행하고, 성공 후 `/etc/happygallery/redis.env`도 같은 값으로 갱신한다.

```bash
sudo install -m 600 -o "$USER" -g "$(id -gn)" \
  deploy/k3s/examples/redis-rotation.env.example \
  /etc/happygallery/redis-rotation.env
CONFIRM_REDIS_CREDENTIAL_ROTATION=rotate-happygallery-redis \
  ./deploy/k3s/scripts/rotate-redis-credentials.sh \
  /etc/happygallery/redis-rotation.env
```

관리자 최초 설정 토큰은 상시 Secret에 넣지 않는다.

```bash
./deploy/k3s/scripts/admin-setup.sh enable /etc/happygallery/admin-setup.env
# HTTPS 공개 주소로 /api/v1/admin/setup을 한 번 실행
./deploy/k3s/scripts/admin-setup.sh disable
```

## 3. 이미지 빌드와 k3s import

애플리케이션과 프런트 이미지는 현재 Git commit의 40자리 SHA로 태깅한다. 스크립트는 dirty worktree를 거부하고 Gradle clean build가 만든 실행 JAR `bootstrap/build/libs/happygallery-app.jar`만 사용한다. 모듈 간 테스트 classpath용 `*-plain.jar`는 배포 입력이 아니다. 프런트 런타임 이미지는 운영 의존성을 설치한 뒤 서버 실행에 쓰지 않는 npm/npx를 제거한다. 운영 설정으로 빌드한 실제 app/frontend 이미지에서 Trivy HIGH/CRITICAL과 EOL OS를 차단하고, 이미지 아키텍처와 k3s 노드 아키텍처 일치를 확인한 뒤 `docker save` 결과를 k3s containerd로 import한다. import 후 containerd content digest를 읽고 `tag@sha256:digest` 별칭을 함께 보존한다.

```bash
export VITE_TOSS_CLIENT_KEY='운영 client key'
export VITE_SENTRY_DSN='선택값'
./deploy/k3s/scripts/build-import-images.sh
```

백엔드·프런트 Deployment는 `imagePullPolicy: Never`이므로 import되지 않은 이미지는 실행되지 않는다. `latest`를 사용하지 않으며 rollback 대상 이전 이미지와 digest 별칭도 containerd에서 지우지 않는다. 빌드 중 출력한 `APP_IMAGE`, `FRONTEND_IMAGE`, `APP_IMAGE_DIGEST`, `FRONTEND_IMAGE_DIGEST`, `IMAGE_TAG`를 release env에 기록한다. rollout은 tag가 가리키는 실제 containerd digest와 기록값이 다르면 중단하고, Pod manifest에는 `tag@sha256:digest`를 사용한다.

## 4. DNS, TLS와 release env

`release.env.example`을 저장소 밖에 복사한다. OAuth callback은 아래 exact URL이어야 하며 Google/Naver/Kakao 콘솔에도 똑같이 등록한다.

```text
https://<PUBLIC_HOST>/api/v1/auth/social/callback/google
https://<PUBLIC_HOST>/api/v1/auth/social/callback/naver
https://<PUBLIC_HOST>/api/v1/auth/social/callback/kakao
```

cert-manager는 HTTP-01을 사용하므로 인증서 최초 발급과 갱신 시 외부 TCP 80 접근이 필요하다. 공개 서비스는 Traefik 80/443뿐이며 app, MySQL, Redis, Prometheus, Alertmanager와 Actuator는 ClusterIP/Pod 네트워크 밖으로 노출하지 않는다.

## 5. 최초 rollout과 후속 rollout

최초 배포:

```bash
./deploy/k3s/scripts/rollout.sh /etc/happygallery/release.env
```

후속 배포는 Flyway 실행 전에 최근 암호화 복구 묶음이 필요하다. `VERIFIED_RECOVERY_BUNDLE`에는 마지막에 게시된 `happygallery-<시각>.recovery.env` 경로를 넣는다. rollout은 이 commit marker와 checksum이 48시간 이내인지, marker가 가리키는 DB·미디어 archive와 각 sidecar, 호환 release의 metadata·manifest·runtime image metadata·image archive와 각 sidecar가 모두 존재하고 일치하는지 확인한 뒤에만 진행한다.

스크립트는 다음 순서로 실행한다. app Service는 전용 Traefik `ServersTransport`를 사용해 upstream 연결 3초, 응답 헤더 30초, idle keep-alive 15초를 적용한다. Redis는 256MiB 컨테이너 한도보다 낮은 192MiB에서 `noeviction`으로 쓰기를 거절해 커널 OOM 종료 대신 애플리케이션의 fail-open/fail-closed 정책으로 장애를 드러낸다.

1. cert-manager/Traefik CRD, runtime Secret, containerd 이미지와 digest 일치 확인
2. 실제 host, ACME email, commit SHA tag와 content digest로 manifest 렌더링
3. server-side dry-run
4. MySQL, Redis, app, frontend, Prometheus, Alertmanager, Grafana rollout 대기
5. Certificate Ready, 내부 Actuator, Prometheus target, HTTP -> HTTPS, SSR SEO/API 경계 검증
6. 적용한 manifest와 이미지 식별자를 `$HOME/.local/state/happygallery/releases`에 보존

실패 시 자동 rollback하지 않는다. 새 이미지의 Flyway가 이미 실행됐을 수 있으므로 DB 호환성과 백업을 먼저 확인한다.
app Deployment는 `Recreate` 전략을 사용한다. 단일 노드에서 구 binary와 Flyway 적용 후의 새 schema가 겹쳐 실행되는 위험을 피하는 대신 app rollout 동안 짧은 API 중단을 수용한다. 비영속 단일 Redis와 클러스터링을 끈 단일 Alertmanager도 `Recreate`로 교체해 rollout 중 서로 다른 상태를 가진 두 Pod가 동시에 서비스되지 않게 한다.

V102는 휴대폰 인증 HMAC 입력에 인증 목적을 추가하고 기존 미완료 인증을 모두 폐기한다. 따라서
V102 적용 전에는 app 쓰기를 중단한 상태에서 복구 묶음을 확인해야 한다. 적용 뒤 이전 binary를
현재 DB에 연결하는 image-only rollback은 금지한다. 이전 release로 돌아가야 하면 app을 0으로
유지한 채 V102 적용 전 DB와 호환 image를 같은 복구 묶음에서 복원하고 대사·활성화 절차를 따른다.

## 6. 검증과 내부 관리 접근

```bash
./deploy/k3s/scripts/verify.sh happy-gallery.com
kubectl -n happygallery get events --sort-by=.lastTimestamp
kubectl -n happygallery logs deployment/app --since=15m
kubectl -n happygallery port-forward service/prometheus 9090:9090
kubectl -n happygallery port-forward service/grafana 3000:3000
```

검증 스크립트는 모든 workload ready replica, MySQL·미디어·Prometheus·Alertmanager·Grafana PVC, private Service 유형, 내부 `app-management:8081` health, Prometheus scrape target과 활성 Alertmanager target, 공개 TLS와 API JSON 오류를 확인한다. 공개 경로에서는 루트 SSR HTML의 canonical·실제 본문·CSP nonce, `robots.txt`, `sitemap.xml`, 알 수 없는 route의 HTTP 404를 함께 검증한다. 운영 readiness는 DB와 Redis를 포함하므로 둘 중 하나가 내려가면 app은 ready endpoint에서 제외되고 Prometheus `AppDown` 경보가 발생한다. 결제 대사·환불·알림 outbox·주문 승인 대기·예약 취소 후속 작업은 DB backlog의 건수와 처리 예정·선점·생성 시각을 기준으로 15초마다 스냅샷하고, 갱신 지연도 별도 경보로 확인한다. `OrderApprovalPending`과 `BookingCancellationTaskPending`은 처리할 일이 남은 동안 warning을 유지하고 business receiver가 30분마다 다시 알린다. `OrderApprovalDeadlineApproaching`은 가장 오래된 승인 대기 주문이 18시간을 넘어 승인 마감까지 6시간 이하로 남은 상태가 5분 지속되면 critical로 알린다. 예약 확정과 후속 작업 없는 취소는 사건별 경보를 만들지 않고 관리자 예약 일정에서 확인한다. Alertmanager의 business receiver가 앞의 두 warning을, critical receiver가 승인 마감 경보를 실제 운영 채널로 전달하는지 점검한다. 결제 `paymentProvider` 서킷의 `OPEN` 또는 최근 2분 차단 호출은 즉시 critical, `alimtalkNotification`·`smsNotification`·`phoneVerificationSms`·`emailVerification`의 같은 조건은 즉시 warning으로 전달하고 Grafana에서 상태·실패율·호출 결과·차단 호출을 함께 확인한다. Grafana는 외부 Ingress가 없는 cluster 내부 익명 Viewer이며 운영자 `kubectl port-forward`로만 연다. `SKIP_PUBLIC_CHECK=true`는 DNS 연결 전 내부 점검에만 사용한다. 정적 연결 확인만으로 외부 receiver 수신 성공을 증명할 수 없으므로 실제 테스트 alert 수신 확인은 별도 운영 점검이다.

운영 호스트와 공유기·방화벽에서는 다음도 별도로 확인한다.

- 외부에서 80/443 이외 app 8080/8081, MySQL 3306, Redis 6379, Prometheus 9090 접근 불가
- 실제 브라우저에서 Secure 세션 cookie, CSRF, Google/Naver/Kakao callback, 결제 confirm
- 실제 클라이언트 IP별 rate-limit 분리
- 운영 호스트 재부팅 후 k3s, PVC와 workload 자동 복구
- `df -h`, `kubectl top` 또는 호스트 모니터링을 통한 디스크/메모리 여유

## 7. 외부 암호화 복구 백업

백업 스크립트는 원래 app replica를 확인하고 1이면 0으로 축소해 Pod 종료를 기다린다. 이어 MySQL Pod의 dump를 stdout으로만 내보내 호스트가 `gzip -> age`로 암호화해 외부 mount에 직접 기록하고, 전용 유지보수 Pod가 `app-media` PVC를 읽어 상품 이미지 archive도 같은 방식으로 암호화한다. 미디어 archive가 끝나면 원래 replica를 복구하며, 키 회전처럼 이미 0이었던 경우에는 계속 0으로 유지한다. 이 계획 중단으로 DB·미디어 백업과 애플리케이션 보존 배치·관리자 쓰기를 상호 배제한다. 중단 시간은 데이터 크기와 원격 전송 속도에 따라 달라지므로 실제 백업·재기동 시간을 개통 전에 측정한다. 평문 SQL이나 이미지 archive는 생성하지 않는다. 각 암호문에 SHA-256 sidecar를 만들며 기본 보존 기간은 30일이다. 미디어 기능 도입 전부터 운영한 클러스터에 PVC가 아직 없으면 백업 스크립트가 독립된 `app-media-pvc.yaml`을 먼저 적용하므로, 새 app manifest를 배포하기 전에도 기존 DB와 빈 미디어 볼륨을 하나의 복구 묶음으로 만들 수 있다.

app 쓰기가 중단된 상태에서 DB 스냅샷을 먼저 만들고 미디어를 뒤이어 보관한다. `happygallery-<시각>.recovery.env`의 `DATABASE_BACKUP`과 `MEDIA_BACKUP`은 분리해서 복원할 수 없는 하나의 복구 단위다.

DB만 복원되고 실행할 바이너리가 사라지는 상황을 막기 위해 같은 외부 매체의 `releases/<IMAGE_TAG>/`에는 호환 app/frontend와 MySQL·Redis·Prometheus·Alertmanager·Grafana 이미지 archive, digest metadata와 렌더링 manifest를 commit SHA별 한 번 보존한다. runtime workload 목록은 `runtime-images-from-manifest.rb` 한 곳만 소유하며, 백업과 복원은 해당 release의 `manifests.yaml`과 `runtime-images.env`를 parser가 대조해 만든 key·image·digest inventory를 순서대로 처리한다. 추출한 참조는 고정 tag 또는 SHA-256 digest 형식이어야 하며, containerd의 실제 digest와 archive checksum을 기존과 같이 검증한다. 각 복구 백업의 `happygallery-<시각>.recovery.env`는 DB·미디어 파일, release 경로, Flyway schema version, active 암호화 키 ID·keyring SHA-256 fingerprint와 키 회전 단계를 묶는다. fingerprint는 키 원문을 저장하지 않으면서 같은 ID에 잘못된 키를 넣은 복구도 차단한다. 모든 산출물은 먼저 `.partial`로 완성하고 DB·미디어 archive와 sidecar, recovery sidecar 순서로 이름을 확정한 뒤 `recovery.env`를 마지막에 게시한다. 따라서 같은 시각의 `recovery.env`가 없는 중단 산출물은 완성된 복구 묶음으로 사용하지 않는다. release archive는 여러 복구 백업이 공유하므로 자동 보존 정리에서 삭제하지 않는다. 해당 release를 가리키는 복구 백업이 더 없고 별도 복원 검증을 마친 뒤에만 수동 삭제한다.

1. 복원 전용 age identity를 운영 호스트와 분리해 보관하고 public recipient만 운영 호스트에 둔다.
2. 외부 매체가 실제 mount된 상태에서 전용 백업 디렉터리에 marker를 한 번 만든다.
3. 매체가 빠지면 host의 빈 mountpoint에 백업하지 않도록 marker가 없을 때 스크립트가 실패한다.

```bash
sudo mkdir -p /mnt/off-device/happygallery
sudo touch /mnt/off-device/happygallery/.happygallery-off-device-backup-target
sudo chmod 700 /mnt/off-device/happygallery

export BACKUP_DIR=/mnt/off-device/happygallery
export BACKUP_AGE_RECIPIENT='age1...'
./deploy/k3s/scripts/backup-mysql.sh
./deploy/k3s/scripts/prune-backups.sh
```

systemd 6시간 간격 실행(예시 timer가 `Asia/Seoul`을 명시한다):

```bash
sudo install -m 600 deploy/k3s/examples/backup.env.example /etc/happygallery/backup.env
sudo install -m 600 deploy/k3s/examples/backup-alert.env.example /etc/happygallery/backup-alert.env
sudo install -m 644 deploy/k3s/systemd/happygallery-backup.service.example /etc/systemd/system/happygallery-backup.service
sudo install -m 644 deploy/k3s/systemd/happygallery-backup.timer.example /etc/systemd/system/happygallery-backup.timer
sudo install -m 644 deploy/k3s/systemd/happygallery-backup-failure@.service.example /etc/systemd/system/happygallery-backup-failure@.service
sudo install -m 644 deploy/k3s/systemd/happygallery-backup-watchdog.service.example /etc/systemd/system/happygallery-backup-watchdog.service
sudo install -m 644 deploy/k3s/systemd/happygallery-backup-watchdog.timer.example /etc/systemd/system/happygallery-backup-watchdog.timer
sudo systemctl daemon-reload
sudo systemctl start happygallery-backup.service
sudo systemctl enable --now happygallery-backup.timer happygallery-backup-watchdog.timer
systemctl list-timers happygallery-backup.timer happygallery-backup-watchdog.timer
```

예시 unit은 저장소가 `/opt/happygallery`에 있다고 가정한다. 실제 checkout 경로와 `kubectl` 경로가 다르면 unit과 `/etc/happygallery/backup.env`를 함께 수정한다.

성공한 실행은 `/var/lib/happygallery/backup.last-success`를 갱신하고, 실패하거나 30분 실행 제한을 넘으면 별도 HTTPS webhook unit을 호출한다. 실행 제한으로 종료할 때는 app 원복 trap이 완료되도록 10분 종료 유예를 둔다. systemd service는 app을 내리기 전에 내부 Alertmanager에 `AppDown`만 최대 45분 silence로 등록하고 종료 시 즉시 해제한다. silence 생성에 실패하면 계획 중단을 시작하지 않으며, 백업이나 silence 해제가 실패하면 기존 `OnFailure` webhook이 알린다. 호스트가 비정상 종료돼 해제하지 못해도 45분 뒤 자동 만료된다.

독립 watchdog은 15분마다 heartbeat를 검사해 7시간 넘게 정체되거나 파일이 사라지면 같은 webhook 경로로 알린다. 설치 직후 첫 성공 heartbeat를 만들기 위해 위 순서처럼 백업 service를 한 번 성공시킨 뒤 timer를 활성화한다. watchdog도 같은 운영 호스트에서 실행되므로 전원·호스트 장애는 알 수 없고, 외부 uptime 모니터도 heartbeat 또는 공개 health를 별도로 확인해야 한다. 현재 DB 논리 dump와 미디어 archive 기준 RPO는 약 6시간이며 PITR나 미디어 증분 복제는 제공하지 않는다. 주문량과 이미지 변경량이 늘거나 6시간 손실을 허용할 수 없게 되면 MySQL binlog 외부 연속 보관과 미디어 증분 복제로 전환한다.

## 8. 복원 훈련

복원은 분기마다 별도 테스트 namespace/클러스터에서 훈련한다. 운영 DB 복원이 필요하면 먼저 현재 DB의 추가 백업을 만들고 유지보수 창을 연다.

```bash
kubectl -n happygallery scale deployment/app --replicas=0

export CONFIRM_RESTORE=restore-happygallery
./deploy/k3s/scripts/restore-recovery-backup.sh \
  /mnt/off-device/happygallery/happygallery-YYYYMMDDTHHMMSSZ.recovery.env \
  /secure/off-device/age-identity.txt

# app은 계속 0 replica다. 출력된 일회용 대사 토큰을 기록하고 아래 세 대사를 먼저 완료한다.
export RESTORED_IMAGE_TAG='<호환 release의 IMAGE_TAG>'
export RESTORE_RECONCILIATION_TOKEN='<복원 완료 시 출력된 timestamp-checksum 토큰>'
export CONFIRM_RESTORED_RELEASE="$RESTORED_IMAGE_TAG"
export CONFIRM_RESTORED_PAYMENT_RECONCILIATION="$RESTORE_RECONCILIATION_TOKEN"
export CONFIRM_RESTORED_NOTIFICATION_RECONCILIATION="$RESTORE_RECONCILIATION_TOKEN"
export CONFIRM_RESTORED_PRIVACY_REQUEST_RECONCILIATION="$RESTORE_RECONCILIATION_TOKEN"
./deploy/k3s/scripts/activate-restored-release.sh \
  /mnt/off-device/happygallery/releases/"$RESTORED_IMAGE_TAG"
./deploy/k3s/scripts/verify.sh happy-gallery.com
```

복원 절차는 다음 조건을 강제한다.

- app desired replica가 0이고 종료 중인 app Pod까지 실제 0개
- ciphertext SHA-256 일치
- age 인증 복호화와 gzip 무결성 통과
- 복원 후 `mysqlcheck` 통과
- 미디어 archive checksum·tar 무결성 통과 후 `app-media` PVC를 같은 백업 시점으로 교체
- DB 시점과 불일치할 Redis 세션·rate-limit 상태 삭제
- runtime active/previous 암호화·HMAC·비회원 토큰 keyring의 ID/fingerprint와 백업 메타데이터 일치
- 복원된 `flyway_schema_history`와 백업 메타데이터의 schema version 일치
- DB를 DROP하기 전 외부 이미지 archive checksum 검증, 필요 이미지 containerd import, app/frontend와 MySQL·Redis·Prometheus·Alertmanager·Grafana 전체 digest 재검증
- 데이터 복원과 검증이 끝나도 app 0 replica 유지
- `BACKUP_CREATED_AT`과 검증한 `recovery.env` SHA-256을 결합한 대사 토큰을 성공한 복원에만 발급
- app이 0인 동안 PG 콘솔·API의 결제/환불 결과와 복원 DB의 결제 시도·대사 필요 건 비교
- 알림 제공자 발송 결과와 복원 DB의 outbox·발송 로그 비교
- 저장소 밖 개인정보 열람·정정·삭제 요청 접수대장과 복원 DB 처리 결과 비교
- 세 대사 완료 확인 값을 해당 복원 대사 토큰과 일치시킨 뒤에만 app/frontend digest 반영과 app scale-up
- 토큰 marker는 활성화 직전에 `pending`에서 실행별 고유 `activating`, 성공 후 `consumed`로 원자적으로 이동해 재사용 차단. `pending -> activating` 이동에 성공해 marker 소유권을 얻은 실행만 실패 정리 수행

복원 진입점은 DB 교체부터 미디어 PVC 교체와 대사 토큰 발급까지 `restores/.restore-operation.lock` 디렉터리를 원자적으로 선점한다. 정상 종료와 오류·일반 종료 신호에서는 자신이 선점한 실행 락을 정리하므로, 같은 운영 호스트에서 서로 다른 복구 묶음을 동시에 실행해도 두 번째 실행은 DB를 DROP하기 전에 실패한다. `SIGKILL`이나 전원 차단 뒤 실행 락이 남으면 자동으로 회수하지 않는다. 복원 프로세스가 실제로 끝났는지와 DB·미디어 교체 지점을 먼저 확인한 뒤 운영자가 락을 정리한다. 이 일시 실행 락은 아래의 지속성 있는 대사 상태 marker와 별개다.

대사 토큰 marker는 release 상태 디렉터리와 같은 상위 경로의 `restores/`에 600 권한으로 둔다. 완료하지 않은 `pending` 또는 실행별 고유 `activating` marker가 있으면 새 복원을 시작하지 않는다. 경쟁 실행 중 `pending -> activating` 이동에 성공한 프로세스만 자기 실행 ID가 포함된 marker와 실패 정리 책임을 얻으며, 이동에 실패한 프로세스는 기존 실행의 app이나 marker를 건드리지 않고 종료한다. marker 이동 직후 일반 종료 신호가 도착해도 고유 marker 존재로 소유권을 판별한다. 활성화 시작 뒤 명령 실패, 명시적 오류, `SIGHUP`·`SIGINT`·`SIGTERM` 종료가 발생하면 소유 실행의 종료 정리가 app을 다시 0개로 내리고 Pod 종료를 확인한 뒤 marker를 `pending`으로 되돌려 같은 복원의 재시도를 허용한다. app 중지나 marker 복구를 확인하지 못했거나 `SIGKILL`·전원 차단처럼 종료 정리를 실행할 수 없으면 토큰을 임의 재사용하지 말고 app, DB, 적용 image와 현재 marker를 먼저 수동 확인한다.

활성화 후 회원 로그인, 개인정보 복호화, 이름/전화번호 HMAC 조회, 주문·예약·결제 이력, 상품 이미지 응답과 Flyway 상태를 확인한다. 백업 시점의 active/previous AES·HMAC 키링을 잃었거나 다른 키를 쓰면 데이터 복구가 완료된 것이 아니다. guest token 연속성이 필요하면 백업 시점 guest active/previous 키도 함께 복구한다.

## 9. rollback

release 디렉터리에는 secret 값이 아닌 렌더링 manifest와 tag/digest 이미지 식별자만 남는다. DB/Flyway는 이미지 rollback으로 되돌아가지 않으므로 명시적 확인 없이는 실행되지 않는다.

```bash
export CONFIRM_ROLLBACK='<이전 IMAGE_TAG>'
export ACKNOWLEDGE_DATABASE_NOT_ROLLED_BACK=true
./deploy/k3s/scripts/rollback.sh \
  "$HOME/.local/state/happygallery/releases/<이전 release 디렉터리>"
```

rollback은 보존된 전체 manifest를 재적용하지 않는다. digest로 고정한 app/frontend Deployment 이미지와 app의 `SENTRY_RELEASE`만 되돌리며 MySQL StatefulSet, PVC, Redis, StorageClass, ClusterIssuer, NetworkPolicy 등 stateful/cluster 리소스는 변경하지 않는다. 이전 digest 별칭이 containerd에 남아 있어야 하며, 이전 애플리케이션이 현재 DB schema와 양방향 호환되지 않으면 rollback 대신 DB 복원 또는 수정 배포를 선택한다. DB를 복원해 app이 0인 상태에서는 ready replica를 전제로 하는 rollback 대신 `activate-restored-release.sh`를 사용한다.

특히 V102 이후의 DB는 V102 이전 binary와 호환되지 않는다. 이전 image가 필요하면 V102 적용 전
DB 복구 묶음을 함께 복원해야 하며, 현재 DB를 유지한 채 image만 내리는 rollback은 수행하지 않는다.

## 10. 정적 검증

클러스터 없이 저장소 산출물을 확인한다.

```bash
./deploy/k3s/scripts/validate.sh
```

이 검증은 Kustomize 렌더링, YAML 파싱, Prometheus 경보·Grafana 대시보드 단일 원본 drift, release manifest의 runtime 이미지 추출, shell 구문, probe/종료 유예와 Retain PVC·내부 Prometheus/OAuth callback, app/frontend digest 고정, 운영 불변식의 명시적 환경 변수 고정과 Secret 우회 키 거부, frontend SSR의 내부 API·app 8080 ingress, Ingress의 CSP 비중복, Redis·Prometheus·Alertmanager·Grafana 단일 인스턴스의 `Recreate`, 백업 timer의 `Asia/Seoul` 시각과 DB·미디어 백업 중 app 쓰기 중단·원복, heartbeat watchdog의 독립 실행과 정체 감지, 복원 전 Pod 종료, 데이터 복원 뒤 자동 기동 금지, PG·알림·개인정보 요청 대사 확인과 호환 digest 선반영 순서, 활성화 중 명령 실패·명시적 오류·HUP/INT/TERM 종료의 app drain과 marker 복구, stateful rollback 금지, 데이터 결합 키·DB·Redis Secret 단독 교체 방지, 기존 클러스터의 미디어 PVC 사전 생성, `recovery.env` 최종 게시와 DB·미디어·release sidecar 전체 검증, 데이터 키 회전의 app drain/fresh backup/동일 digest Job/runtime Secret/Redis/app 순서, finalize의 소셜 백필·guest 보존기한·실패 drain, 직접 공개 Service와 `latest` 금지를 확인한다. 실제 TLS, DNS, 방화벽, containerd import, PVC binding, SSR nonce 일치, 브라우저 CSP 콘솔, 백업 mount와 restore/키 회전 성공은 대상 운영 호스트에서만 검증할 수 있다.
