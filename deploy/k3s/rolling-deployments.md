# 앱 교체 중 요청을 유지하는 롤링 배포

단일 노트북의 app/frontend 교체는 새 Pod를 먼저 기동하고 준비가 끝난 뒤 기존 Pod를 종료한다. Kubernetes의 [RollingUpdate](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)에서 `maxUnavailable: 0`, `maxSurge: 1`, `minReadySeconds: 10`을 사용한다. 준비 실패나 자원 부족 때 기존 Pod를 먼저 축소하지 않는다. 노트북·Wi-Fi·DB·Redis 자체 장애나 종료 유예보다 긴 요청까지 보호하는 고가용성 구성은 아니다.

## 실행 순서와 전제

서버 `/opt/happygallery`에서 실행한다. 작업 트리는 깨끗해야 하고, 기존 release manifest와 이미지 SHA/digest, 48시간 이내 검증한 외부 복구 묶음이 있어야 한다. 운영 Toss client key를 기존 안내대로 `VITE_TOSS_CLIENT_KEY`에 설정한다. Secret 값은 출력하거나 이 문서에 기록하지 않는다.

```bash
cd /opt/happygallery
./deploy/k3s/scripts/deploy.sh /etc/happygallery/release.env
```

이 명령은 **빌드 → 취약점 검사 → k3s import → 이미지 설정 자동 갱신 → 롤링 배포 → 공개 경로 검증**을 수행한다. 이미지를 아직 빌드하지 않았다면 `--imported`를 사용하지 않는다. 백업 타이머는 기존 활성 상태만 복구하므로 온라인 백업 전환 중 꺼 둔 타이머는 계속 꺼져 있다.

1. 현재 release와 실제 실행 이미지가 같고 노드가 하나인지 확인한다. 새·이전 commit 사이의 Flyway, OpenAPI, 세션 보안 코드, 공통/운영 설정, Gradle 기반 설정 변경을 거부한다. MySQL·Redis·모니터링 spec과 app-config 변경도 거부한다. 이는 자동 호환성 판정이 아닌 보수적인 차단이며, DTO 의미나 업무 데이터 의미의 호환성은 코드 리뷰가 필요하다.
2. 이전 frontend에서 파일을 받아 새 파일과 함께 `frontend-assets` PVC에 게시한다. 같은 이름의 다른 내용은 거부하며 파일을 덮어쓰거나 이전 파일을 삭제하지 않는다. 전용 정적 서버가 준비되고 `/assets/happygallery-asset-store-v1.txt`가 `shared-assets-v1`을 반환한 뒤 앱 교체로 진행한다.
3. 미디어 PVC의 `.deployment-in-progress/owner`로 새 정기 배치 시작을 잠시 보류한다. 새 app 이미지의 `/app/rolling-deployment-v1` 지원 표시를 확인한다. 이 파일 잠금 방식은 같은 노드의 파일시스템 공유가 전제다.
4. 새 app/frontend가 준비되면 기존 Pod를 종료한다. app readiness에는 DB·Redis 상태가 포함된다. `preStop` 10초 후 [Spring graceful shutdown](https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html)이 처리 중인 요청을 최대 30초 기다린다. 최초 전환의 구 Pod는 원래 배포된 종료 설정을 사용한다.
5. 구 app Pod의 삭제까지 확인하고 배포 검증을 마친 뒤 배치 보호를 해제한다. 다음 cron부터 재개하며 누락된 시각의 cron을 소급 실행하지 않는다. 알림·재시도 지연을 확인하고 일/시간 단위 배치가 걸친 경우 필요한 작업만 관리자 실행으로 보완한다.

공통 Redis를 통해 세션을 공유한다. 배포 도중 Redis 비밀번호·세션 직렬화·암호화 키를 함께 바꾸지 않는다. 첫 롤링 전환에도 기존에 진행 중이던 작업을 강제로 무한히 기다리지는 않으므로 장시간 배치와 겹치지 않는 시각을 선택한다. 블루그린도 공유 DB·세션·파일 호환성 조건은 동일하며, 이 노트북에서는 추가 app 한 개로 교체하는 롤링 방식을 기본으로 한다.

## 실제 요청 연속성 확인

맥의 별도 터미널에서 아래 명령을 실행해 놓고 서버에서 배포한다. HTTPS 페이지 응답과 API는 기존 `verify.sh`로 확인하며, 여기서는 배포 전·중·후의 HTTP 실패 유무를 관찰한다. 완료 후 Ctrl+C로 끝낸다.

```bash
while true; do
  date '+%H:%M:%S'
  for path in /healthz /api/v1/products; do
    curl -sS --connect-timeout 3 --max-time 5 -o /dev/null \
      -w "$path HTTP %{http_code} / %{time_total}s\n" "https://happy-gallery.com$path"
  done
  sleep 1
done
```

다른 창에서는 `sudo k3s kubectl -n happygallery get pods -w`로 구·신 Pod가 겹쳐 실행되는지 확인한다. 로그인한 브라우저를 새로고침하고 배포 전에 열어 둔 페이지의 화면 이동·JS 요청도 검사한다. HTTP 관찰 몇 분의 성공을 모든 장애에 대한 무중단 보장으로 표현하지 않는다.

## 실패와 복구

새 앱이 준비되지 않으면 기존 Pod를 강제로 지우거나 `scale ... --replicas=0`으로 바꾸지 않는다. 로그·이벤트·메모리·readiness부터 확인한다. 구버전과 신버전이 남은 실패에서는 배치 표식을 유지하며 배포 기록 `current`도 바꾸지 않는다. 자동 DB rollback은 하지 않는다.

적용 후 실패했다면 후보 디렉터리의 `metadata.env`, `manifests.yaml`, `previous-app-pods.txt`와 실제 이미지·Pod를 비교한다. 이미지를 현재 기록으로 되돌리거나 후보 버전을 완성하는 결정을 먼저 한다. 기존/신규 배치가 함께 실행되지 않도록 **이전 Pod가 모두 종료되고 목표 app 하나가 Ready인 상태에서만** 소유 표식을 정리한다. 서버에서 다음 읽기 명령으로 확인한다.

```bash
sudo k3s kubectl -n happygallery get pods -l app.kubernetes.io/name=app -o wide
sudo k3s kubectl -n happygallery exec deployment/app -- \
  cat /var/lib/happygallery/media/.deployment-in-progress/owner
```

실제 미디어 마운트는 manifest의 `/var/lib/happygallery/media`를 사용한다. 소유자와 종료 상태를 확인한 뒤 해당 표식의 `owner` 파일과 빈 디렉터리만 제거한다. 성공한 목표 manifest로 공개 검증을 마치고 `releases/current`와 `release.env`를 맞춘다. 백업 타이머도 검증 후 재개한다. SIGKILL/전원 장애의 잔여 표식도 같은 절차를 따른다.

정적 저장소는 현재·이전 이미지에서 다시 만들 수 있는 캐시로 DB/사용자 미디어 백업 대상에 포함하지 않는다. 새 디스크 복구에서는 initContainer가 해당 release의 파일을 게시한다. PVC는 2Gi 요청이지만 local-path의 실제 디스크 사용량 제한은 아니므로 호스트 디스크와 누적 용량을 확인한다. 과거 asset 자동 삭제는 구현하지 않았다. 이미지 보존 기간과 오래 열린 탭 지원 기간을 결정한 뒤 별도 정리 정책을 적용한다.

DB 구조가 바뀌는 release는 호환 컬럼 추가 → 새 코드 전환 → 구 코드 제거 후 제약/컬럼 정리처럼 expand/contract를 설계한다. 차단 검사를 끄거나 `Recreate`로 자동 전환해서 진행하지 않는다.
