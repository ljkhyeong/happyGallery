# ADR-0035: 다중 소셜 계정 모델

**날짜**: 2026-07-17
**상태**: Accepted

---

## 배경

기존 `users.provider`, `users.provider_id` 구조는 회원 한 명이 하나의 인증 제공자만 가질 수 있었다.
Naver 로그인을 추가하면 비밀번호 회원이 Google과 Naver를 함께 연결하거나, 기존 Google 회원이 Naver를 추가하는 경우를 표현할 수 없다.
또한 외부 `provider_id`는 제공자 내부에서만 고유하므로 식별자만 저장해서는 계정을 안전하게 구분할 수 없다.

## 결정

1. `users`는 서비스 회원 자체만 나타내고 인증 제공자 컬럼을 두지 않는다.
2. 소셜 계정은 `user_social_accounts`에 분리한다.
3. 외부 provider ID는 원문 대신 `provider_id_hmac`로 저장하고 `(provider, provider_id_hmac)`를 유일하게 유지해 외부 계정 하나가 여러 회원에 연결되지 않도록 한다.
4. `(user_id, provider)`를 유일하게 유지해 한 회원은 제공자별 계정을 하나씩만 연결한다.
5. 소셜 로그인 시 외부 계정이 이미 연결되어 있으면 해당 회원으로 로그인한다. 외부 계정 연결이 없고 이메일이 기존 회원과 겹치면 제공자와 관계없이 자동 병합하지 않고 `SOCIAL_ACCOUNT_LINK_REQUIRED`를 반환한다.
6. Spring Security OAuth2 Client가 만든 authorization request와 OAuth `state`는 callback 전까지만 서버 세션에 저장하고, callback에서 일치 여부를 확인한 뒤 제거한다.
7. 제공자별 authorize/token/profile 응답 차이는 OAuth2 Client와 web security 어댑터가 처리하고, 애플리케이션 서비스는 공통 `SocialLoginCommand(provider, providerId, email, name)`만 사용한다.
8. Google과 Naver 모두 `state`를 필수로 검증한다. 브라우저가 code, `state`, `redirectUri`를 별도 JSON API로 전달하는 호환 경로는 두지 않는다.
9. Google은 UserInfo의 `email_verified=true`인 프로필만 수용한다.
10. 들어오는 소셜 이메일의 소유 확인만으로 기존 서비스 계정 소유자까지 증명할 수는 없다. 따라서 이메일만으로 로컬·Google·Naver 계정을 자동 병합하지 않는다.
11. 이메일 충돌이 없는 Naver 신규 가입은 허용하되, Naver가 이메일 검증 상태를 제공하지 않는 한 이메일 선점 위험은 남는다. 자체 이메일 검증 또는 임시 계정 모델을 도입하기 전까지 이메일 충돌은 계정 공유 대신 명시적 오류로 종료한다.
12. 소셜 신규 회원은 전화번호 없이 생성한다. OAuth 제공자의 전화번호를 서비스 소유권 확인으로 간주하지 않고, 마이페이지에서 기존 SMS 인증 정책으로 직접 확인한 번호만 등록한다.
13. 최초 전화번호 등록은 회원 행 잠금, 인증 코드 소비, 전화번호 암호화·HMAC 저장, `phoneVerified=true` 전이를 한 트랜잭션에서 처리한다. 이미 번호가 있는 회원의 재인증과 비회원 이력 가져오기는 이 흐름에 섞지 않는다.
14. 주문·예약·8회권은 연락과 결과 통지가 필요한 거래이므로 전화번호가 없거나 인증되지 않은 회원의 결제 준비를 거절한다.

## 마이그레이션

- V45는 기존 `users.provider != LOCAL` 계정을 `user_social_accounts`로 이전하면서 롤링 배포 호환을 위해 `users.provider`, `users.provider_id`와 기존 복합 인덱스를 임시 유지했다.
- V46은 소셜 식별자를 `provider_id_hmac`로 전환하고 `users.provider`, `users.provider_id`와 기존 인덱스를 제거해 과도기를 종료했다.
- V46은 전환한 `(provider, provider_id_hmac)`가 충돌하면 자동 병합하지 않고 마이그레이션을 중단한다.
- `LOCAL`은 소셜 제공자가 아니므로 새 테이블에 저장하지 않는다. 비밀번호 존재 여부가 로컬 로그인 가능 여부를 나타낸다.

## 결과

- 한 회원이 비밀번호, Google, Naver 인증 수단을 함께 연결할 수 있는 저장 구조를 갖는다. 실제 추가 연결은 로그인된 회원의 명시적 연결 흐름을 도입한 뒤 허용한다.
- provider 이름은 중복 정보가 아니라 provider 범위 안에서만 고유한 외부 ID를 해석하기 위한 식별자다.
- 사용자 응답의 단일 `provider` 필드는 실제 로그인 가능 수단을 정확히 표현하지 못하므로 제거한다.
- 소셜 신규 회원은 로그인 직후에도 회원 조회·마이페이지 접근은 가능하지만, SMS로 전화번호를 등록하기 전에는 결제 기반 거래를 시작할 수 없다.

## 관련 문서

- `docs/PRD/0001_기준_스펙/spec.md`
- `docs/PRD/0004_API_계약/spec.md`
- `docs/ADR/0022_시스템_경계_상태_스키마_기준선/adr.md`
- `docs/ADR/0029_외부_HTTP_클라이언트_풀링_기준선/adr.md`
- `docs/ADR/0036_개인정보_평문_제거와_블라인드_인덱스_기준/adr.md`
- [Google OpenID Connect API Reference](https://developers.google.com/identity/openid-connect/reference)
- [Naver 로그인 회원 프로필 안내](https://help.naver.com/service/23029/contents/20553?lang=ko)
