# HANDOFF

이미지·E2E 수정은 원격 main에 반영되어 Production `35508032416`에서 빌드·E2E·이미지 게시까지 통과했다. CD는 새 백업 전에 기존 R2 백업의 48시간 제한에 걸려 실패했다.

- 현재 작업: `a56d9c74` 기준으로 일반 배포의 사전 백업 나이 검사를 최초 전환 분기로 옮겼다. 일반 배포는 기존 `deploy.sh`의 새 백업 생성 → R2 검증을 사용한다. 구형 앱 최초 전환의 48시간 제한·무결성 검사는 유지한다. 변경은 아직 푸시하지 않았다.
- 검증: CD 테스트 11개·48 assertions, `bash deploy/k3s/scripts/validate.sh` 전체, agent-feedback final 통과. 로그 `/tmp/hg-cd-validate-20260921.log`. 관련 스크립트 외 앱·E2E는 변경하지 않았다.
- 운영 확인 대기: 사용자에게 실행 중 앱의 `/app/media-backup-guard-v1` 존재 여부를 요청했다. `LEGACY_APP`이면 이번 순서 수정만으로는 해결되지 않는다. 최근 백업도 만료됐으므로 최초 전환용 앱 중지 백업에 대한 사용자 동의·실행이 필요하다. 서비스 중지나 재배포는 수행하지 않았다.

- 브랜치: `codex/work-e2e-order-navigation`. 작업 기반 `85987973`, 이미지 교체 커밋 `e7cab8bf` 이후 E2E 수정. 이미지·출처·대체 텍스트를 함께 변경했다.
- Production `35492378156`(SHA `85987973`): 병렬 백엔드 검사 모두 성공, 전체 약 9분 20초. 회원 주문 상세 이동에서 상태 필터가 누락되어 E2E 실패, 게시·배포는 건너뛰었다.
- 필터 수정: `frontend/src/features/my/useMyListFilters.ts`. 라우터 반영 전 연속 입력을 마지막 요청 필터에 합치고 실제 URL 변경 시 동기화한다. 새 주문·예약 회귀가 수정 전 실패, 수정 후 관련 6개 통과했다.
- 예약 E2E 수정: `frontend/tests/e2e/guest-booking-pass.smoke.spec.ts`. 예약 전에 확보한 변경 대상이 예약과 겹쳐 사라지는 문제를 재현했다. 예약 완료 후 가능한 회차를 조회하며 변경·취소 검증은 유지한다.
- 검증: 이미지 변경 `npm run build` 성공. 필터·상세 이동 6개 통과. 실제 백엔드 smoke 7개 중 6개 통과 후 예약 fixture 수정, 실패했던 P8-2만 재실행하여 통과. 모두 `--retries=0`. 타입·ESLint·diff 검사 통과. 코드·환경 변경 시 영향받는 범위만 재검증한다.
- 실행 환경: 프론트 43100, upstream 43101, 테스트 백엔드 43080, 격리 MySQL 43306·Redis 43379. 기존 3000 포트는 다른 앱이어서 사용하지 않았다. 로그: `/tmp/hg-e2e-filter-after.log`, `/tmp/hg-e2e-smoke-after.log`, `/tmp/hg-e2e-booking-after.log`.
- CD 잔여 확인: 사용자 fuser 출력에서 root PID `952928`이 CD·배포 잠금을 모두 보유함을 확인했다. 해당 고아 port-forward 종료를 안내했으나 실행 결과는 아직 받지 못했다. 잠금 파일은 삭제하지 않는다. 재발 방지 코드 `85987973`은 원격 main에 반영됐지만 이후 E2E 실패로 배포하지 못했다.
- 다음 행동: 사용자 요청 시 푸시·병합 → Production 결과 확인. 서버 잠금이 남아 있으면 새 PID 조회 후 종료된 배포의 잔여 프로세스만 정리한다. 운영 안내는 `deploy/k3s/cicd.md` 참고.
