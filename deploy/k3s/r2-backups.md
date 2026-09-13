# Cloudflare R2 암호화 백업

R2에 암호화 테스트 파일을 올리고 다시 내려받아 Mac의 age identity로 복호화한 뒤 적용한다. 테스트 통과는 전송 경로와 키의 확인이며, 실제 DB·미디어 복원 검증은 앱 배포 후 별도로 수행한다.

## 저장 방식

- `BACKUP_STORAGE=rclone`은 DB·미디어를 로컬 캐시에 age 암호화한다. 기존 외부 mount marker는 만들지 않는다.
- 앱을 계속 실행하면서 DB 트랜잭션 스냅샷과 미디어를 보관한다. 백업 중에는 이미지 파일의 실제 삭제만 보류하고, 복사가 끝나면 보호를 해제한 뒤 R2에 업로드한다. AppDown 경보는 숨기지 않는다.
- 호환 실행 이미지·release manifest를 함께 올린다. 이미 있는 파일이 다르면 덮어쓰지 않고 실패한다.
- R2 파일 내용을 다시 읽어 로컬과 비교한 뒤 `happygallery-<UTC 시각>.recovery.env`를 마지막에 올린다. `rclone check --download`를 사용하므로 multipart ETag를 SHA-256으로 해석하지 않는다.
- 업로드·검증·보존 정리가 모두 성공해야 기존 systemd service가 `backup.last-success`를 갱신한다. 실패는 기존 장애 메일로 전달된다.
- 로컬 `backup-cache`에는 업로드에 실패한 묶음도 남을 수 있다. 배포 전 복구본 확인과 복원 훈련에는 **R2에서 다시 내려받아 검증한 디렉터리**를 사용한다.

DB·미디어에는 고객 데이터가 있어 age로 암호화한다. release 실행 이미지·manifest·fingerprint metadata는 비공개 버킷에 보관하며 별도로 age 암호화하지 않는다. `app.env`, `rclone.conf`, age identity는 이 전송에 포함하지 않는다. 애플리케이션 암호화 키를 포함한 설정 복구본도 서버 밖에 별도로 보관해야 한다.

## 1. 서버 설정 준비

Ubuntu의 `/opt/happygallery`에서 실행한다. 수동 설치한 rclone을 절대 경로로 사용해 Ubuntu 패키지의 오래된 실행 파일이 선택되지 않게 한다. 운영 확인 버전은 `v1.75.1`이다.

```bash
cd /opt/happygallery
/usr/local/bin/rclone version
sudo chmod 600 /etc/happygallery/rclone.conf
cat /etc/happygallery/backup-recipient.txt
```

기존 `backup.env`가 없다면 예시를 설치한다. 이미 있으면 해당 파일을 편집해 R2 설정을 반영한다. 장애 알림 설정은 그대로 사용한다.

```bash
sudo install -m 600 deploy/k3s/examples/backup-r2.env.example /etc/happygallery/backup.env
sudo vi /etc/happygallery/backup.env
```

`BACKUP_AGE_RECIPIENT`에 앞에서 확인한 `age1...` 공개키를 넣는다. `HAPPYGALLERY_RELEASE_DIR`은 rollout을 실행하는 사용자에 맞춘다. 현재 노트북의 사용자는 `ronaldo`이므로 `/home/ronaldo/.local/state/happygallery/releases`다. 기본 저장 범위는 `hg-r2:happygallery-backups/happygallery`이며, 테스트 파일의 `connectivity-test/`와 분리된다.

```bash
sudo bash -c '
  set -a
  . /etc/happygallery/backup.env
  set +a
  exec bash /opt/happygallery/deploy/k3s/scripts/rclone-backup.sh check
'
```

`R2 백업 경로 조회 OK`를 확인한다. 이 명령은 조회만 하며 실제 백업이나 배포를 시작하지 않는다.

## 2. 배포 시 백업 실행

앱·MySQL·미디어 PVC가 준비되고 rollout의 `releases/current`가 생성된 뒤 진행한다. 앱이 아직 배포되지 않았다면 설정 준비까지만 마친다. 온라인 백업을 지원하는 새 앱과 systemd unit을 먼저 배포한다. [온라인 백업 전환](online-backups.md)에 따라 백업 중 앱이 유지되는지와 새 복구 묶음의 복원을 검증한다.

최초 전환에서 현재 실행 중인 구버전 앱이 `/app/media-backup-guard-v1`을 제공하지 않으면, `deploy-registry-images.sh`가 먼저 검증한 48시간 이내 R2 복구 묶음을 1회 배포 백업으로 재사용한다. 새 이미지 자체 검증과 복구 묶음 무결성 검증이 모두 성공한 경우에만 이 경로를 사용하며, 새 app이 표식을 제공하지 않으면 롤아웃을 성공 처리하지 않는다. 이후 배포는 항상 배포 전 백업 service를 먼저 실행한다.

```bash
sudo install -m 644 deploy/k3s/systemd/happygallery-backup.service.example /etc/systemd/system/happygallery-backup.service
sudo systemctl daemon-reload
```

`deploy.sh`가 배포마다 `sudo systemctl start --wait happygallery-backup.service`를 실행한다. service가 `Result=success`로 끝난 뒤 `CD_BACKUP_ENV`의 R2 설정으로 최신 `recovery.env`를 내려받아 checksum과 release archive를 검증하고, 그 묶음을 사용해 rollout한다. 백업 실패·R2 조회·검증 실패는 배포 실패로 처리한다. 기본 실행 제한은 30분이며 종료 때 삭제 보호 해제와 임시 자원 정리에 10분 유예를 둔다.

배포 사용자가 R2에서 묶음을 다시 읽을 수 있게 CD 검증 설정도 준비한다.

```bash
sudo install -o ronaldo -g ronaldo -m 600 deploy/k3s/examples/cd-backup.env.example /etc/happygallery/cd-backup.env
sudo vi /etc/happygallery/cd-backup.env
```

기존 서버에 설치된 정기 timer와 heartbeat watchdog은 제거한다.

```bash
sudo systemctl disable --now happygallery-backup.timer happygallery-backup-watchdog.timer 2>/dev/null || true
sudo rm -f /etc/systemd/system/happygallery-backup.timer \
  /etc/systemd/system/happygallery-backup-watchdog.service \
  /etc/systemd/system/happygallery-backup-watchdog.timer
sudo systemctl daemon-reload
```

기본 보존 기간은 30일이다. 서버 전체가 꺼진 장애는 외부 감시로 확인해야 한다.

## 3. R2에서 복구 묶음 받기

완료된 metadata 목록을 조회한다. 로컬 캐시의 최신 파일을 복구 성공의 증거로 사용하지 않는다.

```bash
sudo /usr/local/bin/rclone --config /etc/happygallery/rclone.conf \
  lsf hg-r2:happygallery-backups/happygallery \
  --max-depth 1 --files-only --include '/happygallery-*.recovery.env'
```

받을 metadata 파일명을 아래의 예시 시각과 바꾼다. 대상 디렉터리는 아직 존재하지 않아야 한다. 필요한 모든 파일을 임시 디렉터리에 받고 SHA-256과 release 참조를 검증한 뒤 최종 디렉터리를 만든다. 이 단계는 DB를 건드리지 않는다.

```bash
sudo install -d -m 700 /var/lib/happygallery/restore-checks
sudo bash -c '
  set -a
  . /etc/happygallery/backup.env
  set +a
  exec bash /opt/happygallery/deploy/k3s/scripts/rclone-backup.sh download \
    happygallery-YYYYMMDDTHHMMSSZ.recovery.env \
    /var/lib/happygallery/restore-checks/YYYYMMDDTHHMMSSZ
'
```

다운로드 결과의 `.recovery.env`를 `VERIFIED_RECOVERY_BUNDLE`로 사용한다. 실제 복원은 [복원 훈련](README.md#8-복원-훈련)의 app 중지·키 확인·호환 이미지·대사 절차를 따른다. 운영 DB에 곧바로 복원하지 말고 별도 테스트 namespace/클러스터에서 복구를 검증한다. age 개인키는 평소 Mac 등 서버 밖에 보관한다.

## 보존 기간과 용량

보존 정리는 전용 prefix의 정해진 DB·미디어·metadata 파일명에만 적용된다. 파일명의 UTC 백업 시각으로 판단하며, 최근 완료된 복구 묶음이 없으면 마지막 백업을 지우지 않도록 실패한다. 만료된 완료 metadata부터 지우고 나머지를 정리한다. 원격 삭제가 실패한 파일의 로컬 캐시는 남긴다. `releases/<IMAGE_TAG>`는 공유되므로 자동 삭제하지 않는다.

30일은 하루 네 번의 전체 DB·미디어 백업을 모두 포함한다. 첫 실제 백업 뒤 총 저장량을 확인하고 예산에 맞춰 보존 기간을 조정한다. 스크립트는 최소 7일을 허용한다. 공유 이미지 archive, 실패한 로컬 캐시, 예전 다운로드 검증 디렉터리도 용량을 차지하므로 점검한다. 버킷 전체에 일괄 만료 규칙을 걸면 최근 백업이 참조하는 오래된 release archive까지 없어질 수 있으므로 사용하지 않는다.

```bash
sudo /usr/local/bin/rclone --config /etc/happygallery/rclone.conf \
  size hg-r2:happygallery-backups/happygallery
sudo du -sh /var/lib/happygallery/backup-cache /var/lib/happygallery/restore-checks
```

전송의 기준은 [rclone check](https://rclone.org/commands/rclone_check/)와 [Cloudflare R2 rclone 설정](https://developers.cloudflare.com/r2/examples/rclone/)을 따른다.
