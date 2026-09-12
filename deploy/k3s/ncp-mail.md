# 네이버 클라우드 인증 메일

회원 이메일 소유 확인에 한국 리전 Cloud Outbound Mailer를 선택할 수 있다. 기본값은 기존 SMTP이며, `EMAIL_VERIFICATION_PROVIDER=ncp`일 때 HTTP API로 인증 메일을 접수한다.

## 요금과 사용 조건

- 한국 리전은 월 1,000건까지 무료이며 초과분은 건당 0.45원(VAT 별도)이다. 기본 발송 한도는 월 100만 건으로 무료 한도보다 크다. [공식 요금표](https://m.ncloud.com/charge/price/ko), [서비스 발송 한도](https://www.ncloud.com/api-cms/service-product/static/cloudOutboundMailer)
- 현재 앱에는 월 1,000건 초과 발송을 차단하는 기능이 없다. 인증 요청 제한·큐·타임아웃은 월 과금을 막지 않는다. 같은 계정의 다른 메일 발송도 고려해야 하므로 이 앱의 요청 수만으로 무료 사용을 보장하지 않는다.
- 추가요금 없이 운영하려면 활성화 전에 제공자에게 무료 한도 초과 발송을 차단할 수 있는지 확인한다. 공식 안내는 한도 변경 신청 절차를 제공하며, 1,000건에서 자동 차단된다고 안내하지 않는다. 차단 조건이 확인되지 않으면 무과금 구성의 메일 서비스로 선택하지 않는다. [발송 한도 변경 안내](https://guide.ncloud-docs.com/docs/cloudoutboundmailer-troubleshoot-common)

## 서비스와 발신 도메인 준비

1. Ncloud 사업자 계정으로 한국 리전 Cloud Outbound Mailer 이용을 신청한다. 개인 계정은 이용할 수 없다. [계정 조건](https://guide.ncloud-docs.com/docs/cloudoutboundmailer-troubleshoot-common)
2. 발신 도메인 `mail.happy-gallery.com`을 등록한다. 콘솔 안내에 따라 소유 확인·SPF·DKIM DNS 레코드를 Cloudflare에 추가하고 SPF·DKIM 사용 상태와 DMARC 인증 완료를 확인한다. 기존 SPF 레코드를 중복 생성하지 않는다. 네이버는 SPF·DKIM·DMARC가 모두 완료되지 않으면 Gmail 발송을 실패 처리한다. [Gmail 발송 조건](https://guide.ncloud-docs.com/docs/sens-troubleshoot-mail)
3. 메일 발송 권한이 있는 Ncloud API Access Key와 Secret Key를 발급한다. 네이버 로그인에 사용하는 Client ID·Secret과 다른 키다. 키와 전체 환경 파일을 로그·채팅·Git에 남기지 않는다.
4. 발신 주소는 인증한 도메인의 `no-reply@mail.happy-gallery.com`을 사용한다.

공식 문서: [도메인 인증](https://guide.ncloud-docs.com/docs/cloudoutboundmailer-use-domain), [API 인증](https://api.ncloud-docs.com/docs/en/ai-application-service-cloudoutboundmailer), [메일 접수](https://api.ncloud-docs.com/docs/en/ai-application-service-cloudoutboundmailer-createmailrequest).

**서비스 전환 조건(2026-09-12 확인):** 2026-09-17에 SENS로 통합된다. 이 구현은 기존 `/api/v1/mails` 계약을 사용하므로 통합 전에 이용 신청한 서비스 또는 통합 시 자동 이전된 프로젝트가 필요하다. 이전된 프로젝트에서는 기존 API를 12개월간 사용할 수 있다. 통합 이후 새로 만든 프로젝트는 이 구현의 지원 대상이 아니며, 신규 SENS 메일 API 연동을 추가해야 한다. 자동 이전된 `mail-UUID` 프로젝트를 삭제하지 않고, 사용 기한 전에 통합 API로 전환한다. [공식 통합 안내](https://guide.ncloud-docs.com/docs/sens-integrationguide).

## 서버 설정

Ubuntu SSH에서 `vi /etc/happygallery/app.env`로 다음 값을 추가하거나 기존 줄을 수정한다. 파일 전체를 예제로 덮어쓰지 않는다.

```dotenv
EMAIL_VERIFICATION_PROVIDER=ncp
NCP_MAIL_ACCESS_KEY=발급받은_Access_Key
NCP_MAIL_SECRET_KEY=발급받은_Secret_Key
EMAIL_VERIFICATION_FROM=no-reply@mail.happy-gallery.com
```

`EMAIL_VERIFICATION_SMTP_HOST`, `EMAIL_VERIFICATION_SMTP_USERNAME`, `EMAIL_VERIFICATION_SMTP_PASSWORD`는 비워도 된다. SMTP로 돌아갈 때는 provider를 `smtp`로 바꾸고 SMTP 계정과 TLS 설정을 채운다. provider가 없는 기존 파일은 SMTP로 처리한다. 다른 결제·OAuth·알림 설정의 필수 조건은 유지한다.

네이버 API의 풀 획득·연결·응답 제한은 각각 500ms·1초·2초이며, `NCP_MAIL_ACQUIRE_TIMEOUT_MILLIS`, `NCP_MAIL_CONNECT_TIMEOUT_MILLIS`, `NCP_MAIL_TIMEOUT_MILLIS`로 조정한다. 합계보다 `EMAIL_VERIFICATION_TIMEOUT_MILLIS`(기본 7초)를 크게 유지한다. 네이버 API 키·발신 주소는 실행 시 주입하며 이미지에 포함하지 않는다.

## 이미지 재빌드

서버에서 새 코드 커밋을 반영하고 `git status --short`가 빈 출력인지 확인한다. 기존 Netty 보안 수정도 포함되어 있어야 한다. SSH 연결이 끊기지 않는 환경에서 실행한다.

```bash
cd /opt/happygallery
export KUBECONFIG="$HOME/.kube/happygallery.yaml"
set -a
. /etc/happygallery/build.env
set +a
mkdir -p "$HOME/.local/state/happygallery"
HG_BUILD_LOG="$HOME/.local/state/happygallery/build-$(date +%Y%m%d-%H%M%S).log"
set -o pipefail
./deploy/k3s/scripts/build-import-images.sh 2>&1 | tee "$HG_BUILD_LOG"
```

`build.env`는 앞서 운영자가 작성한 신뢰할 수 있는 shell 환경 파일이며 Toss 프런트 키를 담는다. `app.env`를 shell로 불러오지 않는다. 빌드 스크립트는 전체 Gradle 빌드, 앱·프런트 이미지 생성, Trivy HIGH/CRITICAL·EOL 검사, CPU 아키텍처 확인, k3s containerd import를 순서대로 실행한다.

성공 출력의 `IMAGE_TAG`, `APP_IMAGE`, `FRONTEND_IMAGE`, `APP_IMAGE_DIGEST`, `FRONTEND_IMAGE_DIGEST` 다섯 값을 `/etc/happygallery/release.env`에 반영한다. 이미지 import는 배포 완료가 아니다. 외부 서비스 설정을 모두 준비한 뒤 [Secret 생성과 rollout](README.md)을 진행한다.

## 실제 발송 확인

배포 후 본인 계정의 이메일 인증을 한 번 요청해 네이버 콘솔의 최종 발송 결과, 본인 수신함, 인증 코드 등록 성공을 확인한다. Gmail 수신도 확인해 도메인 인증 누락을 점검한다. 자동 테스트는 실제 메일을 발송하지 않는다.

- 성공 응답은 요청 ID가 있고 접수 건수가 1일 때만 인정한다. 수신함 도착이나 스팸 분류까지 보장하지 않는다.
- HTTP 자동 재시도·리다이렉트와 SMTP 자동 fallback은 사용하지 않는다. 응답 유실 뒤 중복 발송을 막기 위해서다.
- 기존 이메일 전용 제한 큐·TimeLimiter·CircuitBreaker를 공유한다. 응답 타임아웃·5xx·불완전 응답은 결과 미확인, 429는 일시 거절, 그 외 일반 4xx는 영구 거절로 처리한다. 인증 코드·수신 주소·API 키·제공자 응답 본문은 로그로 출력하지 않는다.
