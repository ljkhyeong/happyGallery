# 무료 자동 입력 방지 연동

휴대폰 인증문자 남발과 단체 문의 스팸을 줄이기 위해 Cloudflare Turnstile을 연결했다. [Free 플랜](https://developers.cloudflare.com/turnstile/plans/)은 2026-09-12 확인 기준 상업용으로 사용할 수 있으며, 확인 요청 수는 무제한이다. 계정당 위젯 20개, 위젯당 도메인 10개를 제공한다. 이 앱은 위젯 1개를 사용한다. 기존 문자 발송 요금은 별도다.

## 적용 범위

| 요청 | 확인 action |
| --- | --- |
| `POST /api/v1/bookings/phone-verifications` | `phone_verification` |
| `POST /api/v1/group-inquiries` | `group_inquiry` |
| `POST /api/v1/me/group-inquiries` | `group_inquiry` |

회원가입·예약·전화번호 변경 등은 공통 휴대폰 인증 화면을 사용한다. 관리자 문의 수동 등록에는 적용하지 않는다. 기존 IP·전화번호별 요청 제한과 CSRF 검증은 유지한다.

기본값은 비활성이며, 키가 없으면 외부 스크립트를 불러오거나 검증 API를 호출하지 않는다. 활성화하면 서버가 문자 발송·문의 저장 **전에** `X-Bot-Token`을 검증한다. 브라우저가 보낸 성공 여부는 사용하지 않는다.

## 활성화 설정

운영자가 Cloudflare의 Turnstile에서 **Managed** 위젯을 만들고 허용 도메인에 `happy-gallery.com`을 등록한다. DNS 프록시나 별도 수신 웹훅은 필요 없다. [환경변수 예시](examples/app.env.example)에 다음 네 값을 준비했다.

```dotenv
TURNSTILE_ENABLED=false
TURNSTILE_SITE_KEY=
TURNSTILE_SECRET_KEY=
TURNSTILE_HOSTNAME=happy-gallery.com
```

실제 site key와 secret key를 넣은 뒤 `TURNSTILE_ENABLED=true`로 설정한다. 공식 테스트 키는 운영에 사용하지 않는다. 활성화했는데 키·도메인이 비어 있으면 앱 시작을 거부한다. `TURNSTILE_HOSTNAME`에는 프로토콜이나 경로 없이 실제 접속 도메인을 넣는다. 다른 호스트의 토큰은 거절하므로 별도 도메인으로 시험할 때는 해당 환경의 설정도 맞춘다.

이 값들은 기존 [Secret 생성 스크립트](scripts/create-secrets.sh)가 받는 `app.env`에 넣는다. Secret 변경 후 app을 재시작하면 된다. 프론트엔드는 `GET /api/v1/bot-protection`으로 공개 키만 조회하므로 키 변경 때문에 이미지를 다시 빌드할 필요가 없다. 비밀 키는 브라우저에 반환하지 않는다.

로컬 임시 파일 `.env.free-integrations.local`에는 비활성 플래그와 빈 키를 준비했다. 이 파일은 Git에서 제외하며 앱 실행 때 자동으로 읽히지는 않는다. 실행 환경에 필요한 값만 주입한다. 실제 계정 생성·키 발급·클러스터 적용은 수행하지 않았다.

## 검증과 장애 처리

- 서버는 [Siteverify API](https://developers.cloudflare.com/turnstile/get-started/server-side-validation/)의 성공 여부, `hostname`, `action`을 모두 확인한다. 비밀 키와 토큰만 전송하며 전화번호·문의 본문·별도 `remoteip`는 보내지 않는다.
- 토큰은 최대 2,048자, 유효시간 5분, 한 번만 사용 가능하다. 누락·실패·만료·재사용은 `400 INVALID_INPUT`, 통신 장애·잘못된 비밀 키·비정상 응답은 `503 SERVICE_UNAVAILABLE`이다. 이때 문자 발송과 문의 저장은 진행하지 않는다.
- 자동 재시도 없이 공통 HTTP 연결 풀을 사용한다. 기본 연결 대기 0.5초, 연결 1초, 응답 5초로 제한한다.
- 발송·접수 후 성공 여부와 관계없이 새 토큰을 받는다. 입력 내용은 실패해도 유지한다. 위젯 오류나 스크립트 로딩 실패는 화면에서 다시 시도할 수 있다.
- CSP는 Cloudflare의 스크립트·프레임 출처와 기존 nonce를 허용한다. `X-Bot-Token`은 프론트엔드 오류 수집과 서버 로그에서 가린다.

브라우저 위젯은 접속 IP, TLS 연결 특성, 브라우저 정보와 사이트 정보를 Cloudflare에 전달한다. 이를 `2026-09-12-v1` 개인정보처리방침에 반영했고 이전 버전은 보존했다. Cloudflare가 서비스 보호와 제품 개선을 위해 정보를 처리하는 범위는 [공식 개인정보 안내](https://www.cloudflare.com/turnstile-privacy-policy/)에 있다.

검증 범위는 모의 Siteverify 응답, 요청 거절 시 업무 코드 미실행, 브라우저 토큰 만료·재발급·장애 복구다. 실제 키를 사용한 운영 연동은 다음 순서로 확인한다.

1. `happy-gallery.com`에서 위젯이 표시되고 확인 후 요청이 접수되는지 확인한다.
2. 토큰이 없거나 재사용된 요청은 400이며 문자 발송·문의 추가가 없는지 확인한다.
3. 기존 문자 중지 모드를 사용하는 동안은 실제 문자가 발송되지 않는다. 문자 제공자 활성화 후 발송 결과는 별도로 확인한다.

중단하려면 `TURNSTILE_ENABLED=false`로 바꾸고 app을 재시작한다. 이후 기존 요청 제한만 적용된다.
