# HANDOFF

프론트 이미지 교체와 CI E2E 실패 수정을 완료했다. 원격 푸시·CI 재실행·운영 배포는 아직 진행하지 않았다.

- 브랜치: `codex/work-e2e-order-navigation`. 작업 기반 `85987973`, 이미지 교체 커밋 `e7cab8bf` 이후 E2E 수정. 이미지·출처·대체 텍스트를 함께 변경했다.
- Production `35492378156`(SHA `85987973`): 병렬 백엔드 검사 모두 성공, 전체 약 9분 20초. 회원 주문 상세 이동에서 상태 필터가 누락되어 E2E 실패, 게시·배포는 건너뛰었다.
- 필터 수정: `frontend/src/features/my/useMyListFilters.ts`. 라우터 반영 전 연속 입력을 마지막 요청 필터에 합치고 실제 URL 변경 시 동기화한다. 새 주문·예약 회귀가 수정 전 실패, 수정 후 관련 6개 통과했다.
- 예약 E2E 수정: `frontend/tests/e2e/guest-booking-pass.smoke.spec.ts`. 예약 전에 확보한 변경 대상이 예약과 겹쳐 사라지는 문제를 재현했다. 예약 완료 후 가능한 회차를 조회하며 변경·취소 검증은 유지한다.
- 검증: 이미지 변경 `npm run build` 성공. 필터·상세 이동 6개 통과. 실제 백엔드 smoke 7개 중 6개 통과 후 예약 fixture 수정, 실패했던 P8-2만 재실행하여 통과. 모두 `--retries=0`. 타입·ESLint·diff 검사 통과. 코드·환경 변경 시 영향받는 범위만 재검증한다.
- 실행 환경: 프론트 43100, upstream 43101, 테스트 백엔드 43080, 격리 MySQL 43306·Redis 43379. 기존 3000 포트는 다른 앱이어서 사용하지 않았다. 로그: `/tmp/hg-e2e-filter-after.log`, `/tmp/hg-e2e-smoke-after.log`, `/tmp/hg-e2e-booking-after.log`.
- CD 잔여 확인: 사용자 fuser 출력에서 root PID `952928`이 CD·배포 잠금을 모두 보유함을 확인했다. 해당 고아 port-forward 종료를 안내했으나 실행 결과는 아직 받지 못했다. 잠금 파일은 삭제하지 않는다. 재발 방지 코드 `85987973`은 원격 main에 반영됐지만 이후 E2E 실패로 배포하지 못했다.
- 다음 행동: 사용자 요청 시 푸시·병합 → Production 결과 확인. 서버 잠금이 남아 있으면 새 PID 조회 후 종료된 배포의 잔여 프로세스만 정리한다. 운영 안내는 `deploy/k3s/cicd.md` 참고.
