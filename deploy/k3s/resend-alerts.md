# Resend 장애 알림

Alertmanager가 Resend SMTP로 운영자의 수신함에 장애·복구 메일을 보낸다. 별도 웹훅 수신 서비스를 구독하지 않는다. 회원 인증 메일과 같은 Resend 발송 한도를 사용한다. 이 변경은 배포 설정과 운영 스크립트에만 해당하며 앱·프론트엔드 이미지를 다시 빌드하지 않는다.

노트북 자체의 전원·회선 장애는 같은 호스트의 Alertmanager가 감지하거나 전달할 수 없다. [외부 감시](free-integrations.md#2-서버-밖에서-장애-감시)를 별도로 구성한다.

## 1. 서버에 수신 주소 저장

이후 명령은 Ubuntu 서버의 `/opt/happygallery`에서 실행한다. 기존 Resend SMTP 발송과 수신함 도착을 확인한 상태를 전제로 한다.

```bash
cd /opt/happygallery
umask 077
vi /etc/happygallery/alertmanager.env
```

파일에는 실제 수신 이메일 주소 한 개만 입력한다. 아래 예시 주소는 교체한다.

```dotenv
ALERT_EMAIL_TO=operator@example.org
```

```bash
chmod 600 /etc/happygallery/alertmanager.env
```

SMTP 호스트·사용자·비밀키·발신 주소는 `/etc/happygallery/app.env`의 `EMAIL_VERIFICATION_*` 값을 읽는다. `EMAIL_VERIFICATION_PROVIDER=smtp`, STARTTLS `true`, SSL `false`를 사용한다. Resend는 `smtp.resend.com:587`, 사용자 `resend`, 암호는 기존 API key다. 파일 값을 shell에서 실행하거나 비밀키를 출력하지 않는다.

## 2. SMTP 발송 확인과 Secret 저장

아래 명령은 실제 메일 한 건을 보낸다. SMTP 접수 메시지와 실제 수신함 도착을 모두 확인한다. Ruby의 `net/smtp`가 설치되어 있어야 하며 Ubuntu에서 없다면 `sudo apt install ruby-net-smtp`로 준비한다.

```bash
ruby deploy/k3s/scripts/alert-delivery.rb send \
  /etc/happygallery/app.env /etc/happygallery/alertmanager.env \
  'happyGallery 장애 알림 테스트' '운영 서버의 이메일 발송 경로 확인입니다.'
```

수신 확인 후 Kubernetes에 알림 설정만 저장한다. SMS·알림톡 등 다른 자격 증명이 아직 준비되지 않았어도 이 단계는 실행할 수 있다.

```bash
export KUBECONFIG="$HOME/.kube/happygallery.yaml"
./deploy/k3s/scripts/create-alertmanager-secret.sh \
  /etc/happygallery/app.env /etc/happygallery/alertmanager.env
```

수신 주소와 라우팅 설정은 Secret의 `alertmanager.yml`, API key는 `smtp-password`에 저장된다. 임시 파일은 600 권한으로 만들고 실행 종료 시 제거한다. 원본 설정이나 SMTP 계정을 바꾸면 Secret을 다시 생성하고 실행 중인 Alertmanager를 재시작한다.

```bash
sudo k3s kubectl -n happygallery rollout restart deployment/alertmanager
sudo k3s kubectl -n happygallery rollout status deployment/alertmanager --timeout=3m
```

아직 Alertmanager를 배포하지 않았다면 위 재시작은 건너뛰고 다음 단계에서 처음 실행한다.

## 3. Alertmanager 실제 경보 확인

메일 직접 발송 검사만으로 Alertmanager 연동이 검증된 것은 아니다. 전체 앱 배포 전 Alertmanager만 먼저 실행할 수 있다. 이미 실행 중이면 이 적용 단계는 생략한다.

```bash
sudo k3s kubectl apply -f deploy/k3s/base/storage-class.yaml
sudo k3s kubectl -n happygallery apply -f deploy/k3s/base/alertmanager.yaml
sudo k3s kubectl -n happygallery rollout status deployment/alertmanager --timeout=3m
sudo k3s kubectl -n happygallery port-forward --address 127.0.0.1 service/alertmanager 19093:9093
```

같은 서버에 두 번째 SSH 터미널을 열고 테스트 경보를 등록한다. 외부 포트는 열지 않는다. 매번 고유한 `test_id`를 사용해 직전 검사의 재알림 제한과 구분한다.

```bash
ruby -rjson -rtime -e '
  now = Time.now.utc
  puts JSON.generate([{
    labels: {alertname: "ResendDeliveryTest", severity: "warning", test_id: now.iso8601},
    annotations: {summary: "happyGallery 장애 알림 테스트"},
    startsAt: now.iso8601, endsAt: (now + 120).iso8601
  }])
' | curl --fail --silent --show-error \
  -H 'Content-Type: application/json' --data-binary @- \
  http://127.0.0.1:19093/api/v2/alerts
```

최초 경보는 `group_wait=30s` 이후 전달된다. 2분 뒤 경보가 만료되고 복구 메일은 다음 그룹 처리 주기까지 포함해 약 7분 이내에 확인한다. 둘 다 수신했으면 첫 터미널에서 `Ctrl+C`로 port-forward를 종료한다. 수신이 실패하면 Alertmanager Pod 상태와 로그, Resend 발송 이력을 확인한다. 설정 파일·Secret 원문을 로그나 채팅에 붙이지 않는다.

전체 서비스 자격 증명이 준비된 뒤에는 기존 `create-secrets.sh`의 네 번째 인자에도 `/etc/happygallery/alertmanager.env`를 사용한다. Alertmanager만 먼저 실행해도 MySQL은 생성되지 않는다.

## 4. 백업 실패 알림

백업을 설치할 때 `/etc/happygallery/backup-alert.env`를 아래 내용으로 저장한다. 기존 `BACKUP_ALERT_WEBHOOK_URL`은 삭제한다. 이메일과 웹훅을 동시에 지정하면 거부한다.

```dotenv
BACKUP_ALERT_EMAIL_CONFIG=/etc/happygallery/alertmanager.env
BACKUP_ALERT_APP_ENV=/etc/happygallery/app.env
```

기존 systemd 실패 알림 unit과 watchdog은 이 설정을 읽어 SMTP로 직접 보낸다. Kubernetes나 앱에 접근하지 않으므로 두 서비스의 장애 중에도 호스트와 인터넷이 동작하면 발송할 수 있다. 백업 설치 후 실제 실패 알림 테스트까지 수행한다.

## 검증과 근거

- `./deploy/k3s/scripts/validate.sh`: 전체 manifest, 기존 Secret 허용 목록, 이메일·웹훅 설정 생성과 보안 조건 검사. 실제 메일은 발송하지 않는다.
- Alertmanager `v0.32.1`의 `amtool check-config`: 생성한 SMTP·웹훅 설정을 검사한다.
- [Resend SMTP 설정](https://resend.com/docs/send-with-smtp)
- [Alertmanager 이메일 설정](https://prometheus.io/docs/alerting/latest/configuration/#email_config)
