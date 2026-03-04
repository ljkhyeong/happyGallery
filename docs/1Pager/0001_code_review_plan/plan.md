# Code Review Plan

## 목적
- MVP 구현 이후 출시 전 리스크를 빠르게 식별한다.
- 스타일보다 동작 버그, 스펙 불일치, 데이터 정합성, 테스트 누락을 우선 검토한다.

## 리뷰 기준 문서
- 기준 스펙: `docs/PRD/0001_spec/spec.md`
- 설계 판단 참고: `docs/ADR/*`

## 리뷰 순서
1. 변경 범위 정리
- 포맷, 줄바꿈, 문서 전체 재기록처럼 기능과 무관한 diff를 먼저 분리한다.
- 기능 변경 PR은 리뷰 가능한 단위로 유지한다.

2. 스펙 일치성 검토
- 상태값, 시간 경계, 예약금 10%, 슬롯 정원 8명, 버퍼 30분, 8회권 90일 만료 규칙이 스펙과 일치하는지 확인한다.
- 구현 변경이 스펙 변경을 동반하면 문서도 함께 갱신했는지 확인한다.

3. 레이어 책임 검토
- `app`은 흐름 제어에 집중하고, 도메인 규칙은 `domain`에, 외부 연동 구현은 `infra`에 유지되는지 확인한다.
- `app`에 정책이 과도하게 들어가거나 `infra`에 업무 규칙이 들어가지 않았는지 본다.

4. 핵심 도메인 리뷰
- 예약/슬롯: 예약 생성, 변경, 취소, 정원 초과, 버퍼 비활성화, 중복 예약 방지
- 주문/환불: 승인 대기, 자동 환불, 재고 복구, 픽업 만료, 제작 시작 후 환불 불가
- 8회권: 결제, 차감, 만료, 환불, 미래 예약 취소

5. 정합성과 동시성 검토
- 재고와 슬롯은 트랜잭션 안에서 일관되게 갱신되는지 확인한다.
- 수동 처리와 배치가 동시에 실행될 때 중복 환불, 중복 차감, 중복 상태 전이가 없는지 본다.

6. API/예외/알림 검토
- 요청/응답 DTO와 에러 포맷이 일관적인지 확인한다.
- 외부 연동 실패 시 상태 기록과 재시도 경로가 남는지 확인한다.

7. 테스트 검토
- 정책 변경은 `policyTest`, 유스케이스/DB 영향은 `useCaseTest`가 따라오는지 확인한다.
- 경계값, 실패 경로, 경쟁 조건 테스트가 있는지 검토한다.

## 우선 리뷰 대상
- `app/src/main/java/com/personal/happygallery/app/booking/*`
- `app/src/main/java/com/personal/happygallery/app/order/*`
- `app/src/main/java/com/personal/happygallery/app/pass/*`
- `domain/src/main/java/com/personal/happygallery/domain/booking/*`
- `domain/src/main/java/com/personal/happygallery/domain/order/*`
- `domain/src/main/java/com/personal/happygallery/domain/pass/*`
- `common/src/main/java/com/personal/happygallery/common/time/TimeBoundary.java`
- `app/src/main/resources/db/migration/*`
- 관련 `*UseCaseIT`, `*PolicyTest`

## 리뷰 산출물
- 머지 전 반드시 수정할 이슈
- 후속 이슈로 넘길 개선 항목
- 실행한 테스트와 남은 리스크
