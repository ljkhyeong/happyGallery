# 무료 검색엔진 변경 알림

IndexNow로 상품·수업·이벤트·공지의 추가·수정·삭제를 네이버·Bing 등 참여 검색엔진에 알린다. 별도 가입이나 구독료 없이 로컬에서 만든 키를 사용한다. 접수 성공이 검색 노출이나 순위를 보장하지는 않는다. Google 검색 반영은 이 연동의 대상이 아니다. [Bing 무료 제공 안내](https://www.bing.com/indexnow), [네이버 안내](https://searchadvisor.naver.com/guide/indexnow-about)

## 동작

- 홈서버에서 15분 간격으로 공개 목록 API 4개를 조회한다. 첫 실행은 기존 페이지의 비교 기준만 저장하고 검색엔진에 제출하지 않는다.
- 내용이 바뀐 상세 페이지와 해당 목록·홈 주소만 제출한다. 비공개 전환·삭제·종료로 목록에서 빠진 주소도 알린다. 공지 조회수와 목록 정렬 순서 변경은 제외한다.
- 조회 실패·잘못된 응답은 삭제로 처리하지 않는다. 제출 실패는 다음 실행에서 재시도하며, 한 번에 최대 10,000개씩 접수된 묶음만 기록한다. HTTP 202는 키 확인 대기 상태의 접수다.
- 상태 파일에는 공개 페이지 경로와 내용 해시만 저장한다. 개인·주문·예약 데이터는 조회하지 않는다. 별도 DB·컨테이너·라이브러리는 추가하지 않는다.

약관·소개 문구 같은 정적 코드 변경, 후기·Q&A·예약 슬롯은 비교 대상이 아니다. 기존 사이트맵은 계속 제공한다. API가 페이지별 조회 방식으로 바뀌면 모든 페이지를 수집하도록 이 스크립트도 수정해야 한다.

## 배포자가 설정할 항목

코드·모의 응답 검사·설정 예시만 준비했다. 실제 키 설정, 이미지 빌드·배포와 timer 설치는 홈서버 운영자가 진행한다. Ruby와 systemd, 정상 공개된 `https://happy-gallery.com`이 필요하다.

1. `openssl rand -hex 16`으로 키 하나를 만든다. [app.env](examples/app.env.example)의 `INDEXNOW_KEY`에 넣고 [기존 Secret 준비 절차](README.md#2-secret-준비)를 따라 배포한다. 프런트엔드는 앱 Secret 전체가 아닌 이 키 하나만 읽는다. 키를 갱신하면 프런트엔드도 재시작해야 한다.
2. `https://happy-gallery.com/<만든-키>.txt`가 HTTP 200과 키 문자열만 반환하는지 확인한다. 빈 키이면 경로가 만들어지지 않는다. 키 파일은 검색엔진의 사이트 소유 확인용이며 URL을 아는 쪽에서 읽을 수 있다. 운영 비밀번호를 키로 재사용하지 않는다.
3. 아래 예시를 홈서버에 복사한다. `indexnow.env`에 같은 키와 `INDEXNOW_ENABLED=true`를 넣는다. 저장소 경로가 `/opt/happygallery`와 다르면 service의 두 경로도 바꾼다.

```bash
sudo install -d -m 700 /etc/happygallery
sudo install -m 600 deploy/k3s/examples/indexnow.env.example /etc/happygallery/indexnow.env
sudoedit /etc/happygallery/indexnow.env
sudo install -m 644 deploy/k3s/systemd/happygallery-indexnow.service.example /etc/systemd/system/happygallery-indexnow.service
sudo install -m 644 deploy/k3s/systemd/happygallery-indexnow.timer.example /etc/systemd/system/happygallery-indexnow.timer
sudo systemctl daemon-reload
sudo systemctl start happygallery-indexnow.service
sudo journalctl -u happygallery-indexnow.service -n 20 --no-pager
```

4. `비교 기준 저장` 로그를 확인한 뒤 정기 실행을 켠다. 키 파일이나 API 조회가 실패했다면 해당 문제를 먼저 해결한다.

```bash
sudo systemctl enable --now happygallery-indexnow.timer
```

이후 실제 상품 정보를 수정한 뒤 실행 로그에서 `URL 접수`를 확인한다. 게시글 조회수만 늘리면 `변경 없음`이어야 한다. 실제 검색 반영 여부는 네이버 서치어드바이저·Bing Webmaster Tools에서 별도로 확인한다.

## 실패·중지·복구

- 403: 프런트엔드의 키 파일과 env 값, 422: 도메인·키 형식을 확인한다. 429·서버 오류·시간 초과는 다음 정기 실행에서 재시도한다. HTTPS 인증서를 검증하고 리다이렉트는 따라가지 않는다.
- 중지: `sudo systemctl disable --now happygallery-indexnow.timer`. 이미 실행 중이면 service도 중지한다. `INDEXNOW_ENABLED=false`는 수동 실행도 막는다. 공개 키 파일도 없애려면 app.env의 키를 비우고 Secret 갱신 후 프런트엔드를 재시작한다.
- 비교 기준은 systemd가 관리하는 `/var/lib/happygallery-indexnow/pages.json`에 저장한다. DB 복원이나 다른 운영 호스트로 이동할 때 이 파일을 함께 보존하면 기존 기준과 비교할 수 있다. 상태 파일을 삭제하면 다음 실행은 새 기준만 저장하므로 삭제 전의 미전송 변경은 복구하지 못한다.
- 파일 손상은 오류로 멈춘다. 임의로 빈 목록으로 바꾸지 말고 보관한 파일을 복구하거나, 미전송 변경을 잃는다는 점을 확인한 뒤 기준을 다시 만든다. 경로·키·응답 원문은 실행 로그에 남기지 않는다.

명세: [IndexNow POST·키 검증·응답 코드](https://www.indexnow.org/documentation), [변경 시 제출 원칙과 검색엔진 공유](https://www.indexnow.org/faq). 제공 조건은 2026-09-12 확인했다.
