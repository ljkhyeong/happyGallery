# ADR-0035: 다중 소셜 계정 모델

**날짜**: 2026-07-17
**최종 갱신**: 2026-08-27
**상태**: Accepted

---

## 배경

기존 `users.provider`, `users.provider_id` 구조는 회원 한 명이 하나의 인증 제공자만 가질 수 있었다.
Naver와 Kakao 로그인을 추가하면 비밀번호 회원이 Google, Naver, Kakao를 함께 연결하거나, 기존 소셜 회원이 다른 제공자를 추가하는 경우를 표현할 수 없다.
또한 외부 `provider_id`는 제공자 내부에서만 고유하므로 식별자만 저장해서는 계정을 안전하게 구분할 수 없다.

## 결정

1. `users`는 서비스 회원 자체만 나타내고 인증 제공자 컬럼을 두지 않는다.
2. 소셜 계정은 `user_social_accounts`에 분리한다.
3. 외부 provider ID는 원문 대신 `provider_id_hmac`로 저장하고 `(provider, provider_id_hmac)`를 유일하게 유지해 외부 계정 하나가 여러 회원에 연결되지 않도록 한다.
4. `(user_id, provider)`를 유일하게 유지해 한 회원은 제공자별 계정을 하나씩만 연결한다.
5. 소셜 로그인 시 외부 계정이 이미 연결되어 있으면 해당 회원으로 로그인한다. 처음 보는 Google 또는 Kakao 계정의 검증 이메일이 기존 기준 이메일과 겹치면 자동 병합하지 않고 `SOCIAL_ACCOUNT_LINK_REQUIRED`를 반환한다. Naver 프로필 이메일은 충돌 조회에 사용하지 않는다.
6. Spring Security OAuth2 Client가 만든 authorization request와 OAuth `state`는 callback 전까지만 서버 세션에 저장하고, callback에서 일치 여부를 확인한 뒤 제거한다.
7. 제공자별 authorize/token/profile 응답 차이는 OAuth2 Client와 web security 어댑터가 처리하고, 애플리케이션 서비스는 공통 `SocialLoginCommand(provider, providerId, verifiedEmail, name)`만 사용한다. `verifiedEmail`은 Google과 Kakao만 전달하고 Naver는 `null`이다.
8. Google, Naver, Kakao 모두 `state`를 필수로 검증한다. 브라우저가 code, `state`, `redirectUri`를 별도 JSON API로 전달하는 호환 경로는 두지 않는다.
9. Google은 UserInfo의 `email_verified=true`인 프로필만 수용한다.
10. 제공자가 검증한 이메일도 기존 서비스 계정 소유자까지 증명하지는 않으므로 이메일만으로 계정을 자동 병합하지 않는다.
11. Naver 프로필 이메일은 검증된 기준 이메일로 간주하지 않고 저장하지 않는다. 신규 Naver 회원의 `users.email_enc/email_hmac`은 자체 이메일 검증 기능으로 기준 이메일을 등록하기 전까지 `NULL`이다.
12. Kakao는 REST OAuth2 UserInfo의 숫자 `id`를 문자열 provider ID로 정규화한다. `is_email_valid=true`, `is_email_verified=true`인 이메일과 `profile.nickname`을 모두 요구하며, 조건을 만족하지 않으면 일반 로그인과 신규 가입을 거절한다.
13. 소셜 신규 회원은 전화번호 없이 생성한다. OAuth 제공자의 전화번호를 서비스 소유권 확인으로 간주하지 않고, 마이페이지에서 기존 SMS 인증 정책으로 직접 확인한 번호만 등록한다.
14. 전화번호 최초 등록과 변경은 현재 자격 버전에 결합된 최근 본인 확인, 회원 행 잠금, 용도별 새 번호 인증 코드 소비, 전화번호 암호화·HMAC 저장, `phoneVerified=true` 전이를 한 트랜잭션에서 처리한다. `phone_hmac`은 회원 전체에서 유일하며 비회원 이력 가져오기는 이 흐름에 섞지 않는다.
15. 주문·예약·8회권은 연락과 결과 통지가 필요한 거래이므로 전화번호가 없거나 인증되지 않은 회원의 결제 준비를 거절한다.
16. 신규 가입 정책 동의는 CSRF 보호 POST로 5분짜리 가입 의도와 시도 ID를 만든 뒤, 해당 시도로 생성한 OAuth authorization request의 provider·`state`에 한 번 결합한다. 기존 회원 로그인과 공개 authorization GET query는 신규 가입 동의를 만들지 못한다.
17. 기존 회원의 추가 연결은 최근 10분 안에 현재 비밀번호 또는 이미 연결된 동일 소셜 계정으로 본인을 다시 확인한 회원이 CSRF 보호 요청으로 5분짜리 연결 의도와 일회성 시도 ID를 만든 뒤, 해당 시도로 생성한 OAuth authorization request의 `state`를 연결 의도에 한 번 결합한다. 같은 provider의 일반 로그인이나 다른 시도에서 생성한 `state`는 연결에 사용할 수 없다.
18. callback에서는 결합된 `state`, provider, 연결 의도의 자격 버전과 현재 HTTP 세션·회원 행의 회원 ID와 자격 버전을 다시 확인한다. 연결에는 provider ID만 필요하다. 일반 로그인·신규 가입은 Google의 검증 이메일과 이름, Naver의 이름, Kakao의 유효·검증 이메일과 닉네임을 요구한다. 외부 계정이 다른 회원에 연결되어 있거나 같은 provider의 다른 계정이 이미 연결되어 있으면 자동 교체하지 않고 충돌로 종료한다.
19. 회원은 같은 최근 본인 확인을 거친 뒤 연결된 provider를 직접 해제할 수 있지만, 로컬 비밀번호나 다른 소셜 계정 중 하나는 반드시 남겨 마지막 로그인 수단을 잃지 않게 한다. 새 연결 또는 실제 해제로 로그인 수단 집합이 바뀌면 자격 버전을 증가시키고 기존 회원 세션을 모두 폐기해 변경 전 로그인 수단으로 이미 만들어진 세션도 남기지 않는다. 이미 같은 외부 계정이 연결된 멱등 요청은 버전과 세션을 유지한다.
20. `CustomerUserResponse.email`은 검증된 기준 이메일이 없을 때 `null`이다. 신규 Naver 전용 회원은 최근 본인 확인과 별도 메일함 인증을 거쳐 기준 이메일을 한 번 등록할 수 있다. 검증된 휴대폰도 있으면 기존 SMS 비밀번호 재설정으로 최초 로컬 비밀번호를 설정한다.
21. 동시에 들어온 최초 로그인 callback은 사전 조회만으로 직렬화하지 않는다. `IDENTITY` ID를 받기 위한
    신규 회원 insert와 소셜 계정의 `saveAndFlush`가 각각
    `users(email_hmac)`, `(provider, provider_id_hmac)`, `(user_id, provider)` 제약을 확인한다.
    회원 저장 경계의 이메일 유일 제약은 `EMAIL_ALREADY_EXISTS`로 번역하고, 소셜 로그인은 이를
    `SOCIAL_ACCOUNT_LINK_REQUIRED`로 재해석한다. 외부 계정이 먼저 연결되면
    `SOCIAL_ACCOUNT_ALREADY_LINKED` 또는 `SOCIAL_PROVIDER_ALREADY_LINKED` 409로 종료한다.
    사용자가 소셜 로그인을 새로 시작하면 이미 연결된 회원을 조회해 로그인한다.
22. 소셜 재인증 의도도 회원 ID·자격 버전·provider·만료와 OAuth `state`에 결합한다. callback에서 provider가 반환한 ID가 해당 회원에게 이미 연결된 동일 provider ID인지 확인한 뒤에만 현재 세션에 10분짜리 최근 본인 확인 증명을 기록한다.

## 마이그레이션

- V45는 기존 `users.provider != LOCAL` 계정을 `user_social_accounts`로 이전하면서 롤링 배포 호환을 위해 `users.provider`, `users.provider_id`와 기존 복합 인덱스를 임시 유지했다.
- V46은 소셜 식별자를 `provider_id_hmac`로 전환하고 `users.provider`, `users.provider_id`와 기존 인덱스를 제거해 과도기를 종료했다.
- V46은 전환한 `(provider, provider_id_hmac)`가 충돌하면 자동 병합하지 않고 마이그레이션을 중단한다.
- V82는 검증된 기준 이메일이 없는 회원을 표현하도록 `users.email_enc/email_hmac`을 nullable로 변경한다. 이메일 HMAC 유일 제약은 null이 아닌 값에 계속 적용된다.
- V147은 `user_social_accounts.provider` CHECK 제약에 `KAKAO`를 추가한다.
- `LOCAL`은 소셜 제공자가 아니므로 새 테이블에 저장하지 않는다. 비밀번호 존재 여부가 로컬 로그인 가능 여부를 나타낸다.

## 결과

- 한 회원이 마이페이지에서 비밀번호, Google, Naver, Kakao 인증 수단을 명시적으로 연결·해제할 수 있다.
- Naver 프로필 이메일을 저장하거나 이메일 충돌 키로 사용하지 않아 미검증 이메일 선점을 막는다. 기준 이메일이 없는 회원은 provider 프로필과 분리된 자체 이메일 소유 확인을 거쳐 직접 등록할 수 있다.
- provider 이름은 중복 정보가 아니라 provider 범위 안에서만 고유한 외부 ID를 해석하기 위한 식별자다.
- 사용자 응답의 단일 `provider` 필드는 실제 로그인 가능 수단을 정확히 표현하지 못하므로 제거한다.
- 소셜 신규 회원은 로그인 직후에도 회원 조회·마이페이지 접근은 가능하지만, SMS로 전화번호를 등록하기 전에는 결제 기반 거래를 시작할 수 없다.

## 관련 문서

- `docs/PRD/0001_기준_스펙/spec.md`
- `docs/PRD/0004_API_계약/spec.md`
- `docs/ADR/0022_시스템_경계_상태_스키마_기준선/adr.md`
- `docs/ADR/0029_외부_HTTP_클라이언트_풀링_기준선/adr.md`
- `docs/ADR/0036_개인정보_평문_제거와_블라인드_인덱스_기준/adr.md`
- `docs/ADR/0041_회원_이메일_소유_확인/adr.md`
- [Google OpenID Connect API Reference](https://developers.google.com/identity/openid-connect/reference)
- [Naver 로그인 회원 프로필 안내](https://help.naver.com/service/23029/contents/20553?lang=ko)
- [Kakao Login REST API](https://developers.kakao.com/docs/ko/kakaologin/rest-api)
