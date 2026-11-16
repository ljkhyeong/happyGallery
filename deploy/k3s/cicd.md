# GitHub Actions CI와 Argo CD 배포

`main` → 호환성·기존 CI → 운영 이미지 빌드·취약점 검사 → GHCR 게시 → `gitops-production` 브랜치 갱신 → Argo CD 동기화 순서다. GitHub Actions는 검증한 이미지와 희망 상태를 게시한다. 실제 배포 결과는 Argo CD의 `Synced`·`Healthy`와 마지막 `PostSync` 성공으로 확인한다.

| 단계 | 담당과 통과 조건 |
| --- | --- |
| 소스 호환성 | PR 대상 또는 마지막 GitOps 승격 소스와 API·migration·설정 비교 |
| CI | 기존 백엔드·웹 계약·프런트·브라우저 smoke·운영 구성 검사 |
| 이미지 | 동일 실행에서 만든 이미지를 취약점 검사 후 재빌드 없이 GHCR에 게시 |
| GitOps 승격 | 최신 main만 app/frontend digest·manifest·훅을 같은 commit으로 `gitops-production`에 게시 |
| PreSync | 실제 운영 manifest 기준 호환성, 새 백업·R2 복구 묶음, 이미지 출처·digest, 정적 파일 보존, 배치 일시정지 |
| Sync | Argo CD가 공통 리소스 → app → frontend 순서로 적용하고 health 확인 |
| PostSync | 실제 이미지·구 Pod 종료·온라인 백업 표식·인증서·공개 경로 확인, current 기록과 배치 재개 |
| SyncFail | 진행 중 release와 혼합 상태의 배치 일시정지 표식을 보존 |

## 1. 운영 전환 준비

현재 온라인 백업과 수동 배포가 정상인 기존 단일 노드 k3s에 전환한다. `rolling-deployments.md`의 스키마 초기화 등 미완료 전환이 있으면 먼저 해당 절차를 마친다. Argo CD의 PreSync에도 기존 호환성 검사가 적용된다.

1. GitHub 저장소 변수 `CD_ENABLED=false`로 새 이미지 게시·승격을 잠시 멈춘다. 이미 실행 중인 Production과 기존 SSH 배포가 끝났는지 확인한다.
2. 이 변경을 반영한 서버 checkout과 호스트 실행기를 설치한다. 원격 반영은 저장소의 푸시·병합 절차를 따른다.
3. 기존 `/etc/happygallery/release.env`의 `ACME_EMAIL`을 GitHub `production` environment 변수 `PRODUCTION_ACME_EMAIL`에 등록한다. 값이 다르면 manifest 비교에서 적용 전에 중단한다.
4. 아래 Argo CD 설치와 실행기 설치를 마친다. 기존 app/frontend와 runtime Secret·PVC·TLS는 준비돼 있어야 한다.
5. main 반영 후 `CD_ENABLED=true`로 설정하고 Production을 실행해 첫 배포 브랜치를 만든다. Application을 등록해 자동 동기화를 시작한다.
6. Argo CD의 PreSync·Sync·PostSync 성공과 `Synced`·`Healthy`, 실제 HTTPS 경로를 확인한다. Actions 성공은 GitOps 승격 완료를 의미한다.
7. 전환 성공 후 GitHub의 `CD_SSH_PRIVATE_KEY`·`CD_SSH_KNOWN_HOSTS`·`CD_HOST`·`CD_PORT`·`CD_USER`와 서버의 기존 배포 전용 authorized_keys 행을 정리한다.

운영 Secret 원문은 Git에 넣지 않는다. `production` environment는 main만 허용한다. Actions 기본 권한은 읽기이며 GitOps 승격 job에만 `contents:write`, 이미지 게시 job에만 `packages:write`를 부여한다. 브랜치 보호로 main의 PR·CI Gate를 요구하고 `gitops-production`은 배포 실행 권한이 있는 운영자와 게시 workflow만 변경한다.

## 2. Argo CD 설치

공식 non-HA 설치 manifest의 확인한 release를 사용한다. 아래는 전환 작성 시 확인한 `v3.6.1`이다. 노트북 자원 여유와 k3s 버전의 지원 범위를 확인하고 설치한다. 관리 UI는 ClusterIP와 port-forward로 접근한다.

```bash
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl apply --server-side -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/v3.6.1/manifests/install.yaml
kubectl -n argocd rollout status deployment/argocd-repo-server --timeout=5m
kubectl -n argocd rollout status deployment/argocd-server --timeout=5m
kubectl -n argocd rollout status statefulset/argocd-application-controller --timeout=5m
kubectl -n argocd port-forward service/argocd-server 8443:443
```

첫 배포 브랜치가 만들어진 뒤 Application과 전용 AppProject를 등록한다.

```bash
kubectl apply -f deploy/k3s/argocd/application.yaml
kubectl -n argocd get application happygallery
kubectl -n happygallery get jobs,pods
```

Application은 `gitops-production`의 `production/*.yaml`만 읽는다. 이미지·소스 정보인 `release.json`은 Kubernetes 리소스로 읽지 않는다. `prune=false`·`allowEmpty=false`로 시작하고 `selfHeal=true`로 Git과의 차이를 복구한다. Namespace·StorageClass·PVC·MySQL StatefulSet·ClusterIssuer는 삭제 방지 annotation도 갖는다. 리소스 제거가 필요하면 Git diff와 영속 데이터 영향을 확인해 별도 운영 작업으로 처리한다.

## 3. 호스트 실행기 설치

백업·이미지 반입·정적 파일 보존·배치 잠금은 기존 호스트 스크립트를 재사용한다. 훅 Job은 검증된 frontend 이미지의 Node로 `/run/happygallery-gitops/control.sock`에 제한된 JSON 요청을 보낸다. socket은 로컬 파일시스템으로만 접근한다.

아래 예시는 기존 운영 계정 `ronaldo`와 `/home/ronaldo`를 따른다. 다른 계정이면 service의 User·HOME과 기존 sudoers·백업의 release 상태 경로를 같은 계정으로 맞춘다. `cd-kubectl.sh`와 `examples/cd-sudoers.example`의 k3s·백업 실행 권한을 사용한다.

```bash
sudo install -d -m 755 /usr/local/lib/happygallery
sudo install -o root -g root -m 644 deploy/k3s/scripts/gitops-control.rb /usr/local/lib/happygallery/gitops-control.rb
sudo install -o root -g root -m 644 deploy/k3s/scripts/gitops-release.rb /usr/local/lib/happygallery/gitops-release.rb
sudo install -o root -g root -m 755 deploy/k3s/scripts/cd-kubectl.sh /usr/local/libexec/happygallery-kubectl
sudo install -o root -g root -m 644 deploy/k3s/systemd/happygallery-gitops.socket.example /etc/systemd/system/happygallery-gitops.socket
sudo install -o root -g root -m 644 deploy/k3s/systemd/happygallery-gitops@.service.example /etc/systemd/system/happygallery-gitops@.service
sudo systemctl daemon-reload
sudo systemctl enable --now happygallery-gitops.socket
sudo systemctl status happygallery-gitops.socket
```

호스트에는 기존 Docker·k3s·Git·Ruby·flock·백업 도구가 필요하다. 운영 계정은 HTTPS origin에서 소스를 받을 수 있어야 한다. 실행기는 main에 포함된 소스와 현재 GitOps 브랜치의 release가 일치할 때만 준비한다. 요청은 단계·소스 SHA·두 digest·ACME 이메일·manifest 해시로 제한하고 임의 명령은 받지 않는다.

socket mode는 UID 101인 훅 Job이 접속하도록 `0666`, 부모 디렉터리는 `0755`다. 훅은 해당 디렉터리만 read-only hostPath로 마운트하고 API token·Secret·호스트 root 권한을 받지 않는다. 이 socket에 접근할 수 있는 호스트 사용자와 Pod는 게시된 release의 배포를 요청할 수 있으므로, 이 hostPath 사용 권한은 배포 운영 권한으로 관리한다. 노드 공용 `/run`이나 container runtime socket을 마운트하지 않는다.

GitOps 이미지 반입은 `k3s ctr images pull`로 registry digest를 유지한다. Docker에서는 image label·OS·CPU·원격 digest·JAR를 검사하고, containerd에는 manifest의 localhost 별칭을 같은 digest로 등록한다. 기존 수동 Docker archive 반입 경로는 원래 동작을 유지한다.

## 4. 배포 상태와 재시도

- Actions의 `Publish GitOps desired state` 성공 뒤 실제 배포는 Argo CD에서 추적한다. 훅 실패는 Job 로그와 서버의 `~/.local/state/happygallery/cd/gitops.log`를 확인한다.
- 호스트의 `releases/gitops-active`와 `cd/gitops-request.json`은 준비 중인 release를 기록한다. 같은 요청의 재시도는 준비 상태를 이어받는다. 이미 적용된 Pod를 수동으로 이전 이미지로 바꾸지 않는다.
- 공개 경로 검사 실패 뒤에는 실행 이미지에 맞게 current가 갱신되며 완료 재시도로 검사를 다시 수행한다. 구 Pod가 남은 혼합 상태에서는 배치 일시정지를 유지한다.
- 새 revision이 이전 동기화 도중 도착하면 먼저 진행 중 release를 완료한다. 이전 요청의 PostSync가 끝나기 전에 다른 release가 그 상태를 덮지 못하게 한다.
- 훅은 전체 sync에서 실행한다. 운영 Application은 리소스를 선택한 selective sync로 배포하지 않는다. Argo CD 공식 동작상 selective sync에서는 훅이 실행되지 않는다.
- GitOps 도입 후 수동 배포·복원·이미지 rollback·Secret 회전은 먼저 Application 자동 동기화를 중지하고 진행한다. 재개 전 Git의 희망 상태를 실제 복구 상태와 일치시킨다. DB 복구는 기존 별도 절차를 따른다.

진행 중 요청을 완료해야 할 때는 먼저 Argo CD의 현재 작업을 종료하고 자동 동기화를 끈다. 준비한 이미지가 아직 적용되지 않았거나 혼합 상태라면 Application의 targetRevision을 해당 요청이 담긴 이전 GitOps commit으로 고정하고 전체 Sync를 실행한다. 같은 요청의 PreSync가 준비 상태를 이어받아 적용·완료한다. 성공 후 targetRevision을 `gitops-production`으로 복구해 다음 release를 동기화한다.

준비한 app/frontend 이미지가 이미 실행 중이면 구 Pod 종료 상태를 확인한 뒤 아래와 같이 저장된 요청의 complete 단계만 다시 실행할 수도 있다. 수동으로 진행 표식이나 배치 잠금 파일을 삭제하지 않는다.

```bash
sudo -u ronaldo ruby -rjson -e 'request = JSON.parse(File.read("/home/ronaldo/.local/state/happygallery/cd/gitops-request.json")); puts JSON.generate(request.merge("phase" => "complete"))' |
  sudo -u ronaldo env HOME=/home/ronaldo ruby /usr/local/lib/happygallery/gitops-control.rb
```

응답 `ok=true`와 실제 검증 성공을 확인하고 Application의 자동 동기화를 재개한다. 48시간이 지난 준비 상태나 적용되지 않은 서로 다른 이미지가 섞인 실패는 상태를 진단한 뒤 기존 복구 절차로 처리한다.

## 5. 호환성·검증

CI의 Rolling Compatibility는 PR 대상 또는 마지막 GitOps 승격 소스와 비교한다. 첫 전환에서 배포 브랜치가 없을 때만 기존 `Roll out production` 성공 이력을 사용한다. CI와 별도로 PreSync가 현재 운영 manifest와 실제 실행 이미지를 대조하고 전체 누적 소스 차이를 검사한다.

```bash
LANG=ko_KR.UTF-8 bash deploy/k3s/scripts/validate.sh
actionlint .github/workflows/ci.yml .github/workflows/production.yml
```

`validate.sh`에는 기존 백업·복구·롤링 검사와 GitOps 요청 검증·게시 이력·실패 후 재시도 검사가 포함된다. 실제 systemd socket 연결·GHCR 반입·Argo CD sync·백업·공개 HTTPS 경로는 운영 전환에서 추가 확인한다.

## 참고

- [Argo CD 자동 동기화](https://argo-cd.readthedocs.io/en/stable/user-guide/auto_sync/)
- [Argo CD 배포 훅과 wave](https://argo-cd.readthedocs.io/en/stable/user-guide/sync-waves/)
- [Argo CD 설치](https://argo-cd.readthedocs.io/en/stable/operator-manual/installation/)
- [Argo CD v3.6.1](https://github.com/argoproj/argo-cd/releases/tag/v3.6.1)
