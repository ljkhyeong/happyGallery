# HANDOFF

## 남은 후보 처리: 예약 변경 칩·작품 안내·공지 시각·관리자 화면 (2026-10-05)

- 시작 SHA `6e42c3c3`. 아래 상세 화면 점검의 남은 후보 4건을 처리했다. 푸시 미실행.
- 공지 `createdAt`: 공개 공지 목록·상세 응답에 UTC 오프셋(`Z`)을 붙였다(`atOffset(ZoneOffset.UTC)`, 관리자 주문·내 주문과 같은 방식). REST Docs에 `Z` 단정 추가, PRD-0004 2.8 갱신, OpenAPI·Orval 재생성 결과 TS 변경 없음(문자열 타입 유지).
- 로컬 시간 주의: 로컬 `bootRun` JVM이 KST라 DB 기본 생성 시각(`createdAt`)이 로컬 화면에서만 9시간 늦게 보인다(운영·CI JVM은 UTC라 정상). 개선안은 `simple-idea.md` 검토 중 표에 남겼다.
- 예약 변경: `BookingDateChips`·`BookingSlotTime`(`features/booking-create/BookingDateChips.tsx`)을 예약 화면과 회원·비회원 `RescheduleForm`이 함께 쓴다. 빠른 날짜는 칩, 직접 날짜 입력은 유지.
- 작품 상세: `ProductPurchaseGuide`가 수령 방법·배송비(0원이면 "모두 무료")·교환·반품(주문제작 제한)·적립 기준을 표로 보여 준다.
- 관리자: 표는 한글 단어 단위 줄바꿈·제목 줄 고정(모바일은 가로 스크롤), 빈 상태(`EmptyState` → `.empty-state`)는 관리자에서 한 줄로 축소, 주문 메뉴는 자사 주문 목록·교환·환불 요청을 스마트스토어 섹션보다 위에 둔다. 상품 목록 `재고 있음`은 중립, `품절`은 경고색. 캡처 `output/playwright/admin-1005/`(`shoot.mjs`는 E2E 지원 파일의 로컬 기본 관리자 계정으로 로그인, 값은 출력하지 않음).
- 검증: build·typecheck·lint, `restDocsTest`(공지), 영향 E2E 23개 파일 121건 중 118건 통과 → 실패 3건 중 공지 2건은 변경 전 HEAD(stash)에서도 실패(관리자 고객 응대의 `group-inquiries`·스마트스토어 문의 mock 누락)해 mock을 보완하고 통과, `history-pagination` 346은 기존 실패. @smoke 20건 통과.
- 스마트스토어 연동이 꺼진 환경에서는 관련 관리자 섹션이 서버 안내(연동 비활성화) 대신 일반 `CONFLICT` 문구로 보인다. 화면에서 구분하려면 전용 오류 코드(API 계약 변경)가 필요해 이번에는 고치지 않았다.

## 상세 화면 전수 점검·개편 (2026-10-05)

- 시작 SHA `4843d176`(main 반영본). 공개 상세(클래스·작품·이벤트·공지·단체수업·사업자·약관), 비회원 주문·예약 흐름(인증 모달→결제 완료→조회 상세), 회원 상세(예약·주문·장바구니·문의 작성)를 1280/390px로 캡처해 점검했다. 캡처·스크립트 `output/playwright/detail-1005/`(`shoot.mjs`가 비회원 결제 흐름까지 재현, 회원 자격 증명은 `survey-1006/credentials.json`).
- 개편: 장바구니를 체크아웃 2단(담은 작품·수령 방법·결제 수단 | 주문 요약, 표·`.store-purchase-card` 유지, 모바일은 작품 카드)으로 바꿔 모바일 상품명이 한 글자씩 꺾이던 문제를 고쳤다. 결제 완료·실패는 `PaymentResultCard`(종류별 안내). 비회원 주문·예약 조회는 결과를 먼저 보이고 입력은 `hidden`으로 접는다(E2E가 입력값을 읽으므로 지우지 않음, `최신 상태 확인`·`다른 번호로 찾기`). 회원·비회원 예약 상세는 `BookingInfoCard`로 통일. 클래스 상세에 다가오는 일정(최대 8개, 시간 선택 상태로 예약 화면 이동, 조회 실패는 경고 대신 안내 문장)·예약 안내·방문 안내 추가. 공지 상세 서체·다른 공지 4개. 간단 방문 안내의 소개 문단 제거, 푸터 한글 브랜드, 작품 주문표 점토 세로줄 제거, 클래스 목록 "이용권 사용 불가" 제거·모바일 카드 축소, 홈 단계 카드 화살표.
- 검증: build·typecheck·lint, `agent-feedback.rb final 4843d176` 통과. 영향 E2E 29개 파일 140건 중 132건 통과 후 원인 수정(장바구니 문구, 찜 mock에 `/slots/upcoming` 추가, 비회원 재조회는 `최신 상태 확인`), 재실행 통과. @smoke 20건, P8-9(오래된 안내 문구 단정 갱신) 통과.
- 기존 실패(수정 안 함): `customer-account-boundary` 203·355·656, `history-pagination` 346, `shipping-address` 비회원 주소 스크립트 요청 수(2≠3)가 4회 중 1회 간헐 실패 — 변경 전 HEAD(stash)에서도 같은 단정으로 실패 확인.
- 남은 후보: 위 "남은 후보 처리" 항목에서 모두 처리했다.
- 실행 조건: 백엔드 8081은 `timeout 7200000`·`RATE_LIMIT_ENABLED=false`로 띄웠다. `frontend-review`(3030)에 `VITE_TOSS_CLIENT_KEY=test_ck_e2e` 필요. 푸시 미실행.

## 내 정보 영역 공통 셸 개편 완료·main 반영 후 원격 검사 확인 (2026-10-05)

- 시작 SHA `7604f4d4`. 회원 화면 16개를 `features/my/MyShell`(중첩 layout `routes/client/my-layout.tsx`)로 묶었다. 데스크톱 왼쪽 메뉴, 모바일은 홈 묶음 목록·하위 화면 가로 메뉴. 메뉴 이름=화면 h1은 `features/my/myNavigation.ts`(예약 내역·주문 내역·이용권·쿠폰·적립금·내 찜·1:1 문의·내 후기·단체 수업 문의·알림함·빈자리 알림·재입고 알림·기본 배송지). 로그인 확인·안내 카드·로그아웃·`/my#...` 이동은 셸이 맡고 각 페이지의 Container·로그인 분기·"← 내 정보"·"불러온 ~ 중" 칩을 지웠다. 대시보드(MyDashboardHero·MyStatsRow·최근 예약/주문 3건), 예약 상세 카드(이용권 예약은 예약금 행 숨김), 1:1 문의 카드, 스타일은 `_storefront.scss` 끝 절. `MyManagementLinks` 삭제.
- 정리 시작 SHA `130136653f168f226b7df5db3a1424a0b6e438b8`. 사용자가 남은 로컬 변경 분류·커밋·원격 main 푸시를 요청했다. 화면 기능·회귀 테스트·제품 규격과 인계 문서를 나눠 저장하며 기존 로컬 25개 커밋도 main 반영 대상이다. 원격 main `b4edd17d`는 현재 브랜치의 조상이며 강제 푸시는 필요 없다.
- 검증: frontend build·lint 통과(`/tmp/hg-main-build-final.log`, `/tmp/hg-main-lint.log`), 단위 79건(75건 먼저 통과, 로컬 포트 권한으로 막힌 4건은 해당 3개 파일 재실행으로 통과: `/tmp/hg-main-unit{,-retry}.log`), 의존성 audit 0건(`/tmp/hg-main-audit.json`). 회원 화면 관련 12개 spec 41건(`/tmp/hg-main-member-e2e.log`), @smoke 20건(`/tmp/hg-main-smoke.log`), P8-10·P8-7 개별 통과(`/tmp/hg-main-member-payment-retry.log`, `/tmp/hg-main-member-pass-final.log`). 검사 도구 테스트 16건(`/tmp/hg-main-agent-tests.log`), 원격 main 기준 소스 호환성 통과(`/tmp/hg-main-origin-compatibility.log`). 전체 종료 검사 `ruby tools/agent-feedback.rb final 130136653f168f226b7df5db3a1424a0b6e438b8` 통과(`/tmp/hg-main-final-complete.log`), 전체 diff의 메뉴·세션 경계·조회 범위·스타일 담당·테스트 누락을 검토했다. 코드·설정·환경이 같으면 결과를 재사용한다.
- 실패 원인과 보완: 최초 취소 요청까지 센 재연결 검사는 초기 조회 수 대비 한 번 추가를 확인하도록 바꿨다. 세션 키는 유지했다. P8-7은 이용권 가능 클래스·구매한 passId·이용권 결제 경로와 현재 취소 상태를 명시했고, P8-10은 후기 부가 조회 mock 누락으로 실제 401을 받던 문제를 보완했다. PRD `내 정보와 관리 화면`을 최근 각 3건·공통 메뉴 구조와 일치시켰다. 1280/390px 메뉴·폼·긴 문의 줄바꿈 캡처는 `output/playwright/main-member-review/`; 관련 E2E에서 모바일 가로 넘침 없음 확인.
- 남은 행동: main 반영 후 해당 SHA의 CI·Production 결과를 확인한다. 기존 화면 실패 목록의 P8-7·P8-10은 위 보완으로 해결됐다. 다른 기존 실패는 해당 절을 따른다. 디자인 후보(이번 반영 범위 밖): 장바구니 체크아웃 배치, 결제 결과 카드, 푸터 브랜드·VISIT 중복, 모바일 클래스 카드 축소.
- 실행 조건: 확인용 `hg-design-mysql`(3307)·`happygallery-redis`를 재시작해 재사용했다. Java 25는 `~/.gradle/jdks/eclipse_adoptium-25-aarch64-os_x.2/jdk-25.0.3+9/Contents/Home`, 백엔드 8081·`RATE_LIMIT_ENABLED=false`, E2E는 `PLAYWRIGHT_FRONTEND_PORT=3020 PLAYWRIGHT_BACKEND_URL=http://127.0.0.1:8081/api/v1 PLAYWRIGHT_SKIP_MFA_WEB_SERVER=1`. 기존 기본 DB·운영 데이터는 변경하지 않았다.

## 디자인 점검 후속: 결함 정리·예약/4회권/주문서 2단 체크아웃 완료 (2026-10-05)

- 1단계(`0fe51af7`): 이벤트 관련 작품 카드, 먹색 포커스·`btn-outline-primary`, 빈 후기, 로그인 폼 우선, 사진 준비 중 표시, 자정 기준 이벤트 기간.
- 2단계(시작 SHA `0fe51af7`): 사용자가 시안 G(2단 체크아웃, `output/design-candidates/g-checkout-split.html`)를 골랐다. 공용 `PageHeader`·`CheckoutLayout`·`CheckoutPanel`·`CheckoutSummary`(`frontend/src/shared/ui`)로 예약·4회권·주문서를 입력 | 고정 요약 2단으로 바꾸고 이벤트 목록 머리도 `PageHeader`로 맞췄다. 스타일은 `_storefront.scss` 체크아웃 절이 담당한다.
- 결정: 수업은 9개 이상으로 늘 수 있고 대부분 미리 선택돼 들어와 선택 상자(`클래스` label, 시각적으로 숨김)를 유지했다. 날짜만 칩(`data-booking-date`)으로 바꾸고 시간 칩은 `data-slot-id`·`.active`·`N명 예약 가능` 문구를 유지했다. 결제 버튼은 요약 안 한 개만 두고 모바일은 CSS로 하단 고정한다. 단계 패널은 랜드마크로 만들지 않는다(`getByLabel("상품")` 같은 부분 일치 선택자 충돌 방지). 주문서 요약에는 `결제 예정 금액` 중복을 피하려 모바일 금액 줄을 두지 않았다.
- 검증: build·lint·typecheck, `agent-feedback.rb final 0fe51af7` 통과(architectureTest 포함, Gradle은 `~/.gradle` 쓰기 때문에 샌드박스 밖 실행). 영향 E2E 21개 파일 122건 중 103건 통과, 선택자 수정 후 재실행 13건 중 11건 통과(2건은 아래 기존 실패), @smoke 20건, 장바구니 5건 통과. 1280/390px 캡처 `output/playwright/checkout-1005/`(비회원 주문서 인증 흐름 포함), 가로 넘침 없음.
- 기존 실패(이번 변경과 무관, 수정 안 함): `customer-account-boundary` 203(SSR이 확인용 DB에 없는 상품 42 조회)·355·656(mock 상품에 `variants` 없음으로 `useOrderItems` 오류), `event-coupon-admin` 462(이전 457, 관리자 쿠폰 카드 `사용 중지` 중복), `guest-claim-onboarding` P8-9(비회원 주문 조회 문구), `member-self-service` P8-7·P8-10(내 예약 상세), `history-pagination` 346, `picker-resilience` 52·158.
- 남은 행동: ① 테스트 회원으로 마이페이지 계열 실제 화면을 확인하고 범위 결정 ② 다듬기 후보: 홈 사진 반복(레진·양말목), STEP 카드 클릭 단서, 푸터 영문 브랜드·VISIT 중복, 모바일 클래스 카드 길이, 장바구니·결제 결과 화면의 예전 머리.
- 실행 조건: `127.0.0.1:8080`은 다른 프로젝트 앱이 점유한다. 백엔드는 `SERVER_PORT=8081`, 확인용 DB(3307), E2E 병렬 실행 시 `RATE_LIMIT_ENABLED=false`로 띄운다. 프론트 확인은 `.claude/launch.json`의 `frontend-review`(3030 → 8081), E2E는 `PLAYWRIGHT_FRONTEND_PORT=3020 PLAYWRIGHT_BACKEND_URL=http://127.0.0.1:8081/api/v1 PLAYWRIGHT_SKIP_MFA_WEB_SERVER=1`. 푸시·배포 미실행.

## 상황별 수업 찾기·원데이 다음 단계·모바일 하단 탭 (2026-10-05)

- 시작 SHA `4d5ce356`. 솜씨당·아이디어스·프립을 참고한 시안 D·E·F(`output/design-candidates/{d,e,f}-*.html`) 중 사용자가 A+C+D 혼합을 골랐다. 같은 브랜치의 병행 세션 커밋 `391f90ac`(종료 검사)는 겹치는 파일이 없다.
- 백엔드: 클래스 상황 태그 `DATE·WITH_KIDS·FRIENDS·GIFT`를 `classes.situation_tags`(V186, `VARCHAR(100) NULL`) 한 컬럼에 쉼표로 저장한다. OSIV가 꺼져 있어 컬렉션 테이블 대신 `ClassSituationTagsConverter`를 썼고, 무중단 배포 호환 검사 때문에 NOT NULL·기본값 없이 NULL=태그 없음으로 둔다. 관리자 수정은 `situationTags` 생략 시 유지, `[]`이면 모두 지운다. 공개·관리자 응답은 항상 배열을 준다(PRD-0004).
- 화면: 홈 히어로에 상황별 바로가기(이번 주말·오늘 바로=실제 일정, 데이트·아이와 함께·친구 모임·선물 만들기=태그, 이용권·단체)와 원데이→이용권→자격증·창업반 3단계 안내를 넣었다. 클래스 목록은 `?tag=`·`?when=weekend|today` 칩 필터와 카드별 다음 수업을 보여 준다. 주말·오늘 조건은 브라우저에서 서울 날짜로 계산해 SSR에서는 로딩을 표시한다. 하단 탭은 둘러보기 화면(`Layout.tsx`의 `TAB_BAR_PATHS`)에서 992px 미만만 표시해 상품 상세 고정 버튼과 겹치지 않는다.
- 검증: `AdminClassUseCaseIT` 2건(V186 nullable 변경 후 재실행), 관리자 카탈로그 REST Docs, OpenAPI·Orval 재생성, `agent-feedback.rb final 4d5ce356` 통과(호환성 검사·architectureTest·typecheck 포함). 확인용 DB에서 V186 재적용 후 NULL→`[]`, PATCH 저장값 `DATE,FRIENDS` 확인. E2E 영향 59건 중 56건, @smoke 20건, 새 `class-situation-filter.spec.ts` 통과. 1280/390px 캡처는 `output/playwright/after5`.
- 기존 실패(수정 안 함): `history-pagination` 346, `picker-resilience` 52·158. 관리자 API를 모두 `[]`로 mock해 `AdminGroupInquirySection`·`SmartStoreInspectionSection`이 page 객체 대신 배열을 받아 화면 전체가 오류 경계로 넘어간다. 두 컴포넌트는 이번 변경과 무관하다.
- 실행 조건: E2E는 아래 디자인 개편과 같은 환경변수로 실행했다. V186 재확인은 `127.0.0.1:8080` 점유 앱과 섞이지 않게 백엔드를 `SERVER_PORT=8081`로 띄워 확인했다. 푸시·배포 미실행.

## 스토어프런트 디자인 개편·홈 바로 예약 (2026-10-05)

- 시작 SHA `619bb890f1ddfd2121be007011a1fe0f1d361f17`. 같은 브랜치에 병행 세션의 Sentry·JSON-LD·CI 커밋이 먼저 들어왔고 겹치는 파일은 없다. 색 체계를 한지·점토·잎으로 정리하고(민트·분홍·와인 섹션 색과 줄무늬 배경 제거), 주요 버튼을 먹색으로 통일했다. 히어로·작품 카드·클래스 상세의 중복 스타일을 담당 partial 하나로 모았고, container 폭은 1180px로 맞췄다. 담당 범위는 README `프론트엔드 디자인 기준`에 있다.
- 화면: 홈은 사진 카드 + 바로 예약 패널(수업·날짜·시간·남은 자리) → 바로가기 → 사진 클래스 카드(다음 수업·남은 자리) → 4열/모바일 2열 작품 순이다. 상품 상세는 사진(고정)·정보·주문표 2단, 클래스 목록·상세는 사진 카드·고정 예약 패널, 예약 시간은 2열 시간 칩이다. 헤더의 중복 로그인·회원가입 링크를 정리했다. 시안 A·B·C 비교는 `output/design-candidates/`, 사용자가 A+C 혼합을 골랐다.
- 예약 연결: 홈 패널은 `/bookings/new?classId&slotId&selectSlot=1`로 넘기고, 예약 화면은 `selectSlot=1`이고 자리가 남았을 때만 시간을 미리 선택한다. 빈자리 알림 링크는 PRD대로 날짜만 펼친다. 일정 조회 기간·쿼리는 `features/booking-create/upcomingSlots.ts`가 예약·일정 변경·홈에 공통으로 제공한다.
- 검증: 변경 파일 ESLint, `npm run build`(SCSS 경고 없음), `npm run test:unit` 77건, `agent-feedback.rb final 619bb890` 통과(architectureTest 포함). 1280/390px 캡처 결과 모든 공개 화면 가로 넘침 없음(`output/playwright/after{2,3,4}`). E2E는 영향 spec 60건 중 53건 통과 후 원인 수정, 예약·검색 관련 6개 파일 17건, @smoke 20건, 새 `home-quick-booking.spec.ts`, 빈자리 알림 3건 통과.
- 기존 실패(변경 전 HEAD 별도 worktree에서 같은 지점 실패 확인, 수정 안 함): `customer-account-boundary` 203·355·656, `event-coupon-admin` 457, `guest-claim-onboarding` P8-9, `member-self-service` P8-7·P8-10. 관리자 상품 등록에서 기성품 수량 0은 400이 아닌 500(`IllegalArgumentException`)이다. 각각 별도 작업 카드로 남겼다.
- E2E 실행 조건: 이 노트북은 3000번(다른 Next 서버)과 `127.0.0.1:8080`(다른 Spring 앱)이 점유돼 있다. `PLAYWRIGHT_FRONTEND_PORT=3020 PLAYWRIGHT_BACKEND_URL=http://localhost:8080/api/v1 PLAYWRIGHT_SKIP_MFA_WEB_SERVER=1`로 실행했다. 기존 로컬 DB(`happygallery-mysql`)는 V22 체크섬 불일치라 건드리지 않고 확인용 `hg-design-mysql`(3307)을 사용했다. `bootRun` 중 다른 빌드가 산출물을 바꾸면 `NoClassDefFoundError`가 나므로 백엔드를 재시작한 뒤 실제 백엔드 E2E를 돌린다. 푸시·배포 미실행.

## CI/CD 정리 후 원격 반영 대기 (2026-10-05)

- 시작 SHA `23002b0ab4d6091743587147c9659f66622e50a8`. 기존 화면·README·프런트 스킬 변경과 작업 중 추가된 화면 변경은 별도 작업이며 이 커밋에 넣지 않는다. 현재 브랜치를 유지한다.
- CI 이미지 빌드·Trivy 검사를 공통화하고 `security-update-validation.yml`을 제거했다. 보안 봇은 `ci.yml`을 `production_candidate=true`로 직접 실행한다. 실패·취소·예상 밖 생략을 차단하는 `CI Gate`, 작업별 시간 제한, actionlint, 외부 Action SHA 고정·Node 24 실행 환경, JAR 7일 보관을 반영했다. 배포 백업·서버 호환성·digest·배포 직렬화는 유지한다. 구성·적용 절차는 [CI/CD 운영](deploy/k3s/cicd.md)에 있다.
- 실제 원격 조회: 최근 운영 성공 `36722337863`, SHA `b4edd17d`, 약 17분 52초. 과거 인계의 9월 말 변경들은 이 배포 이력을 기준으로 다시 판단한다. `CD_ENABLED=true`, production environment는 main만 허용하지만 main·codexReview 보호와 기존 ruleset은 꺼져 있다. `.github/branch-protection.json`은 적용안만 준비했으며 원격 설정을 바꾸지 않았다.
- 검증: `bash deploy/k3s/scripts/validate.sh` 통과(`/tmp/hg-cicd-validate.log`, Ruby 153건 중 Linux 전용 1건은 macOS에서 생략 후 `ruby:3.3` root 컨테이너에서 개별 통과, `/tmp/hg-cicd-linux-backup.log`). actionlint 1.7.12·ShellCheck 통과, 실제 CI Gate 실행문에 성공·실패·생략·취소 7조건 확인. `ruby tools/agent-feedback-test.rb` 14건 통과(`/tmp/hg-cicd-agent-tests.log`). 최종 검사 `ruby tools/agent-feedback.rb final 23002b0ab4d6091743587147c9659f66622e50a8` 통과(`/tmp/hg-cicd-final.log`). CI/CD diff의 의존 순서·검사 누락·중복을 검토했다. 같은 코드·설정·환경이면 재사용한다.
- 남은 행동: 사용자 원격 푸시 요청 후 PR의 새 CI Gate와 운영 후보 이미지 검사를 확인하고 두 브랜치 보호 적용안을 반영한다. 운영 배포·새 Actions 버전의 원격 실행·새 이미지 실빌드/Trivy는 이번 세션에서 실행하지 않았다. 화면 병행 변경은 해당 작업의 검증 기록을 따른다.
- Dependabot(2026-10-05): `codexReview`에 checkout v7·dependency-review v5·@types/node 26.4.0·trivy-action v0.36.0·upload-artifact v7이 병합돼 이 브랜치와 `.github/workflows/ci.yml`이 충돌한다. 해소할 때 이 브랜치의 SHA 고정 구조를 유지하고, Trivy만 공식 `v0.36.0` 태그 `ed142fd0673e97e23eac54620cfb913e5ce36c25`로 올린다. 3월 태그 탈취 이후 Aqua가 `v` 접두 태그로 재발행해서 `0.36.0`이 아니라 `v0.36.0`이다.
- Dependabot 후속: #126(gradle/actions v6)은 사용자 결정으로 v5 유지, `@dependabot ignore this major version`으로 닫았다. #156(Node 25)도 같은 명령으로 닫았고, 홀수 Node major 제외 규칙을 `.github/dependabot.yml`에 넣었다(`d314d7e7`, Dependabot은 기본 브랜치 설정을 읽으므로 main 반영 후 적용).
- 병합 완료: #159 `Fix: Jackson 2·3과 brace-expansion 보안 패치`(jackson 2.21.7·3.1.7, brace-expansion 5.0.12) → codexReview `56bbbf96`. #158 프론트 의존성 19건(undici 패치용 @scalar/openapi-parser 0.29.10, orval 8.39 생성 클라이언트 재생성 포함) → codexReview `057eaaa9`. 두 PR은 서로의 보안 패치가 없으면 CI가 실패해서, #159 브랜치를 #158에 합쳐 전체 검사 통과 후 순서대로 병합했다. Jackson 3줄은 이 브랜치에도 같은 내용으로 커밋했다(`0796c4c5`).
- #128 vite 8.3.2: `@dependabot rebase`로 최신 codexReview 위에 다시 만들었다(`5c2425e7`). 로컬에서 `npm ci`·audit(0건)·단위 67건·lint·api:check·build 통과. CI가 전체 통과하면 병합한다. `vite.config.ts`의 `__dirname`은 vite의 향후 native 설정 로더 경고가 나오므로 나중에 `import.meta.dirname`으로 바꾼다.
- `main`(jackson 2.21.6·3.1.6)도 신규 CVE 4건에 해당한다. `codex/work-security-*` → main 보안 PR이 필요하며 병합 시 운영 배포가 진행되니 사용자 확인 후 진행한다.
- #158 병합 후 이 브랜치를 codexReview와 합치면 `frontend/package-lock.json`과 `src/generated/api`가 충돌한다. 이 브랜치 쪽을 택한 뒤 `npm install`과 `npm run api:generate`를 orval 8.39로 다시 실행한다.
- 미병합 유지: #120 Gradle 묶음(springdoc 3.1.1로 OpenAPI 산출물이 바뀌고, Sentry 8.56은 `SentryEventSanitizerTest` 재확인 필요), #122 logstash-logback-encoder 9(Jackson 3 전환 필요), #127 TypeScript 7(typescript-eslint 범위 밖).

## Codex 스킬 이식·도메인 스킬 추가 (2026-10-04)

- 시작 SHA `b4edd17dec8414163812a48fc77c8b8843adce2a`, 미커밋 변경 없음. 전역 Codex 스킬(playwright·gh-fix-ci·gh-address-comments·yeet·review-agent·security-*·seo-*)을 이 저장소 기준으로 다시 써서 `happygallery-{ui-verification,github-flows,code-review,security-review,seo-ssr}`를 추가했다. 스킬이 없던 쿠폰·적립금(`benefit-flows`), 후기·이미지 미디어(`review-flows`), 스마트스토어(`smartstore-flows`)도 추가했다. screenshot·sentry·figma·diagram·hope-* 등은 범용이거나 운영 전이라 가져오지 않았다.
- 기존 product(ADR-0046 SKU 규칙)·payment·order·batch·booking·frontend 스킬과 `AGENTS.md`에 새 스킬 연결 문구만 넣었다.
- 검증: `ruby tools/check-agent-skills.rb` 27개 통과, `agent-feedback.rb final` 통과. 스킬에 적은 클래스·식별자·명령은 `rg`로 존재를 확인했다. 문서 변경이라 앱 빌드·테스트는 실행하지 않았다. 셸 locale이 US-ASCII면 검사 도구가 인코딩 오류를 내므로 `LANG=en_US.UTF-8 LC_ALL=en_US.UTF-8`을 붙인다.
- 조사 중 발견한 미확인 후속 후보(수정 안 함): `BatchScheduler` 정산 Javadoc "최근 7일"과 실제 커서 방식 불일치. 공지 JSON-LD 시간대·Organization 참조·Course Offer URL·404 canonical은 2026-10-05에 수정했다(ADR-0045).
- 서버 Sentry 요청 정보·로그 마스킹 후보는 2026-10-05에 사실로 확인해 수정했다. sentry-spring 8.25.0 바이트코드와 실제 전송 envelope으로 확인한 결과, 오류 이벤트와 샘플 트랜잭션 모두 비회원 token 3종 헤더·요청 query·Referer query·외부 호출 `http.query`(우체국 `serviceKey`)를 담았다. Bearer·세션 쿠키는 SDK 기본 목록이 이미 제거했다. `SentryEventSanitizer`(adapter-in-web `monitoring`)가 `beforeSend`·`beforeSendTransaction`에서 제거하고, `SensitiveLogMasker`는 `X-Payment-Status-Token`을 비회원 token 패턴에 포함한다. ADR-0028·PRD 0001 반영. 커밋 마스킹 `85ce4b50`, Sentry `f234633b`.
- 검증: `./gradlew --no-daemon :adapter-in-web:test --tests '*SentryEventSanitizerTest' :bootstrap:test --tests '*SensitiveLogMaskerTest'` 1·4건 통과. 정제 bean을 뺀 같은 테스트는 위 값의 노출로 실패해 수정 전 동작을 재현했다. `RequestIdFilterUseCaseIT` 4건으로 전체 context 기동, `agent-feedback.rb final 619bb890` 통과(architectureTest 포함). 같은 브랜치 병행 세션의 프론트 미커밋 변경은 커밋에 넣지 않았다. 실제 Sentry 서버 수신·푸시는 미실행. Sentry 의존성·필터·`sentry.*` 설정이 바뀌면 `SentryEventSanitizerTest`를 다시 실행한다.
- 확인된 후속 후보(수정 안 함): 공지 `createdAt`은 DB 기본값 UTC인데 `NoticeDetailResponse`·`NoticeListResponse`(관리자 공지 API도 같은 DTO)가 offset 없는 `LocalDateTime`으로 내보낸다. 프론트 `formatDateTime`이 서울 시각으로 해석해 공지 목록·상세·관리자 화면 시각이 9시간 이르게 보인다. 다른 DB 생성 시각처럼 응답을 `atOffset(ZoneOffset.UTC)`로 바꾸는 API 계약 변경이 필요하다(api-contract). JSON-LD는 offset이 있으면 그대로 쓰므로 함께 고칠 필요는 없다.

## 소셜 아이콘·비회원 조회 화면 개편 (2026-09-30)

- 시작 SHA `84b52cc800cc46a2993ca83ba54756663dbcd1ba`, 미커밋 변경 없음. Google 공식 G와 카카오 공식 말풍선 아이콘을 로그인·회원가입 버튼에 추가했다. 제공자 색상과 세 버튼 높이 48px를 유지하며 출처는 `frontend/src/assets/README.md`에 적었다.
- 오늘의집 비회원 주문 조회의 입력 우선 구성을 참고해 `/guest`에 주문·예약 선택과 번호·조회 코드 폼을 배치했다. 개별 주문·예약 조회도 같은 패널·폼을 사용한다. 재발급·결제 확인은 하단 펼침 도움말, 회원 내역 가져오기는 보조 안내로 정리했다. `features/guest-lookup/`이 화면 공통 구성을 담당하며 예약 변경·취소·같은 수업 예약과 세션 소유권 검사는 보존한다. 제품 규격도 반영했다.
- 검증: frontend `npm run build`, 변경 파일 lint, 최종 typecheck 통과(`/tmp/hg-guest-{build,final}.log`). 기존 E2E 9건 통과: 복구 저장 경합·다른 탭 계정 전환·주문 정보 경계 3건(`/tmp/hg-guest-e2e.log`), 결제 복구 2건·비회원 이력 페이지·재예약 2건·비회원 일정 변경 총 6건(`/tmp/hg-guest-flow-e2e.log`). 도움말을 펼치는 단계를 기존 테스트에 추가했다.
- Playwright CLI로 1280/390px 실제 화면과 새 첫 화면의 주문·예약 전달, 잘못된 번호 제출 차단, 조회 코드 URL 비노출, 종류 전환 시 입력 초기화, 도움말 열기 및 모바일 가로 넘침 없음을 확인했다. 캡처 `output/playwright/{guest-lookup,social-icons}-{desktop,mobile}.png`. 로그인·회원가입 1280/390px 네 화면의 아이콘 로딩·높이 48px·동일 폭 확인(`/tmp/hg-guest-social-dimensions.log`). API·DB·의존성·운영 배포 설정은 바꾸지 않았다. 전체 diff에서 보호 규칙 유지·화면 경로·검증 중복을 검토했다. 완료 커밋: 소셜 아이콘 `28b89826`, 비회원 조회 `9876a30c`. 푸시·운영 배포 미실행. 코드·설정·환경이 같으면 위 검증을 재사용한다.

## 소셜 로그인 버튼 크기 통일 (2026-09-30)

- 시작 SHA `7c562e54cddf605cf6155cb20c4e98d903919d2a`, 미커밋 변경 없음. 네이버 전용의 높이·여백·글자 크기를 공통 social-login-button 스타일로 옮기고 flex 중앙 정렬했다. Google·네이버·카카오 모두 높이 48px, 같은 폭과 글자 크기이며 로그인·회원가입에 함께 적용된다.
- 검증: frontend `npm run build`, 지역 lint·최종 타입 검사 통과(`/tmp/hg-social-size-{build,local,final}.log`). Playwright CLI로 1280/390px 로그인·회원가입 네 화면에서 버튼 3개의 높이·폭·글자 크기 일치 확인. 화면 캡처 `output/playwright/social-size-*.png`, 치수 로그 `/tmp/hg-social-size-dimensions.log`. API·인증 흐름·의존성은 동일해 이전 관련 검사 결과를 재사용한다. 최종 diff에서 크기 규칙의 중복과 제공자 색상 누락을 검토했다. 푸시·배포 미실행.

## 배포 검사 정리 경합·이용권 조회 기대값 수정 (2026-09-30)

- 시작 SHA `c338b487421c08bbf6835c2e98e34ecc187b1947`, 미커밋 변경 없음. 실행 `36708978472`에서 프론트엔드 보안 검사·빌드·smoke와 application 세 그룹은 통과했다. 실패는 `rolling-release-test.rb` teardown의 `.git/objects` ENOENT와 `MePassUseCaseIT`의 신규 구매 기대값 120,000원/실제 300,000원이다. 로그 `/tmp/hg-ci-36708978472-failed.log`, 웹 XML `/tmp/hg-ci-36708978472-web/`.
- 임시 Git 저장소를 쓰는 rolling-release·agent-feedback fixture의 `gc.autoDetach`와 `maintenance.autoDetach`를 false로 설정해 삭제 전 자동 정리 종료를 보장한다. 프론트엔드 배포 검사 Ruby도 다른 CI 작업과 같은 3.3으로 명시했다. 신규 구매 조회 기대값은 300,000원으로 변경했고 기존 구매/환불 fixture의 240,000원은 보존했다.
- 검증: `./gradlew --no-daemon :adapter-in-web:check` 전체 통과(일반 192·REST Docs 308·OpenAPI 1건 및 명세 일치 확인), `/tmp/hg-ci-repeat-web-check.log`. Ruby 3.3 Linux에서 빈번한 자동 Git 정리 조건과 실패 seed의 rolling-release 28건·agent-feedback 14건 통과. 배포 전체 검사는 후반 백업 테스트의 USER 누락으로 중단돼 환경에 USER=root를 준비한 후 실패 범위 16건과 마지막 indexnow 14건을 재검증했다. 두 로그 `/tmp/hg-ci-repeat-linux{,-tail}.log`를 합쳐 운영 검사 전 범위 통과를 확인했다. workflow actionlint 통과.
- 검증: 이번 CI의 application-jar로 backend 이미지를 만들고 현재 frontend도 amd64 이미지로 build했다. 앱 실행 계정 JAR 읽기 검사와 Trivy 0.69.3 OS/library HIGH·CRITICAL 모두 0건 통과. 보안 DB·Java DB를 갱신했고 보고서는 `/tmp/hg-ci-repeat-security/{backend,frontend}.json`, 이미지 build 로그 `/tmp/hg-ci-repeat-{app,frontend}-image.log`. 최종 검사 통과(`/tmp/hg-ci-repeat-final.log`), 전체 5개 파일 diff의 테스트 의미·의존성·검사 생략 여부를 검토했다.
- 완료 커밋: 이용권 기대값 `2ff0db91`, Git 정리·Ruby 환경 `ae49a4f2`. 푸시·운영 재배포는 아직 하지 않았다. source·의존성·설정 변경 시 영향받는 검사를 재실행하며 runtime이 같은 이전 npm 전체 검사와 이번 CI smoke/application 성공은 재사용한다. 다음 행동은 사용자 푸시 요청 후 새 원격 CI·두 운영 이미지 검사·rollout 성공 확인이다.

## 프론트엔드 보안 검사 배포 차단 수정 (2026-09-30)

- 시작 SHA `61c7327f0f9883ac735ec322da84951ab38e77bc`, 미커밋 변경 없음. 운영 실행 `36706195838`의 `validate / Frontend Build`는 `npm run audit:dependencies`에서 높은 등급 brace-expansion 및 중간 등급 fast-uri·markdown-it 취약점으로 실패했다. 린트·API 검사·빌드 전에 중단됐으며 실패 로그 `/tmp/hg-frontend-ci-failure.log`를 확인했다.
- 잠금 파일의 간접 의존성만 brace-expansion 5.0.9 → 5.0.12, fast-uri 3.1.7 → 3.1.8, markdown-it 14.3.0 → 14.3.2로 갱신했다. 상위 도구·직접 의존성·보안 검사 기준은 유지한다. 잠금 갱신 시 취약점 0건 확인(`/tmp/hg-frontend-patch.log`).
- 검증: Linux amd64·Node 22.23.2·npm 10.9.8의 격리된 작업 복사본에서 `npm ci` → `audit:dependencies`(취약점 0건) → `test:unit`(74건) → `lint` → `api:check` → `build` 모두 통과(`/tmp/hg-frontend-ci-check.log`). 새 결과물로 `REQUIRE_FRONTEND_DIST=1 deploy/k3s/scripts/validate.sh` 통과(`/tmp/hg-frontend-deploy-check.log`). 최종 검사·타입 검사 통과(`/tmp/hg-frontend-final.log`), 전체 diff에서 세 패키지 외 갱신과 생성 API 변경이 없음을 검토했다.
- 남은 행동: 사용자 푸시 요청 후 수정본으로 CI·배포 확인. 운영 실행 `36706195838`은 이후 취소돼 backend 테스트 일부·smoke·publish·rollout도 중단됐다. 수정본 원격 실행·푸시·배포는 하지 않았다. 이전 화면 작업의 `npm run build` 성공은 프론트엔드 CI 전체 통과와 구분한다. 의존성/설정 변경 시 해당 검사를 다시 실행하며 보안 DB가 갱신될 수 있어 재배포 시 보안 검사는 새 결과를 확인한다.

## 개인정보 보존 배치 이미지 참조 조회 수정 (2026-09-30)

- 시작 SHA `1b3e8cc5a06369a804662cf95e0b1a7ed40f4d54`, 미커밋 변경 없음. 사용자가 제공한 03:30 운영 로그에서 `image_media` 참조 조회의 MySQL 1271 `Illegal mix of collations for operation 'UNION'`을 확인했다. 이미지 삭제 전 조회에서 실패했다. 함께 기록된 결제·환불 복구 배치는 실패 0건이다.
- `JdbcImageMediaReferenceReaderAdapter.findReferencedImageUrls`의 다섯 참조 원본을 `utf8mb4 / utf8mb4_bin`으로 통일해 합친다. DB schema는 바꾸지 않고 파일명 대소문자를 구분한다. 실제 MySQL 8의 서로 다른 collation으로 수정 전 동일 오류를 재현하고 수정 후 전체 참조·한글·대소문자·중복·null 처리를 검증했다.
- 검증: `./gradlew --no-daemon :application:useCaseTest --tests '*JdbcImageMediaReferenceReaderAdapterUseCaseIT' :application:test --tests '*ImageMediaRetentionServiceTest'` 5건 통과(`/tmp/hg-retention-after.log`). 수정 전 재현 로그 `/tmp/hg-retention-before.log`, 지역 컴파일 검사 `/tmp/hg-retention-local.log`. 최종 검사·아키텍처 통과(`/tmp/hg-retention-final.log`), 전체 3개 파일 diff의 참조 누락·의존 방향·중복 구현을 검토했다. 코드·테스트·의존성·환경이 바뀌면 영향받는 검사만 재실행한다.
- 남은 행동: 사용자 푸시 요청 후 배포하고 다음 개인정보 보존 배치의 `failureCount=0` 및 경보 해소를 확인한다. 운영 실행은 아직 검증하지 않았고 SSH home-server 공개키 인증 실패 상태다. 사용자가 보여준 k3s kubeconfig 권한 오류와 실제 배치 SQL 오류는 서로 다른 문제다.

## 헤더·4회권 가격·단체 문의 팝업 (2026-09-30)

- 시작 SHA `14c61e07`, 미커밋 변경 없음. 상단 유틸리티 바를 크림 배경과 이어지는 웜그레이로 변경했다. 단체 문의는 상담 영역 버튼에서 scrollable Modal로 열고 접수 중 닫기를 막는다. 기존 회원·비회원 분기·Turnstile·접수 결과 안내를 유지했다.
- 신규 4회권 기본값과 k3s PASS_TOTAL_PRICE를 300,000원으로 조정했다. 환경 예시·PRD·가격 ADR에 반영하고 기존 구매/결제 준비 스냅샷의 금액은 바꾸지 않았다. API 계약 형식은 동일하다.
- 검증: frontend build, 회원/비회원 접수 2건, Turnstile 입력 보존·재접수 2건, 1280/390px 팝업 닫기·Escape·포커스 복귀 확인. 미리보기 `/tmp/hg-sep30-preview/`. 결제 준비 서버 가격 통합 1건, 아키텍처·타입·미커밋 소스 호환성 최종 검사 통과. 운영 설정 검증 로그 `/tmp/hg-sep30-deploy-validate.log`.
- 사용자 오류는 k3s 관리자 kubeconfig 조회 권한 문제다. 운영 가이드에 sudo 로그 조회를 추가했다. 이후 제공된 03:30 로그로 실제 배치 source/예외를 확인했으며 수정은 위 항목을 참고한다.
- 사용자 승인으로 09-29 Actions PR 생성·승인 허용 옵션은 활성화했고 기본 권한 read 유지했다. 아래 이전 자동화 기록의 설정 차단 상태는 해소됐다. 현재 변경의 푸시·배포는 하지 않았다.

## Trivy 보안 업데이트 PR 자동화 (2026-09-29)

- 시작 SHA `781af329`, 미커밋 변경 없음. 두 이미지 JSON 검사 결과를 보관하고 실패하면 관리 중인 Jackson 2·3/Tomcat/Netty/HttpCore5의 동일 major/minor 패치 수정판만 PR로 제안한다. 배포 실패 상태는 유지하고 자동 병합하지 않는다. 브랜치별 workflow_dispatch로 기존 CI와 두 이미지 보안 검사를 다시 실행한다.
- GitHub Actions 기본 권한 read, `can_approve_pull_request_reviews=false` 확인. PR 생성 허용 옵션은 리뷰 승인 권한도 묶여 있어 auto-review가 변경을 거절했다. 원격 설정 변경 없음. 사용자에게 해당 묶음 권한 승인이 필요함을 알린다. 푸시·실제 PR 생성·원격 CI 실행 미실행.
- 관련 경로: `.github/workflows/production.yml`, `.github/workflows/ci.yml`(2026-10-05 보안 검증 통합), `deploy/k3s/scripts/security-update.rb`, `deploy/k3s/cicd.md`. 로컬 검증 로그 `/tmp/hg-security-auto-*.log`.

## Jackson 보안 검사 배포 차단 (2026-09-29)

- 시작 SHA `6cce6be7`, 미커밋 변경 없음. 운영 실행 `36575335081`은 호환성 검사 이후 backend Trivy에서 CVE-2026-68497(Jackson 2.21.4·3.1.4)로 실패했다. rollout 이전 실패다.
- Boot 관리 BOM을 Jackson 2.21.6·3.1.6으로 재정의했다. `:bootstrap:bootJar`, `:adapter-in-web:test --tests "*SessionCompatibilityTest"`(5개), `:application:architectureTest` 통과. JAR의 두 databind·core 버전 반영 확인. `/tmp/hg-jackson-build.log`, `/tmp/hg-jackson-final.log`.
- 운영과 같은 amd64 이미지 및 Trivy 0.69.3 HIGH/CRITICAL 검사 통과: OS·JAR 모두 취약점 0건. `/tmp/hg-jackson-scan.log`. 원격 푸시·재배포는 미실행이다.

## 호환성 검사 자동화와 데이터 보정 검토 (2026-09-29)

- 시작 SHA `8f909775`, 미커밋 변경 없음. 완료 검사에 관련 변경의 소스 호환성 검사를 자동 연결했다. 임시 인덱스로 미커밋·신규·삭제·작업 중 커밋을 포함하며 사용자 스테이징은 보존한다. 로컬 기준은 작업 시작 SHA, CI 기준은 마지막 성공 rollout으로 구분한다.
- 공방 이메일 전용 예외를 제거했다. 제한된 조건부 문자열 UPDATE는 migration 원문 SHA-256과 대상·범위·구버전 호환·복구·검증 근거를 같은 PR에 기록한다. V185 기록을 추가했고 원문 SQL은 유지했다. 사람의 승인 강제는 브랜치 보호 정책의 영역이며 이 작업에서 원격 설정은 변경하지 않았다.
- `ruby tools/agent-feedback-test.rb` 14개·54 assertions 통과. 임시 인덱스 스테이징 보존·새 SQL 차단·일반 문서 검사 생략 확인. 실제 마지막 운영 SHA `4b8a61bd`부터 현재 작업 트리 호환성 통과. 배포 전체 검증·최종 검사 결과는 세션 로그 `/tmp/hg-auto-*.log` 참조. 푸시·재배포 미실행.
- 아래 이메일 전용 예외 기록은 이전 구현 이력이다. 현재 절차는 `deploy/k3s/rolling-deployments.md`의 데이터 보정 절차를 따른다.

## 이메일 보정 배포 차단 수정 (2026-09-29)

- 시작 SHA `04b0b9c4`, 미커밋 변경 없음. 배포 `36493819298`은 V185 이메일 UPDATE가 자동 허용 대상이 아니어서 Rolling Compatibility에서 실패했고 운영 rollout은 실행되지 않았다. 직전 성공은 `36489698953` / `4b8a61bd`(한국 시각 09-29 07:16 완료)이다.
- 공개 `workshop_profiles.email`의 문자열 이메일 값 대입 + 기존 이메일 값 일치 조건만 허용한다. 일반 UPDATE·회원 데이터·조건 없는 변경·추가 SQL은 차단한다. migration 원문은 변경하지 않았다.
- 검증: rolling-release-test.rb 26개·86 assertions 통과. 수정한 검사기로 실제 운영 기준 `4b8a61bd` → 실패 리비전 `04b0b9c4` 소스 호환성 통과. 원격 푸시·재배포는 미실행이며 다음 요청 시 이 수정까지 포함해야 한다.
- 반복 배치 알림 원인은 여전히 미확인이다. SSH `home-server` 공개키 인증 실패로 운영 로그를 못 읽었다. 기존 아래 인계의 “776b6f9a 미배포”는 과거 상태이며 이후 성공 배포에 포함됐다. 운영 경보 규칙·실패 reason은 서버에서 추가 확인해야 한다.

## smoke·백업 배포 시간 단축 (2026-09-29)

- 시작 SHA `7528c84753e6119a4dcf936e20d01721d3e975f1`, 시작 시 미커밋 변경 없음. 로컬 구현·검증 완료. smoke 커밋 `81010197`. 푸시·운영 반영은 아직 하지 않았다.
- 최신 운영 성공 `36408132625`는 19분 53초. smoke 첫 실행 실패 두 건은 캐시 없는 로컬에서도 `504 Outdated Optimize Dep`로 재현했다. Vite 공통 의존성을 사전 최적화하고 최초 실패 trace·재시도 성공 진단을 보관한다.
- R2 내용 검증을 마친 백업을 `runuser`로 배포 계정 캐시에 전달하도록 추가했다. 원격 백업 조회·시각·전체 해시 검사는 유지한다. 다음 운영 준비: `/opt/happygallery` 백업 스크립트 갱신 후 `backup.env`에 캐시 사용자·경로 설정. 정확한 절차는 `deploy/k3s/cicd.md`의 ‘검증된 백업의 로컬 전달’. 설정 전에는 기존 재다운로드 방식이다.
- 검증: 캐시 없는 CI 조건(`CI=true`, 43220/43221, 백엔드 8088, `--retries=0`)에서 문제의 두 smoke 수정 전 모두 실패(`/tmp/hg-speed-cold-smoke.log`), 수정 후 16.2초 통과(`/tmp/hg-speed-cold-fixed.log`). 전체 `npx playwright test --grep @smoke --retries=0` 20개 약 1분 통과(`/tmp/hg-speed-full-cold.log`). 로컬 결과이며 GitHub 배포 단축 실측은 미확인이다.
- 검증: `npm run build` 통과(`/tmp/hg-speed-build.log`), `bash deploy/k3s/scripts/validate.sh` 전체 통과(`/tmp/hg-speed-validate.log`), Linux ruby:3.3 root에서 `rclone-backup-test.rb` 16개·102 assertions 통과(`/tmp/hg-speed-linux-cache.log`, 실제 runuser 소유권 분리 포함). actionlint와 최종 검사 통과(`/tmp/hg-speed-final2.log`), 전체 diff의 검증 유지·권한 분리·누락 검토 완료. 같은 코드·설정·환경이면 재사용한다.
- 기존 개발 DB migration checksum 불일치로 실제 주문 검증은 별도 `hg-speed-e2e-mysql` 컨테이너와 Java 25 백엔드로 분리했다. 기존 DB 이력은 변경하지 않았다. 다음 행동은 사용자 푸시 요청 후 CI 소요 시간과 운영 캐시 활성화 여부 확인이다.

## 소셜 전화번호 등록 (2026-09-28)

- 시작 SHA `1fb44deeb5d917632b5cff41c0250d2d08daa354`. Naver `mobile`·Kakao `phone_number`를 신규 가입 연락처로 자동 등록하고 Google은 가입 후 기존 마이페이지 SMS 등록으로 안내한다. 제공자 번호 누락·미동의는 신규 가입을 차단하며 기존 회원 번호는 덮어쓰지 않는다.
- 동일 번호 가입은 사전 조회와 DB 유일 제약으로 차단한다. 소셜 연락처는 `phoneVerified=false`를 유지해 과거 비회원 기록이나 비밀번호 복구 권한을 부여하지 않는다. 제품·API 정책은 PRD 0001·0004, 콘솔 설정은 README에 반영했다. 검토 중이던 미추적 PortOne 연동 파일은 제거했다.
- Java 25·Docker: 소셜 가입/동시 중복/이메일 발급 application 통합 검사, 휴대폰 등록 및 제공자 프로필 검사 통과(`/tmp/hg-social-phone-tests2.log`). 기존 웹 인증·이메일 등록·세션 호환 검사 통과(`/tmp/hg-social-phone-tests.log`; 해당 실행의 휴대폰 fixture 실패만 수정 후 tests2에서 재검증). REST Docs·OpenAPI 생성 통과(`/tmp/hg-social-phone-contract.log`), TypeScript API 재생성 결과 계약 파일 변경 없음.
- 프론트 타입 검사 및 모바일·데스크톱 제공자별 가입 6개와 가입 오류 3개 검사 통과. 병행 작업과 포트 충돌을 피해 `PLAYWRIGHT_FRONTEND_PORT=3110 PLAYWRIGHT_SKIP_MFA_WEB_SERVER=1`로 `social-signup-consent.spec.ts`를 실행했다. 로그 `/tmp/hg-social-phone-e2e-isolated.log`, `/tmp/hg-social-phone-error-e2e.log`. 구조·의존 방향 검사와 전체 diff 검토 완료(`/tmp/hg-social-phone-final.log`). 코드·설정·환경이 같으면 결과를 재사용한다.
- 남은 운영 준비: Naver 개발자센터의 휴대전화번호 제공 항목, Kakao Developers의 `phone_number` 권한·동의 항목을 승인·활성화해야 한다. 실제 제공자 계정으로의 연동 검증은 미실행이다. 배포 전 이미 시작된 5분 가입 대기는 새 번호 필드가 없어 재로그인이 필요할 수 있다. 사용자 요청대로 로컬 커밋만 유지하고 푸시·배포하지 않는다.
- 병행 작업의 회원탈퇴 화면 변경은 별도 커밋 `80ee19d4`, `96f30773`으로 처리됐다. 이번 변경에 포함하지 않는다.

## 수동 호환성 목록 제거 (2026-09-27)

- 시작 SHA `458f727cae85e44cffbdcfee504dcf75001bbe39`, 시작 시 미커밋 변경 없음. 사용자 지시에 따라 기존 브랜치에서 로컬 변경만 진행한다. 푸시·배포하지 않는다.
- 구현 커밋 `6ae83845`. 누적 파일 승인 목록을 제거했다. Git diff 기반 API·migration 검사와 서버 manifest 검사는 유지하며 보호 Spring 설정 변경은 항목을 표시하고 별도 전환 대상으로 차단한다.
- `SessionCompatibilityTest`는 실제 운영 기준 `df0d4ead`의 로그인 키·가입 intent·계정 연결/재인증 intent·재인증 증명 저장 계약을 고정한다. 이전 상태 읽기와 현재 저장 형식을 확인하며 Spring context를 추가하지 않는다. 자동 판정 밖의 의미·전환은 PR 템플릿에서 검토한다. 설명: `deploy/k3s/rolling-deployments.md`.
- Java 25에서 `./gradlew --no-daemon :adapter-in-web:test --tests '*SessionCompatibilityTest' --tests '*SocialSignupIntentStoreTest' --tests '*PendingSocialSignupStoreTest'` 통과: 3개 클래스·12개 사례. `/tmp/hg-session-compat.log`. 첫 sandbox 실행은 Gradle 캐시 권한 문제로 시작하지 못했고 승인된 실행에서 통과했다.
- 새 검사기로 마지막 운영 성공 SHA `df0d4ead`부터 기존 로컬 HEAD `458f727c`까지 `check-source` 통과. 수동 목록 없이 기존 소셜 가입 변경이 허용되는 것을 확인했다. 운영 반영·CI 실제 시간·Redis 직렬화 라이브러리 전체 호환성은 검증하지 않았다.

- `bash deploy/k3s/scripts/validate.sh` 전체 통과: `/tmp/hg-automatic-compat-validate.log` (롤링 검사 24개·79 assertions 포함). `ruby tools/agent-feedback.rb final 458f727cae85e44cffbdcfee504dcf75001bbe39` 통과: `/tmp/hg-automatic-compat-final.log`. 새 파일 포함 전체 diff의 검사 유지·의존 방향·중복·문서 일치 검토 완료. 이후 문서에 결과만 추가했다. 코드·설정·의존성·환경이 같으면 이 검증을 재사용한다.
- 다음 행동: 사용자 푸시 요청 시 원격 반영 후 Production 결과와 실행 시간을 확인한다. 개인정보 배치 오류 후속 확인은 아래 항목을 유지한다.

## Spring context·컨테이너 재사용 개선 (2026-09-27)

- 시작 SHA `ccce06dc9c004e6bf2a786979c16f44af8eb8e5b`, 시작 시 미커밋 변경 없음. 기존 `codex/work-deploy-preflight-speed` 브랜치에서 진행했다. 사용자 지시대로 로컬 커밋만 유지하며 푸시·배포하지 않는다.
- 구현·로컬 검증 완료: `95526028`, 인계 기록 `01c7a34a`. context 공유 조건·격리·비동기 정리·전후 측정값은 [ADR-0026](docs/ADR/0026_통합_테스트_프로파일과_TestContainer_기준선/adr.md)에 정리했다. 실제 GitHub CI 단축 시간은 아직 미측정이다.

검증 기록(관련 코드·설정·환경이 같으면 재사용):

- Java 25·Docker 환경. 위 전체 application 검사 통과: `/tmp/hg-context-before.log`, `/tmp/hg-context-after-final.log`. XML 비교 결과 `/tmp/hg-context-before-results/test`, `/tmp/hg-context-after-final-results/test`: 기존 사례 누락 없음, 실패 0. 첫 변경 후 실패 실행은 spy 타입과 알림 정리 문제를 수정했고 시간 도약 경고가 있어 성능 수치에 사용하지 않았다.
- `:test-support:compileTestFixturesJava` 통과. 실패 범위 3개 클래스 재검증 통과: `/tmp/hg-context-spy-retry2.log`. 이후 전체 검사가 최종 변경을 검증했다.
- `./gradlew --no-daemon :adapter-in-web:test --tests '*UseCaseIT' :adapter-in-web:verifyOpenApi` 통과: 웹 통합 16개 클래스·97개 사례와 OpenAPI 일치 확인. `/tmp/hg-context-web-tests.log`.
- `ruby tools/agent-feedback.rb final ccce06dc9c004e6bf2a786979c16f44af8eb8e5b` 통과(실제 architectureTest 포함), `/tmp/hg-context-final-feedback.log`. 전체 diff의 테스트 범위·fixture 의존 방향·격리·중복 구현 검토 완료. 이후 인계 문서만 변경했다.
- 다음 행동: 사용자 푸시 요청 시 아래 배포 개선과 함께 원격 반영하고 Production 결과·소요 시간을 확인한다.


## 배포 실패·CI 시간 개선 (2026-09-27)

- 작업 브랜치 `codex/work-deploy-preflight-speed`. 시작 SHA `776b6f9a7a153fdf7e670176bb06014fb0abae6b`, 시작 시 미커밋 변경 없음.
- 최근 Production `35620477260`은 소셜 가입 변경 4개 파일의 호환성 검토 기록 누락으로 실제 apply 전에 실패했다. 직전 성공은 `35542816886`, SHA `df0d4eade56977562a842d2dc54fb68beb6ecc0a`. 이전 인계의 백업·배포 잠금 문제 이후 성공 배포가 있었으므로 해당 문제를 현재 장애로 재사용하지 않는다.
- 검토 기록을 보완하고 서버와 공용인 소스 호환성 검사를 CI 빌드 앞에 추가했다. 운영은 직전 push가 아닌 마지막 실제 성공 배포와 비교한다. 서버 최종 검사·백업은 유지한다. 세부 조건: `deploy/k3s/cicd.md`, `deploy/k3s/scripts/ci-compatibility.rb`.
- 해당 실행은 총 27분 18초, application 검사 17분 49초, 이미지 게시 3분, 서버 단계 6분 14초. application 테스트의 Spring 환경 반복 기동과 migration 검사가 병목이다. 공통 기능·주문/결제/예약·migration 세 독립 CI 실행기로 분할했다. 로컬 check는 전체 검사 유지. 실제 단축 시간은 다음 Production에서 측정해야 한다.
- 다음 행동: 원격 푸시·병합은 사용자 요청 후 진행한다. 새 실행의 사전 검사·전체 검사·배포 성공과 소요 시간을 확인한다. GitHub 성공 이력과 서버 manifest가 다른 수동 배포/rollback은 서버 최종 검사에서 별도로 감지한다.

검증 기록(관련 코드·설정·환경이 같으면 재사용):

- `bash deploy/k3s/scripts/validate.sh` 전체 통과, `/tmp/hg-deploy-validate.log`. 롤링 검사 25개/85 assertions와 신규 CI 기준 선택 검사 4개/6 assertions 포함.
- `actionlint .github/workflows/ci.yml .github/workflows/production.yml` 통과. 실행 파일 `/tmp/hg-actionlint/actionlint`.
- 실제 GitHub API에서 마지막 성공 배포 SHA 선택 통과. 실패 SHA의 소스 호환성 오류 재현(`/tmp/hg-compat-before.log`), 선언 보완을 반영한 임시 Git snapshot에서 같은 운영 기준 비교 통과.
- Java 25에서 `:application:check -PciTestGroup=<core|commerce|migration>` 및 속성 없는 전체 check를 Gradle `Test.dryRun=true`로 실행해 발견 대상을 비교했다. 기존 CI의 172개 테스트 클래스 전부가 정확히 한 그룹에 포함되고, 동일 dry-run 조건의 780개 사례도 누락·중복 없이 일치했다. 파라미터별 실행은 dry-run에서 펼쳐지지 않는다. 실제 업무 테스트 재실행 성공으로 보고하지 않는다. 로그 `/tmp/hg-ci-{core,commerce,migration,all-selection}.log`, 결과 `/tmp/hg-ci-selection/`.
- Java 25·Gradle 캐시 접근 권한으로 `ruby tools/agent-feedback.rb final 776b6f9a7a153fdf7e670176bb06014fb0abae6b` 통과(실제 architectureTest 포함). 전체 diff에서 누락·중복·검사 의존 관계 검토 완료. 결과 `/tmp/hg-deploy-final-feedback.log`.

## 개인정보 보존 배치 후속 확인

- 실제 오류 원인은 위 2026-09-30 이미지 참조 조회 수정 항목에서 확인했다. 운영 메일은 `BatchExecutionFailed / personal_data_retention / partial`이며 일반 SSH는 공개키 인증 오류 상태다.
- 정상 완료까지 유지하는 `PersonalDataRetentionFailed` 경보는 이전 성공 배포에 반영됐다. 기존 BatchExecutionFailed의 Resolved 메일은 10분 집계 창 종료일 수 있으므로 배치의 정상 재실행으로 판단하지 않는다.
- 수정 배포 후 첫 실행의 실패 개수와 경보 해소를 확인한다. `monitoring/alerts.yml`, `application/src/main/java/com/personal/happygallery/application/batch/DefaultPersonalDataRetentionBatchService.java` 참고.
