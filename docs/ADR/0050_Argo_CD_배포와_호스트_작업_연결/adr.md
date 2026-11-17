# ADR-0050 Argo CD 배포와 호스트 작업 연결

- 상태는 채택이다. 실제 운영 전환은 설치·첫 동기화 검증 후 기록한다.
- 결정일은 2026-11-16이다.

## 배경

운영은 단일 노트북 k3s다. 기존 GitHub Actions Production이 CI와 이미지 게시 후 SSH로 호스트 배포 스크립트를 호출한다. 이 스크립트는 백업·R2 복구 묶음·실행 이미지·소스 호환성을 확인하고, 정적 파일을 보존하고 배치를 일시정지한 뒤 manifest를 적용한다. 배포 전후 작업에는 호스트의 systemd·복구 파일·containerd와 기존 release 이력이 필요하다.

사용자는 GitHub Actions에 CI를 두고 CD를 Argo CD로 전환하기로 했다. 기존 복구·호환성·롤링 배포 조건을 함께 보존한다.

## 결정

1. Actions가 기존 CI·이미지 검사·GHCR 게시 후 검증한 source SHA와 app/frontend digest를 `gitops-production`의 단일 commit으로 승격한다. main 최신 소스만 승격하며 브랜치의 fast-forward push로 게시 경합을 감지한다. 배포 소스 commit을 부모로 보존해 해당 소스 이력을 함께 가져올 수 있다.
2. Argo CD가 `production/*.yaml`을 읽어 공통 리소스 → app → frontend 순서로 적용한다. 전용 AppProject로 저장소·namespace·리소스 종류를 제한한다. 자동 동기화와 self-heal을 사용하고 자동 prune은 끈다. 영속 데이터 관련 리소스에는 삭제 방지 annotation을 둔다.
3. PreSync·PostSync·SyncFail Job이 검증한 frontend 이미지의 Node를 사용해 로컬 Unix socket으로 호스트 실행기에 요청한다. 호스트 실행기는 제한된 필드만 받아 Git의 게시 기록과 대조한다. 외부 배포용 SSH 연결과 Actions의 배포 키는 이 경로에서 필요하지 않다.
4. 호스트는 기존 운영 계정의 k3s·백업 권한과 release 상태 경로를 사용한다. 기존 스크립트를 재사용해 준비와 완료를 수행하고, Argo CD가 본 manifest와 서버가 렌더링한 manifest의 의미상 해시를 비교한다. workload와 helper가 GHCR digest를 직접 참조하고 IfNotPresent로 kubelet의 CRI 이미지 처리를 거친다. containerd의 registry digest 유지와 복구 archive 보존도 함께 검사한다.
5. 준비 상태와 요청을 호스트에 저장한다. 재시도는 같은 release를 이어받으며 혼합 상태에서는 배치 일시정지를 유지한다. 완료 응답 유실도 공개 경로 검사를 재수행해 확인한다. 다른 release는 진행 중인 상태를 덮지 않는다.
6. CI 사전 검사는 마지막 승격 소스와 비교한다. 실제 운영 기준의 전체 누적 호환성 검사는 PreSync에서 current manifest·실행 이미지로 수행한다. 첫 GitOps 승격 전에는 기존 SSH 배포 성공 이력을 CI 기준으로 사용한다.

## 결과와 운영 조건

- Git의 희망 상태와 실제 리소스 차이를 Argo CD에서 확인하고 복구한다. Actions 성공과 실제 배포 성공을 각 시스템에서 구분해 추적한다.
- 호스트 백업과 정적 파일·배치 보호 로직을 재사용한다. Unix socket·systemd 실행기와 배포 단계 상태가 추가 운영 대상이 된다.
- socket 마운트 권한은 배포 요청 권한이다. 훅에는 read-only 전용 hostPath만 제공하며 runtime socket·호스트 root·Kubernetes API token은 제공하지 않는다.
- 전체 sync가 훅 실행 조건이다. selective sync는 운영 배포 경로에서 사용하지 않는다. 수동 배포·복원·키 회전 전에는 자동 동기화를 중지하고 Git 희망 상태를 대조한 뒤 재개한다.
- 단일 노트북의 자원과 장애 범위는 ADR-0037을 따른다. Argo CD 설치 전 자원 여유와 Kubernetes 지원 버전을 확인한다.
- 설치·전환·실패 복구·검증 명령은 [배포 실행 안내](../../../deploy/k3s/cicd.md)에 둔다.

## 참고

- [Argo CD 자동 동기화](https://argo-cd.readthedocs.io/en/stable/user-guide/auto_sync/)
- [Argo CD sync 훅과 wave](https://argo-cd.readthedocs.io/en/stable/user-guide/sync-waves/)
