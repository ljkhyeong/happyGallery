# Cloudflare R2 암호화 백업

R2에 암호화 테스트 파일을 올리고 다시 내려받아 Mac의 age identity로 복호화한 뒤 적용한다. 테스트 통과는 전송 경로와 키의 확인이며, 실제 DB·미디어 복원 검증은 앱 배포 후 별도로 수행한다.

## 저장 방식

- `BACKUP_STORAGE=rclone`은 DB·미디어를 로컬 캐시에 age 암호화한다. 기존 외부 mount marker는 만들지 않는다.
- DB와 미디어를 만드는 동안만 app 쓰기를 중단한다. 원래 replica와 AppDown 경보를 복구한 뒤 R2 업로드를 시작한다.
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

## 2. 첫 실제 백업과 예약 실행

앱·MySQL·미디어 PVC가 준비되고 rollout의 `releases/current`가 생성된 뒤 진행한다. 앱이 아직 배포되지 않았다면 설정 준비까지만 마친다. 첫 실행은 데이터 일관성을 위해 앱을 잠시 중단하므로 운영 개통 전에 백업·재기동 시간을 측정한다.

```bash
sudo install -m 644 deploy/k3s/systemd/happygallery-backup.service.example /etc/systemd/system/happygallery-backup.service
sudo install -m 644 deploy/k3s/systemd/happygallery-backup.timer.example /etc/systemd/system/happygallery-backup.timer
sudo install -m 644 deploy/k3s/systemd/happygallery-backup-watchdog.service.example /etc/systemd/system/happygallery-backup-watchdog.service
sudo install -m 644 deploy/k3s/systemd/happygallery-backup-watchdog.timer.example /etc/systemd/system/happygallery-backup-watchdog.timer
sudo systemctl daemon-reload
sudo systemctl start --no-block happygallery-backup.service
sudo journalctl -u happygallery-backup.service -f
```

로그 보기를 끝낼 때 `Ctrl+C`를 누른다. 백업 service는 계속 실행된다. 이후 결과를 확인한다.

```bash
sudo systemctl show happygallery-backup.service -p ActiveState -p Result -p ExecMainStatus
sudo stat /var/lib/happygallery/backup.last-success
```

`ActiveState=inactive`, `Result=success`, `ExecMainStatus=0`과 이번 실행의 성공 파일 시각을 확인하고, 아래 다운로드·복원 검증까지 마친 뒤 timer를 켠다. 기본 실행 제한은 30분이며 종료 때 앱 원복에 10분 유예를 둔다. 초기 실행 이미지 archive가 크면 제한에 걸릴 수 있으므로 로그와 전송량을 먼저 확인한다.

```bash
sudo systemctl enable --now happygallery-backup.timer happygallery-backup-watchdog.timer
systemctl list-timers happygallery-backup.timer happygallery-backup-watchdog.timer
```

매일 한국 시간 00:30, 06:30, 12:30, 18:30에 실행된다. 기본 보존 기간은 30일이다. 마지막 성공이 7시간 넘게 갱신되지 않으면 기존 watchdog이 경고한다. 서버 전체가 꺼진 장애는 외부 감시로 확인해야 한다.

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
