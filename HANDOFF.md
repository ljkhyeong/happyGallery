# HANDOFF

백엔드 CI 병렬화와 CD 잠금 장애 재발 방지 코드를 작성·검증했다. 서버의 기존 잠금 보유 프로세스 확인과 운영 반영이 남았다.

- 작업 브랜치: `codex/work-ci-performance-cd-lock`. 시작 SHA `0682fce126ab69747c1dd755a7eb2a8363345bdc`, 실제 작업 기반은 동일 소스가 main에 병합된 `5b11aad1`. CI 변경 커밋은 `170beec1`. 이번 작업은 아직 원격 푸시하지 않았다.
- CI: 패키징·기타 모듈, application, 웹 검사를 독립 실행기에서 병렬화했다. E2E는 JAR 준비 후 시작한다. 기존 필수 검사 이름과 전체 검사 범위는 유지한다. `README.md`, `.github/workflows/ci.yml` 참고.
- 측정: 로컬 `./gradlew --no-daemon build --profile` 성공(6분 57초). application 테스트 4분 58초, Spring 시작 14회·177초. mock·속성 차이는 필요한 격리이므로 변경하지 않았다. Java 코드·테스트·설정은 그대로여서 성공 결과를 재사용한다.
- 검증: 전체 빌드와 분리한 세 작업의 `--dry-run` 비교로 92개 태스크 포함 및 테스트·계약 태스크 12개 중복 없음을 확인했다. actionlint 1.7.7로 CI·Production workflow 검사 통과. 원격 병렬 CI의 실제 단축 시간은 아직 미측정이다.
- CD: Production 실행 `35478491041`은 SSH 연결 후 `다른 CD가 실행 중입니다`로 중단됐다. 빌드·E2E·이미지 게시까지 성공했다. `verify.sh`의 중간 셸 종료 뒤 port-forward가 남는 결함을 재현하고 실제 실행 PID 정리·잠금 FD 8/9 상속 차단으로 수정했다.
- CD 검증: `bash deploy/k3s/scripts/validate.sh` 전체 통과. Ruby 3.3/Linux의 verify 회귀 10개·41 assertions 통과. 코드 변경 전에는 새 프로세스 정리 회귀가 실패했다. 관련 스크립트가 바뀌면 영향받는 검사만 재실행한다.
- 운영 확인 대기: 로컬 `ssh home-server`는 공개키 인증 실패. 사용자 서버 출력에는 진행 중인 배포 없이 부모 PID 1인 `952928 sudo -n /usr/local/bin/k3s kubectl -n happygallery port-forward service/app-management 18081:8081`이 남아 있다. 실제 잠금 보유자인지 확인하려고 사용자에게 `sudo fuser -v /home/ronaldo/.local/state/happygallery/cd/.lock /home/ronaldo/.local/state/happygallery/releases/.deploy.lock` 결과를 요청했다. PID는 새 조회로 재확인하고, 잠금 파일 삭제나 일괄 종료는 하지 않는다.
- 다음 행동: 잠금 보유자 확인 → 종료된 배포의 잔여 프로세스만 정리 → 사용자 요청 시 코드 푸시·병합 → Production 재실행. 운영 복구 절차는 `deploy/k3s/cicd.md` 참고. 서버 복구나 재배포 성공을 아직 확인하지 않았다.
