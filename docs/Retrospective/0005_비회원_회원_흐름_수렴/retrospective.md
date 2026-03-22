# guest/member 흐름 수렴 회고

**날짜**: 2026-03-17  
**상태**: Retrospective note

---

## 배경

`main` 히스토리를 보면 공개 guest 흐름이 먼저 빠르게 자리잡고,
그 위에 member store, `/my`, guest claim이 덧붙는 방식으로 기능이 확장됐다.

대표 구간:
- 예약 생성/변경/취소: `052f727`, `26eda11`, `cb5e1d7`
- 주문/패스/상품/주문 승인: `312870d`, `faa4333`, `523719d`, `0ea5c36`
- member store/self-service/claim: `81aaa36`, `3ca53dd`, `ca4f18e`, `6c4321e`, `835589c`

이 흐름은 기능 확장에는 유효했지만,
이제는 guest/member/claimed를 따로 다루는 중복 비용이 커졌다.

---

## 관찰

- guest 예약/주문/8회권을 먼저 구현한 뒤 member 흐름이 추가되면서
  검증, 응답 조합, 조회 모델이 서로 다른 경로에 중복됐다.
- guest claim이 들어오면서 같은 aggregate를 guest와 member 시점에 각각 다르게 읽는 요구가 생겼다.
- 최근 guest token 강화, booking reminder/admin query 보강 같은 작업도
  결국 “guest 전제 모델”을 걷어내는 쪽으로 흘렀다.

---

## 회고 포인트

### 1. booking의 고객 표현을 공통 모델로 정리

As-Is:
- booking 조회와 운영 기능이 여전히 `guest != null` 전제를 품기 쉽다.

To-Be:
- `customerSummary` 또는 `bookingRecipient` 같은 공통 projection을 두고
  guest/member/claimed 상태를 한 모델로 다룬다.

### 2. booking 생성 orchestration을 guest/member 공통 코어로 추출

As-Is:
- 슬롯 활성 검증, 중복 예약 방지, confirm, pass/deposit 분기, 이력/알림이
  guest/member 서비스에 반복된다.

To-Be:
- identity 획득만 분기하고 나머지는 공통 creation support 또는 use case 코어로 묶는다.

### 3. claim 이후 운영 경로를 1급 시나리오로 취급

As-Is:
- claim은 member 전환 기능으로 추가됐지만,
  admin 조회/배치/알림은 guest 기준 가정을 놓치기 쉽다.

To-Be:
- 새 booking/order 기능을 만들 때부터 `guest`, `member`, `claimed` 3상태를 체크리스트로 검토한다.

### 4. guest token 후속은 기존 0005와 연결

As-Is:
- guest 흐름 수렴 논의와 token 전달/만료 논의가 서로 얽혀 있다.

To-Be:
- token 자체는 [0005_비회원_토큰_Signed_만료](../../Idea/0005_비회원_토큰_Signed_만료/idea.md)에서 관리하고,
  이 문서는 guest/member 흐름 수렴 자체에 집중한다.

---

## 현재 결론

guest 흐름 위에 member 흐름을 올리는 방식은 빠른 delivery에는 유효했다.  
다만 이후 비용을 줄이려면 “guest/member/claimed 공통 모델”을 더 일찍 상정했어야 한다.
