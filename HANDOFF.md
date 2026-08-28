# HANDOFF

- 진행 중: `plan.md`의 고객 통합 조회·옵션 조합 품절 작업·후기 유스케이스 분리·예약 부분 취소·만석 회차 빈자리 알림을 순서대로 구현한다.
- 확정 정책: 상품+예약 번들 결제는 보류한다. 다인 예약 부분 취소와 만석 회차 빈자리 알림은 제공한다.
- 먼저 볼 파일: `plan.md`, `docs/PRD/0001_기준_스펙/spec.md`, `docs/PRD/0004_API_계약/spec.md`.
- 차단: 2026-08-25 기준 `happy-gallery.com`은 등록되지 않았고, 현재 개발 호스트는 macOS로 Linux+k3s 운영 환경과 Secret·외부 백업·운영 이미지가 없다.
- 먼저 볼 문서: `plan.md`, `deploy/k3s/README.md`, `docs/ADR/0037_자가_호스팅_배포_토폴로지_기준/adr.md`.
- 다음 필수 입력: 도메인 등록 완료 여부와 운영 대상 Linux 노트북의 접근 방법. 현재 Mac을 운영 서버로 쓸 경우 ADR-0037 토폴로지를 먼저 변경해야 한다.
- Kakao 운영 로그인 사용 전 Kakao Developers의 REST API key·client secret, 닉네임/카카오계정 이메일 동의 항목, exact callback `/api/v1/auth/social/callback/kakao`를 설정해야 한다.
