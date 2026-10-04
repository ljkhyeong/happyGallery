---
name: happygallery-review-flows
description: happyGallery의 상품·클래스 후기 작성·삭제·숨김·감사·신고·도움돼요·공식 답글·보존 정리와, 후기·상품·클래스·이벤트가 함께 쓰는 이미지 미디어의 업로드·정제·공개 판정·참조 보호·파일 삭제를 변경할 때 사용한다. 작성 근거가 되는 주문·예약 완료 처리는 order·booking 스킬, 상품 정보·Q&A는 product 스킬, 알림 전달은 notification 스킬, 화면은 frontend 스킬을 사용한다.
---

# happyGallery 후기·이미지 미디어

## 규칙

- `application/src/main/java/com/personal/happygallery/application/review/`와 `media/`, PRD-0001 3.5, ADR-0043·0044를 확인한다.
- 작성 근거는 세션 회원이 소유한 주문 품목 또는 예약이다. 완료 판단은 `OrderStatus`·`BookingStatus`의 `requireReviewable`을 재사용하고, 완료 상태 정의는 order·booking 스킬에서 바꾼다.
- 원천 점유 조건 `deleted_at IS NULL OR recreation_blocked`를 생성 열 `reserved_*`의 UNIQUE, `find*SourceReservation`, 작성 기회 조회에서 같게 유지한다. 사전 조회는 오류 구분용이고, 동시 작성은 `saveAndFlush`의 UNIQUE 위반을 `REVIEW_ALREADY_EXISTS`로 바꿔 막는다.
- 작성자 삭제는 이미지 참조를 먼저 제거한 뒤 `softDelete`로 본문·별점·답글·현재 숨김 정보를 지운다. tombstone을 재활성화하지 않고 재작성은 새 행으로 만든다.
- `recreation_blocked`는 첫 숨김에서 true가 되고 재공개·삭제로 지우지 않는다. 숨김 이력이 있는 tombstone은 원천을 계속 점유해 `REVIEW_RECREATION_BLOCKED`를 반환한다.
- 공개 목록·평균·분포·후기 수·공개 이미지 판정은 `PUBLISHED`이고 삭제되지 않은 후기만 쓴다. 분포는 필터와 무관한 전체 기준, 필터 결과 수는 별도 값이다. 공개 cursor는 정렬·별점 필터 일치까지 검증한다.
- 관리자 상태 변경은 잠근 후기에서 `expectedContentRevision`·`expectedVersion`을 모두 비교한다. 실제 전이일 때만 변경 전 증거 스냅샷 → 상태 변경 → append-only `ReviewModerationAction` → 알림 순서로 저장하고, 같은 상태 요청은 감사 행을 만들지 않는다.
- `contentRevision`은 회원 본문·별점 수정과 사진 추가·삭제에서만 올린다. 회원 수정은 `expectedContentRevision`, 공식 답글은 `expectedVersion`만 비교하며 두 값을 서로 대체하지 않는다.
- 신고는 자동 숨김하지 않는다. 신고·도움돼요는 `requirePublicInteraction`(공개·미삭제·타인 후기)을 통과해야 하고, 중복은 신고 UNIQUE 변환과 도움돼요 `ON DUPLICATE KEY` 멱등 처리로 막는다.
- 공개 응답은 모든 방문자에게 같은 값만 담고 작성자명은 `MaskingUtil`로 가린다. 회원별 반응·`ownedByMe`·`canInteract`는 `/api/v1/me/reviews/reactions`에서만 준다.
- 후기 요청(배송·픽업·수강 완료, 비회원 이력 가져오기)·숨김·재공개·최초 답글 알림은 `ReviewNotificationPublisher`로 트랜잭션 안에서 저장한다. 발송 직전 현재 상태는 `ReviewNotificationEligibility`가 다시 확인한다.
- 미결 신고 증거는 만료가 없고 결정 시 3년 보존을 시작한다. 보존 배치는 심사 이력 → 종결 신고 → 증거 → 30일 tombstone 순서와 건수 제한을 지키며, 자식 기록이 남은 후기는 FK `RESTRICT`로 보호한다.
- 회원 사진은 `UntrustedImageSanitizer`로 JPEG/PNG 실제 형식·픽셀 상한·EXIF 방향을 확인해 재인코딩한 바이트만 저장한다. 관리자 자산의 WebP 허용 경로를 회원 업로드에 쓰지 않고, 디코딩 포화는 대기 없이 429로 거절한다.
- 사진 업로드는 트랜잭션 밖에서 정제·파일 저장 후 짧은 트랜잭션으로 후기를 잠가 연결한다. 연결 실패는 `deleteIfUnreferenced`로 보상 삭제하고, 남은 파일은 7일 고아 정리가 회수한다.
- 참조 해제 트랜잭션은 행 삭제 후 `ImageMediaReferenceRemovedEvent`만 발행한다. 파일 삭제는 커밋 후 전역 미디어 잠금 아래 최신 참조와 백업 진행 여부를 재확인해 처리하고, 삭제 실패로 요청을 되돌리지 않는다.
- 이미지를 참조하는 테이블을 추가하면 `JdbcImageMediaReferenceReaderAdapter`의 전체 참조 UNION(`utf8mb4_bin`)과 필요한 공개 판정 조건에 넣는다. 참조 저장 전에는 `ImageMediaReferenceGuard.validateAssignment`로 같은 잠금에서 파일 존재를 확인한다.
- 공개 미디어 경로는 공개 참조가 아니면 404다. 숨김 후기 사진은 소유 회원·관리자 경로, 증거 사진은 관리자 evidence 경로로만 제공하고 모든 이미지 응답에 `no-store`를 붙인다.

## 검증

- 도메인 불변식·작성 가능 상태: `./gradlew :application:policyTest --tests "*ReviewPolicyTest"`. 이미지 참조 형식은 `*ImageReferencePolicyTest`를 추가한다.
- 작성·중복·삭제·숨김 감사·집계·cursor·신고·도움돼요·이미지 공개 경계·보존 배치: `./gradlew --no-daemon :application:useCaseTest --tests "*ReviewUseCaseIT"`.
- 사진 처리: `./gradlew :application:test --tests "*ReviewImageUploadServiceTest" --tests "*ReviewImageAttachmentServiceTest" --tests "*UntrustedImageSanitizerTest"`. 파일 삭제는 `*ImageMediaRetentionServiceTest`·`*ImageMediaReferenceRemovedListenerTest`·`*ReviewEvidenceRetentionServiceTest`, 보호 조회는 `*ReviewImageMediaServiceTest`·`*ReviewEvidenceMediaServiceTest`·`*PublicImageMediaServiceTest`를 선택한다.
- 참조 SQL: `./gradlew --no-daemon :application:useCaseTest --tests "*JdbcImageMediaReferenceReaderAdapterUseCaseIT" --tests "*ImagePublicReferenceUseCaseIT"`. 후기 알림 조건은 `*ReviewNotificationEligibilityUseCaseIT`, 증거·tombstone migration은 `--no-daemon :application:test --tests "*ReviewEvidenceMigrationTest"`.
- HTTP는 `api-contract`를 적용하고 `:adapter-in-web:restDocsTest`에서 `CustomerApiRestDocsTest`·`PublicApiRestDocsTest`·`AdminDashboardContentApiRestDocsTest`(관리자 후기·신고)·`ReviewImageMediaApiRestDocsTest`·`AdminReviewEvidenceMediaApiRestDocsTest`·`AdminAuthCatalogApiRestDocsTest`(관리자 미디어) 중 담당 클래스를 선택한다. 사진 업로드 처리율은 `:adapter-in-web:test --tests "*MeReviewControllerRateLimitTest"`.
