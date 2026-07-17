# 개인정보 암호화 — 블라인드 인덱스 방식

**날짜**: 2026-03-24
**상태**: 채택 완료 — [ADR-0036](../../ADR/0036_개인정보_평문_제거와_블라인드_인덱스_기준/adr.md)

---

## 배경

초기 검토에서는 `Guest` 전화번호부터 암호화했고 회원·인증·결제 준비 데이터에는 평문이 남아 있었다.
현재 구현은 회원 이메일·이름·전화번호, 비회원 이름·전화번호와 결제 준비 payload를 AES-GCM으로 보호하고,
정확 일치가 필요한 값은 HMAC 블라인드 인덱스로 조회한다. 현재 결정과 마이그레이션 기준은 ADR-0036을 따른다.

---

## 방식 비교

| 방식 | 동등 검색 | 범위/LIKE | 구현 난이도 | 비고 |
|------|----------|----------|-----------|------|
| **블라인드 인덱스** | O | X | 중 | AES 암호화 + HMAC 검색 컬럼 |
| 애플리케이션 AES만 | X | X | 하 | 검색 불가 |
| DB TDE | O | O | 하 | SQL 접근자에겐 평문 노출 |
| 토크나이제이션 | O (Vault) | X | 상 | 현재 규모에 과도 |

→ **블라인드 인덱스 채택**: 전화번호·이메일 동등 검색이 이미 존재하므로 가장 현실적.

BCrypt는 저장된 한 건의 해시와 평문을 대조하는 비밀번호 검증에는 적합하지만, 매번 랜덤 salt가 들어가 같은 입력도 다른 해시가 된다.
따라서 `WHERE phone_hmac = ?`처럼 입력값으로 검색 키를 만들어 DB 인덱스를 타야 하는 블라인드 인덱스에는 맞지 않는다.

---

## 설계

### 암호화 구조

```
[평문 전화번호] ──→ AES-256-GCM 암호화 ──→ phone_enc (저장용, 복호화 가능)
                ──→ HMAC-SHA256         ──→ phone_hmac (검색용, 단방향)
```

### IV 생성 난수원 선택

- 양방향 암호화는 Spring Security Crypto의 `AesBytesEncryptor` AES-GCM 구현체를 사용한다.
- GCM IV(12바이트)는 Spring Security Crypto의 `KeyGenerators.secureRandom(12)`로 생성한다.
- `java.util.Random`은 시드와 알고리즘 특성상 출력 예측 가능성이 있어, IV처럼 재사용/예측되면 안 되는 값에 적합하지 않다.
- AES-GCM은 IV 품질이 직접 보안성에 영향을 주므로, 암호학적 난수원인 `SecureRandom`을 기본값으로 유지한다.

### 대상 엔티티·필드

| 엔티티 | 평문 필드 | 암호화 필드 | HMAC 인덱스 필드 |
|--------|----------|-----------|----------------|
| `Guest` | 없음 | `name_enc`, `phone_enc` | `name_hmac`, `phone_hmac` |
| `User` | 없음 | `email_enc`, `name_enc`, `phone_enc` | `email_hmac`, `name_hmac`, `phone_hmac` |
| `PhoneVerification` | 없음 | `code_enc` | `phone_hmac`, `code_hmac` |
| `PaymentAttempt` | 없음 | `payload_enc` | 없음 |
| `SocialAccount` | 없음 | 없음 | `provider_id_hmac` |

### 키 관리

- AES 키와 HMAC 키는 환경변수(`ENCRYPT_KEY`, `HMAC_KEY`)로 주입
- 프로덕션: AWS KMS 또는 Secrets Manager 보관
- 로컬/테스트: `application-local.yml`에 고정값

### 채택된 마이그레이션

- Java Flyway `V46__ProtectPlaintextPersonalData`가 애플리케이션 AES/HMAC 키로 기존 데이터를 백필하고 평문 컬럼을 제거한다.
- 정규화 후 회원 이메일, 비회원 전화번호 또는 소셜 식별자가 충돌하면 자동 병합하지 않고 배포를 중단한다.
- 유효기간이 짧은 기존 휴대폰 인증 행은 백필하지 않고 폐기한다.

### 블라인드 인덱스 한계와 대응

- LIKE 검색 불가 → 관리자 이름은 정확 일치만 지원하고, 주문·예약 번호 부분 검색은 비식별 컬럼으로 유지
- 전화번호 11자리 전수 대조 위험 → HMAC에 pepper 추가
- 키 유출 시 HMAC 재생성 필요 → 키 로테이션 절차 문서화

---

## 단계별 적용 계획

| 단계 | 내용 |
|------|------|
| 0단계 | TDE 선적용 (코드 변경 없이 DB 설정) — 선택 |
| 1단계 | Spring Security Crypto 기반 필드 암호화·블라인드 인덱스와 persistence adapter 구현 |
| 2단계 | Flyway 마이그레이션 + 엔티티 변경 |
| 3단계 | 리포지토리 검색을 HMAC 컬럼으로 전환 |
| 4단계 | V46 Java Flyway로 기존 데이터 백필 및 충돌 사전 검증 |
| 5단계 | 회원·비회원·인증·결제 준비 데이터의 평문 컬럼 제거 완료 |

---

## 참고

- [Idea-0015 다중 인스턴스용 Redis 도입](../0015_다중_인스턴스용_Redis_도입/idea.md)
- [Idea-0030 캐시 스탬피드 방어 전략](../0030_캐시_스탬피드_방어_전략/idea.md)
