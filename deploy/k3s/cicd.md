# main 자동 배포

`main` 병합 → 기존 CI → 운영 이미지 빌드·취약점 검사 → GHCR 게시 → SSH → 최근 R2 백업 검증 → 이미지 반입 → 롤링 배포·공개 경로 확인 순서다. 서버에서 `IMAGE_TAG`나 digest를 입력하지 않는다.

PR은 기존 CI를 실행한다. `.github/workflows/production.yml`은 main push 또는 main의 수동 실행에서만 동작하며 `CD_ENABLED=true`일 때 게시·배포한다. `ci.yml`을 재사용하므로 main에서도 백엔드·프런트·브라우저 검사를 통과해야 한다. 운영 이미지를 검사할 때는 CI용 이미지를 중복 빌드하지 않는다.

GitHub의 일반 호스팅 러너는 [공개 저장소에서 무료](https://docs.github.com/en/billing/concepts/product-billing/github-actions)다. GHCR 이미지 저장·전송도 [현재 무료](https://docs.github.com/en/billing/concepts/product-billing/github-packages)다. Actions 로그·artifact 할당량과 GHCR 향후 정책 변경은 별도다. 별도 CI 서버나 Argo CD는 설치하지 않는다.

## 1. 최초 전환 확인

1. 현재 준비 중인 롤링 배포 지원 이미지를 배포하고 `verify.sh`를 통과한다.
2. 온라인 백업을 한 번 실행해 서비스가 유지되는지, R2 업로드가 완료되는지 확인한 뒤 기존 백업 timer를 다시 켠다.
3. 서버에서 적용했던 운영 패치와 이번 CI/CD 코드를 PR로 main에 반영한다. 이 문서 작성만으로 push나 GitHub 설정 변경은 수행되지 않는다.

서버의 `/opt/happygallery`에는 수동 패치 커밋이 남아 있을 수 있다. `reset --hard`로 지우지 않는다. CD는 같은 Git 이력을 공유하는 별도 worktree를 사용한다. 기존 release의 커밋을 찾을 수 없거나 새 main에 DB migration·API 계약·세션·런타임 설정 변경이 있으면 [롤링 호환성 검사](rolling-deployments.md)가 배포를 차단한다. 이때는 해당 변경의 호환성을 먼저 처리하며 검사를 우회하지 않는다.

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

이 설정은 비대화형 배포에 필요한 k3s 관리와 백업 timer 시작·중지를 허용한다. `ronaldo`는 기존 Docker 권한을 포함해 운영 서버를 관리하는 신뢰 계정이며, k3s 권한도 클러스터 관리자 수준이다. 배포 키에는 다음 단계에서 실행 명령·포트 전달 제한을 추가한다.

R2 검증 설정을 별도로 만든다. `rclone.conf`는 기존 파일을 사용하며 `ronaldo`가 읽을 수 있어야 한다. 비밀값을 화면에 출력할 필요는 없다.

```bash
install -m 600 deploy/k3s/examples/cd-backup.env.example /etc/happygallery/cd-backup.env
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

위 설정과 최초 온라인 백업 확인이 끝나면 `CD_ENABLED=true`로 바꾸고 **Actions → Production → Run workflow → main**을 실행한다. 이후 main 병합마다 자동 실행된다. 보호된 main 브랜치와 기존 PR 필수 검사를 유지한다.

1. `validate`: 기존 테스트·E2E가 통과한다.
2. `publish`: 실제 운영 설정으로 빌드한 두 이미지를 HIGH/CRITICAL 취약점 검사 후 GHCR에 올린다.
3. `deploy`: SSH 호스트 키, 최신 main, 백업, 이미지 OS·CPU·commit·source·digest를 확인한다. containerd 반입 후 실제 digest로 release.env를 갱신한다.
4. 기존 `deploy.sh`가 백업과 배포의 충돌을 막고 롤링 배포·공개 경로 확인을 수행한다. 준비되지 않은 새 앱으로 트래픽을 넘기지 않는다.

등록 정보가 틀리거나 서버·R2·GHCR에 연결할 수 없으면 Actions가 실패하며 원인을 로그에 남긴다. 배포 시작 전 실패는 기존 앱을 교체하지 않는다. 배포 도중 실패는 상태를 확인한 뒤 복구한다. DB를 되돌리는 자동 rollback은 실행하지 않는다. GitHub Actions 실패 알림을 켜 두며, 복구가 끝나면 **Production을 main 기준으로 새로 실행**한다. 예전 실행의 commit이 main 최신과 다르면 서버가 거부한다.

운영 배포는 동시에 하나만 실행하며 진행 중인 배포를 다음 push가 취소하지 않는다. 수동 빌드·배포는 기존 `/opt/happygallery`에서 계속 가능하다. CD source는 `~/.local/state/happygallery/cd/source`, release 기록은 수동 배포와 같은 `~/.local/state/happygallery/releases`다.

이 파이프라인은 앱 배포를 자동화한다. root 소유 SSH 진입점·sudoers·systemd unit·k3s 업그레이드는 파일별 운영 절차로 갱신한다. 특히 `/opt/happygallery`의 백업 스크립트는 CI worktree가 갱신돼도 바뀌지 않으므로 해당 스크립트를 변경한 릴리스에서는 호스트 측 갱신도 수행한다.

## 로컬 검증

```bash
ruby deploy/k3s/scripts/tests/cd-test.rb
ruby deploy/k3s/scripts/tests/rclone-backup-test.rb
bash deploy/k3s/scripts/validate.sh
actionlint -shellcheck='' .github/workflows/ci.yml .github/workflows/production.yml
```

로컬 테스트는 잘못된 SSH 명령·오래된 commit·이미지 불일치·전송 실패의 중단과 검증된 이미지 전달, R2 백업 다운로드·손상 거부를 검사한다. GitHub의 실제 토큰 권한, 공유기 SSH 접근, 운영 rollout 성공은 최초 Production 실행으로 별도 확인한다.
