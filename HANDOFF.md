# HANDOFF

## Jackson 보안 검사 배포 차단 (2026-09-29)

- 시작 SHA `6cce6be7`, 미커밋 변경 없음. 운영 실행 `36575335081`은 호환성 검사 이후 backend Trivy에서 CVE-2026-68497(Jackson 2.21.4·3.1.4)로 실패했다. rollout 이전 실패다.
- Boot 관리 BOM을 Jackson 2.21.6·3.1.6으로 재정의했다. `:bootstrap:bootJar`, `:adapter-in-web:test --tests "*SessionCompatibilityTest"`(5개), `:application:architectureTest` 통과. JAR의 두 databind·core 버전 반영 확인. `/tmp/hg-jackson-build.log`, `/tmp/hg-jackson-final.log`.
- 운영과 같은 amd64 이미지 및 Trivy 0.69.3 HIGH/CRITICAL 검사 통과: OS·JAR 모두 취약점 0건. `/tmp/hg-jackson-scan.log`. 원격 푸시·재배포는 미실행이다.

## 호환성 검사 자동화와 데이터 보정 검토 (2026-09-29)

- 시작 SHA `8f909775`, 미커밋 변경 없음. 완료 검사에 관련 변경의 소스 호환성 검사를 자동 연결했다. 임시 인덱스로 미커밋·신규·삭제·작업 중 커밋을 포함하며 사용자 스테이징은 보존한다. 로컬 기준은 작업 시작 SHA, CI 기준은 마지막 성공 rollout으로 구분한다.
- 공방 이메일 전용 예외를 제거했다. 제한된 조건부 문자열 UPDATE는 migration 원문 SHA-256과 대상·범위·구버전 호환·복구·검증 근거를 같은 PR에 기록한다. V185 기록을 추가했고 원문 SQL은 유지했다. 사람의 승인 강제는 브랜치 보호 정책의 영역이며 이 작업에서 원격 설정은 변경하지 않았다.
- `ruby tools/agent-feedback-test.rb` 14개·54 assertions 통과. 임시 인덱스 스테이징 보존·새 SQL 차단·일반 문서 검사 생략 확인. 실제 마지막 운영 SHA `4b8a61bd`부터 현재 작업 트리 호환성 통과. 배포 전체 검증·최종 검사 결과는 세션 로그 `/tmp/hg-auto-*.log` 참조. 푸시·재배포 미실행.
- 아래 이메일 전용 예외 기록은 이전 구현 이력이다. 현재 절차는 `deploy/k3s/rolling-deployments.md`의 데이터 보정 절차를 따른다.

## 이메일 보정 배포 차단 수정 (2026-09-29)

- 시작 SHA `04b0b9c4`, 미커밋 변경 없음. 배포 `36493819298`은 V185 이메일 UPDATE가 자동 허용 대상이 아니어서 Rolling Compatibility에서 실패했고 운영 rollout은 실행되지 않았다. 직전 성공은 `36489698953` / `4b8a61bd`(한국 시각 09-29 07:16 완료)이다.
- 공개 `workshop_profiles.email`의 문자열 이메일 값 대입 + 기존 이메일 값 일치 조건만 허용한다. 일반 UPDATE·회원 데이터·조건 없는 변경·추가 SQL은 차단한다. migration 원문은 변경하지 않았다.
- 검증: rolling-release-test.rb 26개·86 assertions 통과. 수정한 검사기로 실제 운영 기준 `4b8a61bd` → 실패 리비전 `04b0b9c4` 소스 호환성 통과. 원격 푸시·재배포는 미실행이며 다음 요청 시 이 수정까지 포함해야 한다.
- 반복 배치 알림 원인은 여전히 미확인이다. SSH `home-server` 공개키 인증 실패로 운영 로그를 못 읽었다. 기존 아래 인계의 “776b6f9a 미배포”는 과거 상태이며 이후 성공 배포에 포함됐다. 운영 경보 규칙·실패 reason은 서버에서 추가 확인해야 한다.

## smoke·백업 배포 시간 단축 (2026-09-29)

- 시작 SHA `7528c84753e6119a4dcf936e20d01721d3e975f1`, 시작 시 미커밋 변경 없음. 로컬 구현·검증 완료. smoke 커밋 `81010197`. 푸시·운영 반영은 아직 하지 않았다.
- 최신 운영 성공 `36408132625`는 19분 53초. smoke 첫 실행 실패 두 건은 캐시 없는 로컬에서도 `504 Outdated Optimize Dep`로 재현했다. Vite 공통 의존성을 사전 최적화하고 최초 실패 trace·재시도 성공 진단을 보관한다.
- R2 내용 검증을 마친 백업을 `runuser`로 배포 계정 캐시에 전달하도록 추가했다. 원격 백업 조회·시각·전체 해시 검사는 유지한다. 다음 운영 준비: `/opt/happygallery` 백업 스크립트 갱신 후 `backup.env`에 캐시 사용자·경로 설정. 정확한 절차는 `deploy/k3s/cicd.md`의 ‘검증된 백업의 로컬 전달’. 설정 전에는 기존 재다운로드 방식이다.
- 검증: 캐시 없는 CI 조건(`CI=true`, 43220/43221, 백엔드 8088, `--retries=0`)에서 문제의 두 smoke 수정 전 모두 실패(`/tmp/hg-speed-cold-smoke.log`), 수정 후 16.2초 통과(`/tmp/hg-speed-cold-fixed.log`). 전체 `npx playwright test --grep @smoke --retries=0` 20개 약 1분 통과(`/tmp/hg-speed-full-cold.log`). 로컬 결과이며 GitHub 배포 단축 실측은 미확인이다.
- 검증: `npm run build` 통과(`/tmp/hg-speed-build.log`), `bash deploy/k3s/scripts/validate.sh` 전체 통과(`/tmp/hg-speed-validate.log`), Linux ruby:3.3 root에서 `rclone-backup-test.rb` 16개·102 assertions 통과(`/tmp/hg-speed-linux-cache.log`, 실제 runuser 소유권 분리 포함). actionlint와 최종 검사 통과(`/tmp/hg-speed-final2.log`), 전체 diff의 검증 유지·권한 분리·누락 검토 완료. 같은 코드·설정·환경이면 재사용한다.
- 기존 개발 DB migration checksum 불일치로 실제 주문 검증은 별도 `hg-speed-e2e-mysql` 컨테이너와 Java 25 백엔드로 분리했다. 기존 DB 이력은 변경하지 않았다. 다음 행동은 사용자 푸시 요청 후 CI 소요 시간과 운영 캐시 활성화 여부 확인이다.

## 소셜 전화번호 등록 (2026-09-28)

- 시작 SHA `1fb44deeb5d917632b5cff41c0250d2d08daa354`. Naver `mobile`·Kakao `phone_number`를 신규 가입 연락처로 자동 등록하고 Google은 가입 후 기존 마이페이지 SMS 등록으로 안내한다. 제공자 번호 누락·미동의는 신규 가입을 차단하며 기존 회원 번호는 덮어쓰지 않는다.
- 동일 번호 가입은 사전 조회와 DB 유일 제약으로 차단한다. 소셜 연락처는 `phoneVerified=false`를 유지해 과거 비회원 기록이나 비밀번호 복구 권한을 부여하지 않는다. 제품·API 정책은 PRD 0001·0004, 콘솔 설정은 README에 반영했다. 검토 중이던 미추적 PortOne 연동 파일은 제거했다.
- Java 25·Docker: 소셜 가입/동시 중복/이메일 발급 application 통합 검사, 휴대폰 등록 및 제공자 프로필 검사 통과(`/tmp/hg-social-phone-tests2.log`). 기존 웹 인증·이메일 등록·세션 호환 검사 통과(`/tmp/hg-social-phone-tests.log`; 해당 실행의 휴대폰 fixture 실패만 수정 후 tests2에서 재검증). REST Docs·OpenAPI 생성 통과(`/tmp/hg-social-phone-contract.log`), TypeScript API 재생성 결과 계약 파일 변경 없음.
- 프론트 타입 검사 및 모바일·데스크톱 제공자별 가입 6개와 가입 오류 3개 검사 통과. 병행 작업과 포트 충돌을 피해 `PLAYWRIGHT_FRONTEND_PORT=3110 PLAYWRIGHT_SKIP_MFA_WEB_SERVER=1`로 `social-signup-consent.spec.ts`를 실행했다. 로그 `/tmp/hg-social-phone-e2e-isolated.log`, `/tmp/hg-social-phone-error-e2e.log`. 구조·의존 방향 검사와 전체 diff 검토 완료(`/tmp/hg-social-phone-final.log`). 코드·설정·환경이 같으면 결과를 재사용한다.
- 남은 운영 준비: Naver 개발자센터의 휴대전화번호 제공 항목, Kakao Developers의 `phone_number` 권한·동의 항목을 승인·활성화해야 한다. 실제 제공자 계정으로의 연동 검증은 미실행이다. 배포 전 이미 시작된 5분 가입 대기는 새 번호 필드가 없어 재로그인이 필요할 수 있다. 사용자 요청대로 로컬 커밋만 유지하고 푸시·배포하지 않는다.
- 병행 작업의 회원탈퇴 화면 변경은 별도 커밋 `80ee19d4`, `96f30773`으로 처리됐다. 이번 변경에 포함하지 않는다.

## 수동 호환성 목록 제거 (2026-09-27)

- 시작 SHA `458f727cae85e44cffbdcfee504dcf75001bbe39`, 시작 시 미커밋 변경 없음. 사용자 지시에 따라 기존 브랜치에서 로컬 변경만 진행한다. 푸시·배포하지 않는다.
- 구현 커밋 `6ae83845`. 누적 파일 승인 목록을 제거했다. Git diff 기반 API·migration 검사와 서버 manifest 검사는 유지하며 보호 Spring 설정 변경은 항목을 표시하고 별도 전환 대상으로 차단한다.
- `SessionCompatibilityTest`는 실제 운영 기준 `df0d4ead`의 로그인 키·가입 intent·계정 연결/재인증 intent·재인증 증명 저장 계약을 고정한다. 이전 상태 읽기와 현재 저장 형식을 확인하며 Spring context를 추가하지 않는다. 자동 판정 밖의 의미·전환은 PR 템플릿에서 검토한다. 설명: `deploy/k3s/rolling-deployments.md`.
- Java 25에서 `./gradlew --no-daemon :adapter-in-web:test --tests '*SessionCompatibilityTest' --tests '*SocialSignupIntentStoreTest' --tests '*PendingSocialSignupStoreTest'` 통과: 3개 클래스·12개 사례. `/tmp/hg-session-compat.log`. 첫 sandbox 실행은 Gradle 캐시 권한 문제로 시작하지 못했고 승인된 실행에서 통과했다.
- 새 검사기로 마지막 운영 성공 SHA `df0d4ead`부터 기존 로컬 HEAD `458f727c`까지 `check-source` 통과. 수동 목록 없이 기존 소셜 가입 변경이 허용되는 것을 확인했다. 운영 반영·CI 실제 시간·Redis 직렬화 라이브러리 전체 호환성은 검증하지 않았다.

- `bash deploy/k3s/scripts/validate.sh` 전체 통과: `/tmp/hg-automatic-compat-validate.log` (롤링 검사 24개·79 assertions 포함). `ruby tools/agent-feedback.rb final 458f727cae85e44cffbdcfee504dcf75001bbe39` 통과: `/tmp/hg-automatic-compat-final.log`. 새 파일 포함 전체 diff의 검사 유지·의존 방향·중복·문서 일치 검토 완료. 이후 문서에 결과만 추가했다. 코드·설정·의존성·환경이 같으면 이 검증을 재사용한다.
- 다음 행동: 사용자 푸시 요청 시 원격 반영 후 Production 결과와 실행 시간을 확인한다. 개인정보 배치 오류 후속 확인은 아래 항목을 유지한다.

## Spring context·컨테이너 재사용 개선 (2026-09-27)

- 시작 SHA `ccce06dc9c004e6bf2a786979c16f44af8eb8e5b`, 시작 시 미커밋 변경 없음. 기존 `codex/work-deploy-preflight-speed` 브랜치에서 진행했다. 사용자 지시대로 로컬 커밋만 유지하며 푸시·배포하지 않는다.
- 구현·로컬 검증 완료: `95526028`, 인계 기록 `01c7a34a`. context 공유 조건·격리·비동기 정리·전후 측정값은 [ADR-0026](docs/ADR/0026_통합_테스트_프로파일과_TestContainer_기준선/adr.md)에 정리했다. 실제 GitHub CI 단축 시간은 아직 미측정이다.

검증 기록(관련 코드·설정·환경이 같으면 재사용):

- Java 25·Docker 환경. 위 전체 application 검사 통과: `/tmp/hg-context-before.log`, `/tmp/hg-context-after-final.log`. XML 비교 결과 `/tmp/hg-context-before-results/test`, `/tmp/hg-context-after-final-results/test`: 기존 사례 누락 없음, 실패 0. 첫 변경 후 실패 실행은 spy 타입과 알림 정리 문제를 수정했고 시간 도약 경고가 있어 성능 수치에 사용하지 않았다.
- `:test-support:compileTestFixturesJava` 통과. 실패 범위 3개 클래스 재검증 통과: `/tmp/hg-context-spy-retry2.log`. 이후 전체 검사가 최종 변경을 검증했다.
- `./gradlew --no-daemon :adapter-in-web:test --tests '*UseCaseIT' :adapter-in-web:verifyOpenApi` 통과: 웹 통합 16개 클래스·97개 사례와 OpenAPI 일치 확인. `/tmp/hg-context-web-tests.log`.
- `ruby tools/agent-feedback.rb final ccce06dc9c004e6bf2a786979c16f44af8eb8e5b` 통과(실제 architectureTest 포함), `/tmp/hg-context-final-feedback.log`. 전체 diff의 테스트 범위·fixture 의존 방향·격리·중복 구현 검토 완료. 이후 인계 문서만 변경했다.
- 다음 행동: 사용자 푸시 요청 시 아래 배포 개선과 함께 원격 반영하고 Production 결과·소요 시간을 확인한다.


## 배포 실패·CI 시간 개선 (2026-09-27)

- 작업 브랜치 `codex/work-deploy-preflight-speed`. 시작 SHA `776b6f9a7a153fdf7e670176bb06014fb0abae6b`, 시작 시 미커밋 변경 없음.
- 최근 Production `35620477260`은 소셜 가입 변경 4개 파일의 호환성 검토 기록 누락으로 실제 apply 전에 실패했다. 직전 성공은 `35542816886`, SHA `df0d4eade56977562a842d2dc54fb68beb6ecc0a`. 이전 인계의 백업·배포 잠금 문제 이후 성공 배포가 있었으므로 해당 문제를 현재 장애로 재사용하지 않는다.
- 검토 기록을 보완하고 서버와 공용인 소스 호환성 검사를 CI 빌드 앞에 추가했다. 운영은 직전 push가 아닌 마지막 실제 성공 배포와 비교한다. 서버 최종 검사·백업은 유지한다. 세부 조건: `deploy/k3s/cicd.md`, `deploy/k3s/scripts/ci-compatibility.rb`.
- 해당 실행은 총 27분 18초, application 검사 17분 49초, 이미지 게시 3분, 서버 단계 6분 14초. application 테스트의 Spring 환경 반복 기동과 migration 검사가 병목이다. 공통 기능·주문/결제/예약·migration 세 독립 CI 실행기로 분할했다. 로컬 check는 전체 검사 유지. 실제 단축 시간은 다음 Production에서 측정해야 한다.
- 다음 행동: 원격 푸시·병합은 사용자 요청 후 진행한다. 새 실행의 사전 검사·전체 검사·배포 성공과 소요 시간을 확인한다. GitHub 성공 이력과 서버 manifest가 다른 수동 배포/rollback은 서버 최종 검사에서 별도로 감지한다.

검증 기록(관련 코드·설정·환경이 같으면 재사용):

- `bash deploy/k3s/scripts/validate.sh` 전체 통과, `/tmp/hg-deploy-validate.log`. 롤링 검사 25개/85 assertions와 신규 CI 기준 선택 검사 4개/6 assertions 포함.
- `actionlint .github/workflows/ci.yml .github/workflows/production.yml` 통과. 실행 파일 `/tmp/hg-actionlint/actionlint`.
- 실제 GitHub API에서 마지막 성공 배포 SHA 선택 통과. 실패 SHA의 소스 호환성 오류 재현(`/tmp/hg-compat-before.log`), 선언 보완을 반영한 임시 Git snapshot에서 같은 운영 기준 비교 통과.
- Java 25에서 `:application:check -PciTestGroup=<core|commerce|migration>` 및 속성 없는 전체 check를 Gradle `Test.dryRun=true`로 실행해 발견 대상을 비교했다. 기존 CI의 172개 테스트 클래스 전부가 정확히 한 그룹에 포함되고, 동일 dry-run 조건의 780개 사례도 누락·중복 없이 일치했다. 파라미터별 실행은 dry-run에서 펼쳐지지 않는다. 실제 업무 테스트 재실행 성공으로 보고하지 않는다. 로그 `/tmp/hg-ci-{core,commerce,migration,all-selection}.log`, 결과 `/tmp/hg-ci-selection/`.
- Java 25·Gradle 캐시 접근 권한으로 `ruby tools/agent-feedback.rb final 776b6f9a7a153fdf7e670176bb06014fb0abae6b` 통과(실제 architectureTest 포함). 전체 diff에서 누락·중복·검사 의존 관계 검토 완료. 결과 `/tmp/hg-deploy-final-feedback.log`.

## 개인정보 보존 배치 후속 확인

- 실제 배치 오류 원인은 미확인이다. 운영 메일은 `BatchExecutionFailed / personal_data_retention / partial`. 일반 SSH는 공개키 인증 오류이며 사용자가 확인한 당시 새 Pod에는 새벽 03:30 실패 로그가 남아 있지 않았다.
- SHA `776b6f9a`에는 항목별 실패 지표와 정상 완료까지 유지하는 `PersonalDataRetentionFailed` 경보가 있으나 위 배포 실패로 운영 반영되지 않았다. 기존 Resolved 메일은 10분 집계 창 종료였고 정상 재실행을 뜻하지 않았다.
- 새 배포 후 첫 03:30 실행의 항목별 `reason`과 앱 로그로 실제 오류를 확인한다. `monitoring/alerts.yml`, `application/src/main/java/com/personal/happygallery/application/batch/DefaultPersonalDataRetentionBatchService.java` 참고. 소셜 가입 동의 화면과 관련 업무 검증은 이전 커밋에서 완료했고 이번 작업은 배포/CI 설정만 변경했다.
