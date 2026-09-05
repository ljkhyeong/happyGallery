# happy-gallery.com 클라우드 운영 준비

2026-09-05 기준 준비안이다. 운영 VM·백업 저장소는 아직 생성하지 않았다. 현재 실행 계획은 루트 `plan.md`, 클라우드 전환 결정은 [ADR-0049](../../docs/ADR/0049_저예산_클라우드_운영_기준/adr.md), 실제 서비스 배포·복구 명령은 [k3s 운영 절차](../k3s/README.md)를 따른다.

## 한국 이용자 기준의 우선 검토안

운영자가 한국 이용자와 AWS·Google Cloud 사용을 우선 검토하도록 요청했다. 기존 Contabo 일본·Hetzner 백업 구매안은 보류한다. 가격과 메모리 용량만으로 일본 서버를 최선으로 판단하지 않는다. 아래는 공급자·사양 비교이며 구매 확정이나 예산 증액 승인이 아니다.

AWS Lightsail은 [서울 리전 `ap-northeast-2`](https://docs.aws.amazon.com/lightsail/latest/userguide/understanding-regions-and-availability-zones-in-amazon-lightsail.html)을 지원한다. 한국 이용자가 주 대상이므로 서울을 먼저 검토한다. 국내 통신망에서 실제 응답 시간을 측정한 결과는 아직 없다.

| 후보 | 정상 요금, 무료 체험·약정 할인 제외 | 판단 |
| --- | --- | --- |
| AWS Lightsail 서울 2GB | $12/월, IPv4·SSD 60GB·전송 3TB 포함 | 예산 안에서 검토할 수 있으나 현재 전체 구성을 그대로 올릴 수 있다고 보장하지 않음 |
| AWS Lightsail 서울 4GB | $24/월, IPv4·SSD 80GB·전송 4TB 포함 | 우선 운영 검증 후보. 원래 월 3만 원 목표보다 비용이 높음 |
| Google Compute Engine 서울 e2-small 2GiB | $0.02149143/시간, 730시간 약 $15.69 | 디스크·IPv4·외부 전송·백업 별도 |
| Google Compute Engine 서울 e2-medium 4GiB | $0.04298286/시간, 730시간 약 $31.38 | 같은 상시 서버 방식에서는 Lightsail보다 기본 비용이 높음 |

- AWS 가격은 [공식 요금표](https://aws.amazon.com/lightsail/pricing/), Google 가격은 [범용 VM 요금표](https://cloud.google.com/products/compute/pricing/general-purpose)에서 Seoul을 선택해 확인했다. Google E2 소형은 공유 코어 상품이므로 표기된 가상 CPU 수를 전용 코어 수로 해석하지 않는다.
- Google의 [일반 VM 외부 IPv4](https://cloud.google.com/vpc/network-pricing)는 시간당 $0.005, 730시간 약 $3.65가 추가된다. 무료 1시간을 제외하기 전의 견적이다. 서버 요금만으로 두 업체의 총비용을 비교하지 않는다.
- 견적용 환율 $1=1,500원과 세금 여유 10%를 가정하면 Lightsail 2GB 서버는 약 19,800원, 4GB는 약 39,600원이다. 이는 실시간 환율·최종 청구액이 아니다. 별도 백업·환전 비용까지 포함하면 2GB는 월 2~3만 원을 목표로 검토할 여지가 있고, 4GB는 월 4~5만 원 수준의 예산 검토가 필요하다. 백업 대상·보존 용량에 따라 더 들 수 있다.
- 현재 메모리 한도 합계 약 4.75GiB는 실제 사용량이나 최소 요구 메모리가 아니다. 8GB를 필수 조건으로 확정하지 않는다. 2GB·4GB에서 기동, 주문·예약 동시 처리, 배치·백업과 재기동을 측정한 결과도 아직 없다.
- 2GB 검토 시 JVM·MySQL·연결 풀과 모니터링 상주 구성을 조정한다. k3s 유지 여부도 실측 대상이며, 변경한다면 별도 운영 구성을 구현하고 ADR·백업·배포 절차를 함께 갱신한다. 기존 개발용 Compose를 그대로 운영에 올리지 않는다.
- 백업 저장 국가와 업체도 서버와 함께 다시 정한다. 국내 저장을 원하면 유럽 Storage Box를 자동 채택하지 않는다. 저장 방식이 달라지면 기존 원격 mount 백업과의 호환성 또는 전송 절차를 먼저 검증한다.

## 기존 일본 서버 견적 — 구매 보류

아래는 서버·백업 등 인프라 예산이다. 문자·알림톡·이메일 발송, 결제 수수료와 도메인 갱신은 별도다. 기존 계정과 예산 포함 범위는 운영자 답변 전이므로 확정 청구액으로 보지 않는다.

| 항목 | 구매 후보 | 확인 금액 |
| --- | --- | --- |
| 운영 VM | Contabo Cloud VPS 4, 일본, 4 vCPU·8GB RAM·100GB SSD, IPv4 1개, 1개월 계약 | 기본 €5.50 + 일본 €2.55 = **€8.05/월**, 초기 설치 €0 |
| 외부 백업 | Hetzner Storage Box BX11, 1TB | **€3.20/월**, VAT 제외·초기 설치 €0 |
| DNS | 현재 Cloudflare DNS 유지 | 추가 유료 요금제 불필요 |
| TLS | 기존 cert-manager/Let's Encrypt | 인증서 구매비 없음 |
| 내부 모니터링 | 기존 Prometheus·Alertmanager·Grafana | VM 비용에 포함 |

공급자 표시 합계는 **€11.25/월**이다. 견적용 환율 €1=1,800원을 가정하면 20,250원이며, 세금·카드 환전 비용·여유분을 잡아 **월 2.3~2.6만 원**을 목표로 한다. 이 환율은 현재 환율 조회값이 아니다. 가입 국가와 결제 통화에 따라 세금·청구액이 달라질 수 있으므로 최종 월 청구 예상이 3만 원을 넘으면 구매하지 않고 구성을 다시 비교한다. 자동 유료 증설·추가 디스크·유료 제어판은 신청하지 않는다.

- [Contabo 구성 페이지](https://contabo.com/de/vps/cloud-vps-core-4)에서 1개월 계약과 일본을 선택한 뒤 월 합계·당일 청구액 €8.05를 확인했다. 첫 페이지의 24개월 할인 가격을 사용하지 않았다. Contabo 자동 백업은 위 금액에 포함하지 않았으며 별도 업체 백업을 사용한다.
- [Hetzner Storage Box](https://www.hetzner.com/storage/storage-box/)의 BX11, VAT 제외 €3.20·1TB 표시를 확인했다. 데이터 위치는 독일 또는 핀란드다. 실제 주문 국가 설정에서 다시 확인한다.
- [AWS Lightsail](https://aws.amazon.com/lightsail/pricing/)의 IPv4 Linux는 2GB $12, 4GB $24다. 기존 전체 구성의 메모리 한도 합계가 약 4.75GiB이고 OS·k3s도 필요하므로 2GB를 기본 운영 사양으로 채택하지 않는다. 메모리 한도 합계는 실제 사용량 측정값이 아니다.
- [Hetzner 변경 요금표](https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/)상 싱가포르 CPX22는 €26.49에 IPv4가 별도여서 이번 예산에서 제외했다.
- [OVHcloud VPS](https://www.ovhcloud.com/en-sg/vps/)의 8GB 광고 가격은 12개월 선결제였다. 실제 구성 화면에 아시아 지역 품절이 표시돼 현재 구매안으로 쓰지 않는다.
- [Oracle Always Free](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)는 현재 A1 총 2 OCPU·12GB 상당을 제공하지만 지역 가용 용량과 유휴 회수 조건이 있다. 생성·유지 가능성을 확인하지 않은 무료 자원을 운영 비용의 전제로 삼지 않는다.

## 현재 도메인 상태

공개 DNS 조회에서 네임서버 `jeremy.ns.cloudflare.com`, `ali.ns.cloudflare.com`을 확인했다. 루트 도메인의 A/AAAA와 `www` CNAME 응답은 없었다. Cloudflare 계정 권한과 DNSSEC·플랜 상태까지 확인한 것은 아니다.

| 레코드 | 설정할 값 | 시점 |
| --- | --- | --- |
| `A @` | 구매 후 배정된 VM 공인 IPv4, 초기 DNS only | 호스트 방화벽과 k3s 준비 후 |
| `AAAA @` | 초기에는 추가하지 않음 | IPv6 방화벽·라우팅을 실제 검증한 뒤 |
| `www` | 초기에는 추가하지 않음 | 별도 인증서·대표 주소 리다이렉트를 구현한 뒤 |
| 이메일 관련 MX/TXT | SMTP 제공자가 지정한 값 | 발신 도메인 인증 시, 기존 레코드 보존 |

현재 ingress·인증서는 `happy-gallery.com` 한 호스트만 처리한다. `www` CNAME만 추가해도 HTTPS 리다이렉트가 된다고 가정하지 않는다. Cloudflare 프록시를 바로 켜거나 로그인·장바구니·결제 API에 전체 캐시 규칙을 적용하지 않는다.

```text
이용자 -> Cloudflare DNS -> 운영 VM :443 -> k3s Traefik
                                             -> 웹 SSR
                                             -> Spring Boot -> MySQL / Redis
운영 VM -> 암호화된 복구 묶음 -> 다른 업체의 백업 저장소
외부 uptime 감시 -> happy-gallery.com
```

## 기존 일본 후보 선택값과 공통 호스트 준비

아래 첫 항목은 보류한 일본 견적의 선택값이다. 서울 후보가 확정되면 해당 공급자의 사양·OS·방화벽에 맞게 갱신한 뒤 생성한다.

- 1대, 1개월 계약, x86_64, 4 vCPU, RAM 8GB, SSD 100GB, 일본, IPv4 포함.
- Linux는 Ubuntu LTS를 사용하고 구매 화면에서 지원되는 버전을 기록한다. 유료 OS·패널·추가 모니터링은 선택하지 않는다.
- 관리자 비밀번호는 사이트에서 운영자가 직접 입력한다. 첫 접속에서 일반 운영 계정과 SSH 공개키를 설정하고 실제 키 로그인을 확인한 뒤 암호 로그인과 직접 root 로그인을 제한한다.
- 클라우드 방화벽에서 TCP 80/443은 공개하고 TCP 22는 운영자 IP만 허용한다. TCP 6443·3306·6379·8080·8081·9090·9093·3000, UDP 8472를 외부에 열지 않는다. 노드 내부·Pod 간 통신은 k3s 요구사항에 맞춰 유지한다.
- 시간 동기화, 재부팅 후 k3s 자동 기동, 보안 업데이트와 디스크 경보를 설정한다. VM 전체 메모리·디스크 여유를 기록하고 운영 중인 VM에서 대규모 빌드를 함께 돌리지 않는다.
- 현재 `build-import-images.sh`는 빌드와 k3s 반입을 한 호스트에서 수행한다. 첫 배포는 서비스 기동 전에 실행할 수 있다. 이후 운영 배포 전에는 별도 빌드 환경에서 검사한 이미지를 전달하는 절차를 먼저 분리한다.

## 외부 백업 연결

기존 스크립트는 원격 파일시스템 mount를 요구한다. Storage Box의 SFTP/SSHFS 연결을 후보로 두며, 구매 후 아래 검증을 마쳐야 실제 백업 대상으로 인정한다. S3/R2 버킷을 임의의 FUSE mount로 바꿔 연결하는 방식은 검증된 대체 경로가 아니다.

1. 백업 전용 하위 계정·디렉터리와 SSH 공개키를 설정하고 원격 호스트 키를 제공자 정보와 대조한다.
2. VM의 `/mnt/off-device/happygallery`에 mount한다. `.happygallery-off-device-backup-target` marker는 원격 디렉터리에만 만든다. mount 해제 시 로컬 빈 디렉터리에서 marker가 보이지 않는지 확인한다.
3. 원격 디렉터리에서 파일 생성·이름 변경·권한 제한·재읽기·체크섬 검증을 수행한다. 연결 중단 시 백업이 실패로 끝나고 완료 marker를 남기지 않는지 확인한다.
4. 기존 `backup.env.example`과 systemd timer를 사용한다. 6시간 간격·30일 보존으로 시작하고 DB·이미지·배포 archive의 실제 합계 크기가 1TB 안에 드는지 측정한다. 자동 용량 증설은 설정하지 않는다.
5. `ENCRYPT_KEY`, `HMAC_KEY`, 서명 키와 `age` 복호화 키는 운영자가 별도 보관한다. DB 백업 계정에 복호화 키를 함께 넣지 않는다.
6. 테스트 주문·이미지가 있는 상태로 백업한 뒤 별도 환경에서 복원한다. 일치하는 DB·미디어·이미지 digest·키를 확인하고 복구 소요 시간을 기록한다.

**현재 제약:** 백업은 app replica를 0으로 내려 DB와 미디어를 같은 구간에 저장한다. 하루 4회 주문·예약·로그인 API가 중단될 수 있고, 원격 전송 지연이 중단 시간을 늘릴 수 있다. 실제 중단 시간과 복구 시간을 측정하기 전에는 무중단 운영 또는 특정 복구 시간을 약속하지 않는다. 이용 가능한 수준이 아니면 공개 전에 백업 방식을 보완한다.

## 개통 순서와 완료 기준

| 순서 | 실행할 일 | 완료 증거 |
| --- | --- | --- |
| 1 | 클라우드 계정·구매 후보·최종 월 비용 확정 | 지역·사양·월 계약·세금 포함 견적 |
| 2 | VM·외부 백업 생성, SSH·방화벽·시간 동기화 | 운영자 키 로그인, 공개 포트 확인 |
| 3 | k3s·Traefik·cert-manager 설치, 백업 mount 준비 | 노드 Ready, mount 중단 시 실패 검증 |
| 4 | 운영 Secret과 프런트 운영 빌드 키 준비 | 저장소 밖 600 권한 파일, 허용 키 검사 |
| 5 | DNS·TLS와 첫 배포 | 인증서 Ready, SSR·API·404 응답 구분 |
| 6 | 관리자 MFA·실제 제공자 연동 | 로그인·문자·메일·결제/취소 성공 기록 |
| 7 | 외부 감시·백업·복원·재부팅 훈련 | 실제 알림 수신, 복원 데이터·기동 확인 |
| 8 | 소규모 동시 조회·예약·주문 부하 확인 | OOM·재시작·5xx 없음, 메모리·응답 지연 기록 |
| 9 | 운영 개시 | 미완료 필수 항목 없음, 운영자가 초기 데이터 확인 |

구매와 제공자 키 입력은 아직 진행하지 않았다. 키 값은 채팅이나 Git에 기록하지 않고 아래 파일·제공자 콘솔에서 관리한다.

- `deploy/k3s/examples/app.env.example`: DB·암호화·서명·Toss·OAuth·NHN·SMTP·Sentry 설정 목록.
- `deploy/k3s/examples/release.env.example`: `PUBLIC_HOST=happy-gallery.com`, 실제 ACME 이메일, 이미지 SHA/digest.
- Google/Naver/Kakao callback: `https://happy-gallery.com/api/v1/auth/social/callback/{google|naver|kakao}`.
- Toss 웹훅: `https://happy-gallery.com/api/v1/webhooks/toss-payments`.
- NHN: 발신번호·알림톡 템플릿 승인과 실제 수신 확인. SMTP: 발신 도메인 인증과 가입 이메일 수신 확인.
- 개인정보처리방침·사업자 고지: 실제 공급자, 데이터 저장 국가, 문의·발신 이메일 등 운영 값과 화면을 대조한다.
- 관리자 초기 설정 후 임시 setup token을 제거하고 MFA 복구 코드를 별도 보관한다.

## 이번 준비에서 확인한 것

`./deploy/k3s/scripts/validate.sh`가 통과했다. YAML 42개 문서, 운영 비밀값 허용 키, 이미지·내부 포트·TLS 구성, 기존 자격증명 회전 검증을 포함한다. 이는 정적 검증이며 실제 클라우드 배포·결제·백업 복원 성공을 뜻하지 않는다.
