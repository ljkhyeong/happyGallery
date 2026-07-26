# ADR-0036: 개인정보 평문 제거와 블라인드 인덱스 기준

**날짜**: 2026-07-17
**최종 갱신**: 2026-07-26
**상태**: Accepted

---

## 컨텍스트

회원 이메일·이름·전화번호, 비회원 이름, 휴대폰 인증 정보와 결제 준비 payload 일부가 DB에 평문으로 남아 있었다.
관리자 세션 토큰과 처리율 제한의 클라이언트 식별자도 Redis 키에 원문으로 노출될 수 있었고, 알림·결제 외부 호출 로그에는 수신자 정보나 외부 예외 원문이 포함될 수 있었다.

동시에 로그인, 비회원 이력 연결, 소셜 로그인과 관리자 검색은 개인정보의 정확 일치 조회가 필요하다. 복호화 가능한 AES 암호문만 저장하면 인덱스 조회가 불가능하고, HMAC만 저장하면 화면 표시와 외부 발송에 필요한 값을 복원할 수 없다.

## 결정

### 1. 입력을 먼저 표준 형식으로 통일한다

- 휴대폰 번호는 ASCII와 Unicode 공백, 하이픈을 제거한 뒤 `^01[0-9]{8,9}$`를 만족하는 숫자 문자열로 저장·검색·발송한다.
- 이메일은 앞뒤 공백을 제거하고 소문자로 통일한다.
- 이름은 앞뒤 공백을 제거한다.
- 이름은 기존 스키마와 같은 최대 100자로 제한하고, 4바이트 문자도 암호문이 잘리지 않도록 `name_enc`를 `VARCHAR(1024)`로 둔다.
- 암호화와 HMAC 생성은 항상 이 표준값을 사용한다.

### 2. 복원이 필요한 값은 AES-GCM, 정확 일치 조회는 HMAC을 사용한다

- `users`: 검증된 기준 이메일이 있을 때만 채우는 nullable `email_enc/email_hmac`, `name_enc/name_hmac`, 소셜 회원은 nullable인 `phone_enc/phone_hmac`
- `guests`: `name_enc/name_hmac`, `phone_enc/phone_hmac`
- `phone_verifications`: `phone_hmac`, `code_hmac`, `code_enc`
- `payment_attempt`: 내부 결제 payload 전체를 `payload_enc`에 저장하고, confirm 재응답에 필요한 비회원 원문 접근 토큰은 `fulfilled_access_token_enc`에 저장한다. 비회원 공개 입력의 인증 코드는 prepare에서 소비한 뒤 저장하지 않고, 결제 context·orderId·정규화 전화번호에 귀속된 HMAC 증거만 암호화 payload에 넣는다. 비회원 결제 상태 복구를 위해 정규화 휴대폰의 HMAC과 비식별 키 버전을 `owner_phone_hmac/owner_phone_hmac_key_id`에 추가 저장한다.
- `bookings`: `BOOKED` 예약 소유자의 현재 정규화 휴대폰 HMAC을 `owner_phone_hmac`에 저장한다.
  `active_owner_phone_hmac`과 `(slot_id, active_owner_phone_hmac)` 유일 인덱스로 회원·비회원 테이블을
  가로지르는 동일 전화번호 중복 예약을 차단한다. 회원 전화번호 변경은 활성 예약 HMAC도 같은 트랜잭션에서
  갱신하며 충돌 시 전체 변경을 거절한다. 취소·완료·노쇼 예약은 이 값을 즉시 제거한다.
- `user_social_accounts`: `provider_id_enc/provider_id_hmac` 저장. V63 이전 행의 `provider_id_enc`는 다음 소셜 로그인 전까지 nullable
- `fulfillments`: 주문 시점 구조화 배송지 JSON을 `shipping_address_enc`에 저장
- `admin_user`: TOTP 비밀키를 `totp_secret_enc`에 저장. MFA 복구 코드는 복호화할 필요가 없으므로 `admin_mfa_recovery_code.code_hash`에 무작위 salt가 있는 비밀번호 해시만 저장

회원과 비회원의 평문 이메일·이름·전화번호 컬럼, 휴대폰 인증 평문 전화번호·코드, 소셜 provider ID 평문 컬럼과 결제 payload JSON 평문 컬럼은 제거한다. Google의 검증 이메일은 회원 기준 이메일로 보호 저장하지만 Naver 프로필 이메일은 저장하지 않는다. 비밀번호는 단방향 해시를 유지한다. 비회원 접근 토큰은 주문·예약에 서명 토큰 전체 해시만 저장하고, 동일 confirm 재응답에 필요한 원문만 `payment_attempt`에 AES-GCM 암호문으로 저장한다.
배송지는 검색 인덱스를 만들지 않으며 목록·검색 응답에는 포함하지 않는다. `V52`가
`shipping_address_enc`를 추가하고, 소유권이 확인된 고객 주문 상세와 관리자 단건 이행 조회에서만
복호화한다. `V63`은 향후 소셜 HMAC 키를 완전히 교체할 수 있도록 `provider_id_enc`를 추가한다.

### 3. 검색 기능은 암호화 방식에 맞춰 제한한다

- 기준 이메일이 있는 회원의 이메일, 회원·비회원 이름, 전화번호와 소셜 식별자는 HMAC 정확 일치로 조회한다.
- 관리자 주문·예약 검색은 표시 주문·예약 번호 ID 정확 일치와 이름·표준화 휴대폰 HMAC 정확 일치를 제공한다. 암호화된 개인정보의 부분 일치는 제공하지 않는다.
- 목록과 상세 응답은 조회된 암호문을 애플리케이션 계층에서 복호화한 뒤 조립한다.

### 4. Redis와 로그에도 같은 원칙을 적용한다

- 처리율 제한 버킷은 IP 또는 인증 토큰 원문이 아니라 HMAC을 키 식별자로 사용한다.
- 관리자 세션은 토큰 HMAC을 Redis 키로, 세션 JSON의 AES-GCM 암호문을 값으로 저장한다.
- 관리자 MFA 로그인 challenge는 원문 대신 HMAC만 저장한다. 관리자 인증 감사 이력의 사용자명·유효하지 않은 challenge도 원문 대신 HMAC과 당시 키 ID만 저장한다.
- 관리자 username은 개인 이메일·실명이 아닌 영문·숫자 기반 운영 식별자로 제한하고 로그에는 남기지 않는다.
- 공개 클라이언트 모니터링 요청의 `path/source/target`은 임의 문자열이므로 로그에 남기지 않고, 서버가 정한 이벤트 종류와 내부 ID만 기록한다.
- 로그에는 전화번호, 이름, 인증 코드, 결제 키와 외부 예외 원문을 남기지 않는다. 필요한 경우 내부 ID, 상태 코드와 예외 타입만 기록한다.
- 영속 실패 사유에는 외부 응답 원문 대신 통제된 오류 문구를 저장한다.

### 5. V46에서 기존 데이터를 전환한다

Java Flyway `V46__ProtectPlaintextPersonalData`가 운영 AES/HMAC 키를 사용해 다음 순서로 실행한다.

1. 기존 회원·비회원·결제 준비·소셜 식별 데이터를 읽어 표준화하고 암호문과 HMAC을 계산한다.
2. 정규화 후 회원 이메일, 비회원 전화번호와 provider별 소셜 식별자 중복을 검사한다.
3. 충돌이 있으면 자동 병합하지 않고 마이그레이션을 중단한다.
4. 보호 컬럼을 백필한 뒤 평문 컬럼을 제거하고 최종 유일 제약과 인덱스를 생성한다.
5. 유효기간이 짧은 기존 휴대폰 인증 행은 이관하지 않고 테이블을 새 보호 스키마로 다시 만든다.
6. 사용되지 않는 `fulfillments.address`, `pickup_store` 컬럼을 제거한다.

MySQL DDL은 트랜잭션 롤백을 보장하지 않으므로 배포 전 충돌 검사를 통과해야 한다. 운영 배포에는 애플리케이션에서 사용하는 것과 같은 `ENCRYPT_KEY`, `HMAC_KEY`가 반드시 제공되어야 한다.
V46은 서비스 최초 배포 전 단일 전환을 전제로 하며, 실패하면 개발 데이터베이스를 재생성해 처음부터 다시 적용한다. 운영 데이터가 생긴 뒤의 축소 마이그레이션에는 이 방식을 재사용하지 않는다.
V82는 검증된 기준 이메일이 없는 신규 Naver 회원을 표현하도록 `users.email_enc/email_hmac`을 nullable로 변경한다.

### 6. 필드 AES/HMAC 키는 유지보수 창에서 함께 회전한다

- 신규 AES-GCM 암호문은 `hg:<keyId>:<base64>` 형식으로 저장한다. 접두사가 없는 기존 암호문은 active/previous AES 키링으로만 복호화하고, 새 쓰기는 active 키만 사용한다.
- 필드 설정은 하나의 `active-key-id`, active `ENCRYPT_KEY`/`HMAC_KEY`, 선택적인 `PREVIOUS_ENCRYPT_KEYS`/`PREVIOUS_HMAC_KEYS`로 구성한다. 이전 키 목록은 `keyId=64자리hex` 형식이며 AES와 HMAC의 active 키 ID는 같아야 한다.
- HMAC 컬럼은 기존 `CHAR(64)`를 유지해 키 ID를 저장하지 않는다. 일반 쓰기는 active HMAC만 생성하고, 키 전환기의 소셜 로그인만 active와 previous HMAC 후보를 모두 조회한다.
- 회전은 일반 app Pod를 모두 중지한 유지보수 창에 `KEY_ROTATION_ENABLED=true`인 임시 유지보수 Job으로 한 번 수행한다. Job은 web adapter bean을 구성하기 위해 동일한 servlet 이미지를 `SERVER_PORT=0`, `MANAGEMENT_PORT=0`으로 기동하지만 Service가 없고 기본 deny NetworkPolicy가 적용되어 외부 트래픽을 받지 않는다. 실행기는 source 키 ID가 AES/HMAC previous 키링에 모두 있고 active 키 ID와 다른지 검사하고 회전 runner 완료 후 context를 닫는다.
- 회원, 비회원, 결제 시도, 배송지, 관리자 TOTP 비밀키와 암호문이 있는 소셜 계정을 ID 순서로 읽어 active AES로 재암호화하고 active HMAC을 다시 계산한다. 기준 이메일이 없는 회원의 null 이메일 필드는 그대로 두고 HMAC을 만들지 않는다. 활성 예약의 `owner_phone_hmac`은 회전된 현재 회원 또는 비회원 HMAC에서 갱신하고, 종결 예약에는 HMAC을 보존하지 않는다. 비회원 결제의 `owner_phone_hmac`은 복호화한 구조화 payment payload의 정규화 휴대폰에서 재생성하며 문자열 검색으로 추출하지 않는다. 전체 DB 갱신과 휴대폰 인증 행 삭제는 600초 제한의 단일 트랜잭션이며 `data_key_rotation_lock` 단일 행의 `FOR UPDATE NOWAIT` 잠금을 커밋·롤백까지 유지해 중복 실행을 막는다.
- `phone_verifications`에는 전화번호 암호문이 없어 `phone_hmac`과 `code_hmac`을 새 키로 재생성할 수 없다. 회전 시 행을 전량 삭제하고 사용자는 인증번호를 다시 요청한다.
- V63 이전 소셜 계정은 `provider_id_enc`가 `NULL`이라 일괄 HMAC 재생성이 불가능하다. 로그인 입력 provider ID를 previous HMAC 후보로 찾은 뒤 active AES/HMAC으로 즉시 채운다. `provider_id_enc IS NULL`이 0건이 되기 전에는 previous HMAC 키를 제거하지 않는다.
- 회전 검증과 새 키 기준 백업을 마친 뒤에만 [`finalize-data-key-rotation.sh`](../../../deploy/k3s/scripts/finalize-data-key-rotation.sh)로 runtime previous 키를 제거한다. payload가 이미 제거된 최근 최종 결제의 휴대폰 HMAC은 원문을 복원할 수 없어 이전 키 ID를 유지하며, 30일 보존 배치가 제거할 때까지 previous HMAC 키를 유지한다. finalize 스크립트는 active 키 ID가 아닌 `owner_phone_hmac`이나 관리자 TOTP 암호문이 한 건이라도 남으면 중단한다. 보존 중인 과거 백업에 필요한 구키는 해당 백업 보존 기간 동안 분리 복구 저장소에 유지한다.

### 7. 업무가 끝난 임시 암호문에는 보존 기한을 둔다

- 최종 상태(`CONFIRMED`, `FAILED`, `COMPENSATED`, `CANCELED`)의 `payment_attempt`는 생성 30일 뒤 `payload_enc`, `fulfilled_access_token_enc`, `owner_phone_hmac`과 `status_access_token_hash`를 제거한다.
- 복구·대사·보상 진행 상태는 개인정보가 필요하더라도 자동 정리하지 않으며, 먼저 운영 상태를 종결해야 한다.
- 결제 시도 행의 금액, 상태, PG 식별자와 최종 도메인 ID는 감사·대사를 위해 유지한다.
- `phone_verifications`는 짧은 경합·장애 확인 기간을 고려해 만료 후 1일 뒤 행 전체를 삭제한다.
- 관리자 인증 감사 이력은 보안 사고 확인 기간을 위해 180일 보존한 뒤 행 전체를 삭제한다. 복구 코드 해시는 MFA 해제 또는 재등록 때 즉시 제거한다.
- 매일 03:30 배치가 행 잠금 아래 결제 암호문을 건별 정리하고 만료 인증 행을
  `(expires_at, id)` 순서로 100건씩 짧은 트랜잭션에서 삭제한다.

## 결과

### 장점

- 대상 DB 컬럼과 Redis 처리율 제한·관리자 세션 저장소에서 보호 대상 원문이 노출되지 않는다.
- 로그인·이력 연결·소셜 로그인과 이름 정확 일치는 인덱스 조회를 유지한다.
- 전화번호 형식 차이 때문에 서로 다른 HMAC이 생성되는 문제를 제거한다.
- 암호문에 키 ID를 남기고 previous 키를 제한적으로 읽어 운영 데이터를 잃지 않고 필드 키를 교체할 수 있다.

### 단점

- 암호화된 이름과 전화번호의 부분 검색은 지원하지 않는다.
- 암호화·HMAC 키 분실이나 잘못된 키로는 기존 데이터를 복원하거나 조회할 수 없다.
- V46 실행 시 기존 휴대폰 인증은 무효화되어 사용자가 인증 코드를 다시 요청해야 한다.
- 필드 키 회전도 진행 중인 휴대폰 인증을 모두 무효화하고 app 중지 시간이 필요하다.
- V63 이전 소셜 계정이 모두 다시 로그인해 `provider_id_enc`를 채우기 전에는 previous HMAC 키를 폐기할 수 없다.
- 정규화 충돌이 발견되면 운영자가 원본 이력을 확인해 해소한 뒤 다시 배포해야 한다.
- 30일이 지난 결제 confirm 결과는 비회원 원문 접근 토큰을 다시 반환할 수 없으며 새 결제 흐름으로 진입해야 한다.

## 참고

- [Idea-0031 개인정보 암호화](../../Idea/0031_개인정보_암호화_블라인드_인덱스/idea.md)
- [ADR-0022 시스템 경계·상태·스키마 기준선](../0022_시스템_경계_상태_스키마_기준선/adr.md)
- [ADR-0024 비회원 접근 토큰 강화](../0024_비회원_토큰_강화/adr.md)
- [ADR-0028 로그 마스킹](../0028_배포_준비_알림_연동_로그_마스킹/adr.md)
- [ADR-0033 결제 confirm 경계](../0033_결제_confirm_트랜잭션과_보상_경계/adr.md)
- [ADR-0037 자가 호스팅 배포 토폴로지 기준](../0037_자가_호스팅_배포_토폴로지_기준/adr.md)
