# happyGallery

`happyGallery`는 오프라인 공방의 상품 주문, 클래스 예약, 이용권과 회원·관리자 기능을 제공한다.
백엔드는 Spring Boot 멀티 모듈, 프론트엔드는 React Router + Vite를 사용한다. 공개 화면은 SSR, 회원·관리자 화면은 CSR로 제공한다.

## 한눈에 보기

| 사용자 | 주요 기능 |
| --- | --- |
| 비회원 | 휴대폰 인증으로 주문·예약, 조회 코드로 조회·취소·반품·교환, 조회 코드 재발급, 가입 후 주문·예약 가져오기 |
| 회원 | 주문·예약·이용권 관리, 장바구니, 쿠폰·적립금, 후기·문의, 찜·알림·기본 배송지, 로그인 수단·연락처 관리와 탈퇴 |
| 관리자 | 주문·배송·픽업·환불, 재고·클래스·예약 운영, 스마트스토어 연동·정산, 문의·후기·콘텐츠 관리, 매출·이용률 조회 |

관리자는 ‘오늘 할 일’에서 승인 대기 주문, 반품·교환, 재고 부족, 예약 취소 후 정산, 문의와 결제·환불·알림 오류를 확인한다.

세부 조건과 설계는 다음 문서를 따른다.

- [제품 명세](docs/PRD/0001_기준_스펙/spec.md): 주문·옵션·재고, 예약·이용권, 쿠폰·적립금, 후기·문의, 공방 프로필·공휴일·알림 규칙
- [API 계약](docs/PRD/0004_API_계약/spec.md): 요청·응답, 오류, 검색·페이지 조회, 회원·비회원 접근 방식
- [상품·재고 설계](docs/ADR/0012_상품_재고_결정/adr.md): 옵션·각인, 장바구니 수량, 주문 당시 정보 보존과 데이터 전환
- [결제·환불 설계](docs/ADR/0033_결제_confirm_트랜잭션과_보상_경계/adr.md): `prepare` → `confirm`, 중복 결제 방지, 결과 조회·복구와 영수증
- [쿠폰·적립금 설계](docs/ADR/0042_이벤트_쿠폰_적립금과_결제_혜택_원장/adr.md): 발급·사용 예약·확정, 할인액 배분, 환불 시 복원·회수
- [스마트스토어 주문 연동](docs/ADR/0048_스마트스토어_주문_운영_연동/adr.md)과 [재고 동기화](docs/ADR/0047_스마트스토어_재고_동기화/adr.md): 상품 연결, 발주·발송·반품·교환, 문의 답변, 정산·CSV 내보내기

회원은 `HG_SESSION` 쿠키, 관리자는 Bearer 세션, 비회원 조회는 `X-Access-Token`을 사용한다.
브라우저의 비관리자 상태 변경 요청에는 `XSRF-TOKEN` 쿠키와 `X-XSRF-TOKEN` 헤더로 CSRF 검증을 적용한다.

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

- 스마트스토어 주문·정산 수집은 선점 시각을 DB의 마이크로초 정밀도에 맞춘다. 나노초를 포함하는 서버 시각에서도 수집 완료·실패 해제가 동작하며, 만료 후 재선점한 작업은 이전 실행이 해제하지 못한다.
- 스마트스토어 연동이 꺼진 동안에는 수집 중인 실행이 없거나 시작 후 5분 이상 지났을 때 주문 조회 시작점을 현재 시각으로 옮기고 처리 기록을 비운다. 비활성 상태에서 활성 설정으로 재기동하면 시작 시각을 DB에 먼저 저장하고 재기동 전후의 가장 이른 시각부터 수집해 중지 기간 주문은 제외하고 첫 배치 전 주문은 포함한다. 5분 미만인 실행은 끝날 때까지 활성화 경계를 적용하지 않으며, 만료된 이전 실행이 조회 시작점을 되돌릴 수 없다.
- 스마트스토어 자동 재고 전송과 수동 상품 반영은 직전 주문 수집이 끝나야 실행한다. 수집 실패·페이지 잔여·처리 중에는 전송을 보류하며, 매핑 누락·재고 부족 등 미반영 주문이 있으면 해당 원상품만 보류한다. 관리자가 주문을 재처리하면 최신 재고 전송을 다시 요청한다. 보류는 외부 전송 실패 횟수에 포함하지 않는다.
- 스마트스토어 재고 연동을 재등록한 뒤 이전 세대나 이전 요청 버전의 전송이 늦게 끝나면 현재 전송 중·완료 요청에 최신 수량 보정 요청을 남긴다. 이전 응답을 새 요청의 성공·실패로 기록하지 않으며, 이미 대기 중인 재시도 간격과 최종 실패 상태는 유지한다.
- 스마트스토어 재고 동기화가 완료된 상품은 완료 시각에서 24시간이 지나면 재검증 대상으로 전환한다. 매분 최대 100개씩 최신 절대 재고를 다시 전송하며, 실제 완료 시각은 기존 대기열·주문 수집·미반영 주문·외부 재시도 상태에 따라 24시간보다 늦을 수 있다.
- 스마트스토어 부분반품은 배송 중 상태에서도 완료 클레임 수량으로 구분해 검수 후에만 재고를 복원한다. 재시도·재수집은 검수 결과를 유지하고, 뒤이은 취소나 추가 반품으로 판매 불가 반품까지 복원하지 않는다. V168은 누적 반품·검수·복원 수량을 추가하며 기존 기록은 다음 주문 수집 때 전환한다. 주문 수집·검수 서버를 구버전과 혼용하지 않는다. 과거 오류로 이미 자동 복원된 부분반품 재고는 일괄 차감하지 않으므로 실물 재고를 확인해 조정한다.
- `local` 프로필에서는 DB가 비어 있으면 기본 클래스 3종과 관리자 계정 `admin / admin1234`를 자동 생성한다.
- 로컬과 개발 환경에서는 `X-Admin-Key: dev-admin-key`를 사용할 수 있다.
- `prod`가 아닌 환경에서는 실제 알림·인증 SMS·이메일 인증 SMTP·결제 대신 테스트용 발송기와 `FakePaymentProvider`를 사용한다.
- 스마트스토어 계정 유형은 `SELF`(기본값)와 `SELLER`만 허용한다. 빈 값과 지원하지 않는 값은 설정 검증에서 거절한다.
- 스마트스토어 주문·재고·문의·정산 연동은 기본 비활성화다. 운영에서는 `SMARTSTORE_ENABLED=true`, `SMARTSTORE_CLIENT_ID`, bcrypt salt 형식의 `SMARTSTORE_CLIENT_SECRET`을 설정하고 위임 판매자 방식이면 `SMARTSTORE_ACCOUNT_TYPE=SELLER`, `SMARTSTORE_ACCOUNT_ID`도 함께 주입한다. 활성화한 시점부터 변경 주문을 매분 수집하며 과거 주문을 소급 차감하지 않는다. 배송정보 암호문은 기존 데이터 키 회전 명령에서 다른 배송지 암호문과 함께 재암호화한다.
- 연결된 스마트스토어 원상품 번호를 바꾸거나 연동을 해제할 때는 기존 원상품의 판매 중지와 재고 확인을 완료했다고 확인해야 한다. 서버는 기존 원상품 주문 수집을 마치고 재고 미반영 주문이 없는지 확인한 뒤 최신 매핑 개정으로 저장·삭제한다. 변경·해제 후 기존 원상품 재고는 자동 보정하지 않지만 과거 연결은 늦게 들어온 기존 주문 식별에만 보존한다.
- k3s 운영 배포의 Prometheus 경보는 내부 Alertmanager를 거쳐 저장소 밖 Secret으로 주입한 외부 HTTPS webhook에 전달한다. 운영 호스트 자체 장애 감시는 별도 외부 uptime 서비스가 필요하다.
- 운영 환경은 DB·Redis를 readiness에 포함하고, 환불·알림 outbox·주문 승인 대기·예약 취소 후속 작업·스마트스토어 주문 처리 결과 미확정의 DB backlog, 결제·알림 CircuitBreaker 상태와 호출 결과, 모든 정기 배치의 마지막 정상 완료 시각과 이미지 저장소 용량을 Prometheus·Grafana에서 감시한다. 스마트스토어 주문·재고 동기화는 5분, 정산 대사는 2시간 동안 정상 완료가 없으면 별도 critical 경보를 보낸다. 업무 알림은 휘발성 사건 수가 아니라 아직 처리되지 않은 DB 상태를 기준으로 유지한다.
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
- OpenAPI 명세 갱신: `./gradlew --no-daemon :adapter-in-web:openapi3`
- 앱 실행: `./gradlew :bootstrap:bootRun`

`./gradlew build`의 `check` 단계에는 REST Docs 계약 테스트와 Controller·DTO와 OpenAPI 명세의 일치 여부 검사가 포함된다. 빠른 로컬 확인이 필요할 때만 위 개별 태스크를 사용한다.
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
- REST Docs는 실제 HTTP 요청·응답 예시를 검증하고, OpenAPI는 도구가 읽는 API 명세이며 프론트엔드 코드 생성의 원본이다.
- React feature 계층의 HTTP API 호출은 모두 생성 client를 사용한다. feature wrapper는 생성 함수와 서버 DTO를 재사용하고 React Query 키·캐시·화면용 뷰 모델만 관리한다. OAuth 로그인 시작처럼 브라우저가 URL로 직접 이동하는 흐름은 HTTP API wrapper가 아니므로 생성 client 대상에서 제외한다.
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
- 인프라 목표: 보유 Linux 노트북의 단일 노드 k3s, Kubernetes Ingress, MySQL 영속 볼륨, cluster 내부 Redis
- 로컬 개발·복구 진단: Docker Compose, Nginx reverse proxy
- 모니터링: Actuator, Prometheus, Grafana, Sentry
- API 계약: Spring REST Docs, Springdoc OpenAPI
- 테스트: JUnit 5, Testcontainers, Playwright

### 프론트엔드 디자인 기준

- 햇빛이 드는 공방을 중심 이미지로 삼고 한지색, 점토색, 잎색을 기본 팔레트로 사용한다.
- 본문은 Pretendard, 전시 제목과 브랜드 표기는 Gowun Batang 계열을 사용한다.
- 공통 색상과 컴포넌트 변수는 `frontend/src/styles/_variables.scss`에서 관리한다. `frontend/src/styles/global.scss`는 Bootstrap과 `_foundation.scss`, `_admin.scss`, `_storefront.scss`, `_atelier.scss`, `_brand.scss`를 불러오는 순서만 정의한다.
- 홈과 클래스·단체수업 화면은 `frontend/src/assets/happygallery`의 실제 공방 사진을 사용한다. 사진 원문은 같은 디렉터리의 `SOURCES.md`에 기록하며, 외부 이미지 CDN에 런타임 의존하지 않는다.
- 공개 화면은 요청 시점 SSR로 본문·제목·설명·canonical·Open Graph·JSON-LD를 제공하고, 인증·결제·고객 이력·관리자 경로는 client-only 화면과 `noindex`로 분리한다.
- 대표 운영 origin은 `https://happy-gallery.com`이며 robots·sitemap·canonical에서 같은 origin만 사용한다.

## 운영/배포

- 상품·장바구니 키 전환은 [상품·재고 ADR](docs/ADR/0012_상품_재고_결정/adr.md)의 전환·롤백 조건을 따른다. V163 전후의 백엔드는 함께 실행하지 않는다.
- 스마트스토어 반품 검수 요청은 검수 대상 버전이 필요하다. 서버와 관리자 화면을 함께 배포하고 기존 화면은 새로고침한다.

현재는 보유 노트북(i5-8250U·RAM 16GB·표시 저장용량 477GB)에 Ubuntu Server 24.04 LTS를 설치해 운영 가능성을 먼저 검증한다. Windows 삭제·Linux 설치와 24시간 가동은 가능하며, BE3600 공유기에 Wi-Fi로 연결한다. SSD 여부·무선랜 호환성·자동 재접속·공유기 설정은 확인이 필요하다. [노트북 설치 준비](deploy/laptop/README.md)에서 시작한다. 월 2~3만 원 목표는 유지하며 실제 전기료·외부 백업 비용을 확인한다. 클라우드 VM 구매는 보류하고 [AWS·Google Cloud 서울 비교](deploy/cloud/README.md)를 대체 후보로 보관한다.

BATON·IntentTrace 등 개인 프로젝트의 공동 운영은 [프로젝트별 구성과 격리 검토](deploy/laptop/multi-project.md)에 정리했다. 단일 k3s에서 프로젝트를 분리하는 권고안이며, 프로젝트 전체 자원 할당량·디스크 제한과 다른 프로젝트 배포는 아직 적용하지 않았다.

```text
브라우저 -> Cloudflare DNS -> 공유기/호스트 방화벽 -> k3s Ingress(TLS)
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
- 대표 공개 주소는 `https://happy-gallery.com`으로 확정했다. 실제 운영 호스트에서 DNS·방화벽·TLS·검색엔진 소유확인·백업 중단 시간·복원 훈련과 핵심 사용자 흐름을 검증하기 전에는 운영 중으로 간주하지 않는다.
- 기준 공방 프로필에는 공개 결제에 필요한 대표자명, 전자우편주소와 통신판매업 신고번호가 포함된다. 배포 전 footer·사업자 정보 화면의 표시값을 확인해야 하며, `prod` 프로필은 연락처·주소·사업자등록번호를 포함한 필수 온라인 판매 고지가 완성되기 전 모든 결제 prepare를 `503`으로 차단한다. 표시 근거는 전자상거래법 [제10조](https://www.law.go.kr/LSW/lsLinkCommonInfo.do?chrClsCd=010202&lsJoLnkSeq=1022342373)와 [제13조](https://www.law.go.kr/LSW/lsLinkCommonInfo.do?chrClsCd=010202&lsJoLnkSeq=1022341933)다.

현재 운영 목표와 배포·데이터 보호 조건은 [ADR-0037](docs/ADR/0037_자가_호스팅_배포_토폴로지_기준/adr.md)을 따른다. [ADR-0049 저예산 클라우드 운영 기준](docs/ADR/0049_저예산_클라우드_운영_기준/adr.md)은 노트북 운영 조건을 충족하지 못할 때 다시 검토할 대안이다. 공개 검색 문서와 SSR·canonical·sitemap·HTTP 상태 코드 처리 규칙은 [ADR-0045](docs/ADR/0045_공개_페이지_SSR과_SEO_전달_경계/adr.md)를 따른다. 이전 AWS 구조와 배포 설정은 [Idea-0028](docs/Idea/0028_CloudFront_S3_ALB_배포_구조/idea.md), [Idea-0029](docs/Idea/0029_GitHub_Actions_CI_CD_배포_Fargate/idea.md), [Idea-0039](docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md)에 역사 기록으로 남긴다.

## 주요 환경 변수

`*_MILLIS`, `*_SECONDS`, `*_HOURS` 환경 변수는 기존 숫자 계약을 유지한다. `application.yml`이 각각 `ms`, `s`, `h` 단위를 붙여 애플리케이션의 `Duration` 설정으로 바인딩하므로, 기존 배포 값은 바꾸지 않아도 된다. 이메일 인증 SMTP의 host·port·자격 증명·TLS·transport timeout은 `spring.mail.*`로 연결되어 Spring Boot가 `JavaMailSender`를 자동 구성하고, 애플리케이션은 발신 주소·제목, 메일 발송 전체를 제한하는 TimeLimiter와 타임아웃 순서만 관리한다.

Delivery API를 처음 연결할 때는 `DELIVERY_TRACKING_ENABLED=false`와 직접 생성한 `DELIVERY_WEBHOOK_SECRET`을 먼저 배포한다. 그다음 `https://<운영 호스트>/api/v1/webhooks/delivery-tracking`을 같은 secret으로 Delivery API에 등록해 `endpointId`를 받은 뒤 API 키·endpoint ID와 함께 연동을 활성화한다. 웹훅 URL은 외부에서 접근 가능한 HTTPS여야 한다.

Toss 운영 콘솔에는 결제 상태 변경 웹훅 URL로 `https://<운영 호스트>/api/v1/webhooks/toss-payments`를 등록한다. 웹훅은 `PAYMENT_STATUS_CHANGED`만 수신 기록하고, 알려진 `orderId`를 기존 결제 대사 흐름으로 확인한다.

상품 상세·주문서·장바구니·예약금·이용권 구매 화면에서 `카드·간편결제`, `네이버페이`, `카카오페이`를 선택한다. 네이버페이와 카카오페이는 토스 경유 자체창으로 열며 별도 PG 서버를 두지 않는다. 전용창 선택 시 토스 결제 약관 동의를 받은 뒤 prepare를 요청하고, 승인·부분취소·정산은 기존 토스 경로를 사용한다. 결제수단을 바꾸면 약관 동의를 다시 받는다. 운영 사용 전 토스 가맹점에서 두 간편결제를 사용할 수 있는지 확인하고 실제 결제·전체취소·부분취소를 검증해야 한다. [공식 자체창 연동 안내](https://docs.tosspayments.com/guides/v2/payment-window/integration-direct)

결제창 취소·실패 화면은 같은 고객 세션의 구매 화면으로 돌아가는 버튼을 제공한다. 복귀 전에 저장된 결제 ID로 `POST /api/v1/payments/{orderId}/abandon`을 호출해 승인 전 결제와 쿠폰·적립금 예약을 함께 종료한다. 승인 처리가 이미 시작됐으면 복귀하지 않고 현재 결제 상태를 표시한다. SDK가 현재 화면에서 오류를 반환해도 같은 종료를 시도한다. 브라우저 자체를 닫거나 종료 요청이 실패한 경우에는 기존 30분 만료 배치가 미시작 결제를 정리한다. 상품·주문서·장바구니·이용권은 원래 경로로, 예약은 선택한 클래스에서 최신 가능 시간을 다시 고르며 새 결제는 자동 요청하지 않는다. 저장 정보 없음·고객 변경·외부 주소는 홈·상품 목록으로 안내한다.

주소 검색은 [Kakao 우편번호 서비스](https://postcode.map.kakao.com/guide)를 사용한다. 이용료와 API 키가 필요 없으며, 검색 장애 때는 직접 입력할 수 있다. 기존 `ROAD_ADDRESS_*` 환경 변수는 제거한다.

공휴일은 [한국천문연구원 특일 정보 API](https://www.data.go.kr/data/15012690/openapi.do)의 무료 활용 신청 후 `PUBLIC_HOLIDAY_SERVICE_KEY`를 주입하고 `PUBLIC_HOLIDAY_ENABLED=true`로 켠다. 매일 04:20(서울)에 현재 연도와 다음 연도를 갱신하며, 조회 실패 때는 마지막으로 정상 수집한 데이터를 유지한다. 해당 연도 데이터가 없으면 기존 공휴일 계산을 사용한다.

개인 캘린더 추가는 무료 오픈소스 [ical.js](https://github.com/kewisch/ical.js)로 ICS 파일을 생성하며 외부 계정 연동이 필요 없다.

외부 HTTP 풀의 `keep-alive`는 서버가 연결 유지 시간을 보내지 않을 때 기본값으로 적용한다. 같은 값으로 연결 최대 수명과 유휴 연결 정리 기준도 설정한다.

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
| `PASS_TOTAL_PRICE` | 백엔드 | 4회권 결제 금액 (기본 120,000원) |
| `ORDER_SHIPPING_FEE` | 백엔드 | 배송 주문에 더하는 고정 배송비, 기본 `0`원 |
| `DELIVERY_TRACKING_ENABLED` | 백엔드 | Delivery API 배송조회 연동 활성화 여부, 기본 `false` |
| `DELIVERY_API_KEY` / `DELIVERY_API_SECRET_KEY` | 백엔드 | Delivery API 호출 자격 증명 |
| `DELIVERY_WEBHOOK_ENDPOINT_ID` / `DELIVERY_WEBHOOK_SECRET` | 백엔드 | 배송조회 등록 대상 웹훅 ID와 수신 서명 검증 키 |
| `DELIVERY_API_ACQUIRE_TIMEOUT_MILLIS` / `DELIVERY_API_CONNECT_TIMEOUT_MILLIS` / `DELIVERY_API_TIMEOUT_MILLIS` | 백엔드 | 배송조회 연결 풀 획득·연결·응답 상한, 기본 `500` / `1000` / `3000` |
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

## 문서 안내

- 요구사항 기준: [docs/PRD/0001_기준_스펙/spec.md](docs/PRD/0001_기준_스펙/spec.md)
- API 계약: [docs/PRD/0004_API_계약/spec.md](docs/PRD/0004_API_계약/spec.md)
- 설계 결정: [docs/ADR](docs/ADR/)
- 배경 메모와 검토 기록: [docs/Idea](docs/Idea/)
- 회고와 트러블슈팅 기록: [docs/Retrospective](docs/Retrospective/)

`docs/Idea`는 배경 메모다. 현재 동작과 운영 기준은 PRD와 ADR을 먼저 본다.
