# HANDOFF

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
