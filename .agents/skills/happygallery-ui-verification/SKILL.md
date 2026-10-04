---
name: happygallery-ui-verification
description: happyGallery 화면 변경을 실제 브라우저에서 확인하거나 화면 버그를 재현·캡처할 때 사용한다. 화면 코드 수정은 happygallery-frontend-flows, E2E spec 추가·수정은 해당 도메인 스킬과 frontend 스킬을 함께 사용한다.
---

# happyGallery 화면 확인

## 실행 준비

- 백엔드가 필요하면 `docker compose up -d mysql redis` 후 `./gradlew :bootstrap:bootRun`(8080), 프론트는 `frontend`에서 `npm run dev`(3000)를 실행한다. 이미 실행 중인 서버는 `/healthz`·`/actuator/health`로 확인하고 재사용한다.
- 관리자 계정과 개발용 관리자 key는 README `로컬 기본값`을 따른다. 회원·비회원 데이터는 화면 가입이나 API로 새로 만들고, 실제 개인정보와 운영 계정은 쓰지 않는다.
- 결제는 Toss 테스트 key와 E2E `installTossPaymentStub` 방식만 사용한다. 실제 PG·SMS·카카오·스마트스토어 쓰기 호출은 하지 않는다.
- 백엔드 없이 배치·상태만 확인할 때는 브라우저 요청을 가로채 생성 타입과 같은 형식으로 응답한다. 공개 SSR loader는 Node가 호출하므로 가로챌 수 없다. `tests/e2e/ssr-upstream.mjs` fixture나 실제 백엔드를 사용한다.

## 확인 절차

- Playwright CLI(`npx --yes --package @playwright/cli playwright-cli`)나 에이전트 내장 브라우저를 사용한다. 일회성 확인을 위해 `tests/e2e`에 spec을 추가하지 않는다.
- 산출물은 Git에서 제외된 `output/playwright/<작업명>/`에 둔다. CLI는 이 폴더에서 실행해 snapshot·console 로그가 다른 작업과 섞이지 않게 한다.
- 데스크톱 1280px, 모바일 390px에서 확인한다. 관리자 화면은 데스크톱을 기본으로 하고 공용 레이아웃을 바꿨을 때 모바일을 추가한다.
- snapshot으로 요소 ref를 얻고 이동·모달·탭 전환 뒤 다시 snapshot한다. 높이·폭·글자 크기·가로 넘침은 `eval`로 `getBoundingClientRect`와 `scrollWidth`를 읽어 수치로 남긴다.
- 변경한 요소의 배치와 크기, 모바일 가로 넘침, 로딩·오류·빈 상태, 폼 검증과 제출 차단, console error, 실패한 네트워크 요청을 확인한다.
- 조회 코드·비회원 token·전화번호가 URL·console·캡처 파일명에 남지 않는지 확인한다. 개인정보가 보이는 캡처는 커밋·PR에 첨부하지 않는다.
- 공개 페이지의 HTTP 상태와 head는 브라우저 대신 `curl`로 SSR 응답을 확인한다. 검색 노출 항목은 `happygallery-seo-ssr`를 따른다.

## 결과 기록

- 확인한 화면·폭·조작·결과, 캡처 경로, 확인하지 못한 항목과 이유(백엔드 미실행·외부 연동 등)를 남긴다. 캡처만으로 통과를 보고하지 않는다.
- 사용자 흐름이 바뀌어 회귀 보호가 필요하면 기존 E2E 시나리오에 단계를 추가하고, frontend 스킬의 E2E 선택 기준에 따라 실행한다.
