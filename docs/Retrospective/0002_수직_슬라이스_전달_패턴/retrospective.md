# vertical slice delivery 방식 회고

**날짜**: 2026-03-17  
**상태**: Retrospective note

---

## 가설

도메인별 기능을 `domain -> infra -> app -> docs/test` 순으로 vertical slice로 완성하는 방식은
복잡한 공방 운영 기능을 빠르게 쌓는 데 유효하다.

---

## 검증 방법

대표 slice를 기준으로 커밋 흐름을 확인했다.

- 예약 생성: `7a28520`, `a4918c1`, `f6ca34d`, `052f727`, `90cb72f`
- 예약 변경: `699962d`, `469907f`, `2c0db0e`, `26eda11`, `347ded9`, `eb7f9fd`
- 주문/패스/상품/재고/제작/픽업: `312870d`, `faa4333`, `523719d`, `0ea5c36`, `09c2b09`, `966cc48`
- 알림/배치/admin 인증: `a86bdf0`, `8a7f01e`, `fc105cf`

---

## 결과

- 각 slice는 기능 단위로 범위를 자르기 쉬웠고,
  문서와 테스트를 함께 붙이면 요구사항 추적이 비교적 수월했다.
- 빠른 delivery에는 효과적이었고,
  실제로 2월 말부터 3월 초까지 코어 운영 기능을 짧은 기간 안에 채울 수 있었다.

한계도 확인됐다.
- guest/member 중복 로직
- controller 조합 로직 잔존
- 운영 query/read model 정리 지연

즉, slice delivery 자체는 유효하지만,
몇 개의 slice가 쌓인 뒤에는 공통화 checkpoint가 필요하다.

---

## 결론

vertical slice 방식은 유지할 가치가 있다.  
다만 2~3개 slice가 누적될 때마다 “공통화/경계 정리 스텝”을 의도적으로 끼워 넣는 편이 더 낫다.
