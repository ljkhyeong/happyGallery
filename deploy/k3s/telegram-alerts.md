# 무료 Telegram 운영 알림

서버 경보·복구와 백업 실패를 Telegram 개인 채팅이나 비공개 운영 그룹으로 받는다. [Telegram Bot Platform](https://core.telegram.org/bots)은 사용자와 개발자에게 무료다. 유료 대량 발송은 사용하지 않으며, 백업 발송 코드도 `allow_paid_broadcast=false`를 명시한다. 공식 제공 범위는 2026-09-12 확인했다.

## 구현 범위

- 서버 경보는 [Alertmanager의 Telegram 설정](https://prometheus.io/docs/alerting/latest/configuration/#telegram_config)을 사용한다. 별도 중계 서버나 SMTP 계정이 필요 없다.
- 기존 경보 조건과 재알림 간격을 유지한다. critical은 1시간, 일반 warning은 4시간, 주문 승인·예약 취소 후속 작업 warning은 30분이며 복구 알림도 보낸다.
- 백업 실패·백업 성공 기록 정체는 같은 봇 설정으로 Telegram Bot API를 직접 호출한다. 앱·Kubernetes가 중단돼도 호스트와 인터넷이 동작하면 발송할 수 있다.
- 기존 이메일과 HTTPS 웹훅은 계속 사용할 수 있다. 한 설정 파일은 하나의 채널만 선택한다.

전원·홈서버 전체·인터넷 회선 장애는 서버 안에서 알릴 수 없다. [서버 밖 장애 감시](free-integrations.md#2-서버-밖에서-장애-감시)를 함께 사용한다. 서버 복구 알림과 달리 백업 경로는 실패·정체만 알리며 성공은 기존 heartbeat로 확인한다.

## 운영자가 준비할 값

[공식 BotFather 안내](https://core.telegram.org/bots/tutorial#obtain-your-bot-token)에 따라 봇을 생성한다. 알림을 받을 개인 채팅에서 봇을 시작하거나 비공개 그룹에 추가하고, 해당 채팅의 숫자 ID를 확인한다. 그룹 ID는 음수일 수 있다. 운영 메시지를 받을 권한만 부여한다.

```dotenv
ALERT_PROVIDER=telegram
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
```

[예시 파일](examples/alertmanager-telegram.env.example)을 `/etc/happygallery/alertmanager-telegram.env`에 600 권한으로 저장하고 실제 값을 채운다. 봇 토큰은 채팅·저장소·명령행 인자에 넣지 않는다. 빈 값·잘못된 채팅 ID·다른 채널 설정이 섞인 파일은 Secret 생성 전에 거부한다. 로컬 `.env.telegram-alerts.local`에도 빈 키를 준비했으며 자동으로 읽거나 활성화하지 않는다.

기존 `create-secrets.sh`의 네 번째 인자에 이 파일을 지정한다. Alertmanager 설정만 따로 준비할 수도 있다.

```bash
./deploy/k3s/scripts/create-alertmanager-secret.sh \
  /etc/happygallery/app.env \
  /etc/happygallery/alertmanager-telegram.env
```

Telegram은 `app.env`의 SMTP·NCP 메일 자격 증명을 사용하지 않는다. 봇 토큰은 `happygallery-alertmanager` Secret의 `telegram-bot-token` 파일로 저장하고 설정에서는 파일 경로만 참조한다. 기존 Secret 전체 디렉터리 mount를 사용하므로 새 볼륨이나 이미지가 필요 없다. 실행 중인 Alertmanager는 Secret 변경 후 재시작한다.

## 백업 알림 연결

`/etc/happygallery/backup-alert.env`에서 다음 값을 사용한다. 이전 `BACKUP_ALERT_WEBHOOK_URL`과 `BACKUP_ALERT_EMAIL_CONFIG`는 함께 지정하지 않는다.

```dotenv
BACKUP_ALERT_CONFIG=/etc/happygallery/alertmanager-telegram.env
```

기존 systemd 실패 알림 unit과 watchdog이 이 파일을 읽는다. 호스트에는 기존 운영 스크립트와 Ruby의 `net/http`·OpenSSL·CA 인증서가 필요하다. 새 Ruby gem은 사용하지 않는다. 기존 이메일용 `BACKUP_ALERT_EMAIL_CONFIG`도 호환되므로 이메일 설정을 옮길 필요는 없다.

백업 API 호출은 HTTPS 인증서를 검증하고 연결 3초·읽기/쓰기 10초·전체 20초로 제한한다. HTTP 200과 JSON의 `ok=true`를 모두 확인하며, 실패·시간 초과 시 성공으로 표시하거나 즉시 재전송하지 않는다. [무료 요청 제한](https://core.telegram.org/bots/faq#my-bot-is-hitting-limits-how-do-i-avoid-this)을 넘으면 유료 발송으로 전환하지 않고 실패한다. 백업 watchdog의 다음 점검이나 운영자의 재확인으로 이어간다.

## 검증과 실제 수신 확인

자동 검사는 설정 생성, 세 채널 호환, 토큰 파일 분리, 잘못된 설정 거부, 무료 발송 옵션, HTTP 오류·잘못된 응답·시간 초과, 백업 설정 전달을 확인한다. 운영 버전 `prom/alertmanager:v0.32.1`의 `amtool check-config`로 생성한 설정도 검사한다. 실제 봇 생성·Secret 적용·메시지 발송은 수행하지 않았다.

운영자가 키를 설정한 뒤 다음을 확인한다.

1. Alertmanager에 임시 경보를 등록해 발생·복구 메시지가 같은 운영 채팅에 도착하는지 확인한다. 방법은 [기존 테스트 경보 절차](resend-alerts.md#3-alertmanager-실제-경보-확인)를 사용하며, 수신 채널을 Telegram으로 확인한다.
2. 백업 실패 알림 unit을 시험해 해당 unit과 호스트 이름이 표시되는지 확인한다. 실제 백업 성공으로 heartbeat도 확인한다.
3. 메시지 수신이 끝난 뒤 자동 백업과 watchdog을 활성화한다. API 접수 성공만으로 휴대폰 알림 수신까지 보장하지는 않는다.

Telegram으로 보내는 서버 경보는 경보명·심각도·요약, 백업 경보는 unit·호스트 이름이다. 고객 연락처·주문 본문·원본 로그는 추가하지 않는다. 토큰과 제공자 오류 본문은 스크립트 출력에 남기지 않는다.
