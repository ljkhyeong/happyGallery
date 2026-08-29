# happyGallery

`happyGallery`는 오프라인 공방의 상품 주문, 클래스 예약, 8회권, 이벤트, 회원 쿠폰·적립금과 관리자 운영을 다루는 서비스다.
백엔드는 Spring Boot 멀티 모듈 애플리케이션이고, 프론트엔드는 React Router + Vite 기반으로 공개 경로 SSR과 회원·관리자 경로 CSR을 함께 제공한다.

## 한눈에 보기

| 사용자 | 주요 기능 |
| --- | --- |
| 비회원 | 휴대폰 인증 기반 주문/예약 생성, 토큰 기반 조회·취소·지연 응답·주문 클레임, SMS 기반 조회 정보 복구, 회원가입 후 기존 이력 가져오기 |
| 회원 | 상품 주문·취소·지연 응답·주문 클레임, 완료 주문·수강 후기·사진·도움돼요·신고, 쿠폰 발급·사용, 적립금 조회·사용, 클래스 예약, 8회권 구매·사용·환불, 장바구니, 알림함, 소셜 계정·휴대폰·기준 이메일 관리, 회원 탈퇴 |
| 관리자 | 오늘 할 일 중심 운영 화면과 판매 중 품절 상품·옵션 조합 재고 작업, 스마트스토어 주문 발주·발송·클레임·재고·상품 문의·정산 연동, PG 정산 불일치 확인, 상품·클래스·이벤트·쿠폰 콘텐츠/이미지, 후기 상태·감사 이력·신고·공식 답글 관리, 공방 프로필·기본 개방 예약 캘린더, 전화·메신저·방문 예약 등록, 예약 검색·운영자 취소·후속 정산, 8회권 검색·환불, 주문 승인/거절/배송/픽업·클레임, 환불 재처리, 전체 미답변 Q&A/문의 답변, 상품·주문·슬롯 분석 |

- 주문/예약/8회권은 `POST /api/v1/payments/prepare` -> `POST /api/v1/payments/confirm` 표준 결제 경로를 사용한다. 예약 결제창은 Toss의 `CARD` 통합 창에서 카드와 간편결제를 선택하며, 서버는 준비 요청값이 아니라 PG 승인 응답의 실제 결제수단과 영수증 URL을 저장한다. confirm은 선점·PG 승인·도메인 저장 트랜잭션을 분리하고 Toss 멱등키와 실패 보상 환불을 사용한다. 결제 완료·상태 조회 응답은 저장된 영수증 링크를 고객에게 제공한다. 결제 상태 웹훅은 중복 전송을 제거한 뒤 기존 PG 대사를 실행하고, 최근 7일 Toss 정산은 매시간 로컬 결제·환불 거래키와 금액을 대조해 불일치를 관리자 오늘 할 일에 남긴다. 고객은 소유권이 확인된 `GET /api/v1/payments/{orderId}`로 처리 결과를 확인하며, 비회원이 브라우저 저장소를 잃으면 SMS 인증 기반 `POST /api/v1/guest-records/payment-status-recovery`로 결제 ID 목록과 공통 상태 조회 토큰을 함께 복구한다.
- 공개 이벤트는 별도 이벤트 탭과 홈 추천 영역에서 조회하고 관리자는 게시 기간·대표 이미지·관련 상품·이벤트 쿠폰·홈 추천 여부를 관리한다. 회원은 이벤트 상세에서 연결된 공개 쿠폰을 바로 받을 수 있다.
- 회원 상품 주문은 공개 발급 쿠폰 한 장과 적립금을 함께 사용할 수 있다. 쿠폰과 적립금은 결제 준비 시점에 30분 예약하고 주문 생성 시 사용 확정하며, 주문에는 상품액·배송비·쿠폰 할인·적립금·PG 결제액과 품목별 배분을 보존하고 상세 화면에서도 이를 구분해 표시한다. 배송·픽업 완료 시 혜택 적용 뒤 상품 순결제액의 1%를 1년 만료 적립금으로 지급한다.
- 이메일·소셜 최초 가입과 비회원 주문·예약 결제는 `GET /api/v1/policies/current`로 조회한 이용약관·개인정보처리방침 버전에 동의해야 한다. 서버는 현재 버전을 다시 검증하고 회원 또는 결제 시도에 동의 유형·목적·서버 수락 시각을 추가 이력으로 보존하며, 프론트의 append-only 버전 registry가 동의 당시 불변 본문을 계속 제공한다.
- 8회권 사용 예약은 운영자가 일정을 일괄 배정하지 않는다. 회원이 예약 가능 슬롯을 직접 선택해 한 회차씩 예약하고 크레딧 1회를 사용한다.
- 정규 공예 8회권은 구매 시 계획을 저장하고, 운영자가 사용 가능으로 지정한 비향수 클래스에만 적용한다. 회원은 내 8회권에서 잔여 횟수 정산 환불을 요청하고 진행 상태를 조회한다.
- 환불은 요청 이력을 먼저 커밋한 뒤 PG를 호출한다. 실행 유실·일시 실패는 최초 멱등키로 복구하고, 결과 불명 상태는 PG 취소 내역을 먼저 조회해 성공 여부를 화해한 뒤에만 취소 재호출 여부를 결정한다.
- 배송 주문은 `ORDER_SHIPPING_FEE` 고정액을 결제 준비 시 확정해 주문에 저장한다. 상품명·단가, 배송비, 쿠폰·적립금·PG 금액 배분과 택배사·운송장 정보는 과거 주문을 재현할 수 있게 스냅샷으로 유지한다.
- 주문제작 상품은 고정 사양과 1~180일 예상 제작 기간을 필수로 관리한다. 관리자는 필수·선택 선택형 옵션, 각인 같은 직접입력 옵션과 선택 조합별 추가 금액·재고·판매 여부를 등록한다. 고객은 서로 다른 조합과 문구를 여러 품목으로 담을 수 있지만, 같은 SKU의 장바구니 합산 수량은 현재 재고를 넘을 수 없다. 비회원도 이 기기의 장바구니에서 상품·옵션·가격·수량을 확인하고 수정·삭제하며, 로그인하면 회원 장바구니로 병합한다. 서버는 결제 준비 시 SKU 재고와 `기본가 + 옵션 추가금`을 다시 확정해 상품 유형·옵션·문구·사양·관리 방법·제작 기간과 함께 주문 항목에 고정한다.
- 관리자는 상품별 스마트스토어 원상품 번호를 연결하고, 주문제작 상품은 각 내부 옵션 조합에 네이버 옵션 ID를 매핑한다. 주문 차감·환불 복구·수동 조정과 같은 재고 변경은 같은 트랜잭션에서 동기화 요청으로 합쳐지고, 배치가 최신 절대 수량만 스마트스토어에 반영한다. 기성품은 원상품 재고, 주문제작품은 모든 옵션 조합 재고를 한 상품 단위 요청으로 전송한다.
- 스마트스토어 주문 상세의 수령인·연락처·배송지는 암호문으로 저장하고 관리자 단건 화면에서만 복호화한다. 관리자는 발주·발송, 취소·반품, 교환 수거·보류·거절·재배송을 네이버 API로 처리한다. 상품 문의와 주문·배송 고객 문의를 함께 조회·답변하고, 정산 지급일 커서로 장애 기간의 누락 날짜를 순차 복구한다. 상품 가격·판매 상태·옵션 가격은 채널 차이를 먼저 확인하고 명시적으로 반영한다. 외부 처리 성공 직후 로컬 상태를 추측해 바꾸지 않고 다음 주문 변경 피드에서 확정 상태를 수집한다.
- 운영 알림은 NHN의 요청 접수 식별자를 저장한 뒤 최종 수신 결과를 별도 조회한다. 실제 성공이 확인된 건만 알림함의 `SENT`로 확정하고, 알림톡 최종 실패가 확인된 경우에만 SMS를 다음 채널로 요청한다.
- 배송·픽업이 끝난 주문은 회원 세션 또는 비회원 접근 토큰으로 품목·수량별 환불·교환 클레임을 접수한다. 관리자는 환불액과 반품 재고 복구 여부를 결정하고, 고객 반환액을 PG 환불과 적립금 복원으로 분해해 상품별 배분을 승인 시 고정한다. 교환품 재고는 승인 시 다시 차감하며 PG 환불은 기존 비동기 복구 경계를 사용하고 0원 PG 주문은 내부 혜택 복구만으로 종결한다.
- 관리자는 기성품의 승인 전 재고 부족과 주문제작의 제작 중 일정 변경에 대해 주문 처리 지연을 제안할 수 있다. 고객이 수락한 뒤 `/api/v1/admin/orders/{id}/resume-after-delay`로 재개하면 기성품은 이행 대기, 주문제작은 제작 중으로 돌아간다. 예상 출고일 설정·갱신과 지연·재개 이력은 Bearer 세션이면 관리자 ID를 남기고, 로컬 API key 작업이면 행위자 ID를 `null`로 남긴다. 출고일 변경 이력에는 이전·이후 날짜를 기록한다.
- 공개 상품 Q&A는 일반글 상세를 비밀번호 없이 조회하고, 비밀글 상세에만 비밀번호 검증을 요구한다.
- 회원은 배송·픽업이 끝난 주문 품목 또는 완료된 클래스 예약을 근거로 별점·본문·사진 후기를 남긴다. 공개 후기는 별점 분포·필터·정렬, 공방 공식 답글과 도움돼요를 제공하며, 회원은 부적절한 후기를 신고할 수 있다. 회원 수정과 관리자 상태·답글 변경은 각각 콘텐츠 revision과 행 version으로 오래된 화면의 덮어쓰기를 막는다. 관리자의 숨김·재공개는 당시 콘텐츠 증거와 함께 감사 이력으로 남고, 숨김 이력이 있는 후기는 삭제해도 같은 거래로 재작성할 수 없다. 증거 없는 일반 삭제 후기 행은 30일 뒤 파기하고, 숨김·삭제 후기와 분쟁 증거에서만 쓰는 사진은 공개 경로에서 차단한다.
- 관리자 고객 통합 검색은 고객명 또는 정확한 휴대폰 번호로 주문·예약·8회권 이용 내역을 한 번에 보여준다. 주문·예약 결과의 `운영` 액션은 해당 대상을 기존 운영 패널에서 바로 찾고 후속 처리할 수 있게 연결한다.
- 고객은 클래스별 향후 1~30일 회차를 날짜별로 탐색하고 일반 예약은 해당 클래스의 남은 회차 정원까지 접수한다. 만석 회차에는 회원 또는 휴대폰을 인증한 비회원이 1회성 빈자리 알림을 신청하며, 좌석을 선점하지 않고 자리가 실제 예약 가능해진 순간 안내받아 선착순으로 예약한다. 회원은 예약 화면에서 사라진 회차의 대기 알림도 마이페이지에서 클래스명·일시와 함께 확인하고 취소할 수 있다. 인원만큼 슬롯 정원과 예약금·잔금을 반영하며, 취소 보상 마감 전에는 1명 이상을 남겨 인원을 부분취소하고 줄어든 예약금만 환불받을 수 있다. 8회권 예약은 1명으로 제한한다. 운영자 사정으로 예약을 취소하면 고객 취소 마감과 무관하게 PG 예약금 환불 또는 유효한 8회권 크레딧 복구를 시작한다. 오프라인에서 받은 예약금, 이미 받은 현장 잔금과 만료 이용권 보상은 금액과 사유가 있는 영속 후속 작업으로 남겨 관리자가 완료 처리한다.
- 고객 주문 상세는 `ORD-00000001` 형식 주문번호, 결제 당시 상품 구매조건, 배송지와 택배사 배송 상태·진행 이력을 소유권 확인 뒤 제공한다. 관리자는 지원 택배사를 선택해 운송장을 등록하며 외부 배송조회 등록 실패는 배치가 재시도한다. 관리자 주문·예약 검색은 표시 번호, 이름 또는 정확한 휴대폰 번호로 찾는다.
- 관리자는 이름·전화번호·`PASS-00000001` 형식 이용권 번호로 8회권을 찾아 잔여 횟수, 미래 예약, 예상 환불액과 환불 상태를 확인한 뒤 환불한다.
- 상품·클래스 설명과 대표 이미지, 공방 주소·영업·주차·소개·사업자·문의 정보를 관리자 화면에서 관리한다. 주문 배송지와 공방 주소는 공식 도로명주소 검색 결과를 적용하거나 직접 입력할 수 있다. 공개 footer와 사업자 정보는 현재 공방 프로필을 사용하고, 이용약관·개인정보처리방침은 과거 동의 본문이 바뀌지 않도록 버전별 문서로 보존한다. 예약 일정은 기본 운영시간을 모두 열고 공휴일·휴무일·예약 불가 시간만 닫으며, 고객 조회 시 필요한 회차를 자동으로 준비한다. 공휴일은 저장된 공공데이터 연도별 스냅샷을 우선하고 해당 연도 스냅샷이 없을 때만 기존 계산 정책으로 대체한다.
- 기준 공방 프로필은 `해피갤러리`, `충북 충주시 계명대로 161 1층`, 네이버 플레이스 `https://m.place.naver.com/place/21668321`, `010-9635-5608`, 대표 `홍지현`, 사업자등록번호 `303-11-87052`, 통신판매업 신고번호 `2011-충북 충주-127`, 전자우편 `ssi1972@naver.com`, 카카오톡 `ssim1972`를 사용한다. 네이버톡톡·네이버 블로그·인스타그램·스마트스토어 링크도 공방 프로필에서 함께 관리한다.
- 회원은 `HG_SESSION`, 관리자는 Bearer 세션, 비회원은 `X-Access-Token`을 사용한다.
- Google·Naver·Kakao 계정은 마이페이지에서 일회성 연결 시도와 OAuth `state`를 검증해 명시적으로 연결·해제하며, 이메일 일치만으로 기존 회원과 자동 병합하지 않는다. Google 검증 이메일과 Kakao의 유효·검증 이메일은 가입 시 기준 이메일로 사용한다. 신규 Naver 회원의 이메일은 `null`로 시작하고, 최근 본인 확인 뒤 메일함으로 받은 6자리 코드를 검증해 본인이 소유한 이메일을 한 번 등록할 수 있다.
- 브라우저의 비관리자 상태 변경 요청은 `XSRF-TOKEN` 쿠키와 `X-XSRF-TOKEN` 헤더로 CSRF를 방어한다.
- 상세 요구사항은 [기준 스펙](docs/PRD/0001_기준_스펙/spec.md), HTTP 계약은 [API 계약](docs/PRD/0004_API_계약/spec.md)을 기준으로 본다.

## 빠른 시작

### 요구사항

- Java 25
- Node.js 22.22+
- Docker / Docker Compose

### 실행

1. MySQL과 Redis 실행

```bash
docker compose up -d mysql redis
```

2. 백엔드 실행

```bash
./gradlew :bootstrap:bootRun
```

3. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

### 로컬 주소

- 프론트엔드: `http://localhost:3000`
- 백엔드: `http://localhost:8080`
- 헬스 체크: `http://localhost:8080/actuator/health`

## 로컬 기본값

- `local` 프로필에서는 DB가 비어 있으면 기본 클래스 3종과 관리자 계정 `admin / admin1234`를 자동 생성한다.
- 로컬과 개발 환경에서는 `X-Admin-Key: dev-admin-key`를 사용할 수 있다.
- `prod`가 아닌 환경에서는 실제 알림·인증 SMS·이메일 인증 SMTP·결제 대신 테스트용 발송기와 `FakePaymentProvider`를 사용한다.
- 스마트스토어 주문·재고·문의·정산 연동은 기본 비활성화다. 운영에서는 `SMARTSTORE_ENABLED=true`, `SMARTSTORE_CLIENT_ID`, bcrypt salt 형식의 `SMARTSTORE_CLIENT_SECRET`을 설정하고 위임 판매자 방식이면 `SMARTSTORE_ACCOUNT_TYPE=SELLER`, `SMARTSTORE_ACCOUNT_ID`도 함께 주입한다. 활성화한 시점부터 변경 주문을 매분 수집하며 과거 주문을 소급 차감하지 않는다. 배송정보 암호문은 기존 데이터 키 회전 명령에서 다른 배송지 암호문과 함께 재암호화한다.
- k3s 운영 배포의 Prometheus 경보는 내부 Alertmanager를 거쳐 저장소 밖 Secret으로 주입한 외부 HTTPS webhook에 전달한다. 노트북 자체 장애 감시는 별도 외부 uptime 서비스가 필요하다.
- 운영 환경은 DB·Redis를 readiness에 포함하고, 환불·알림 outbox·주문 승인 대기·예약 취소 후속 작업의 DB backlog, 결제·알림 CircuitBreaker 상태와 호출 결과, 모든 정기 배치의 마지막 정상 완료 시각과 이미지 저장소 용량을 Prometheus·Grafana에서 감시한다. 업무 알림은 휘발성 사건 수가 아니라 아직 처리되지 않은 DB 상태를 기준으로 유지한다.
- SMTP 장애가 주문·예약 API 전체를 비정상으로 만들지 않도록 Spring Mail health indicator는 기본 비활성화한다. 이메일 발송 장애는 알림 CircuitBreaker와 실패 로그로 관측하며, 독립 SMTP health가 필요한 환경에서만 `MAIL_HEALTH_ENABLED=true`로 켠다.
- `prod`가 아닌 환경은 Google/Naver/Kakao OAuth 자리표시자 자격 증명으로 기동한다. 실제 제공자 로그인은 각 개발자 콘솔의 자격 증명과 localhost exact callback을 환경 변수로 설정해 검증한다.
- `local`이 아닌 환경에서 최초 관리자 계정이 필요하면 `ADMIN_SETUP_TOKEN`을 주입하고 `/api/v1/admin/setup`을 호출한다.
- 반복 E2E처럼 짧은 시간에 인증/관리 요청이 몰리는 로컬 검증에서는 `RATE_LIMIT_ENABLED=false`를 사용할 수 있다.

전체 로컬 스택이 필요하면 프론트 빌드 후 Docker Compose를 실행한다.

```bash
cd frontend
npm install
npm run build
cd ..
docker compose up -d --build
```

- `http://localhost`: nginx + 프론트 정적 파일 + `/api` 프록시
- `http://localhost:9090`: Prometheus
- `http://localhost:9093`: Alertmanager
- `http://localhost:3001`: Grafana

## 주요 명령어

### 백엔드

- 전체 빌드: `./gradlew build`
- 전체 테스트: `./gradlew test`
- 정책 테스트: `./gradlew :application:policyTest`
- 통합 테스트: `./gradlew --no-daemon :application:useCaseTest`
- API 계약 문서 테스트: `./gradlew --no-daemon :adapter-in-web:restDocsTest`
- OpenAPI 스냅샷 갱신: `./gradlew --no-daemon :adapter-in-web:openapi3`
- 앱 실행: `./gradlew :bootstrap:bootRun`

`./gradlew build`의 `check` 단계에는 REST Docs 계약 테스트와 Controller/DTO 대비 OpenAPI 스냅샷 drift 검증이 포함된다. 빠른 로컬 확인이 필요할 때만 위 개별 태스크를 사용한다.
배포용 `:bootstrap:bootJar` 산출물은 `bootstrap/build/libs/happygallery-app.jar`로 고정하며
Docker, CI artifact와 k3s 이미지 반입이 모두 이 경로만 사용한다. Gradle 모듈 간 테스트 classpath에
필요한 `*-plain.jar`는 별도로 생성되지만 배포 도구는 wildcard로 JAR을 선택하지 않는다. Gradle
Wrapper 배포 ZIP은 저장소의 SHA-256으로 검증하고 CI는 wrapper JAR 무결성도 검사한다.

### 프론트엔드

- 개발 서버: `cd frontend && npm run dev`
- 프로덕션 빌드: `cd frontend && npm run build`
- ESLint·React Hooks 검사: `cd frontend && npm run lint`
- 프런트 단위 보안 회귀 검사: `cd frontend && npm run test:unit`
- npm 고위험 취약점 검사: `cd frontend && npm run audit:dependencies`
- TypeScript API client 생성: `cd frontend && npm run api:generate`
- 생성 client 최신 상태 검증: `cd frontend && npm run api:check`
- E2E 브라우저 설치: `cd frontend && npm run e2e:install`
- E2E smoke: `cd frontend && npm run e2e`
- E2E 도메인별 실행: `cd frontend && npm run e2e:payment`, `npm run e2e:identity`, `npm run e2e:admin`
- E2E 전체 실행: `cd frontend && npm run e2e:full`

## 테스트 기준

- `@UseCaseIT`는 MySQL/Redis Testcontainers와 고정 `Clock`을 사용한다.
- `@UseCaseIT`는 실제 `BatchScheduler`를 mock 처리해 cron과 테스트 본문이 같은 데이터를 경쟁하지 않게 하며, 스케줄 위임은 별도 테스트로 검증한다.
- REST Docs 스니펫은 `:adapter-in-web:restDocsTest`가 `adapter-in-web/build/generated-snippets`에 생성한다.
- Springdoc은 Controller와 웹 DTO에서 키 순서를 정규화한 `docs/PRD/0004_API_계약/openapi3.json`을 만들고, Orval은 이를 `frontend/src/generated/api`의 TypeScript client와 DTO로 변환한다.
- REST Docs는 실제 HTTP 요청·응답 예시를 검증하고, OpenAPI 스냅샷은 기계 판독 계약과 프론트 생성 코드의 원본을 담당한다.
- React feature 계층의 HTTP API 호출은 모두 생성 client를 사용한다. feature wrapper는 생성 함수와 서버 DTO를 재사용하고 React Query key·cache·화면용 view model만 소유한다. OAuth 로그인 시작처럼 브라우저가 URL로 직접 이동하는 흐름은 HTTP API wrapper가 아니므로 생성 client 대상에서 제외한다.
- Playwright 실행 전 백엔드는 `http://localhost:8080`에서 실행 중이어야 한다.
- 기본 E2E는 핵심 실사용 흐름과 오류 복구를 포함한 `@smoke` 대표 경로만 실행하고 MFA 전용 개발 서버는 생략한다. 전체 P8 회귀는 `e2e:full` 또는 도메인별 스크립트로 실행한다.
- `codexReview`와 `main` 대상 PR은 Gradle/npm/GitHub Actions 변경의 Dependency Review, npm audit, ESLint·React Hooks 검사와 app/frontend 컨테이너의 Trivy HIGH/CRITICAL 검사를 통과해야 한다. Dependabot은 Gradle, npm, GitHub Actions와 Dockerfile의 첫 번째 `FROM` 이미지를 매주 확인하고 일반 버전 갱신 PR은 `codexReview`로 보낸다. 다단계 Dockerfile의 두 번째 이후 `FROM`은 자동 갱신 대상이 아니므로 Trivy와 명시적 버전 점검으로 관리한다. GitHub 정책상 보안 갱신 PR은 기본 브랜치인 `main`을 대상으로 한다.

테스트 선택 기준은 [ADR-0027](docs/ADR/0027_테스트_전략과_최소_테스트_세트_기준선/adr.md), E2E 실행 시간 조정 배경은 [Retrospective-0009](docs/Retrospective/0009_프론트_E2E_실행_시간_슬림화/retrospective.md)에 남긴다.

## 저장소 구조

| 경로 | 역할 |
| --- | --- |
| `bootstrap/` | 애플리케이션 시작점, 공통 설정, Flyway, 로깅 |
| `adapter-in-web/` | HTTP API, 필터, 요청/응답 처리 |
| `adapter-out-persistence/` | JPA, MyBatis, 데이터베이스 연동 |
| `adapter-out-external/` | 결제, 알림, OAuth, Redis 세션, 외부 API 연동 |
| `application/` | 유스케이스, 서비스, 배치, 포트 정의 |
| `domain/` | 도메인 모델, 정책, 예외 |
| `test-support/` | 웹 DTO·영속성 repository에 의존하는 통합 테스트 fixture |
| `frontend/` | React 기반 사용자 화면과 관리자 화면 |
| `frontend/src/generated/api/` | OpenAPI 파생 TypeScript client와 DTO, 수동 편집 금지 |
| `monitoring/` | Prometheus, Grafana, Alertmanager 설정 |

- 운영 코드 의존 방향: `bootstrap -> adapter-in-web/out-* -> application -> domain`
- `test-support`는 테스트 variant에서만 소비하며 운영 산출물에는 포함하지 않는다.
- 일반 조회와 저장은 JPA, 관리자 검색과 대시보드 집계는 MyBatis를 사용한다.

## 기술 스택

- 백엔드: Spring Boot 4.1.0, Spring Security, Java 25, Gradle
- 프론트엔드: React Router Framework Mode, React 19, Vite, TypeScript, Orval
- 데이터베이스: MySQL 8, Flyway
- 세션과 캐시: Redis, Spring Session
- 인프라 목표: 단일 노트북 k3s, Kubernetes Ingress, MySQL 영속 볼륨, cluster 내부 Redis
- 로컬 개발·복구 진단: Docker Compose, Nginx reverse proxy
- 모니터링: Actuator, Prometheus, Grafana, Sentry
- API 계약: Spring REST Docs, Springdoc OpenAPI
- 테스트: JUnit 5, Testcontainers, Playwright

### 프론트엔드 디자인 기준

- 햇빛이 드는 공방을 중심 이미지로 삼고 한지색, 점토색, 잎색을 기본 팔레트로 사용한다.
- 본문은 Pretendard, 전시 제목과 브랜드 표기는 Gowun Batang 계열을 사용한다.
- 공통 색상과 컴포넌트 변수는 `frontend/src/styles/_variables.scss`에서 관리한다. `frontend/src/styles/global.scss`는 Bootstrap과 `_foundation.scss`, `_admin.scss`, `_storefront.scss`, `_atelier.scss`, `_brand.scss`의 로딩 순서만 소유한다.
- 홈과 클래스·단체수업 화면은 `frontend/src/assets/happygallery`의 실제 공방 사진을 사용한다. 사진 원문은 같은 디렉터리의 `SOURCES.md`에 기록하며, 외부 이미지 CDN에 런타임 의존하지 않는다.
- 공개 화면은 요청 시점 SSR로 본문·제목·설명·canonical·Open Graph·JSON-LD를 제공하고, 인증·결제·고객 이력·관리자 경로는 client-only 화면과 `noindex`로 분리한다.
- 대표 운영 origin은 `https://happy-gallery.com`이며 robots·sitemap·canonical에서 같은 origin만 사용한다.

## 운영/배포

AWS 운영 배포는 폐기했다. 목표 운영 환경은 소유한 단일 노트북의 단일 노드 k3s다.

```text
브라우저 -> DNS/공유기/방화벽 -> k3s Ingress(TLS)
                                  -> /api/* -> Spring Boot -> cluster 내부 MySQL/Redis
                                  -> 그 외   -> React Router SSR
```

- 프론트엔드와 API는 같은 origin으로 제공하고 외부에는 ingress의 HTTP/HTTPS 포트만 연다.
- 애플리케이션, MySQL, Redis와 관리·모니터링 포트는 외부에 직접 공개하지 않는다.
- Docker Compose는 로컬 개발, 통합 검증과 복구 진단용이다. 현재 `local` 프로필과 개발 기본값을 사용하므로 운영 배포 기준이 아니다.
- [`deploy/k3s`](deploy/k3s/README.md)에 namespace, ingress/TLS, MySQL·미디어 PVC, 비공개 Actuator/Prometheus, secret 주입, 불변 이미지 import, rollout·rollback, DB·미디어 암호화 백업·복원 절차를 둔다.
- k3s Secret 생성은 파일별 허용 키만 받는다. 운영 모드·`prod` 단일 프로필·관리자 MFA 등록 강제·처리율 제한·Secure cookie 같은 불변식은 Secret보다 우선하는 manifest 환경 변수와 Spring context·Flyway 생성 전 환경 검증으로 고정한다.
- 운영 관리자 로그인은 MFA 미등록 세션을 등록 전용으로 제한한다. 인증 앱을 잃었지만 복구 코드가 남아 있으면 해당 코드로 로그인한 세션에서 현재 비밀번호를 확인해 MFA를 초기화하고 다시 등록할 수 있다. 초기화는 관리자 ID별 5회/10분 fail-closed 제한을 적용한 뒤 DB 잠금과 비밀번호 확인을 수행한다. DB·미디어 복원 뒤에도 app은 자동 기동하지 않는다. 복구 묶음마다 백업 생성시각과 복구 환경 해시로 일회성 대사 토큰을 만들고, 운영자가 PG·알림·개인정보 요청 대사를 완료한 뒤 같은 토큰으로 세 확인값을 제출해야 호환 이미지를 한 번만 활성화한다.
- 운영 프런트 Node SSR 서버는 응답별 nonce와 Toss SDK, 외부 폰트, Sentry를 반영한 CSP를 `Report-Only`로 제공한다. 아직 중앙 위반 수집기는 없으므로 배포 전 실제 브라우저 콘솔에서 핵심 화면을 확인한 뒤 강제 정책 전환을 별도로 결정한다.
- 대표 공개 주소는 `https://happy-gallery.com`으로 확정했다. 실제 노트북에서 DNS·공유기·방화벽·TLS·검색엔진 소유확인·복원 훈련과 핵심 사용자 흐름을 검증하기 전에는 운영 중으로 간주하지 않는다.
- 기준 공방 프로필에는 공개 결제에 필요한 대표자명, 전자우편주소와 통신판매업 신고번호가 포함된다. 배포 전 footer·사업자 정보 화면의 표시값을 확인해야 하며, `prod` 프로필은 연락처·주소·사업자등록번호를 포함한 필수 온라인 판매 고지가 완성되기 전 모든 결제 prepare를 `503`으로 차단한다. 표시 근거는 전자상거래법 [제10조](https://www.law.go.kr/LSW/lsLinkCommonInfo.do?chrClsCd=010202&lsJoLnkSeq=1022342373)와 [제13조](https://www.law.go.kr/LSW/lsLinkCommonInfo.do?chrClsCd=010202&lsJoLnkSeq=1022341933)다.

운영 목표와 불변 조건은 [ADR-0037 자가 호스팅 배포 토폴로지 기준](docs/ADR/0037_자가_호스팅_배포_토폴로지_기준/adr.md)을, 공개 검색 문서와 SSR·canonical·sitemap·HTTP 상태 경계는 [ADR-0045 공개 페이지 SSR과 SEO 전달 경계](docs/ADR/0045_공개_페이지_SSR과_SEO_전달_경계/adr.md)를 따른다. 이전 AWS 구조와 배포 설정은 [Idea-0028](docs/Idea/0028_CloudFront_S3_ALB_배포_구조/idea.md), [Idea-0029](docs/Idea/0029_GitHub_Actions_CI_CD_배포_Fargate/idea.md), [Idea-0039](docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md)에 역사 기록으로 남긴다.

## 주요 환경 변수

`*_MILLIS`, `*_SECONDS`, `*_HOURS` 환경 변수는 기존 숫자 계약을 유지한다. `application.yml`이 각각 `ms`, `s`, `h` 단위를 붙여 애플리케이션의 `Duration` 설정으로 바인딩하므로, 기존 배포 값은 바꾸지 않아도 된다. 이메일 인증 SMTP의 host·port·자격 증명·TLS·transport timeout은 `spring.mail.*`로 연결되어 Spring Boot가 `JavaMailSender`를 자동 구성하고, 애플리케이션은 발신 주소·제목·바깥 TimeLimiter와 계층 불변식만 관리한다.

Delivery API를 처음 연결할 때는 `DELIVERY_TRACKING_ENABLED=false`와 직접 생성한 `DELIVERY_WEBHOOK_SECRET`을 먼저 배포한다. 그다음 `https://<운영 호스트>/api/v1/webhooks/delivery-tracking`을 같은 secret으로 Delivery API에 등록해 `endpointId`를 받은 뒤 API 키·endpoint ID와 함께 연동을 활성화한다. 웹훅 URL은 외부에서 접근 가능한 HTTPS여야 한다.

Toss 운영 콘솔에는 결제 상태 변경 웹훅 URL로 `https://<운영 호스트>/api/v1/webhooks/toss-payments`를 등록한다. 웹훅은 `PAYMENT_STATUS_CHANGED`만 수신 기록하고, 알려진 `orderId`를 기존 결제 대사 흐름으로 확인한다.

도로명주소와 공휴일 연동은 각각 공식 서비스 승인키를 발급한 뒤 키를 먼저 주입하고 `ROAD_ADDRESS_ENABLED`, `PUBLIC_HOLIDAY_ENABLED`를 켠다. 주소 검색 장애 때 고객과 관리자는 주소를 직접 입력할 수 있고, 공휴일 조회 실패 때는 마지막 정상 스냅샷을 보존한다.

| 이름 | 위치 | 설명 |
| --- | --- | --- |
| `TOSS_SECRET_KEY` | 백엔드 `prod` | Toss Payments secret key |
| `PAYMENT_TIMEOUT_MILLIS` | 백엔드 | PG 호출 바깥 TimeLimiter, 기본 `5000` |
| `TOSS_ACQUIRE_TIMEOUT_MILLIS` / `TOSS_CONNECT_TIMEOUT_MILLIS` / `TOSS_TIMEOUT_MILLIS` | 백엔드 `prod` | Toss 연결 풀 획득·연결·응답 상한, 기본 `500` / `1000` / `3000`; 합이 바깥 TimeLimiter보다 작아야 함 |
| `TOSS_SETTLEMENT_TIMEOUT_SECONDS` / `TOSS_SETTLEMENT_MAX_CONNECTIONS` | 백엔드 `prod` | Toss 정산 조회 전용 응답 상한·커넥션 수, 기본 `60` / `2`; 승인·환불용 짧은 풀과 분리 |
| `VITE_TOSS_CLIENT_KEY` | 프론트 빌드 | Toss SDK client key |
| `VITE_API_TARGET` | 프론트 개발 서버 | `/api` 프록시 대상, 기본 `http://localhost:8080` |
| `PAYMENT_EXECUTOR_POOL_SIZE` | 백엔드 | PG 호출 실행 스레드 수, 기본 `4` |
| `ASYNC_EXECUTOR_CORE_SIZE` / `ASYNC_EXECUTOR_MAX_SIZE` | 백엔드 | 알림·환불 커밋 후 실행기 기본/최대 스레드 수, 기본 `2`/`4` |
| `ASYNC_EXECUTOR_QUEUE_CAPACITY` | 백엔드 | 알림·환불 커밋 후 실행 신호 대기열 크기, 기본 `100` |
| `BATCH_SCHEDULER_POOL_SIZE` | 백엔드 | Spring 스케줄러 스레드 수, 기본 `4` |
| `DB_HIKARI_IDLE_TIMEOUT_MS` / `DB_HIKARI_MAX_LIFETIME_MS` | 백엔드 | 유휴 커넥션 정리·최대 수명, 기본 `300000` / `540000`; 유휴 정리가 최대 수명보다 먼저 실행돼야 함 |
| `PAYMENT_EXECUTOR_QUEUE_CAPACITY` | 백엔드 | PG 호출 대기열 크기, 기본 `20` |
| `ALIMTALK_NOTIFICATION_EXECUTOR_POOL_SIZE` / `ALIMTALK_NOTIFICATION_EXECUTOR_QUEUE_CAPACITY` | 백엔드 | Alimtalk timeout 보호 실행기, 기본 `2` / `5` |
| `SMS_NOTIFICATION_EXECUTOR_POOL_SIZE` / `SMS_NOTIFICATION_EXECUTOR_QUEUE_CAPACITY` | 백엔드 | 일반 SMS timeout 보호 실행기, 기본 `2` / `5` |
| `PHONE_VERIFICATION_EXECUTOR_POOL_SIZE` / `PHONE_VERIFICATION_EXECUTOR_QUEUE_CAPACITY` | 백엔드 | 휴대폰 인증 SMS timeout 보호 실행기, 기본 `2` / `10` |
| `EMAIL_VERIFICATION_EXECUTOR_POOL_SIZE` / `EMAIL_VERIFICATION_EXECUTOR_QUEUE_CAPACITY` | 백엔드 | 이메일 인증 SMTP timeout 보호 실행기, 기본 `2` / `10` |
| `NOTIFICATION_TIMEOUT_MILLIS` | 백엔드 | 알림 외부 호출 전체 TimeLimiter, 기본 `5000` |
| `ALIMTALK_TIMEOUT_MILLIS` / `SMS_TIMEOUT_MILLIS` | 백엔드 `prod` | NHN 응답 대기 상한, 기본 `2000` (연결 풀 `500` + 연결 `1000`보다 바깥 TimeLimiter가 크게 유지돼야 함) |
| `EMAIL_VERIFICATION_SMTP_HOST` / `EMAIL_VERIFICATION_SMTP_PORT` | 백엔드 `prod` | 회원 이메일 소유 확인용 SMTP 서버와 포트, 기본 포트 `587` |
| `EMAIL_VERIFICATION_SMTP_USERNAME` / `EMAIL_VERIFICATION_SMTP_PASSWORD` / `EMAIL_VERIFICATION_FROM` | 백엔드 `prod` | 이메일 인증 SMTP 자격 증명과 발신 주소 |
| `EMAIL_VERIFICATION_TIMEOUT_MILLIS` | 백엔드 `prod` | SMTP 큐 대기를 포함한 전용 TimeLimiter, 기본 `7000`; 아래 transport timeout 합보다 커야 함 |
| `EMAIL_VERIFICATION_CONNECTION_TIMEOUT_MILLIS` / `EMAIL_VERIFICATION_READ_TIMEOUT_MILLIS` / `EMAIL_VERIFICATION_WRITE_TIMEOUT_MILLIS` | 백엔드 `prod` | SMTP 연결·읽기·쓰기 대기 상한, 기본 `1000` / `2000` / `2000` |
| `EMAIL_VERIFICATION_STARTTLS_ENABLED` / `EMAIL_VERIFICATION_SSL_ENABLED` | 백엔드 `prod` | SMTP TLS 모드, 기본 `true` / `false`; 정확히 하나를 켜며 인증서 호스트명을 검증 |
| `MAIL_HEALTH_ENABLED` | 백엔드 | Spring Mail health indicator 활성화 여부, 기본 `false`; 이메일 장애가 전역 readiness를 내리지 않게 알림 CircuitBreaker로 분리 관측 |
| `PASS_TOTAL_PRICE` | 백엔드 | 8회권 결제 금액 |
| `ORDER_SHIPPING_FEE` | 백엔드 | 배송 주문에 더하는 고정 배송비, 기본 `0`원 |
| `DELIVERY_TRACKING_ENABLED` | 백엔드 | Delivery API 배송조회 연동 활성화 여부, 기본 `false` |
| `DELIVERY_API_KEY` / `DELIVERY_API_SECRET_KEY` | 백엔드 | Delivery API 호출 자격 증명 |
| `DELIVERY_WEBHOOK_ENDPOINT_ID` / `DELIVERY_WEBHOOK_SECRET` | 백엔드 | 배송조회 등록 대상 웹훅 ID와 수신 서명 검증 키 |
| `DELIVERY_API_ACQUIRE_TIMEOUT_MILLIS` / `DELIVERY_API_CONNECT_TIMEOUT_MILLIS` / `DELIVERY_API_TIMEOUT_MILLIS` | 백엔드 | 배송조회 연결 풀 획득·연결·응답 상한, 기본 `500` / `1000` / `3000` |
| `ROAD_ADDRESS_ENABLED` / `ROAD_ADDRESS_CONFIRMATION_KEY` | 백엔드 | 도로명주소 검색 연동 활성화 여부와 주소기반산업지원서비스 검색 API 승인키, 기본 비활성 |
| `ROAD_ADDRESS_ACQUIRE_TIMEOUT_MILLIS` / `ROAD_ADDRESS_CONNECT_TIMEOUT_MILLIS` / `ROAD_ADDRESS_TIMEOUT_MILLIS` | 백엔드 | 도로명주소 연결 풀 획득·연결·응답 상한, 기본 `500` / `1000` / `3000` |
| `PUBLIC_HOLIDAY_ENABLED` / `PUBLIC_HOLIDAY_SERVICE_KEY` | 백엔드 | 한국천문연구원 특일 정보 연동 활성화 여부와 공공데이터포털 서비스키, 기본 비활성 |
| `PUBLIC_HOLIDAY_ACQUIRE_TIMEOUT_MILLIS` / `PUBLIC_HOLIDAY_CONNECT_TIMEOUT_MILLIS` / `PUBLIC_HOLIDAY_TIMEOUT_MILLIS` | 백엔드 | 공휴일 조회 연결 풀 획득·연결·응답 상한, 기본 `500` / `1000` / `5000` |
| `MEDIA_STORAGE_PATH` | 백엔드 | 관리자·후기 업로드 이미지 저장 경로, 로컬 기본 `./data/media` |
| `REVIEW_IMAGE_MAX_CONCURRENT_DECODES` | 백엔드 | 회원 후기 사진 동시 디코딩 상한, 기본 `2`; 포화 시 대기 없이 `429` 반환 |
| `GUEST_TOKEN_EXPIRY_HOURS` | 백엔드 | 비회원 주문·예약 접근 및 결제 상태 조회 토큰 수명, 기본 `720`시간 |
| `GUEST_TOKEN_RECOVERY_EXPIRY_HOURS` | 백엔드 | 비회원 조회 정보 복구 토큰 수명, 기본 `24`시간 |
| `GOOGLE_OAUTH_CLIENT_ID` | 백엔드 `prod` | Google 로그인 client ID |
| `GOOGLE_OAUTH_CLIENT_SECRET` | 백엔드 `prod` | Google 로그인 client secret |
| `GOOGLE_OAUTH_REDIRECT_URI` | 백엔드 `prod` | Google에 등록한 exact backend callback URI (`https://<host>/api/v1/auth/social/callback/google`) |
| `NAVER_OAUTH_CLIENT_ID` | 백엔드 `prod` | Naver 로그인 client ID |
| `NAVER_OAUTH_CLIENT_SECRET` | 백엔드 `prod` | Naver 로그인 client secret |
| `NAVER_OAUTH_REDIRECT_URI` | 백엔드 `prod` | Naver에 등록한 exact backend callback URI (`https://<host>/api/v1/auth/social/callback/naver`) |
| `KAKAO_OAUTH_CLIENT_ID` | 백엔드 `prod` | Kakao 로그인 REST API key |
| `KAKAO_OAUTH_CLIENT_SECRET` | 백엔드 `prod` | Kakao 로그인 client secret |
| `KAKAO_OAUTH_REDIRECT_URI` | 백엔드 `prod` | Kakao Developers에 등록한 exact backend callback URI (`https://<host>/api/v1/auth/social/callback/kakao`) |
| `ALIMTALK_APP_KEY` | 백엔드 `prod` | NHN Cloud Alimtalk 서비스 app key |
| `ALIMTALK_SECRET_KEY` | 백엔드 `prod` | NHN Cloud Alimtalk `X-Secret-Key` 값 |
| `ALIMTALK_SENDER_KEY` | 백엔드 `prod` | NHN Cloud에 등록한 카카오 발신 프로필 키 |
| `SMS_API_KEY` | 백엔드 `prod` | NHN Cloud 일반·인증 SMS app key |
| `SMS_API_SECRET` | 백엔드 `prod` | NHN Cloud SMS API secret |
| `SMS_SENDER_NUMBER` | 백엔드 `prod` | 사전 등록한 SMS 발신 번호 |
| `RATE_LIMIT_ENABLED` | 백엔드 | 로컬 반복 검증 시 처리율 제한 off 가능 |
| `RATE_LIMIT_KEY_PREFIX` | 백엔드 | 환경별 Redis 처리율 제한 키 prefix |
| `REDIS_CONNECT_TIMEOUT` | 백엔드 | Redis 연결 대기 상한, 기본 `1s` |
| `REDIS_COMMAND_TIMEOUT` | 백엔드 | Redis 명령 대기 상한, 기본 `1s` |
| `SESSION_SECURE_COOKIE` | 백엔드 | 회원 세션 쿠키의 Secure 여부, 기본 `true`이며 local 프로필은 `false` |
| `FORWARD_HEADERS_STRATEGY` | 백엔드 `prod` | 통제된 ingress 구성 후 `native`로 설정 |
| `FIELD_ENCRYPTION_KEY_ID` | 백엔드 `prod` | 활성 AES/HMAC 키 쌍의 버전 ID, 기본 `v1` |
| `ENCRYPT_KEY` | 백엔드 `prod` | 활성 개인정보 AES-256 키, 64자리 hex |
| `HMAC_KEY` | 백엔드 `prod` | 활성 블라인드 인덱스 HMAC 키, 64자리 hex |
| `PREVIOUS_ENCRYPT_KEYS` / `PREVIOUS_HMAC_KEYS` | 백엔드 `prod` | 회전 중에만 유지하는 `keyId=64자리hex` 이전 키 목록 |
| `GUEST_TOKEN_HMAC_SECRET` | 백엔드 `prod` | 활성 비회원 접근 토큰 서명 키 |
| `GUEST_TOKEN_PREVIOUS_HMAC_SECRET` | 백엔드 `prod` | 회전 전 발급 토큰의 만료까지 한시적으로 검증하는 이전 키 |
| `ADMIN_SETUP_TOKEN` | 백엔드 | 최초 관리자 계정 생성용 일회성 토큰 |
| `ADMIN_REQUIRE_MFA_ENROLLMENT` | 백엔드 | MFA 미등록 관리자 세션을 등록 전용으로 제한하며 `prod`는 항상 `true` |

환경별 전체 설정은 [application.yml](bootstrap/src/main/resources/application.yml)과 [application-local.yml](bootstrap/src/main/resources/application-local.yml)을 기준으로 확인한다.
데이터 결합 키는 Secret을 직접 수정하지 않고 [k3s 데이터 키 회전 절차](deploy/k3s/README.md#2-secret-준비)로만 교체한다.

Naver 로그인 운영 등록 조건:

- Naver Developers 애플리케이션에 서비스 origin과 정확한 백엔드 콜백 URI `${서비스 origin}/api/v1/auth/social/callback/naver`를 등록한다.
- 회원 프로필의 이름 제공 항목을 사용하도록 설정한다. 서비스는 provider ID와 이름을 요구하고, Naver 프로필 이메일은 검증된 기준 이메일로 저장하지 않는다. 기준 이메일이 없는 회원은 마이페이지에서 별도 SMTP 소유 확인을 마친 뒤 직접 등록한다.
- 로그인 버튼은 [Naver 로그인 버튼 사용 가이드](https://developers.naver.com/docs/login/bi/bi.md)의 공식 심벌과 지정 색상을 사용한다.

Kakao 로그인 운영 등록 조건:

- Kakao Developers에서 카카오 로그인을 활성화하고 정확한 백엔드 콜백 URI `${서비스 origin}/api/v1/auth/social/callback/kakao`를 등록한다.
- 동의 항목에서 닉네임과 카카오계정 이메일을 제공하도록 설정한다. 서비스는 이메일이 유효하고 검증된 경우에만 기준 이메일로 사용하며, 두 상태를 확인할 수 없으면 로그인을 거절한다.
- 보안을 위해 client secret을 활성화하고 `KAKAO_OAUTH_CLIENT_SECRET`에 별도로 보관한다.

## 문서 진입점

- 요구사항 기준: [docs/PRD/0001_기준_스펙/spec.md](docs/PRD/0001_기준_스펙/spec.md)
- API 계약: [docs/PRD/0004_API_계약/spec.md](docs/PRD/0004_API_계약/spec.md)
- 설계 결정: [docs/ADR](docs/ADR/)
- 배경 메모와 검토 기록: [docs/Idea](docs/Idea/)
- 회고와 트러블슈팅 기록: [docs/Retrospective](docs/Retrospective/)

`docs/Idea`는 배경 메모다. 현재 동작과 운영 기준은 PRD와 ADR을 먼저 본다.
