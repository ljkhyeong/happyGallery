# 문자·알림톡 심사 대기 중 사전 점검

NHN 본인 인증·발신 번호·알림톡 템플릿 심사가 끝나기 전에 서버, 관리자 화면,
이메일과 백업을 준비할 때 사용한다. 실제 고객의 가입·예약·주문을 받는 운영 모드는 아니다.

## 동작

| 설정 | 문자·알림톡 | 휴대폰 인증 | 이메일 |
| --- | --- | --- | --- |
| `NOTIFICATION_MODE=nhn` 또는 키 생략 | NHN 실제 발송, 여섯 자격 증명 필수 | NHN 접수 성공한 코드만 사용 | 기존 제공자로 발송 |
| `NOTIFICATION_MODE=disabled` | 발송하지 않고 각 채널을 `PERMANENT_FAILURE`/`FAILED`로 기록 | 기존 `503 SERVICE_UNAVAILABLE`, 코드 활성화 안 함 | 기존 제공자로 발송 |

빈 값·오타·`fake`는 Secret 생성과 앱 시작에서 거부한다. 운영 profile, 관리자 MFA,
암호화, OAuth, 결제·이메일의 필수 설정은 유지한다. 인증 코드나 전화번호를 대신 출력하지 않는다.
중지 모드는 시작 로그에 경고를 남긴다. 이때 발생한 고객 알림은 최종 실패하므로
승인 후에도 자동 재발송되지 않는다. 관리자는 필요한 건만 확인해서 재시도한다.

## 최초 적용

1. 이 변경이 포함된 커밋으로 이미지를 다시 빌드·검사·import한다.
   `build-import-images.sh`가 출력한 다섯 이미지 값을 `release.env`에 반영한다.
2. **노트북 서버의 SSH 터미널**에서 `vi /etc/happygallery/app.env`를 열고
   `NOTIFICATION_MODE=disabled`를 한 줄만 넣는다. 여섯 NHN 키는 빈 상태로 둘 수 있다.
   `SPRING_PROFILES_ACTIVE`를 바꾸거나 가짜 자격 증명을 넣지 않는다.
3. 준비된 환경 파일을 k3s Secret에 등록한다.

   ```bash
   cd /opt/happygallery
   ./deploy/k3s/scripts/create-secrets.sh \
     /etc/happygallery/mysql.env \
     /etc/happygallery/redis.env \
     /etc/happygallery/app.env \
     /etc/happygallery/alertmanager.env
   ```

4. [최초 배포 절차](README.md)에 따라 `rollout.sh /etc/happygallery/release.env`를 실행한다.
   MySQL을 별도로 먼저 생성하지 않는다. 기존 MySQL이 있으면 검증된 최근 복구 묶음이 필요하다.
5. 앱·DB·프런트의 정상 시작을 확인하고 관리자 초기 설정, 실제 이메일과 외부 백업·복구를 점검한다.
   휴대폰 인증과 고객 문자 알림은 미완료 항목으로 유지한다.

## 승인 후 실제 발송 전환

1. NHN 본인 인증·발신 번호 승인과 사용할 `HG_*` 알림톡 템플릿 승인을 확인한다.
2. 최근 외부 백업과 복구 키를 확인한 뒤 `app.env`의 여섯 NHN 키를 채우고
   `NOTIFICATION_MODE=nhn`으로 바꾼다. 키는 출력·커밋하지 않는다.
3. 위 `create-secrets.sh`로 Secret을 갱신하고 앱을 재시작한다. 이 패치가 이미 포함된 이미지라면
   설정 전환을 위해 이미지를 다시 빌드할 필요는 없다.

   ```bash
   sudo k3s kubectl -n happygallery rollout restart deployment/app
   sudo k3s kubectl -n happygallery rollout status deployment/app --timeout=5m
   ```

4. 관리자 본인 번호로 휴대폰 인증의 실제 수신과 코드 소비, 고객 알림의 실제 도착과
   제공자 결과 조회를 확인한 후 서비스를 개시한다. 키 입력이나 앱 시작만으로 승인·수신을 판단하지 않는다.
