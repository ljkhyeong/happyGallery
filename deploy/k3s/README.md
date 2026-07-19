# happyGallery 단일 노트북 k3s 운영

이 디렉터리는 ADR-0037의 단일 노드 운영 토폴로지를 실행 가능한 형태로 만든다. Docker Compose는 계속 로컬 개발·복구 진단용이며 이 manifest의 대체물이 아니다.

```text
Internet -> router/firewall :80/:443 -> k3s Traefik
                                      -> /api/* -> app:8080
                                      -> 그 외   -> frontend:8080
app -> mysql:3306 (Retain PVC)
app -> redis:6379 (비영속 세션/처리율 상태)
prometheus -> app-management:8081/actuator/prometheus (cluster 내부 전용)
           -> alertmanager:9093 -> 외부 HTTPS webhook
```

단일 노트북, 디스크, 전원, 공유기, 인터넷 또는 k3s 장애는 전체 서비스 중단으로 이어진다. 이 구성은 고가용성을 제공하지 않는다.
Prometheus는 애플리케이션 내부 지표와 alert rule을 평가하고 내부 Alertmanager가 저장소 밖 Secret의 HTTPS webhook으로 전달한다. 다만 둘 다 같은 노트북에 있으므로 노트북 자체 장애는 알릴 수 없다. 외부 uptime 감시와 webhook 수신자는 노트북 밖에 별도로 둔다.

## 디렉터리

| 경로 | 역할 |
| --- | --- |
| `base/` | Namespace, Deployment/StatefulSet, ClusterIP Service, Ingress/TLS, PVC, NetworkPolicy |
| `cluster/` | k3s 기본 Traefik의 전달 헤더 신뢰 경계 설정 |
| `images/` | React 정적 이미지와 SPA/API 경계 Nginx 설정 |
| `examples/` | 저장소 밖에 만들 운영 env 파일의 키 목록 |
| `scripts/` | secret 생성, 이미지 import, rollout/rollback, 검증, 백업/복원 |
| `systemd/` | 노트북 호스트에서 일일 외부 백업을 실행하는 unit 예시 |

## 1. 외부 전제

- Linux 노트북 한 대에 단일 노드 k3s, Docker, Git, Java 21, `age`, `curl`을 설치한다.
- k3s는 `secrets-encryption: true`로 설치하고 `/etc/rancher/k3s/k3s.yaml`을 root 또는 지정 운영자만 읽게 한다.
- k3s 기본 Traefik과 local-path provisioner를 사용한다. 다른 Ingress/StorageClass를 쓰려면 manifest와 검증 스크립트를 함께 변경한다.
- cert-manager `v1.20.2` 정적 manifest를 공식 release에서 받아 출처와 checksum/signature를 검증해 노트북에 보관한다.
- 공개 DNS A/AAAA 레코드가 노트북의 실제 공개 주소를 가리켜야 한다. 공유기 포트 전달과 호스트 방화벽은 TCP 80/443만 허용한다.
- CGNAT이면 일반 포트 전달만으로 외부 공개가 불가능하다. 공인 IP 또는 통제 가능한 터널/프록시를 먼저 준비하고 전달 헤더 신뢰 경계를 다시 설계한다.
- MySQL 백업 대상은 노트북 내부 디스크가 아닌 분리된 USB 디스크, NAS 또는 원격 mount여야 한다.

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

## 2. Secret 준비

예제 파일을 `/etc/happygallery` 같은 저장소 밖 경로에 만든 뒤 권한을 제한한다. 값에는 shell 따옴표를 넣지 않는다.

```bash
sudo install -d -m 700 -o "$USER" -g "$(id -gn)" /etc/happygallery
sudo install -m 600 -o "$USER" -g "$(id -gn)" deploy/k3s/examples/mysql.env.example /etc/happygallery/mysql.env
sudo install -m 600 -o "$USER" -g "$(id -gn)" deploy/k3s/examples/redis.env.example /etc/happygallery/redis.env
sudo install -m 600 -o "$USER" -g "$(id -gn)" deploy/k3s/examples/app.env.example /etc/happygallery/app.env
sudo install -m 600 -o "$USER" -g "$(id -gn)" deploy/k3s/examples/alert-webhook-url.example /etc/happygallery/alert-webhook-url
```

- `ENCRYPT_KEY`, `HMAC_KEY`: 각각 `openssl rand -hex 32`
- DB/Redis/guest token 비밀값: 충분히 긴 암호학적 난수
- Toss, Google, Naver, NHN Cloud Alimtalk·SMS: 각 제공자 운영 자격증명
- Alimtalk: NHN Cloud에 카카오 발신 프로필을 연결하고 `KakaoTemplateCatalog`의 모든 `HG_*` 템플릿을 승인받은 뒤 `ALIMTALK_SENDER_KEY`를 설정
- 알림 timeout: 예제의 `NOTIFICATION_TIMEOUT_MILLIS=5000`은 NHN transport 단계 합(`acquire 500 + connect 1000 + response 2000`)보다 크게 유지한다. 역전된 값은 애플리케이션 기동 시 거부한다.
- `ENCRYPT_KEY`와 `HMAC_KEY`: DB 백업과 물리적으로 분리된 복구 저장소에도 보관

```bash
./deploy/k3s/scripts/create-secrets.sh \
  /etc/happygallery/mysql.env \
  /etc/happygallery/redis.env \
  /etc/happygallery/app.env \
  /etc/happygallery/alert-webhook-url
```

`alert-webhook-url`에는 Alertmanager JSON을 받을 외부 HTTPS endpoint 한 줄만 둔다. URL은 `happygallery-alertmanager` Secret의 파일로 mount되며 manifest, release metadata와 로그에는 기록하지 않는다. critical은 1시간, warning은 4시간 반복 간격으로 같은 receiver에 전달하고 해결 알림도 보낸다. 최초 rollout 전에 수신 서비스에서 테스트 alert가 실제 도착하는지 확인한다.

Kubernetes Secret은 base64 인코딩일 뿐 자체 암호화가 아니다. k3s datastore 암호화, kubeconfig/host 접근 제한, off-device 복구 키 보관을 함께 적용한다. `create-secrets.sh`는 기존 MySQL PVC가 있으면 DB 비밀번호를 Secret에서만 바꾸는 동작을 거부한다. MySQL 공식 이미지의 초기화 환경 변수는 기존 데이터 디렉터리의 계정 비밀번호를 바꾸지 않기 때문이다.

기존 `happygallery-app` Secret의 `ENCRYPT_KEY`, `HMAC_KEY`, `GUEST_TOKEN_HMAC_SECRET`도 일반 교체를 거부한다. 이 값들은 기존 암호문, 블라인드 인덱스와 접근 토큰에 결합되어 있으므로 데이터 재암호화·인덱스 재생성·토큰 전환을 포함한 별도 키 회전 절차가 필요하며, 그 절차는 아직 구현하지 않았다. 기존 MySQL PVC에서 이 Secret이 유실됐다면 새 키를 만들지 말고 분리 보관한 기존 값을 먼저 복구한다. 운영 env와 분리 복구본에는 반드시 현재 값을 유지한다. Toss/OAuth/알림 같은 일반 app Secret을 바꾸면 `kubectl -n happygallery rollout restart deployment/app`으로 새 Pod에 반영한다.

기존 MySQL 자격증명은 유지보수 창에서 DB 계정과 Kubernetes Secret을 함께 회전한다. 새 비밀번호는 저장소 밖 600 권한 파일로 준비하고, 성공 후 `/etc/happygallery/mysql.env`의 `MYSQL_ROOT_PASSWORD`/`MYSQL_PASSWORD`와 `/etc/happygallery/app.env`의 `DB_PASSWORD`도 같은 값으로 갱신한다. 스크립트는 app Pod가 실제로 모두 종료된 뒤 두 계정을 한 SQL 문장으로 바꾸고, Secret 갱신, MySQL 재시작, app 재기동 순서로 처리한다. 중간 실패 시 app은 중지 상태로 남겨 불일치 자격증명으로 쓰기가 재개되지 않게 한다.

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

애플리케이션과 프런트 이미지는 현재 Git commit의 40자리 SHA로 태깅한다. 스크립트는 dirty worktree를 거부하고 Gradle clean build에서 non-plain 실행 jar를 정확히 하나 선택하며, 이미지 아키텍처와 k3s 노드 아키텍처 일치를 확인한 뒤 `docker save` 결과를 k3s containerd로 import한다. import 후 containerd content digest를 읽고 `tag@sha256:digest` 별칭을 함께 보존한다.

```bash
export VITE_TOSS_CLIENT_KEY='운영 client key'
export VITE_SENTRY_DSN='선택값'
./deploy/k3s/scripts/build-import-images.sh
```

백엔드·프런트 Deployment는 `imagePullPolicy: Never`이므로 import되지 않은 이미지는 실행되지 않는다. `latest`를 사용하지 않으며 rollback 대상 이전 이미지와 digest 별칭도 containerd에서 지우지 않는다. 빌드 중 출력한 `APP_IMAGE`, `FRONTEND_IMAGE`, `APP_IMAGE_DIGEST`, `FRONTEND_IMAGE_DIGEST`, `IMAGE_TAG`를 release env에 기록한다. rollout은 tag가 가리키는 실제 containerd digest와 기록값이 다르면 중단하고, Pod manifest에는 `tag@sha256:digest`를 사용한다.

## 4. DNS, TLS와 release env

`release.env.example`을 저장소 밖에 복사한다. OAuth callback은 아래 exact URL이어야 하며 Google/Naver 콘솔에도 똑같이 등록한다.

```text
https://<PUBLIC_HOST>/api/v1/auth/social/callback/google
https://<PUBLIC_HOST>/api/v1/auth/social/callback/naver
```

cert-manager는 HTTP-01을 사용하므로 인증서 최초 발급과 갱신 시 외부 TCP 80 접근이 필요하다. 공개 서비스는 Traefik 80/443뿐이며 app, MySQL, Redis, Prometheus, Alertmanager와 Actuator는 ClusterIP/Pod 네트워크 밖으로 노출하지 않는다.

## 5. 최초 rollout과 후속 rollout

최초 배포:

```bash
./deploy/k3s/scripts/rollout.sh /etc/happygallery/release.env
```

후속 배포는 Flyway 실행 전에 최근 암호화 백업이 필요하다. `VERIFIED_BACKUP_FILE`과 같은 이름의 `.sha256` 파일이 존재하고 48시간 이내여야 rollout이 진행된다.

스크립트는 다음 순서로 실행한다.

1. cert-manager/Traefik CRD, runtime Secret, containerd 이미지와 digest 일치 확인
2. 실제 host, ACME email, commit SHA tag와 content digest로 manifest 렌더링
3. server-side dry-run
4. MySQL, Redis, app, frontend, Prometheus, Alertmanager rollout 대기
5. Certificate Ready, 내부 Actuator, Prometheus target, HTTP -> HTTPS, SPA/API 경계 검증
6. 적용한 manifest와 이미지 식별자를 `$HOME/.local/state/happygallery/releases`에 보존

실패 시 자동 rollback하지 않는다. 새 이미지의 Flyway가 이미 실행됐을 수 있으므로 DB 호환성과 백업을 먼저 확인한다.
app Deployment는 `Recreate` 전략을 사용한다. 단일 노드에서 구 binary와 Flyway 적용 후의 새 schema가 겹쳐 실행되는 위험을 피하는 대신 app rollout 동안 짧은 API 중단을 수용한다. 비영속 단일 Redis와 클러스터링을 끈 단일 Alertmanager도 `Recreate`로 교체해 rollout 중 서로 다른 상태를 가진 두 Pod가 동시에 서비스되지 않게 한다.

## 6. 검증과 내부 관리 접근

```bash
./deploy/k3s/scripts/verify.sh gallery.example.com
kubectl -n happygallery get events --sort-by=.lastTimestamp
kubectl -n happygallery logs deployment/app --since=15m
kubectl -n happygallery port-forward service/prometheus 9090:9090
```

검증 스크립트는 모든 workload ready replica, MySQL PVC, private Service 유형, 내부 `app-management:8081` health, Prometheus scrape target과 활성 Alertmanager target, 공개 TLS와 API JSON 오류를 확인한다. `SKIP_PUBLIC_CHECK=true`는 DNS 연결 전 내부 점검에만 사용한다. 정적 연결 확인만으로 외부 receiver 수신 성공을 증명할 수 없으므로 실제 테스트 alert 수신 확인은 별도 운영 점검이다.

호스트/공유기에서는 다음도 별도로 확인한다.

- 외부에서 80/443 이외 app 8080/8081, MySQL 3306, Redis 6379, Prometheus 9090 접근 불가
- 실제 브라우저에서 Secure 세션 cookie, CSRF, Google/Naver callback, 결제 confirm
- 실제 클라이언트 IP별 rate-limit 분리
- 노트북 재부팅 후 k3s, PVC와 workload 자동 복구
- `df -h`, `kubectl top` 또는 호스트 모니터링을 통한 디스크/메모리 여유

## 7. 외부 암호화 백업

백업은 MySQL Pod에서 dump를 stdout으로만 내보내고 호스트가 `gzip -> age`로 암호화해 외부 mount에 직접 기록한다. 평문 SQL 파일은 생성하지 않는다. 성공 후 암호문 SHA-256 sidecar를 만들며 기본 보존 기간은 30일이다.

1. 복원 전용 age identity를 노트북과 분리해 보관하고 public recipient만 노트북에 둔다.
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

systemd 일일 실행:

```bash
sudo install -m 600 deploy/k3s/examples/backup.env.example /etc/happygallery/backup.env
sudo install -m 644 deploy/k3s/systemd/happygallery-backup.service.example /etc/systemd/system/happygallery-backup.service
sudo install -m 644 deploy/k3s/systemd/happygallery-backup.timer.example /etc/systemd/system/happygallery-backup.timer
sudo systemctl daemon-reload
sudo systemctl enable --now happygallery-backup.timer
systemctl list-timers happygallery-backup.timer
```

예시 unit은 저장소가 `/opt/happygallery`에 있다고 가정한다. 실제 checkout 경로와 `kubectl` 경로가 다르면 unit과 `/etc/happygallery/backup.env`를 함께 수정한다.

백업 성공 알림은 systemd 실패 알림 또는 별도 호스트 모니터에 연결해야 한다. timer만 켜고 최근 파일과 journal을 확인하지 않으면 백업 완료로 보지 않는다.

## 8. 복원 훈련

복원은 분기마다 별도 테스트 namespace/클러스터에서 훈련한다. 운영 DB 복원이 필요하면 먼저 현재 DB의 추가 백업을 만들고 유지보수 창을 연다.

```bash
kubectl -n happygallery scale deployment/app --replicas=0

CONFIRM_RESTORE=restore-happygallery \
  ./deploy/k3s/scripts/restore-mysql.sh \
  /mnt/off-device/happygallery/happygallery-YYYYMMDDTHHMMSSZ.sql.gz.age \
  /secure/off-device/age-identity.txt

# app이 0인 동안 백업 시점 DB schema와 호환되는 digest release를 먼저 지정한다.
export CONFIRM_RESTORED_RELEASE='<호환 release의 IMAGE_TAG>'
./deploy/k3s/scripts/activate-restored-release.sh \
  "$HOME/.local/state/happygallery/releases/<호환 release 디렉터리>"
./deploy/k3s/scripts/verify.sh gallery.example.com
```

복원 절차는 다음 조건을 강제한다.

- app desired replica가 0이고 종료 중인 app Pod까지 실제 0개
- ciphertext SHA-256 일치
- age 인증 복호화와 gzip 무결성 통과
- 복원 후 `mysqlcheck` 통과
- DB 시점과 불일치할 Redis 세션·rate-limit 상태 삭제
- app이 0인 상태에서 선택한 release의 app/frontend digest 반영 후 app scale-up

복원 후 회원 로그인, 개인정보 복호화, 이름/전화번호 HMAC 조회, 주문·예약·결제 이력과 Flyway 상태를 확인한다. `ENCRYPT_KEY`/`HMAC_KEY`를 잃었거나 백업 시점과 다른 키를 쓰면 데이터 복구가 완료된 것이 아니다.

## 9. rollback

release 디렉터리에는 secret 값이 아닌 렌더링 manifest와 tag/digest 이미지 식별자만 남는다. DB/Flyway는 이미지 rollback으로 되돌아가지 않으므로 명시적 확인 없이는 실행되지 않는다.

```bash
export CONFIRM_ROLLBACK='<이전 IMAGE_TAG>'
export ACKNOWLEDGE_DATABASE_NOT_ROLLED_BACK=true
./deploy/k3s/scripts/rollback.sh \
  "$HOME/.local/state/happygallery/releases/<이전 release 디렉터리>"
```

rollback은 보존된 전체 manifest를 재적용하지 않는다. digest로 고정한 app/frontend Deployment 이미지와 app의 `SENTRY_RELEASE`만 되돌리며 MySQL StatefulSet, PVC, Redis, StorageClass, ClusterIssuer, NetworkPolicy 등 stateful/cluster 리소스는 변경하지 않는다. 이전 digest 별칭이 containerd에 남아 있어야 하며, 이전 애플리케이션이 현재 DB schema와 양방향 호환되지 않으면 rollback 대신 DB 복원 또는 수정 배포를 선택한다. DB를 복원해 app이 0인 상태에서는 ready replica를 전제로 하는 rollback 대신 `activate-restored-release.sh`를 사용한다.

## 10. 정적 검증

클러스터 없이 저장소 산출물을 확인한다.

```bash
./deploy/k3s/scripts/validate.sh
```

이 검증은 Kustomize 렌더링, YAML 파싱, shell 구문, probe/종료 유예/PVC/내부 Prometheus/OAuth callback, app/frontend digest 고정, Redis·Alertmanager 단일 인스턴스의 `Recreate`, 복원 전 Pod 종료와 호환 digest 선반영 순서, stateful rollback 금지, 데이터 결합 키·DB·Redis Secret 단독 회전 방지, 회전 실패 시 app drain, 직접 공개 Service와 `latest` 금지를 확인한다. 실제 TLS, DNS, 방화벽, containerd import, PVC binding, 백업 mount와 restore 성공은 대상 노트북에서만 검증할 수 있다.
