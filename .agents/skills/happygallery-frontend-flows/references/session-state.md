# 세션과 비동기 결과

## 회원·비회원 개인 상태

- 세션 snapshot은 로컬 버전·공유 `hg_customer_session_boundary` epoch·확인한 회원 ID로 구성한다.
- 로그인·가입·로그아웃·탈퇴·활성 세션의 401·회원 ID 변경에서 새 epoch를 발행한다. `storage`, `pageshow`, 탭이 다시 보이는 시점에 확인하고 `["me"]` 상태를 지운 뒤 `/me`를 다시 읽는다. 401이 아닌 `/me` 오류를 로그아웃으로 처리하지 않는다.
- 회원 소유 query key는 `["me", ...]` 아래 둔다. 계정 변경 시 query를 취소·제거하고 주문·기록 연결·비밀 Q&A 등 개인 데이터가 있는 로컬 상태도 초기화한다.
- 인증 상태에 의존하는 작업은 전체 snapshot을 시작 시 보관한다. 비회원 결제도 포함한다. 모든 await·외부 동작 뒤와 storage·Toss·toast·이동·query 무효화 전에 snapshot을 재확인한다.
- `CustomerSessionChangedError`는 오래된 결과 폐기를 뜻한다. retry·사용자 오류 표시를 하지 않고, 오래된 흐름이 저장한 임시 결제값을 정리한다.
- 비회원 기록·결제 복구값도 epoch와 회원 ID에 묶는다. 저장된 소유자와 예상 값이 모두 같을 때만 읽기·삭제를 적용한다. access token을 전달하는 router state에는 전체 snapshot을 넣고 도착 화면에서 검증한다.
- 세션 버전이 바뀌면 비회원 조회·주문·예약·예약 생성 화면의 개인 상태를 초기화한다.

## 관리자 상태

- token은 `useAdminKey()`를 통해 `sessionStorage`의 `hg_admin_token`에 저장한다. `AdminPage`가 `onAuthError`를 전달한다.
- `401 UNAUTHORIZED`만 token을 지우고 인증 화면으로 돌린다. `INVALID_CREDENTIALS`는 입력 오류로 표시한다.
- 관리자 신원이 바뀌면 관리자 query를 취소·제거한다. 로그아웃은 token·cache를 먼저 지운 후 서버 삭제를 시도한다.
