# local/dev 지원 기능 경계 회고

**날짜**: 2026-03-17  
**상태**: Retrospective note

---

## 배경

`main` 히스토리 후반으로 갈수록 local 전용 seed, 개발용 실패 주입 API, 테스트 지원 API가 늘어났다.

대표 구간:
- `e009c5d` 프로필 분리
- `fc105cf` admin 인증 필터
- member/guest E2E 확장 구간의 local 보조 코드 반영
- 관측성과 운영 보강 구간의 개발용 지원 기능 증가

최근 보안 정리에서도 기본 관리자 계정, dev API key, OTP 노출을 local 경계 안으로 다시 밀어 넣는 작업이 필요했다.

---

## 관찰

- local/dev 편의 기능은 처음에는 유용하다.
  하지만 profile 경계가 약하면 운영 기본값과 섞이기 쉽다.
- local-only controller/service가 feature별로 흩어지면
  무엇이 운영 코드이고 무엇이 지원 도구인지 읽기 어려워진다.
- 테스트와 E2E가 커질수록 개발용 지원 API는 늘어날 가능성이 높다.

---

## 회고 포인트

### 1. local 지원 기능의 명명 규칙을 고정

As-Is:
- local seed, 실패 주입 API, 디버그 조회 기능이 기능별로 흩어진다.

To-Be:
- `Local*Service`, `Local*Controller`, `*DevHook` 같은 명명과 위치 규칙을 고정한다.

### 2. unsafe default는 공통 설정에 두지 않는다

As-Is:
- dev 편의 설정이 base config에 들어갔다가 나중에 걷어내는 일이 생긴다.

To-Be:
- 기본값은 안전하게 두고, local/dev에서만 명시적으로 opt-in 한다.

### 3. E2E 지원 API는 운영 기능과 분리해 목록화

As-Is:
- E2E를 위해 추가한 dev endpoint가 점진적으로 늘어난다.

To-Be:
- local 전용 지원 API를 한곳에 목록화하고,
  README나 운영 문서에는 “운영 경로와 분리된 지원 기능”으로 명확히 적는다.

---

## 현재 결론

local/dev 지원 기능은 앞으로도 계속 필요하다.  
문제는 “있느냐 없느냐”가 아니라 “기본 설정과 운영 코드에 얼마나 섞이느냐”다.
처음부터 지원 기능을 별도 경계로 관리하는 규율이 필요하다.
