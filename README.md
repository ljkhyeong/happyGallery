# happyGallery

`happyGallery`는 오프라인 공방의 상품 주문, 클래스 예약, 8회권, 관리자 운영을 다루는 서비스다.
백엔드는 Spring Boot 멀티 모듈 애플리케이션이고, 프론트엔드는 Vite + React SPA다.

## 한눈에 보기

| 사용자 | 주요 기능 |
| --- | --- |
| 비회원 | 휴대폰 인증 기반 주문/예약 생성, 토큰 기반 조회·취소·지연 응답·주문 클레임, SMS 기반 조회 정보 복구, 회원가입 후 기존 이력 가져오기 |
| 회원 | 상품 주문·취소·지연 응답·주문 클레임, 클래스 예약, 8회권 구매·사용·환불, 장바구니, 알림함, 소셜 계정·휴대폰 관리, 회원 탈퇴 |
| 관리자 | 오늘 할 일 중심 운영 화면, 상품·클래스 콘텐츠/이미지, 공방 프로필·슬롯 일괄 관리, 예약 검색·운영자 취소·후속 정산, 8회권 검색·환불, 주문 승인/거절/배송/픽업·클레임, 환불 재처리, Q&A/문의 답변 |

- 주문/예약/8회권은 `POST /api/v1/payments/prepare` -> `POST /api/v1/payments/confirm` 표준 결제 경로를 사용한다. confirm은 선점·PG 승인·도메인 저장 트랜잭션을 분리하고 Toss 멱등키와 실패 보상 환불을 사용하며, 고객은 소유권이 확인된 `GET /api/v1/payments/{orderId}`로 처리 결과를 확인한다. 비회원이 브라우저 저장소를 잃으면 SMS 인증 기반 `POST /api/v1/guest-records/payment-status-recovery`로 결제 ID 목록과 공통 상태 조회 토큰을 함께 복구한다.
- 이메일·소셜 최초 가입과 비회원 주문·예약 결제는 `GET /api/v1/policies/current`로 조회한 이용약관·개인정보처리방침 버전에 동의해야 한다. 서버는 현재 버전을 다시 검증하고 회원 또는 결제 시도에 동의 유형·목적·서버 수락 시각을 추가 이력으로 보존한다.
- 8회권 사용 예약은 운영자가 일정을 일괄 배정하지 않는다. 회원이 예약 가능 슬롯을 직접 선택해 한 회차씩 예약하고 크레딧 1회를 사용한다.
- 정규 공예 8회권은 구매 시 계획을 저장하고, 운영자가 사용 가능으로 지정한 비향수 클래스에만 적용한다. 회원은 내 8회권에서 잔여 횟수 정산 환불을 요청하고 진행 상태를 조회한다.
- 환불은 요청 이력을 먼저 커밋한 뒤 PG를 호출한다. 실행 유실·일시 실패는 최초 멱등키로 복구하고, 결과 불명 상태는 PG 취소 내역을 먼저 조회해 성공 여부를 화해한 뒤에만 취소 재호출 여부를 결정한다.
- 배송 주문은 `ORDER_SHIPPING_FEE` 고정액을 결제 준비 시 확정해 주문에 저장한다. 상품명·단가, 배송비와 택배사·운송장 정보는 과거 주문을 재현할 수 있게 스냅샷으로 유지한다.
- 배송·픽업이 끝난 주문은 회원 세션 또는 비회원 접근 토큰으로 품목·수량별 환불·교환 클레임을 접수한다. 관리자는 환불액과 반품 재고 복구 여부를 결정하고, 환불액의 상품별 배분을 승인 시 고정해 매출 통계에 반영한다. 교환품 재고는 승인 시 다시 차감하며 모든 PG 환불은 기존 비동기 복구 경계를 사용한다.
- 관리자는 기성품의 승인 전 재고 부족과 주문제작의 제작 중 일정 변경에 대해 주문 처리 지연을 제안할 수 있다. 고객이 수락한 뒤 `/api/v1/admin/orders/{id}/resume-after-delay`로 재개하면 기성품은 이행 대기, 주문제작은 제작 중으로 돌아간다. 예상 출고일 설정·갱신과 지연·재개는 Bearer 세션의 관리자 ID로 처리 이력에 남기며, 출고일 변경 이력에는 이전·이후 날짜를 기록한다.
- 공개 상품 Q&A는 일반글 상세를 비밀번호 없이 조회하고, 비밀글 상세에만 비밀번호 검증을 요구한다.
- 관리자 주문·예약 검색 결과의 `운영` 액션은 해당 주문 또는 예약을 기존 운영 패널에서 바로 찾고 후속 처리할 수 있게 연결한다.
- 고객은 클래스별 향후 1~30일 예약 가능 슬롯을 날짜별로 탐색한다. 운영자 사정으로 예약을 취소하면 고객 취소 마감과 무관하게 예약금 전액 환불 또는 유효한 8회권 크레딧 복구를 시작하며, 처리 관리자와 사유를 예약 이력에 남긴다. 이미 받은 현장 잔금이나 만료 이용권 보상은 영속 후속 작업으로 남겨 관리자가 완료 처리한다.
- 관리자는 이름·전화번호·`PASS-00000001` 형식 이용권 번호로 8회권을 찾아 잔여 횟수, 미래 예약, 예상 환불액과 환불 상태를 확인한 뒤 환불한다.
- 상품·클래스 설명과 대표 이미지, 공방 주소·영업·주차·소개·사업자·문의 정보를 관리자 화면에서 관리한다. 공개 footer와 이용약관·개인정보처리방침·사업자 정보 화면은 같은 공방 프로필을 사용한다. 반복 슬롯은 기간·요일·시각 조합을 미리 본 뒤 일괄 생성한다.
- 기준 공방 프로필은 `해피갤러리`, `충북 충주시 계명대로 161 1층`, 네이버 플레이스 `https://m.place.naver.com/place/21668321`, `010-9635-5608`, 대표 `홍지현`, 사업자등록번호 `303-11-87052`, 통신판매업 신고번호 `2011-충북 충주-127`, 전자우편 `ssi1972@naver.com`, 카카오톡 `ssim1972`를 사용한다. 네이버톡톡·네이버 블로그·인스타그램·스마트스토어 링크도 공방 프로필에서 함께 관리한다.
- 회원은 `HG_SESSION`, 관리자는 Bearer 세션, 비회원은 `X-Access-Token`을 사용한다.
- Google·Naver 계정은 마이페이지에서 일회성 연결 시도와 OAuth `state`를 검증해 명시적으로 연결·해제하며, 이메일 일치만으로 기존 회원과 자동 병합하지 않는다. Google의 검증 이메일만 기준 이메일로 저장하고, 신규 Naver 회원의 이메일은 자체 검증 전까지 `null`이다.
- 브라우저의 비관리자 상태 변경 요청은 `XSRF-TOKEN` 쿠키와 `X-XSRF-TOKEN` 헤더로 CSRF를 방어한다.
- 상세 요구사항은 [기준 스펙](docs/PRD/0001_기준_스펙/spec.md), HTTP 계약은 [API 계약](docs/PRD/0004_API_계약/spec.md)을 기준으로 본다.

## 빠른 시작

### 요구사항

- Java 21
- Node.js 22.18+
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
- `prod`가 아닌 환경에서는 실제 알림·인증 SMS·결제 대신 테스트용 발송기와 `FakePaymentProvider`를 사용한다.
- k3s 운영 배포의 Prometheus 경보는 내부 Alertmanager를 거쳐 저장소 밖 Secret으로 주입한 외부 HTTPS webhook에 전달한다. 노트북 자체 장애 감시는 별도 외부 uptime 서비스가 필요하다.
- 운영 환경은 DB·Redis를 readiness에 포함하고, 환불·알림 outbox의 DB backlog, 결제·알림 CircuitBreaker 상태와 호출 결과, 모든 정기 배치의 마지막 정상 완료 시각과 이미지 저장소 용량을 Prometheus·Grafana에서 감시한다.
- `prod`가 아닌 환경의 Google/Naver 로그인은 외부 인증 화면 없이 테스트용 콜백으로 즉시 돌아온다.
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

### 프론트엔드

- 개발 서버: `cd frontend && npm run dev`
- 프로덕션 빌드: `cd frontend && npm run build`
- ESLint·React Hooks 검사: `cd frontend && npm run lint`
- npm 고위험 취약점 검사: `cd frontend && npm run audit:dependencies`
- TypeScript API client 생성: `cd frontend && npm run api:generate`
- 생성 client 최신 상태 검증: `cd frontend && npm run api:check`
- E2E 브라우저 설치: `cd frontend && npm run e2e:install`
- E2E smoke: `cd frontend && npm run e2e`
- E2E 도메인별 실행: `cd frontend && npm run e2e:payment`, `npm run e2e:identity`, `npm run e2e:admin`
- E2E 전체 실행: `cd frontend && npm run e2e:full`

## 테스트 기준

- `@UseCaseIT`는 MySQL/Redis Testcontainers와 고정 `Clock`을 사용한다.
- REST Docs 스니펫은 `:adapter-in-web:restDocsTest`가 `adapter-in-web/build/generated-snippets`에 생성한다.
- Springdoc은 Controller와 웹 DTO에서 키 순서를 정규화한 `docs/PRD/0004_API_계약/openapi3.json`을 만들고, Orval은 이를 `frontend/src/generated/api`의 TypeScript client와 DTO로 변환한다.
- REST Docs는 실제 HTTP 요청·응답 예시를 검증하고, OpenAPI 스냅샷은 기계 판독 계약과 프론트 생성 코드의 원본을 담당한다.
- 현재 React 실사용 전환 범위는 공개 상품 목록·카테고리·상세 조회다. 새 엔드포인트는 필수값·nullable·enum 정확성을 확인한 뒤 같은 방식으로 전환한다.
- Playwright 실행 전 백엔드는 `http://localhost:8080`에서 실행 중이어야 한다.
- 기본 E2E는 `@smoke` 대표 경로만 실행한다. 전체 P8 회귀는 `e2e:full` 또는 도메인별 스크립트로 실행한다.
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

- 백엔드: Spring Boot 4.0.7, Spring Security, Java 21, Gradle
- 프론트엔드: Vite, React 19, TypeScript, Orval
- 데이터베이스: MySQL 8, Flyway
- 세션과 캐시: Redis, Spring Session
- 인프라 목표: 단일 노트북 k3s, Kubernetes Ingress, MySQL 영속 볼륨, cluster 내부 Redis
- 로컬 개발·복구 진단: Docker Compose, Nginx
- 모니터링: Actuator, Prometheus, Grafana, Sentry
- API 계약: Spring REST Docs, Springdoc OpenAPI
- 테스트: JUnit 5, Testcontainers, Playwright

### 프론트엔드 디자인 기준

- 햇빛이 드는 공방을 중심 이미지로 삼고 한지색, 점토색, 잎색을 기본 팔레트로 사용한다.
- 본문은 Pretendard, 전시 제목과 브랜드 표기는 Gowun Batang 계열을 사용한다.
- 공통 색상과 컴포넌트 변수는 `frontend/src/styles/_variables.scss`에서 관리한다. `frontend/src/styles/global.scss`는 Bootstrap과 `_foundation.scss`, `_admin.scss`, `_storefront.scss`, `_atelier.scss`, `_brand.scss`의 로딩 순서만 소유한다.
- 홈과 클래스·단체수업 화면은 `frontend/src/assets/happygallery`의 실제 공방 사진을 사용한다. 사진 원문은 같은 디렉터리의 `SOURCES.md`에 기록하며, 외부 이미지 CDN에 런타임 의존하지 않는다.
- SPA 경로 변경 때 공개 화면의 제목·설명·Open Graph 메타를 갱신하고, 인증·결제·고객 이력·관리자 경로는 `noindex`와 `robots.txt`로 검색 노출 대상에서 제외한다.

## 운영/배포

AWS 운영 배포는 폐기했다. 목표 운영 환경은 소유한 단일 노트북의 단일 노드 k3s다.

```text
브라우저 -> DNS/공유기/방화벽 -> k3s Ingress(TLS)
                                  -> /api/* -> Spring Boot -> cluster 내부 MySQL/Redis
                                  -> 그 외   -> React SPA
```

- 프론트엔드와 API는 같은 origin으로 제공하고 외부에는 ingress의 HTTP/HTTPS 포트만 연다.
- 애플리케이션, MySQL, Redis와 관리·모니터링 포트는 외부에 직접 공개하지 않는다.
- Docker Compose는 로컬 개발, 통합 검증과 복구 진단용이다. 현재 `local` 프로필과 개발 기본값을 사용하므로 운영 배포 기준이 아니다.
- [`deploy/k3s`](deploy/k3s/README.md)에 namespace, ingress/TLS, MySQL·미디어 PVC, 비공개 Actuator/Prometheus, secret 주입, 불변 이미지 import, rollout·rollback, DB·미디어 암호화 백업·복원 절차를 둔다.
- 운영 프런트 Nginx는 Toss SDK, 외부 폰트, Sentry와 JSON-LD hash를 반영한 CSP를 `Report-Only`로 제공한다. 아직 중앙 위반 수집기는 없으므로 배포 전 실제 브라우저 콘솔에서 핵심 화면을 확인한 뒤 강제 정책 전환을 별도로 결정한다.
- 현재 공개 운영 주소와 자동 배포 workflow는 없다. 실제 노트북에서 DNS·공유기·방화벽·TLS·복원 훈련과 핵심 사용자 흐름을 검증하기 전에는 운영 중으로 간주하지 않는다.
- 기준 공방 프로필에는 공개 결제에 필요한 대표자명, 전자우편주소와 통신판매업 신고번호가 포함된다. 배포 전 footer·사업자 정보 화면의 표시값을 확인해야 하며, `prod` 프로필은 연락처·주소·사업자등록번호를 포함한 필수 온라인 판매 고지가 완성되기 전 모든 결제 prepare를 `503`으로 차단한다. 표시 근거는 전자상거래법 [제10조](https://www.law.go.kr/LSW/lsLinkCommonInfo.do?chrClsCd=010202&lsJoLnkSeq=1022342373)와 [제13조](https://www.law.go.kr/LSW/lsLinkCommonInfo.do?chrClsCd=010202&lsJoLnkSeq=1022341933)다.

운영 목표와 불변 조건은 [ADR-0037 자가 호스팅 배포 토폴로지 기준](docs/ADR/0037_자가_호스팅_배포_토폴로지_기준/adr.md)을 따른다. 이전 AWS 구조와 배포 설정은 [Idea-0028](docs/Idea/0028_CloudFront_S3_ALB_배포_구조/idea.md), [Idea-0029](docs/Idea/0029_GitHub_Actions_CI_CD_배포_Fargate/idea.md), [Idea-0039](docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md)에 역사 기록으로 남긴다.

## 주요 환경 변수

| 이름 | 위치 | 설명 |
| --- | --- | --- |
| `TOSS_SECRET_KEY` | 백엔드 `prod` | Toss Payments secret key |
| `VITE_TOSS_CLIENT_KEY` | 프론트 빌드 | Toss SDK client key |
| `VITE_API_TARGET` | 프론트 개발 서버 | `/api` 프록시 대상, 기본 `http://localhost:8080` |
| `PAYMENT_EXECUTOR_POOL_SIZE` | 백엔드 | PG 호출 실행 스레드 수, 기본 `4` |
| `PAYMENT_EXECUTOR_QUEUE_CAPACITY` | 백엔드 | PG 호출 대기열 크기, 기본 `20` |
| `ALIMTALK_NOTIFICATION_EXECUTOR_POOL_SIZE` / `ALIMTALK_NOTIFICATION_EXECUTOR_QUEUE_CAPACITY` | 백엔드 | Alimtalk timeout 보호 실행기, 기본 `2` / `5` |
| `SMS_NOTIFICATION_EXECUTOR_POOL_SIZE` / `SMS_NOTIFICATION_EXECUTOR_QUEUE_CAPACITY` | 백엔드 | 일반 SMS timeout 보호 실행기, 기본 `2` / `5` |
| `PHONE_VERIFICATION_EXECUTOR_POOL_SIZE` / `PHONE_VERIFICATION_EXECUTOR_QUEUE_CAPACITY` | 백엔드 | 휴대폰 인증 SMS timeout 보호 실행기, 기본 `2` / `10` |
| `NOTIFICATION_TIMEOUT_MILLIS` | 백엔드 | 알림 외부 호출 전체 TimeLimiter, 기본 `5000` |
| `ALIMTALK_TIMEOUT_MILLIS` / `SMS_TIMEOUT_MILLIS` | 백엔드 `prod` | NHN 응답 대기 상한, 기본 `2000` (연결 풀 `500` + 연결 `1000`보다 바깥 TimeLimiter가 크게 유지돼야 함) |
| `PASS_TOTAL_PRICE` | 백엔드 | 8회권 결제 금액 |
| `ORDER_SHIPPING_FEE` | 백엔드 | 배송 주문에 더하는 고정 배송비, 기본 `0`원 |
| `MEDIA_STORAGE_PATH` | 백엔드 | 관리자 업로드 이미지 저장 경로, 로컬 기본 `./data/media` |
| `GUEST_TOKEN_EXPIRY_HOURS` | 백엔드 | 비회원 주문·예약 접근 및 결제 상태 조회 토큰 수명, 기본 `720`시간 |
| `GUEST_TOKEN_RECOVERY_EXPIRY_HOURS` | 백엔드 | 비회원 조회 정보 복구 토큰 수명, 기본 `24`시간 |
| `GOOGLE_OAUTH_CLIENT_ID` | 백엔드 `prod` | Google 로그인 client ID |
| `GOOGLE_OAUTH_CLIENT_SECRET` | 백엔드 `prod` | Google 로그인 client secret |
| `GOOGLE_OAUTH_REDIRECT_URI` | 백엔드 `prod` | Google에 등록한 exact backend callback URI (`https://<host>/api/v1/auth/social/callback/google`) |
| `NAVER_OAUTH_CLIENT_ID` | 백엔드 `prod` | Naver 로그인 client ID |
| `NAVER_OAUTH_CLIENT_SECRET` | 백엔드 `prod` | Naver 로그인 client secret |
| `NAVER_OAUTH_REDIRECT_URI` | 백엔드 `prod` | Naver에 등록한 exact backend callback URI (`https://<host>/api/v1/auth/social/callback/naver`) |
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
| `FORWARD_HEADERS_STRATEGY` | 백엔드 `prod` | 통제된 ingress 구성 후 `native`로 설정 |
| `RATE_LIMIT_TRUST_FORWARDED` | 백엔드 | 통제된 ingress 구성 후에만 `true`로 설정 |
| `FIELD_ENCRYPTION_KEY_ID` | 백엔드 `prod` | 활성 AES/HMAC 키 쌍의 버전 ID, 기본 `v1` |
| `ENCRYPT_KEY` | 백엔드 `prod` | 활성 개인정보 AES-256 키, 64자리 hex |
| `HMAC_KEY` | 백엔드 `prod` | 활성 블라인드 인덱스 HMAC 키, 64자리 hex |
| `PREVIOUS_ENCRYPT_KEYS` / `PREVIOUS_HMAC_KEYS` | 백엔드 `prod` | 회전 중에만 유지하는 `keyId=64자리hex` 이전 키 목록 |
| `GUEST_TOKEN_HMAC_SECRET` | 백엔드 `prod` | 활성 비회원 접근 토큰 서명 키 |
| `GUEST_TOKEN_PREVIOUS_HMAC_SECRET` | 백엔드 `prod` | 회전 전 발급 토큰의 만료까지 한시적으로 검증하는 이전 키 |
| `ADMIN_SETUP_TOKEN` | 백엔드 | 최초 관리자 계정 생성용 일회성 토큰 |

환경별 전체 설정은 [application.yml](bootstrap/src/main/resources/application.yml)과 [application-local.yml](bootstrap/src/main/resources/application-local.yml)을 기준으로 확인한다.
데이터 결합 키는 Secret을 직접 수정하지 않고 [k3s 데이터 키 회전 절차](deploy/k3s/README.md#2-secret-준비)로만 교체한다.

Naver 로그인 운영 등록 조건:

- Naver Developers 애플리케이션에 서비스 origin과 정확한 백엔드 콜백 URI `${서비스 origin}/api/v1/auth/social/callback/naver`를 등록한다.
- 회원 프로필의 이름 제공 항목을 사용하도록 설정한다. 서비스는 provider ID와 이름을 요구하고, Naver 프로필 이메일은 검증된 기준 이메일로 저장하지 않는다.
- 로그인 버튼은 [Naver 로그인 버튼 사용 가이드](https://developers.naver.com/docs/login/bi/bi.md)의 공식 심벌과 지정 색상을 사용한다.

## 문서 진입점

- 요구사항 기준: [docs/PRD/0001_기준_스펙/spec.md](docs/PRD/0001_기준_스펙/spec.md)
- API 계약: [docs/PRD/0004_API_계약/spec.md](docs/PRD/0004_API_계약/spec.md)
- 설계 결정: [docs/ADR](docs/ADR/)
- 배경 메모와 검토 기록: [docs/Idea](docs/Idea/)
- 회고와 트러블슈팅 기록: [docs/Retrospective](docs/Retrospective/)

`docs/Idea`는 배경 메모다. 현재 동작과 운영 기준은 PRD와 ADR을 먼저 본다.
