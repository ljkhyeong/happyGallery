# 앱을 유지하는 온라인 백업

정기 백업은 app replica를 변경하거나 재기동하지 않는다. 백업 때문에 AppDown 경보를 숨기지도 않는다. 이 전환으로 자동 백업에 따른 계획 중단을 제거하며, 단일 노드 장애나 `Recreate` 배포 자체의 중단까지 없애는 것은 아니다.

## 일관성 조건

1. 실행 중인 app 이미지에 `/app/media-backup-guard-v1`이 있어야 한다. 새 이미지의 파일 삭제 경로가 삭제 보호를 지원한다는 배포 계약이며, 구버전 앱에서는 백업을 거부한다. 키 회전 등으로 이미 앱이 0개인 경우에는 앱을 켜지 않고 백업한다.
2. 백업 Pod가 미디어 PVC에 `.backup-in-progress/owner`를 만든다. 앱은 기존 이미지 참조 행 잠금 뒤 보호 상태를 확인해 물리적 파일 삭제를 보류한다. 고객 요청의 DB 참조 변경은 계속 처리하며 남은 파일은 기존 고아 정리로 회수한다.
3. 백업은 같은 `image_media_reference_lock` 행을 잠깐 잠근 뒤 즉시 해제한다. 보호 생성 전에 시작된 삭제가 종료됐는지 확인하는 과정이며, DB dump 동안 이 잠금을 유지하지 않는다.
4. 업무 테이블이 모두 InnoDB인지 확인하고 `mysqldump --single-transaction --quick --skip-lock-tables`로 읽는다. 일반 백업의 운영 DB에서 CHECK TABLE은 실행하지 않는다. 테이블 검사는 별도 DB로 복원한 뒤 수행한다.
5. DB 스냅샷 뒤 완성된 jpg/png/webp 파일 목록을 고정해 읽는다. 파일 이름은 UUID이고 내용은 덮어쓰지 않는 기존 계약을 따른다. 업로드 중 임시 파일, `.orphaned`, 삭제 보호 디렉터리는 보관하지 않는다. 이미지가 없으면 표준 빈 tar를 만든다.
6. 미디어 복사 후 소유한 삭제 보호만 해제한다. DB·미디어 암호화가 끝난 뒤 R2 업로드와 내용 검증을 수행한다. 스냅샷 뒤 업로드된 여분 파일이 포함될 수 있지만 스냅샷이 참조하는 파일은 보존된다. 복원 뒤 여분 파일은 고아 정리 대상이다.

MySQL의 [single-transaction 조건](https://dev.mysql.com/doc/refman/8.4/en/mysqldump.html#option_mysqldump_single-transaction)에 따라 백업 중 ALTER/CREATE/DROP/RENAME/TRUNCATE TABLE을 실행하지 않는다. 배포·스키마 변경·데이터 키 회전 전에는 타이머를 중지하고 진행 중인 백업이 끝날 때까지 기다린다. 백업은 deployment generation, Secret resourceVersion, 현재 release, Flyway 버전 변경을 발견하면 완료 marker를 게시하지 않는다. 이 확인이 수동 DDL의 동시 실행을 허용한다는 뜻은 아니다.

## 기존 중단 방식에서 전환

Ubuntu 서버에서 예약을 중지한다. 이미 진행 중인 백업 서비스는 이 명령으로 중단되지 않는다. 기존 백업이 실행 중이면 앱 원복과 백업 종료를 확인한 뒤 파일을 교체한다.

```bash
sudo systemctl disable --now happygallery-backup.timer
sudo systemctl show happygallery-backup.service -p ActiveState -p SubState
```

패치를 적용하고 기존 [이미지 빌드·배포 절차](README.md)로 새 app을 배포한다. 이 한 번의 `Recreate` 배포에는 기존 배포 중단이 발생한다. 배포한 image tag/digest와 `releases/current` 기록을 먼저 맞춘다. 실행 중인 앱의 지원 표시를 확인한다.

```bash
sudo k3s kubectl -n happygallery exec deployment/app -- test -f /app/media-backup-guard-v1
```

백업 unit도 반드시 갱신해서 과거 AppDown silence hook을 제거한다. 타이머는 아직 켜지 않는다.

```bash
cd /opt/happygallery
sudo install -m 644 deploy/k3s/systemd/happygallery-backup.service.example /etc/systemd/system/happygallery-backup.service
sudo systemctl daemon-reload
sudo systemctl start --no-block happygallery-backup.service
sudo journalctl -u happygallery-backup.service -f
```

검증은 다음 범위를 포함한다.

- 백업 전후 app Pod UID·재시작 횟수가 같고 계속 Ready인지 확인한다. 백업 중 공개 API의 주기적 요청도 성공해야 한다.
- 실제 이미지가 있는 백업으로 이미지 참조 제거·새 이미지 업로드와 백업을 함께 실행해 확인한다. DB를 별도 MySQL에 복원하고 참조 이미지 파일의 존재와 내용을 검사한다.
- `Result=success`, 성공 heartbeat 갱신, R2 다운로드·checksum, DB 복원·테이블 검사와 미디어 복원이 통과한 뒤 기존 6시간 주기 타이머를 다시 켠다.
- 새 예약 백업의 소요 시간·CPU·메모리·디스크 I/O를 측정한다. 앱을 유지해도 백업 부하로 응답이 느려질 수 있으므로 실제 요청 결과를 기준으로 조정한다.

## 실패와 삭제 보호 정리

일반 오류·종료 신호에서는 소유한 삭제 보호를 해제하고 임시 암호문과 유지보수 Pod를 정리한다. SIGKILL·전원 장애 또는 Kubernetes 통신 장애로 보호가 남으면 이미지 삭제가 계속 보류되고 다음 백업의 보호 생성도 실패한다. 보호를 시간만 보고 자동 해제하지 않는다.

서비스 프로세스와 `media-backup-*` Pod가 더 이상 백업을 수행하지 않는지 확인한 뒤, 유지보수 Pod로 PVC를 열어 `.backup-in-progress/owner`의 실행 식별자를 확인한다. 해당 실행이 끝났음을 확인한 경우에만 그 owner 파일과 빈 보호 디렉터리를 지운다. 다른 파일·이미지·PVC는 지우지 않는다. 이후 수동 백업을 다시 검증한다. 보호를 해제하지 못한 백업은 성공으로 게시하지 않는다.
