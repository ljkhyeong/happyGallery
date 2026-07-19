# Plan

이 파일은 현재 활성 작업만 유지한다.
완료된 구현과 장기 설계는 `docs/PRD`, `docs/ADR`, `README.md`를 기준으로 확인한다.

## Active Goal

**2026-07-19 기준:** Toss prepare/confirm, 전 도메인 가격 스냅샷, 비동기 환불 복구, 제한 큐 기반 알림 보호, 알림 outbox, Spring Security·CSRF, 개인정보 암호화·블라인드 인덱스, Google/Naver 로그인은 구현되어 있다.

현재 목표는 남은 결제 우회 경로, 자가 호스팅 운영 구성, 실제 SMS 인증, 회원가입 휴대폰 소유 확인을 순서대로 닫는 것이다.

## 남은 작업

| 우선순위 | 작업 | 현재 상태 | 완료 기준 |
| --- | --- | --- | --- |
| 1 | 장바구니 결제 경로 | `POST /api/v1/me/cart/checkout`이 결제 prepare/confirm을 우회해 주문을 즉시 생성함 | Toss prepare/confirm으로 전환하거나 명시적 무결제·후불 계약으로 분리하고 PRD/API/프론트/E2E를 함께 갱신 |
| 2 | 자가 호스팅 운영 구성 | ADR-0037만 확정됐고 Kubernetes 산출물은 없음 | 단일 노트북 k3s manifest, ingress/TLS, PVC·백업, secret, 이미지 전달, rollout/rollback과 검증 절차 구현 |
| 3 | 실 SMS와 회원 휴대폰 소유 확인 | 일반 알림 SMS adapter는 있으나 인증 코드는 전용 sender에 연결되지 않았고 signup은 `phoneVerified=false`로 저장 | `PhoneVerificationSender` 경계와 실제/가짜 adapter를 연결한 뒤, 별도 회원 소유 확인 use case와 signup UI/API 계약 구현 |

## 실행 순서

1. 장바구니 결제 우회 경로를 정리한다.
2. ADR-0037을 기준으로 자가 호스팅 운영 산출물을 만든다.
3. 인증 코드 전용 SMS 발송 경계를 구현한다.
4. 회원가입 휴대폰 소유 확인을 구현한다.

## 검증 기준

- 백엔드 정책: `./gradlew :application:policyTest`
- DB·트랜잭션·Testcontainers 흐름: `./gradlew --no-daemon :application:useCaseTest`
- HTTP 계약: `./gradlew --no-daemon :adapter-in-web:restDocsTest`
- 프론트: `cd frontend && npm run build`
- 브라우저: 변경 도메인에 따라 `npm run e2e:payment`, `e2e:identity`, `e2e:admin` 중 최소 범위 실행

## 문서 동기화

- 사용자 동작과 정책: `docs/PRD/0001_기준_스펙/spec.md`
- HTTP 요청·응답: `docs/PRD/0004_API_계약/spec.md`
- 오래 유지할 설계 결정: 관련 `docs/ADR/`
- 다음 세션에 필요한 진행 상태만: `HANDOFF.md`
