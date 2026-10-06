# main 자동 배포

`main` 병합 → 소스 호환성 사전 검사 → 기존 CI → 운영 이미지 빌드·취약점 검사 → GHCR 게시 → SSH → 배포별 백업 실행·R2 검증 → 이미지 반입 → 롤링 배포·공개 경로 확인 순서다. 서버에서 `IMAGE_TAG`나 digest를 입력하지 않는다.

PR은 `.github/workflows/ci.yml`을 실행한다. CI는 수동 실행도 지원하며 `production_candidate=true`이면 운영 설정의 이미지를 검증한다. `.github/workflows/production.yml`은 main push 또는 main의 수동 실행에서만 동작하며 `CD_ENABLED=true`일 때 게시·배포한다. `ci.yml`을 재사용하므로 main에서도 백엔드·프런트·브라우저 검사를 통과해야 한다. 운영 이미지를 검사할 때는 CI용 이미지를 중복 빌드하지 않는다.

| 단계 | 담당과 통과 조건 |
| --- | --- |
| 소스 호환성 | PR 기준 브랜치 또는 마지막 성공 배포와 API·migration·설정 비교 |
| 코드 검증 | 백엔드 모듈·application 세 그룹·웹 계약, 프런트 단위·lint·API·build, 브라우저 smoke |
| 운영 구성 | `validate.sh`의 manifest·배포·백업·복구 검사, actionlint의 workflow·shell 검사 |
| 이미지 검증 | 공통 `ci-images.sh` 빌드·앱 사용자 JAR 읽기 검사, `scan-images` action의 양쪽 이미지 검사 |
| CI Gate | 필요한 작업이 모두 성공해야 통과. PR 외 Dependency Review와 운영 배포가 뒤에서 수행할 이미지 검사만 생략 허용 |
| 게시·배포 | 검사한 이미지를 재빌드 없이 게시하고 digest를 전달. 백업·서버 호환성·순차 rollout·공개 경로 검증 유지 |

공통 이미지 검사는 HIGH/CRITICAL 취약점·비밀값과 OS 지원 종료를 확인한다. 한쪽이 실패해도 다른 쪽을 검사하고 JSON 보고서를 7일 보관한다. 어느 쪽이든 실패하거나 실행되지 않으면 다음 단계로 진행하지 않는다. `production` 태그 빌드와 게시에는 main 제한을 유지하며 `candidate`는 운영 설정으로 검증만 한다. PR용 `ci`는 운영 변수가 있어도 테스트 클라이언트 키와 빈 Sentry DSN을 사용한다.

빌드·검사 작업은 2~30분으로 제한하고 운영 배포는 기존 60분을 유지한다. 실행 중인 배포를 취소하지 않으며, 재사용 CI의 동시 실행 그룹에는 호출 workflow 이름을 넣어 서로 다른 실행이 같은 대기열을 공유하지 않게 한다. JAR artifact는 누락 시 즉시 실패하고 7일 보관한다. 외부 Actions는 확인한 commit SHA로 고정하며 Dependabot으로 갱신한다. Node 24 실행 환경으로 전환하되 Gradle Actions는 기존 라이선스의 v5 계열을 유지한다.

GitHub의 일반 호스팅 러너는 [공개 저장소에서 무료](https://docs.github.com/en/billing/concepts/product-billing/github-actions)다. GHCR 이미지 저장·전송도 [현재 무료](https://docs.github.com/en/billing/concepts/product-billing/github-packages)다. Actions 로그·artifact 할당량과 GHCR 향후 정책 변경은 별도다. 별도 CI 서버나 Argo CD는 설치하지 않는다.

## 배포 전 검사와 실행 시간

`Rolling Compatibility`가 통과해야 백엔드·프런트 빌드가 시작된다. PR은 대상 브랜치와 비교하고, 운영 배포는 GitHub Actions에서 마지막으로 `Roll out production`이 성공한 commit과 비교한다. 실패한 push나 CD를 끈 실행을 배포 기준으로 삼지 않는다. 조회 실패·기준 이력 부재는 검사를 중단한다. 수동 서버 배포나 rollback으로 실제 운영 버전이 달라진 경우까지 GitHub 이력이 보장하지는 않으므로 서버의 manifest 비교도 유지한다.

사전 검사는 서버와 같은 `rolling-release.rb check-source <repo> <기존 SHA> <후보 SHA>`를 사용한다. 비호환 OpenAPI·migration과 보호 Spring 설정 변경을 이미지 생성과 서버 백업 전에 발견한다. 인증·세션 저장 계약은 웹 CI 테스트에서 검증하며, 서버의 최종 workload·설정 검사와 백업은 생략하지 않는다.

application 검사는 `ciTestGroup=core|commerce|migration` 세 실행기로 분리한다. 주문·결제·예약과 migration 패키지를 각각 분리하고 core는 나머지 전부를 실행한다. 각 실행기의 DB는 독립적이며 로컬 `:application:check`는 속성 없이 전체 범위를 유지한다. 웹 검사와 필수 통합·브라우저 검사는 그대로 실행한다.

2026-10-05 조회 기준 최근 운영 성공은 [Production 36722337863](https://github.com/ljkhyeong/happyGallery/actions/runs/36722337863), SHA `b4edd17d`다. 2026-09-30 22:32~22:50 KST에 약 17분 52초가 걸렸다. CI는 약 5분 37초, 이미지 게시 2분 35초, 서버 배포 9분 33초였다. application 세 그룹은 각각 3~4분으로, 현재 가장 긴 구간은 서버 백업·배포다. 이 기록은 이번 공통화 이전의 실측이며 새 구성의 시간은 원격 반영 후 비교한다. 백업을 생략해 시간을 줄이지 않는다.

## 브랜치 보호 적용

2026-10-05 확인 결과 `main`·`codexReview`는 보호되지 않았고 기존 `jkrule` ruleset도 비활성 상태였다. `production` environment는 main만 허용했고 `CD_ENABLED=true`, Actions 기본 토큰은 읽기 권한이었다. 따라서 당시에는 CI 실패가 있어도 병합을 강제 차단하지 않았다. 저장소 파일 변경만으로 원격 보호 설정이 바뀌지는 않는다.

[branch-protection.json](../../.github/branch-protection.json)은 두 브랜치에 적용할 설정이다. PR 경유, 최신 대상 브랜치와의 CI Gate 성공, 대화 해결, 관리자 포함 적용, 강제 푸시·삭제 금지를 요구한다. 1인 개발 흐름을 유지해 다른 사람의 승인은 필수로 두지 않는다. 검사는 GitHub Actions 앱 ID `15368`로 제한한다. 기존 ruleset은 생성·갱신 차단과 광범위 우회 항목이 있어 그대로 활성화하지 않는다.

새 workflow를 원격에 반영하고 해당 PR의 `CI Gate`가 실제 생성·성공한 뒤 적용한다. 아직 존재하지 않는 검사를 필수로 지정해 병합을 막지 않는다. 이 문서의 설정은 준비된 적용안이며 원격 적용 완료 기록이 아니다.

```bash
for branch in main codexReview; do
  gh api --method PUT "repos/ljkhyeong/happyGallery/branches/$branch/protection" \
    --input .github/branch-protection.json
done
```

적용 후 두 브랜치의 protection API에서 필수 검사와 관리자 적용을 다시 읽어 확인한다. [GitHub 브랜치 보호 문서](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)의 필수 상태 검사는 `skipped`도 허용하므로, 개별 job 대신 생략 사유까지 확인하는 `CI Gate`를 필수로 사용한다. 봇 PR도 CI를 직접 수동 실행해 같은 검사 이름을 제공한다.

## 1. 최초 전환 확인

1. 현재 준비 중인 롤링 배포 지원 이미지를 배포하고 `verify.sh`를 통과한다.
2. 배포 진입점이 배포마다 백업 service를 실행하고 새 R2 복구 묶음을 검증하는지 확인한다. 일반 배포는 이전 백업의 나이로 차단하지 않는다. 온라인 백업 미지원 앱의 최초 전환에만 기존 48시간 내 복구 묶음을 요구한다. 이것도 만료됐다면 앱 중지 백업을 별도 승인·진행한 후 전환해야 한다. 기존 백업 timer와 watchdog은 설치하지 않는다.
3. 서버에서 적용했던 운영 패치와 이번 CI/CD 코드를 PR로 main에 반영한다. 이 문서 작성만으로 push나 GitHub 설정 변경은 수행되지 않는다.

서버의 `/opt/happygallery`에는 수동 패치 커밋이 남아 있을 수 있다. `reset --hard`로 지우지 않는다. CD는 같은 Git 이력을 공유하는 별도 worktree를 사용한다. [롤링 호환성 검사](rolling-deployments.md)는 일반 설정값·문서 변경과 호환 API·SQL 확장을 허용하고, 필드 삭제·타입 변경·데이터 전환이 필요한 변경을 구체적인 경로와 함께 표시한다. 변경 파일은 Git이 찾으며 수동 승인 목록은 사용하지 않는다. 세션은 고정된 이전 저장 계약 테스트로 확인한다. 자동 판정 밖의 의미·전환은 해당 PR에서 검토하고, 보호 설정 변경은 별도 전환을 준비한다.

## 2. 서버에 배포 진입점 설치

아래 명령은 CI/CD 파일이 있는 서버 checkout에서 `ronaldo`로 실행한다. 기존 서버 설정은 `/etc/happygallery`, 수동 저장소는 `/opt/happygallery`를 사용한다.

```bash
cd /opt/happygallery
sudo install -d -m 755 /usr/local/libexec
sudo install -o root -g root -m 755 deploy/k3s/scripts/accept-cd-ssh.sh /usr/local/libexec/happygallery-cd-ssh
sudo install -o root -g root -m 755 deploy/k3s/scripts/cd-kubectl.sh /usr/local/libexec/happygallery-kubectl
sudo visudo -cf deploy/k3s/examples/cd-sudoers.example &&
sudo install -o root -g root -m 440 deploy/k3s/examples/cd-sudoers.example /etc/sudoers.d/happygallery-cd
sudo visudo -c
sudo -n /usr/local/bin/k3s kubectl get nodes
```

이 설정은 비대화형 배포에 필요한 k3s 관리와 배포별 백업 service 실행을 허용한다. `ronaldo`는 기존 Docker 권한을 포함해 운영 서버를 관리하는 신뢰 계정이며, k3s 권한도 클러스터 관리자 수준이다. 배포 키에는 다음 단계에서 실행 명령·포트 전달 제한을 추가한다.

R2 검증 설정을 별도로 만든다. `rclone.conf`는 기존 파일을 사용하며 `ronaldo`가 읽을 수 있어야 한다. 비밀값을 화면에 출력할 필요는 없다.

```bash
sudo install -o ronaldo -g ronaldo -m 600 deploy/k3s/examples/cd-backup.env.example /etc/happygallery/cd-backup.env
vi /etc/happygallery/cd-backup.env
test -r /etc/happygallery/rclone.conf && printf 'R2 설정 읽기 OK\n'
```

기존 `backup.env`와 `RCLONE_CONFIG`, `RCLONE_BACKUP_REMOTE`를 일치시킨다. CD는 최근 48시간 내 완성된 R2 복구 묶음을 다운로드하고 모든 SHA-256을 검사한다. age 개인키는 사용하지 않는다. 처음 수행했던 DB 복원 훈련과 별개로 전송 무결성을 자동 검사하는 단계다. 검증 캐시는 `~/.local/state/happygallery/cd/backups`에 두 세대만 보관한다.

## 3. 서버의 GHCR 읽기 인증

[GHCR 문서](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)에 따라 GitHub의 **Personal access tokens → Tokens (classic)**에서 `read:packages` 토큰을 만든다. 첫 게시 패키지는 기본 비공개다. 서버에는 읽기 토큰만 저장하고 게시에는 워크플로의 `GITHUB_TOKEN`을 사용한다.

서버에서 실행한다. 토큰 만료 시 서버의 로그인도 갱신해야 한다.

```bash
read -r -s -p 'GHCR 읽기 토큰: ' HG_GHCR_TOKEN
printf '\n'
printf '%s' "$HG_GHCR_TOKEN" | docker login ghcr.io -u ljkhyeong --password-stdin
unset HG_GHCR_TOKEN
chmod 700 "$HOME/.docker"
chmod 600 "$HOME/.docker/config.json"
```

Docker는 이 인증을 서버 사용자 계정에 보관한다. 토큰은 Git 저장소나 이미지에 넣지 않는다.

## 4. Mac에서 배포 전용 SSH 키 생성

사용 중인 개인 SSH 키와 분리한다. 자동 실행용 키이므로 암호문 입력 없이 쓸 수 있게 만들고, 서버에서는 고정된 배포 명령만 받는다.

```bash
test ! -e "$HOME/.ssh/id_ed25519_happygallery_cd" &&
ssh-keygen -t ed25519 -N '' -C happygallery-github-actions -f "$HOME/.ssh/id_ed25519_happygallery_cd"
cat "$HOME/.ssh/id_ed25519_happygallery_cd.pub"
```

서버의 `~/.ssh/authorized_keys`를 `vi`로 열어 아래 형식의 **한 줄**을 추가한다. 기존 Mac 접속 키는 유지하고, `ssh-ed25519 AAAA...` 부분에 방금 만든 공개키를 넣는다.

```text
restrict,command="/usr/local/libexec/happygallery-cd-ssh" ssh-ed25519 AAAA... happygallery-github-actions
```

```bash
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

배포 명령은 `deploy <40자리 commit> <app digest> <frontend digest>`만 허용한다. 그 commit이 origin/main의 최신 값인지 다시 확인한다. 별도 worktree가 수정된 경우에도 중단한다.

Mac에서 이미 신뢰한 `home-server` 접속으로 서버 공개키를 가져와 known_hosts 한 줄을 만든다. GitHub 실행 때 임의로 키를 수집해 신뢰하지 않는다.

```bash
ssh home-server 'cat /etc/ssh/ssh_host_ed25519_key.pub' |
awk '{print "[jklhome.tplinkdns.com]:22222 " $1 " " $2}'
```

공유기의 기존 외부 SSH 포트가 22222인 구성이다. 실제 설정이 다르면 위 포트와 아래 `CD_PORT`를 함께 변경한다. 새 Kubernetes 포트를 공개할 필요는 없다.

## 5. GitHub 변수와 Secret 등록

저장소 **Settings → Secrets and variables → Actions → Variables**에 등록한다.

| 이름 | 값 |
| --- | --- |
| `CD_ENABLED` | 준비 중에는 `false`, 마지막에 `true` |
| `PRODUCTION_TOSS_CLIENT_KEY` | 현재 운영 프런트 빌드에 쓰는 Toss 클라이언트 키 |
| `PRODUCTION_SENTRY_DSN` | 사용 중이면 프런트 Sentry DSN, 아니면 생략 |

Toss 클라이언트 키는 브라우저에 제공되는 값이다. Toss **시크릿 키**, DB·OAuth·Resend 비밀값은 여기에 넣지 않고 서버 설정에 유지한다.

**Settings → Environments → New environment**에서 `production`을 만든다. Deployment branches는 `main`만 허용한다. 사용자 선택에 따라 자동 배포하므로 required reviewer 승인은 설정하지 않는다.

| 구분 | 이름 | 값 |
| --- | --- | --- |
| Environment variable | `CD_HOST` | `jklhome.tplinkdns.com` |
| Environment variable | `CD_PORT` | `22222` |
| Environment variable | `CD_USER` | `ronaldo` |
| Environment secret | `CD_SSH_PRIVATE_KEY` | Mac의 `id_ed25519_happygallery_cd` 파일 전체 |
| Environment secret | `CD_SSH_KNOWN_HOSTS` | 앞서 만든 `[도메인]:포트 ssh-ed25519 ...` 한 줄 |

Mac에서 `pbcopy < ~/.ssh/id_ed25519_happygallery_cd`로 복사해 Secret 입력란에 붙여넣는다. 파일의 BEGIN/END 줄까지 포함하며 채팅에는 보내지 않는다.

## 6. 최초 실행과 이후 운영

위 설정과 최초 온라인 백업 확인이 끝나면 `CD_ENABLED=true`로 바꾸고 **Actions → Production → Run workflow → main**을 실행한다. 이후 main 병합마다 자동 실행된다. 위 브랜치 보호 설정의 실제 적용 여부도 확인한다.

1. `validate`: 기존 테스트·E2E가 통과한다.
2. `publish`: 실제 운영 설정으로 빌드한 두 이미지를 HIGH/CRITICAL 취약점 검사 후 GHCR에 올린다.
3. `deploy`: SSH 호스트 키, 최신 main, 배포 시 새로 만든 R2 백업, 이미지 OS·CPU·commit·source·digest를 확인한다. 최초 온라인 백업 전환에서만 현재 구버전 앱이 지원 표식을 제공하지 않으면 먼저 검증한 R2 복구 묶음을 1회 사용하고, 새 app 표식을 확인한다. containerd 반입 후 실제 digest로 release.env를 갱신한다.
4. 기존 `deploy.sh`가 백업 성공과 복구 묶음 검증을 통과한 뒤 롤링 배포·공개 경로 확인을 수행한다. 준비되지 않은 새 앱으로 트래픽을 넘기지 않는다.

롤아웃이 끝나면 공개 경로 확인(`verify.sh`) 전에 새 release를 `current`로 기록한다. 백업은 `current`와 실행 중인 이미지가 같아야 하므로, 예전처럼 확인 실패 뒤 `current`가 이전 release에 남으면 다음 배포의 백업이 모두 거부된다. 이미 그 상태라면 `deploy.sh`가 백업 전에 실행 중인 app·frontend 이미지와 digest가 같은 release로 `current`를 맞춘다. 백업이 실패하면 service 결과와 백업 스크립트의 `오류:`·`[happygallery]` 문구를 Actions 로그에 남긴다. 저장소가 공개라 rclone 같은 외부 도구 출력은 옮기지 않으며 서버에서 `journalctl -u happygallery-backup.service`로 본다.

등록 정보가 틀리거나 서버·R2·GHCR에 연결할 수 없으면 Actions가 실패하며 원인을 로그에 남긴다. 배포 시작 전 실패는 기존 앱을 교체하지 않는다. 배포 도중 실패는 상태를 확인한 뒤 복구한다. DB를 되돌리는 자동 rollback은 실행하지 않는다. GitHub Actions 실패 알림을 켜 두며, 복구가 끝나면 **Production을 main 기준으로 새로 실행**한다. 예전 실행의 commit이 main 최신과 다르면 서버가 거부한다.

운영 배포는 동시에 하나만 실행하며 진행 중인 배포를 다음 push가 취소하지 않는다. 수동 빌드·배포는 기존 `/opt/happygallery`에서 계속 가능하다. CD source는 `~/.local/state/happygallery/cd/source`, release 기록은 수동 배포와 같은 `~/.local/state/happygallery/releases`다.

### `다른 CD가 15분 넘게 실행 중`으로 중단된 경우

SSH 진입점은 잠금이 잡혀 있으면 바로 실패하지 않고 최대 15분 기다린다. 그래도 풀리지 않으면 중단한다. 잠금 파일이 존재하는 것과 잠금을 보유한 프로세스가 있는 것은 다르다. `.lock` 파일을 삭제하면 기존 잠금을 우회해 동시 배포가 실행될 수 있으므로 삭제하지 않는다. 서버에서 다음을 확인한다.

```bash
sudo lslocks --notruncate -o COMMAND,PID,TYPE,MODE,PATH
ps -eo pid,ppid,lstart,args | grep -E 'accept-cd|happygallery-cd-ssh|deploy-registry|deploy.sh|port-forward'
```

실제 배포가 진행 중이면 종료를 기다린다. 이전 배포가 끝났고 검증용 `port-forward`만 남았다는 것이 확인되면 해당 PID만 종료하고 잠금 해제를 확인한 뒤 Production을 재실행한다. PID를 확인하지 않은 일괄 종료나 강제 잠금 해제는 하지 않는다.
`verify.sh`는 포트 전달을 실제 명령 PID로 시작하고 종료 시 회수한다. 두 포트 전달 프로세스에는 CD·배포 잠금 FD 9·8을 전달하지 않는다. 이 수정은 앞으로 실행할 검증에 적용되며 이미 남은 프로세스를 자동 종료하지 않는다.

이 파이프라인은 앱 배포를 자동화한다. root 소유 SSH 진입점·sudoers·systemd unit·k3s 업그레이드는 파일별 운영 절차로 갱신한다. 특히 `/opt/happygallery`의 백업 스크립트는 CI worktree가 갱신돼도 바뀌지 않으므로 해당 스크립트를 변경한 릴리스에서는 호스트 측 갱신도 수행한다.

2026-10-06 변경(잠금 대기·백업 journal 읽기)은 `accept-cd-ssh.sh`와 `cd-sudoers.example`을 바꿨다. 서버의 `/opt/happygallery`를 이 커밋으로 갱신한 뒤 [2. 서버에 배포 진입점 설치](#2-서버에-배포-진입점-설치)의 `install` 명령을 다시 실행한다. 갱신 전에도 배포는 동작하며, 잠금은 기존처럼 바로 실패하고 백업 실패 로그에는 journal 권한 안내만 남는다.

## 로컬 검증

```bash
bash deploy/k3s/scripts/validate.sh
actionlint
```

로컬 테스트는 잘못된 SSH 명령·오래된 commit·이미지 불일치·전송 실패의 중단과 검증된 이미지 전달, R2 백업 다운로드·손상 거부를 검사한다. GitHub의 실제 토큰 권한, 공유기 SSH 접근, 운영 rollout 성공은 최초 Production 실행으로 별도 확인한다.

### 검증된 백업의 로컬 전달

매 배포의 R2 업로드는 원격 파일 내용을 다시 읽어 로컬과 비교한다. 이 검증을 마친 묶음을 배포 캐시에 전달하면 약 1GB의 복구 이미지를 다시 다운로드하지 않는다. 원격 목록의 최신 백업 시각 확인과 로컬 전체 SHA-256 검사는 유지한다.

호스트의 `/opt/happygallery`에 `rclone-backup.sh`, `cache-verified-backup.sh`와 관련 변경을 반영한 뒤 root 소유 `/etc/happygallery/backup.env`에 다음을 추가한다. 사용자·경로는 실제 CD 설정과 맞춘다.

```dotenv
RCLONE_VERIFIED_CACHE_USER=ronaldo
RCLONE_VERIFIED_CACHE_DIR=/home/ronaldo/.local/state/happygallery/cd/backups
```

root는 백업 원본만 읽고 `runuser`로 배포 계정에 전달한다. 파일 추출·검증·캐시 게시는 배포 계정 권한으로 실행하며 원본 백업의 600 권한을 바꾸지 않는다. 원격 검증 실패 시 캐시를 만들지 않고, 전달·해시 검증 실패도 백업 실패로 처리한다. 임시 디렉터리에서 완성한 뒤 게시하며 원격 경로가 다른 캐시는 거절한다. 설정을 생략하면 기존 다운로드 방식을 유지한다. 캐시는 기존 정책대로 두 세대를 보관한다.

Browser Smoke는 Vite 공통 의존성을 시작 시 미리 최적화해 첫 화면 로딩 중 `504 Outdated Optimize Dep`로 hydration이 중단되는 일을 막는다. 진단은 재시도 성공을 포함해 항상 보관한다. 전체 테스트 통과 여부와 별개로 flaky 결과와 최초 실패 trace를 확인한다.

`ssr-root-document.spec.ts`(@smoke)는 운영 `verify.sh`와 같은 홈 SSR 조건(H1의 "해피갤러리", 대표 canonical, CSP Report-Only nonce)을 PR에서 확인한다. 화면 개편이 이 조건을 깨면 롤아웃 뒤가 아니라 PR에서 실패한다. 두 곳의 H1 정규식이 같은지는 `validate.sh`가 검사한다.


## Trivy 실패 후 보안 업데이트 PR

운영 이미지 검사 결과는 `production-security-reports` artifact에 JSON으로 7일 보관한다. 두 이미지 검사는 독립적으로 실행하되 어느 한쪽이라도 실패하거나 실행되지 않으면 게시와 rollout을 차단한다. 실패 후 별도 최소 권한 job이 수정 가능한 의존성을 판정한다.

현재 자동 수정 범위는 `build.gradle`에서 관리하는 Jackson 2·3 BOM, Tomcat, Netty, HttpCore5, Spring Framework(`org.springframework:` 모듈만, Boot·Security 제외)다. Trivy의 설치 버전이 현재 선언과 같고 같은 major/minor 계열의 더 높은 패치 수정판이 있을 때만 올린다. 복수 취약점은 필요한 패치 중 높은 버전으로 맞춘다. OS·npm·미등록 라이브러리, 수정판 없음, 계열 전환, 버전 불일치는 실행 요약과 PR에 별도 처리 사유를 남긴다. 기존 Dependabot 주간 업데이트는 유지한다. 이 자동화는 모든 취약점의 해결을 보장하지 않는다.

브랜치는 `codex/work-security-<실패 SHA 앞 12자리>`, PR 대상은 실패한 운영 소스와 같은 `main`이다. 같은 SHA 재실행은 기존 브랜치·PR을 재사용하고, main이 이미 바뀌었으면 오래된 보고서로 PR을 만들지 않는다. 자동 병합·재배포는 하지 않는다. 이는 운영 보안 패치용 경로이며 일반 기능 작업은 기존 codexReview 경로를 따른다.

봇 PR의 이벤트 실행 정책과 관계없이 `gh workflow run ci.yml --ref <보안 브랜치> -f production_candidate=true`로 CI를 직접 실행한다. 기존 테스트·smoke와 함께 같은 JAR로 두 운영 이미지를 한 번만 빌드·검사한다. 중복 실행하던 `security-update-validation.yml`은 제거했다. 이미지 게시나 서버 접속 권한은 없다. 검토자는 해당 브랜치 최신 commit의 `CI Gate` 성공을 확인하고 병합한다. 이후 기존 Production 흐름으로 다시 검증·배포한다.

`security-scan.yml`은 매일 03:30(KST) main을 `ci.yml`(`production_candidate=true`)로 다시 검증한다. 코드가 그대로여도 새로 공개된 이미지 취약점·npm 권고·날짜에 따라 깨지는 테스트를 배포 전에 알 수 있다. 이미지 검사가 실패하면 `container-security-reports`로 위와 같은 보안 PR을 만들고, npm·테스트 실패는 실패한 실행과 GitHub 알림으로 확인한다. PR 생성은 운영 배포와 같은 `open-security-update-pr.sh`를 쓴다.

저장소 Actions 설정에서 GitHub Actions의 PR 생성 허용이 필요하다. 별도 PAT는 사용하지 않으며 업데이트 job에만 contents/pull-requests/actions 쓰기 권한을 부여한다. PR 생성·검증 실행 API가 거절되면 해당 job을 실패로 남긴다. 최초 사용 전 `ci.yml`의 workflow_dispatch 지원이 main에 반영돼 있어야 한다.
