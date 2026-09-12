# 추가 구독료 없는 외부 연동

`happy-gallery.com`과 보유 홈서버의 단일 노드 k3s를 기준으로 한다. 요금과 제공 범위는 2026-09-12 공식 안내를 확인했다. 도메인 갱신·전기·회선 비용과 기존 결제·문자 발송 수수료는 별도다.

## 적용 기준

| 기능 | 선택 | 적용 상태 |
| --- | --- | --- |
| 배송조회 | 택배사 공식 조회 화면 | 한진에 CJ대한통운·롯데·우체국 링크 추가. 운송장 복사와 복사 실패 안내 제공 |
| 공휴일 | 한국천문연구원 특일 정보 API | 기존 구현 사용. 무료 활용 신청과 키 주입 후 매일 현재·다음 연도 갱신 |
| 주소 검색 | Kakao 우편번호 서비스 | 기존 무료 검색 사용. 키 불필요, 장애 시 직접 입력 |
| 개인 캘린더 | ical.js로 ICS 다운로드 | 기존 구현 사용. 일정 변경을 자동 반영하는 구독·양방향 동기화는 아님 |
| 유동 공인 IP | Cloudflare DNS API + cloudflare-ddns | 별도 k3s addon 제공. 홈서버에서 5분마다 IPv4 확인 |
| HTTPS | cert-manager + Let's Encrypt | 기존 자동 발급·갱신 사용. HTTP-01 검증용 TCP 80 유지 |
| 외부 장애 감시 | HetrixTools 무료 Website·Cron Job Monitor | 공개 URL 2개와 백업 1개 감시. 계정 설정과 systemd 연결 필요 |
| 백업 보관 | 기존 암호화 백업 + 보유한 외부 저장소 | 같은 집 밖의 저장 공간 필요. 무료 무제한 저장소를 전제하지 않음 |

Delivery API는 현재 국내 신규 가입자에게 무료 운영 플랜을 제공하지 않는다. 따라서 `DELIVERY_TRACKING_ENABLED=false`를 유지한다. 공식 사이트에서 최신 배송 상황을 확인할 수 있지만, 이 방식으로 해피갤러리 DB의 배송 상태가 자동 갱신되지는 않는다. 주문 배송 완료는 기존대로 관리자가 확정한다. 택배 계약에 무료 API가 포함돼 있다면 계약사의 이용 조건을 확인한 뒤 별도 연결할 수 있다. [Delivery API FAQ](https://www.deliveryapi.co.kr/faq)

공휴일·주소·ICS 설정은 [프로젝트 README](../../README.md#주요-환경-변수)에 있다. 공휴일 API 장애 시 마지막 수집값과 기존 계산을 사용하는 복구 경로는 유지한다.

## 1. Cloudflare DNS 자동 갱신

도메인의 권한 DNS는 Cloudflare로 확인했다. [Cloudflare Free](https://www.cloudflare.com/plans/free/)의 DNS와 [공식 동적 DNS 안내](https://developers.cloudflare.com/dns/manage-dns-records/how-to/managing-dynamic-ip-addresses/)를 사용하며 갱신 프로그램을 직접 구현하지 않는다. [favonia/cloudflare-ddns 1.17.0](https://github.com/favonia/cloudflare-ddns/releases/tag/v1.17.0)의 다중 아키텍처 이미지 digest를 manifest에 고정했다.

아래 명령은 **서비스를 공개할 홈서버에서** 실행한다. 개발 PC나 다른 회선에서 실행하면 그 회선 주소로 DNS가 바뀐다. 공유기의 내부 IP 예약·TCP 80/443 전달을 먼저 준비한다. DDNS는 CGNAT나 통신사의 인바운드 차단을 해결하지 않는다.

1. Cloudflare에서 `happy-gallery.com` 영역만 대상으로 `Zone DNS Edit`와 `Zone Read` 권한의 API 토큰을 만든다. 계정 전체 키는 사용하지 않는다.
2. A 레코드 `happy-gallery.com`을 **DNS only**로 설정한다. `PROXIED=false`는 기존 레코드의 프록시 설정을 강제로 바꾸지 않는다. IPv6를 검증하지 않았다면 기존 AAAA도 확인해 제거한다. addon의 `IP6_PROVIDER=none`은 AAAA를 삭제하지 않는다.
3. 토큰을 홈서버의 비공개 파일에 저장하고 Secret과 addon을 적용한다. 토큰은 저장소·채팅에 붙여넣지 않는다.

```bash
sudo install -d -m 700 /etc/happygallery
sudo install -m 600 /dev/null /etc/happygallery/cloudflare-ddns-token
sudoedit /etc/happygallery/cloudflare-ddns-token
sudo k3s kubectl create namespace happygallery-ops --dry-run=client -o yaml | sudo k3s kubectl apply -f -
sudo k3s kubectl -n happygallery-ops create secret generic cloudflare-ddns \
  --from-file=token=/etc/happygallery/cloudflare-ddns-token \
  --dry-run=client -o yaml | sudo k3s kubectl apply -f -
sudo k3s kubectl apply -k deploy/k3s/addons/cloudflare-ddns
sudo k3s kubectl -n happygallery-ops rollout status deployment/cloudflare-ddns --timeout=120s
sudo k3s kubectl -n happygallery-ops logs deployment/cloudflare-ddns --tail=30
```

Pod 기동만으로 DNS 갱신 성공을 판단하지 않는다. 로그의 갱신 결과와 `dig A happy-gallery.com +short`가 홈서버 회선의 공인 IPv4와 일치하는지 확인한다. 첫 배포·회선 재연결 후에는 다른 회선에서 HTTPS 접속도 확인한다. 갱신 주기와 DNS 캐시 때문에 변경 반영에는 시간이 걸린다.

addon은 `happygallery-ops`에만 배포하며 공개 Service·Ingress, Kubernetes API 토큰이 없다. 토큰 파일을 읽는 비특권 컨테이너에 DNS·HTTPS 송신만 허용한다. 토큰을 바꾸면 Secret 갱신 후 Deployment를 재시작한다. 갱신을 중지할 때는 replica를 0으로 줄인다. `DELETE_ON_STOP=false`라 기존 DNS 레코드는 남는다.

app 릴리스의 백업·복원 목록에는 이 독립 addon이 포함되지 않는다. 노드 복구 시 이 절차로 다시 적용하고 토큰은 별도 비밀값 복구 저장소에서 가져온다. Tunnel이나 Cloudflare 프록시 전환은 실제 IP·전달 헤더 검증이 필요한 별도 변경이다.

## 2. 서버 밖에서 장애 감시

[HetrixTools 무료 플랜](https://hetrixtools.com/pricing/uptime-monitor/)은 1분 간격 모니터 15개를 제공한다. [이용약관](https://hetrixtools.com/terms-of-service/)은 개인·조직의 무료 계정과 내부 업무용 사용을 허용한다. Free 계정에서 아래 3개만 만들고 이메일 Contact List를 연결한다. 전화·문자 크레딧이나 유료 플랜은 구매하지 않는다. **무료 유지 조건은 90일 이내 대시보드 로그인**이므로 월간 백업·복원 점검 때 함께 로그인한다.

| 이름 | 검사 대상 | 설정 |
| --- | --- | --- |
| 홈페이지 | `https://happy-gallery.com/` | Website Monitor, 1분 간격, HTTP 200, TLS 유효성 확인 |
| 공개 API | `https://happy-gallery.com/api/v1/products/categories` | Website Monitor, 1분 간격, HTTP 200 |
| 백업 | 전용 Heartbeat URL | Heartbeat → Cron Job, Timeout 420분(6시간 주기 + 1시간 여유) |

공개 API 검사는 백엔드·DB 조회 경로도 확인한다. Actuator·Grafana를 외부에 열지 않는다. 이 검사만으로 로그인·결제·주문 정상 동작까지 보장하지는 않는다.

기존 백업은 app 쓰기를 잠시 중단하므로 외부 감시가 중단을 탐지할 수 있다. 실제 백업 시간을 측정한 뒤 필요한 시간만 점검 시간으로 등록한다. 홈서버 전원·회선 장애를 감지할 외부 감시는 계속 유지한다.

### 백업 성공 알림 연결

HetrixTools의 `Add Monitor → Heartbeat Monitor → Cron Job`에서 백업 항목을 만들고 발급된 비공개 URL을 다음 파일에 저장한다. URL은 백업 완료 여부만 전달하고 백업 파일·개인정보를 보내지 않는다. [생성·Timeout 설정](https://docs.hetrixtools.com/add-a-cron-job-monitor-heartbeat-monitor/), [공식 호출 안내](https://docs.hetrixtools.com/how-to-use-the-cron-job-monitor/)

```bash
sudo install -m 600 deploy/k3s/examples/backup-heartbeat.env.example /etc/happygallery/backup-heartbeat.env
sudoedit /etc/happygallery/backup-heartbeat.env
sudo install -d -m 755 /etc/systemd/system/happygallery-backup.service.d
sudo install -m 644 deploy/k3s/systemd/backup-external-heartbeat.conf.example \
  /etc/systemd/system/happygallery-backup.service.d/external-heartbeat.conf
sudo systemctl daemon-reload
sudo systemctl cat happygallery-backup.service
```

기존 백업 unit을 먼저 설치해야 한다. 추가 `ExecStartPost`가 백업·보존 정리·로컬 성공 기록 뒤에 붙었는지 확인한다. 다음 실제 백업 성공 때 첫 알림을 보내고 외부 화면에서 정상 상태를 확인한다. 첫 성공 전에는 감시 항목이 실패 상태일 수 있다. 백업 실행 없이 성공 알림만 수동 전송하지 않는다.

백업 실패 시 성공 알림을 보내지 않는다. 외부 통신 실패는 이미 성공한 백업을 실패로 바꾸지 않으며, 마지막 알림 후 7시간이 지나면 외부 서비스에서 미수신을 알린다. 기존 systemd 실패 webhook과 로컬 watchdog은 빠른 실패 감지용으로 유지한다.

## 3. 운영 적용 확인

- 설정 검사: 저장소 루트에서 `deploy/k3s/scripts/validate.sh`를 실행한다.
- 서버 적용: DDNS Secret·실제 A 레코드·라우터 접근·TLS 발급 및 갱신을 확인한다.
- 외부 감시: 홈페이지·API 정상 확인, 실제 백업 성공 기록과 첫 heartbeat 수신을 확인한다. 별도 시험 heartbeat로 미수신 알림도 검증한다.
- 데이터 보호: 호스트와 집을 잃어도 복구할 수 있는 외부 사본과 복호화 키를 확보하고 [복원 절차](README.md)를 검증한다. 무료 저장 용량을 넘으면 무조건 과금하는 서비스를 연결하지 않는다.

이 저장소 변경은 설정과 연결 절차를 제공한다. 외부 계정 등록·토큰 발급·DNS 변경·운영 k3s 배포는 실제 운영 환경에서 위 확인을 마쳐야 완료된다.
