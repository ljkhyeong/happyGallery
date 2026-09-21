# HANDOFF

## 소셜 가입·개인정보 보존 배치 후속 확인 (2026-09-22)

- 브랜치 `codex/work-social-consent-retention`, 시작 SHA `df0d4eade56977562a842d2dc54fb68beb6ecc0a`, 시작 시 미커밋 변경 없음. 로컬 구현·검증 완료, 원격 푸시·배포는 요청받지 않아 수행하지 않았다.
- 소셜 로그인 신규 사용자는 서버 세션에 5분간 검증된 프로필을 보관하고 callback의 짧은 동의 화면에서 가입 완료 POST를 호출한다. CSRF·현재 정책 버전·세션/시도 일치·만료·재사용을 검증하며 동의 전 회원을 만들지 않는다. 가입 후 원래 목적지로 복귀하고 기존 회원은 바로 로그인한다.
- 배치 오류 자체는 미해결: 운영 메일 `BatchExecutionFailed / personal_data_retention / partial`을 확인했지만 서버 SSH는 `Permission denied (publickey)`였다. 사용자가 조회한 9/21 19:45경 앱 Pod `app-6c59f48975-cp4zt`는 실행 11시간·재시작 0회이고 새벽 03:30 정리 기록·이전 컨테이너 로그·호스트 검색 결과가 없었다. 과거 실패 항목이나 원인을 단정하지 않는다.
- 확인된 알림 문제는 수정: 기존 Resolved는 10분 실패 집계 창 종료였다. 새 `PersonalDataRetentionFailed`는 항목별 실패 시각과 전체 성공 시각의 최근 7일 이력을 비교하고 Pod 교체 후에도 감지한다. 수집 이력 유실·7일 창 만료는 별도 한계다. 업무 배치 이름은 실제 `exported_job` 라벨로 조회하도록 정체 경보도 수정했다. 새 지표가 없는 이전 앱은 기존 경보를 유지한다.
- 다음 행동: 사용자 푸시·배포 요청 시 기존 CD 인계 조건을 확인해 배포한다. 앱과 Prometheus/Grafana 설정 반영 후 첫 03:30 배치의 `reason`과 같은 시각 로그로 실제 오류를 수정하고 정상 완료를 확인한다. 다음 경로: `monitoring/alerts.yml`, `application/.../batch/DefaultPersonalDataRetentionBatchService.java`, `deploy/k3s/README.md`.

검증 기준은 시작 SHA와 이번 변경이다. 관련 코드·설정·의존성·환경이 그대로면 아래 성공을 재사용한다.

- 인증: `./gradlew --no-daemon :adapter-in-web:test --tests '*PendingSocialSignupStoreTest' --tests '*SocialSignupIntentStoreTest' --tests '*CustomerAuthUseCaseIT' :adapter-in-web:restDocsTest --tests '*SocialSignupApiRestDocsTest'` 통과(`/tmp/hg-social-tests.log`). 기존 회원 재로그인 검증 추가 후 해당 `CustomerAuthUseCaseIT.socialLogin_doesNotTrustLegacyPolicyQueryParameters`만 다시 통과(`/tmp/hg-social-returning-test.log`). `RateLimitFilterTest` 통과(`/tmp/hg-social-ratelimit.log`).
- 계약: `:adapter-in-web:openapi3`와 `frontend npm run api:generate` 완료. 생성 diff는 신규 가입 완료 API만 포함한다.
- 화면: `npm run typecheck`·ESLint 통과. Playwright 43100/43101에서 소셜 관련 8개 시나리오의 최종 결과 통과(재시도 0, 로그 `/tmp/hg-social-e2e.log`, `/tmp/hg-social-e2e-retry.log`). 첫 실행의 fixture 인자 오류와 기존 제공자 수 2개 기대값을 수정해 실패 범위만 재실행했다. 실제 외부 제공자 인증은 모의 응답이며 운영 OAuth 검증은 미실행이다.
- 배치: `:application:useCaseTest --tests '*PaymentAttemptExpiryBatchUseCaseIT.cleanUpExpiredSensitiveData_preservesRecoverableRecords'` 통과(`/tmp/hg-retention-test.log`), `:application:test --tests '*BatchFailureMetricsTest'` 통과(`/tmp/hg-batch-metrics-test.log`). 테스트 DB에서는 실패 재현 안 됨.
- 관측성: 운영 안내의 promtool 명령으로 `monitoring/alerts.test.yml` 통과(`/tmp/hg-alert-tests.log`), `bash deploy/k3s/scripts/validate.sh` 전체 통과(`/tmp/hg-monitor-validate.log`).
- 구조·최종 검토: `ruby tools/agent-feedback.rb final df0d4eade56977562a842d2dc54fb68beb6ecc0a` 통과(`/tmp/hg-final-feedback.log`). 전체 diff에서 의존 방향·계약·생성물·중복 구현 검토 완료. 이후 인계 문서만 수정했다.

## 이전 배포 작업 인계 (아래 운영 미확인 사항 유지)

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
