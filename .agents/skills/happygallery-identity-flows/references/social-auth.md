# 소셜 인증 규칙

- Google·Naver는 Spring Security OAuth2 Client를 사용한다. callback URI는 서버가 정하고 OAuth state가 포함된 authorization request는 검증 전까지 Redis HTTP session에 둔다.
- token·UserInfo 호출은 application 트랜잭션 전에 끝내고 `SocialLoginCommand(provider, providerId, email, name)`만 전달한다.
- Google은 OIDC의 검증된 email만 수락한다. Naver는 `client_secret_post`, 저장된 authorization request의 state, 성공한 중첩 `response` profile을 사용한다.
- 성공 시 기존 세션을 한 번 회전하고 장기 인증 상태는 `customerUserId`, `customerCredentialVersion`, Spring Session principal index만 보관한다. OAuth token·SecurityContext는 저장하지 않는다.
- 시작·callback 경로는 `CustomerSecurityRoutes`를 사용하고 응답을 캐시하지 않는다. provider별 HTTP pool·timeout을 유지한다.
- 신규 소셜 가입 동의는 CSRF로 보호한 `POST /api/v1/auth/social/signup-intents/{provider}`에서 생성한다. authorization GET에는 짧은 만료의 opaque attempt ID만 전달하고 provider·OAuth state를 한 번 연결해 callback에서 소비한다. GET query의 동의값은 신뢰하지 않는다.
- 기존 로그인·계정 연결은 가입 동의를 새로 만들지 않는다. 같은 email의 동시 최초 로그인은 정확한 DB unique 제약 위반을 계정 연결 필요 결과로 변환한다.
- 가입·계정 연결·재인증의 임시 상태는 하나의 값으로 관리하고 Redis Session에는 JSON 문자열 하나로 저장한다. state 연결 시 전체를 교체하고 소비 시 전체를 제거해 rollback 이미지도 세션을 읽을 수 있게 한다.
