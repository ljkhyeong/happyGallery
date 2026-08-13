package com.personal.happygallery.application.review;

import com.personal.happygallery.adapter.out.persistence.admin.AdminUserRepository;
import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.booking.ClassRepository;
import com.personal.happygallery.adapter.out.persistence.booking.SlotRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderItemRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;
import com.personal.happygallery.adapter.out.persistence.review.ReviewRepository;
import com.personal.happygallery.adapter.out.persistence.review.ReviewImageRepository;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.review.port.in.ReviewUseCase;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewCreationStatus;
import com.personal.happygallery.domain.review.ReviewReportReason;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import com.personal.happygallery.domain.review.ReviewSort;
import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class ReviewUseCaseIT {

    @Autowired ReviewUseCase reviewUseCase;
    @Autowired ReviewRepository reviewRepository;
    @Autowired ReviewImageRepository reviewImageRepository;
    @Autowired ReviewImageAttachmentService imageAttachmentService;
    @Autowired ReviewEvidenceRetentionService evidenceRetentionService;
    @Autowired ReviewTombstoneRetentionService tombstoneRetentionService;
    @Autowired UserStorePort userStore;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired ClassRepository classRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired AdminUserRepository adminUserRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;

    private Long createdAdminId;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearNotificationLogs();
        cleanupSupport.clearReviewData();
        cleanupSupport.clearOrderData();
        cleanupSupport.clearBookingData();
        cleanupSupport.clearUsers();
        if (createdAdminId != null) {
            adminUserRepository.deleteById(createdAdminId);
            adminUserRepository.flush();
        }
    }

    @Test
    @DisplayName("공개 후기 집계는 숨김 후기를 제외하고 재공개하면 다시 포함한다")
    void publicSummaryExcludesHiddenReviewAndUsesCurrentTargetName() {
        User user = createUser("review-summary@example.com", "01074000001", "후기 회원");
        ProductOrderSource productSource = createProductOrderSource(user, true, "도자기 화병");
        ClassBookingSource classSource = createClassBookingSource(user, "향수 클래스");

        ReviewUseCase.ReviewItem productReview = reviewUseCase.createProductReview(
                user.getId(), productSource.orderItem().getId(), 5, "포장이 꼼꼼해요");
        ReviewUseCase.ReviewItem classReview = reviewUseCase.createClassReview(
                user.getId(), classSource.booking().getId(), 3, "차분하게 배웠어요");
        var firstMyPage = reviewUseCase.listMyReviews(user.getId(), null, 1);
        var secondMyPage = reviewUseCase.listMyReviews(
                user.getId(), firstMyPage.nextCursor(), 1);

        assertSoftly(softly -> {
            ReviewUseCase.PublicReviewPage productPage = reviewUseCase.listProductReviews(
                    productSource.product().getId(), null, 20);
            softly.assertThat(productPage.summary().reviewCount()).isEqualTo(1L);
            softly.assertThat(productPage.summary().averageRating()).isEqualTo(5.0);
            softly.assertThat(productPage.summary().histogram().rating5()).isEqualTo(1L);
            softly.assertThat(productPage.reviews().content())
                    .singleElement()
                    .satisfies(item -> softly.assertThat(item.authorName())
                            .isEqualTo("후기 회원"));
            ReviewUseCase.ReviewSummary classSummary = reviewUseCase.listClassReviews(
                    classSource.bookingClass().getId(), null, 20).summary();
            softly.assertThat(classSummary.reviewCount()).isEqualTo(1L);
            softly.assertThat(classSummary.averageRating()).isEqualTo(3.0);
            softly.assertThat(reviewUseCase.listAdminReviews(
                            null, null, null, 20).content())
                    .extracting(ReviewUseCase.ReviewItem::authorName)
                    .containsOnly("후기 회원");
            softly.assertThat(classReview.targetType()).isEqualTo(ReviewTargetType.CLASS);
            softly.assertThat(reviewUseCase.listMyBookingReviews(
                            user.getId(), classSource.booking().getId()))
                    .singleElement()
                    .satisfies(item -> softly.assertThat(item.id()).isEqualTo(classReview.id()));
            softly.assertThat(firstMyPage.hasMore()).isTrue();
            softly.assertThat(firstMyPage.content()).hasSize(1);
            softly.assertThat(secondMyPage.hasMore()).isFalse();
            softly.assertThat(secondMyPage.content()).hasSize(1);
        });

        AdminUser admin = adminUserRepository.saveAndFlush(
                new AdminUser("review-admin-summary", "password-hash"));
        createdAdminId = admin.getId();
        ReviewUseCase.ReviewItem hidden = reviewUseCase.updateStatus(
                productReview.id(),
                ReviewStatus.HIDDEN,
                "운영 정책 위반",
                productReview.contentRevision(),
                productReview.version(),
                admin.getId());
        ReviewUseCase.PublicReviewPage hiddenPage = reviewUseCase.listProductReviews(
                productSource.product().getId(), null, 20);

        assertSoftly(softly -> {
            softly.assertThat(hidden.status()).isEqualTo(ReviewStatus.HIDDEN);
            softly.assertThat(hidden.hiddenReason()).isEqualTo("운영 정책 위반");
            softly.assertThat(hidden.hiddenByAdminId()).isEqualTo(admin.getId());
            softly.assertThat(hiddenPage.summary().reviewCount()).isZero();
            softly.assertThat(hiddenPage.summary().averageRating()).isZero();
            softly.assertThat(hiddenPage.reviews().content()).isEmpty();
            softly.assertThat(reviewUseCase.listAdminReviews(
                            ReviewTargetType.PRODUCT,
                            ReviewStatus.HIDDEN,
                            null,
                            20).content())
                    .singleElement()
                    .satisfies(item -> softly.assertThat(item.id()).isEqualTo(productReview.id()));
            softly.assertThat(reviewUseCase.listAdminReviews(
                            null, null, null, 20).content())
                    .extracting(ReviewUseCase.ReviewItem::id)
                    .containsExactlyInAnyOrder(productReview.id(), classReview.id());
            softly.assertThat(reviewUseCase.listMyReviews(user.getId(), null, 20).content())
                    .extracting(ReviewUseCase.ReviewItem::id)
                    .contains(productReview.id(), classReview.id());
        });

        productSource.product().updateDetails(
                "도자기 화병 새 이름", null, 30_000L, null, null);
        productRepository.saveAndFlush(productSource.product());
        ReviewUseCase.ReviewItem republished = reviewUseCase.updateStatus(
                productReview.id(),
                ReviewStatus.PUBLISHED,
                null,
                hidden.contentRevision(),
                hidden.version(),
                admin.getId());

        assertSoftly(softly -> {
            softly.assertThat(republished.hiddenReason()).isNull();
            softly.assertThat(republished.hiddenAt()).isNull();
            softly.assertThat(republished.hiddenByAdminId()).isNull();
            softly.assertThat(reviewUseCase.listProductReviews(
                            productSource.product().getId(), null, 20).summary().reviewCount())
                    .isEqualTo(1L);
            softly.assertThat(reviewUseCase.listMyOrderReviews(
                            user.getId(), productSource.order().getId()))
                    .singleElement()
                    .satisfies(item -> softly.assertThat(item.targetName())
                            .isEqualTo("도자기 화병 새 이름"));
        });
    }

    @Test
    @DisplayName("같은 주문 품목에 대한 동시 후기 작성은 데이터베이스 유일 제약으로 한 건만 성공한다")
    void concurrentProductReviewCreationAllowsOneReview() throws Exception {
        User user = createUser("review-concurrency@example.com", "01074000002", "동시 후기 회원");
        ProductOrderSource source = createProductOrderSource(user, true, "동시 후기 상품");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ConcurrentResult result;
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Throwable> first = executor.submit(() -> createReview(
                    ready, start, user.getId(), source.orderItem().getId(), "첫 번째 후기"));
            Future<Throwable> second = executor.submit(() -> createReview(
                    ready, start, user.getId(), source.orderItem().getId(), "두 번째 후기"));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            result = new ConcurrentResult(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS));
        }

        Throwable failure = Stream.of(result.firstFailure(), result.secondFailure())
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(Stream.of(result.firstFailure(), result.secondFailure())
                            .filter(Objects::isNull)
                            .count())
                    .isEqualTo(1L);
            softly.assertThat(failure)
                    .isInstanceOfSatisfying(
                            HappyGalleryException.class,
                            exception -> softly.assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS));
            softly.assertThat(reviewRepository.count()).isEqualTo(1L);
        });
    }

    @Test
    @DisplayName("미완료 원천과 타인 소유 원천에는 후기를 작성하거나 조회할 수 없다")
    void reviewSourceRequiresCompletionAndOwnership() {
        User owner = createUser("review-owner@example.com", "01074000003", "후기 소유자");
        User other = createUser("review-other@example.com", "01074000004", "다른 회원");
        ProductOrderSource pending = createProductOrderSource(owner, false, "미완료 상품");

        assertThatThrownBy(() -> reviewUseCase.createProductReview(
                owner.getId(), pending.orderItem().getId(), 4, "아직 완료 전"))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED));
        assertThatThrownBy(() -> reviewUseCase.createProductReview(
                other.getId(), pending.orderItem().getId(), 4, "타인 후기"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> reviewUseCase.listMyOrderReviews(
                other.getId(), pending.order().getId()))
                .isInstanceOf(NotFoundException.class);
        assertThat(reviewUseCase.listMyOrderReviews(
                owner.getId(), pending.order().getId())).isEmpty();
    }

    @Test
    @DisplayName("후기 원천과 다른 상품 또는 클래스를 조합하면 데이터베이스가 거절한다")
    void reviewSourceAndTargetMustMatch() {
        User user = createUser("review-source-pair@example.com", "01074000005", "원천 검증 회원");
        ProductOrderSource productSource = createProductOrderSource(user, true, "구매한 상품");
        Product unrelatedProduct = productRepository.saveAndFlush(
                new Product("다른 상품", ProductType.READY_STOCK, 20_000L));
        ClassBookingSource classSource = createClassBookingSource(user, "수강한 클래스");
        BookingClass unrelatedClass = classRepository.saveAndFlush(
                new BookingClass("다른 클래스", "CRAFT", 90, 40_000L, 20));
        LocalDateTime now = LocalDateTime.now(clock);

        assertThatThrownBy(() -> reviewRepository.saveAndFlush(Review.forProduct(
                user.getId(),
                productSource.orderItem().getId(),
                unrelatedProduct.getId(),
                5,
                "잘못 연결된 상품 후기",
                now)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> reviewRepository.saveAndFlush(Review.forClass(
                user.getId(),
                classSource.booking().getId(),
                unrelatedClass.getId(),
                5,
                "잘못 연결된 클래스 후기",
                now)))
                .isInstanceOf(DataIntegrityViolationException.class);

        reviewUseCase.createClassReview(
                user.getId(), classSource.booking().getId(), 5, "정상 클래스 후기");
        assertThatThrownBy(() -> reviewUseCase.createClassReview(
                user.getId(), classSource.booking().getId(), 4, "중복 클래스 후기"))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("일반 후기 삭제는 내용을 비식별화하고 같은 거래의 재작성을 허용한다")
    void ownerCanUpdateAndSoftDeleteReview() {
        User owner = createUser("review-edit-owner@example.com", "01074000005", "수정 회원");
        User other = createUser("review-edit-other@example.com", "01074000006", "다른 수정 회원");
        ProductOrderSource source = createProductOrderSource(owner, true, "수정 후기 상품");
        ReviewUseCase.ReviewItem created = reviewUseCase.createProductReview(
                owner.getId(), source.orderItem().getId(), 3, "수정 전 후기");

        ReviewUseCase.ReviewItem updated = reviewUseCase.updateReview(
                owner.getId(), created.id(), created.contentRevision(), 4, "수정한 후기");

        assertSoftly(softly -> {
            softly.assertThat(updated.rating()).isEqualTo(4);
            softly.assertThat(updated.content()).isEqualTo("수정한 후기");
            softly.assertThat(updated.updatedAt()).isAfterOrEqualTo(updated.createdAt());
            softly.assertThat(updated.edited()).isTrue();
        });
        assertThatThrownBy(() -> reviewUseCase.updateReview(
                other.getId(), created.id(), created.contentRevision(), 5, "타인 수정"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> reviewUseCase.deleteReview(other.getId(), created.id()))
                .isInstanceOf(NotFoundException.class);

        reviewUseCase.deleteReview(owner.getId(), created.id());

        Review tombstone = reviewRepository.findById(created.id()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(tombstone.getDeletedAt()).isNotNull();
            softly.assertThat(tombstone.getRating()).isNull();
            softly.assertThat(tombstone.getContent()).isNull();
            softly.assertThat(tombstone.isRecreationBlocked()).isFalse();
            softly.assertThat(reviewUseCase.listProductReviews(
                            source.product().getId(), null, 20).summary().reviewCount())
                    .isZero();
            softly.assertThat(reviewUseCase.getProductReviewCreationState(
                            owner.getId(), source.orderItem().getId()).status())
                    .isEqualTo(ReviewCreationStatus.AVAILABLE);
        });

        ReviewUseCase.ReviewItem recreated = reviewUseCase.createProductReview(
                owner.getId(), source.orderItem().getId(), 5, "다시 작성한 후기");
        assertThat(recreated.id()).isNotEqualTo(created.id());
    }

    @Test
    @DisplayName("30일 지난 일반 후기 tombstone만 삭제하고 활성·신고·차단 후기는 보존한다")
    void reviewTombstoneRetentionDeletesOnlyUnblockedEvidenceFreeRows() {
        User owner = createUser(
                "review-retention-owner@example.com", "01074000028", "후기 보존 회원");
        User reporter = createUser(
                "review-retention-reporter@example.com", "01074000029", "후기 보존 신고자");
        ProductOrderSource ordinarySource = createProductOrderSource(
                owner, true, "일반 삭제 후기 상품");
        ProductOrderSource recentSource = createProductOrderSource(
                owner, true, "최근 삭제 후기 상품");
        ProductOrderSource reportedSource = createProductOrderSource(
                owner, true, "신고 보존 후기 상품");
        ProductOrderSource blockedSource = createProductOrderSource(
                owner, true, "재작성 차단 후기 상품");

        ReviewUseCase.ReviewItem ordinary = reviewUseCase.createProductReview(
                owner.getId(), ordinarySource.orderItem().getId(), 4, "정리할 후기");
        reviewUseCase.deleteReview(owner.getId(), ordinary.id());
        ReviewUseCase.ReviewItem recreated = reviewUseCase.createProductReview(
                owner.getId(), ordinarySource.orderItem().getId(), 5, "다시 작성한 활성 후기");

        ReviewUseCase.ReviewItem recent = reviewUseCase.createProductReview(
                owner.getId(), recentSource.orderItem().getId(), 4, "아직 보존할 후기");
        reviewUseCase.deleteReview(owner.getId(), recent.id());

        ReviewUseCase.ReviewItem reported = reviewUseCase.createProductReview(
                owner.getId(), reportedSource.orderItem().getId(), 2, "신고 증거 후기");
        reviewUseCase.createReport(
                reporter.getId(), reported.id(), ReviewReportReason.OTHER, "분쟁 확인 중");
        reviewUseCase.deleteReview(owner.getId(), reported.id());

        ReviewUseCase.ReviewItem blocked = reviewUseCase.createProductReview(
                owner.getId(), blockedSource.orderItem().getId(), 1, "숨김 조치 후기");
        AdminUser admin = adminUserRepository.saveAndFlush(
                new AdminUser("review-retention-admin", "password-hash"));
        createdAdminId = admin.getId();
        ReviewUseCase.ReviewItem hidden = reviewUseCase.updateStatus(
                blocked.id(),
                ReviewStatus.HIDDEN,
                "운영 정책 위반",
                blocked.contentRevision(),
                blocked.version(),
                admin.getId());
        reviewUseCase.deleteReview(owner.getId(), hidden.id());

        LocalDateTime cutoff = LocalDateTime.now(clock)
                .minus(ReviewTombstoneRetentionService.RETENTION);
        jdbcTemplate.update(
                "UPDATE reviews SET deleted_at = ? WHERE id IN (?, ?, ?)",
                cutoff,
                ordinary.id(),
                reported.id(),
                blocked.id());
        jdbcTemplate.update(
                "UPDATE reviews SET deleted_at = ? WHERE id = ?",
                cutoff.plusNanos(1_000),
                recent.id());
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM reviews WHERE id = ?", reported.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM reviews WHERE id = ?", blocked.id()))
                .isInstanceOf(DataIntegrityViolationException.class);

        int deleted = tombstoneRetentionService.deleteBatchBefore(cutoff, 100);

        assertSoftly(softly -> {
            softly.assertThat(deleted).isEqualTo(1);
            softly.assertThat(reviewRepository.findById(ordinary.id())).isEmpty();
            softly.assertThat(reviewRepository.findById(recreated.id())).isPresent();
            softly.assertThat(reviewRepository.findById(recent.id())).isPresent();
            softly.assertThat(reviewRepository.findById(reported.id())).isPresent();
            softly.assertThat(reviewRepository.findById(blocked.id()))
                    .get()
                    .extracting(Review::isRecreationBlocked)
                    .isEqualTo(true);
        });
    }

    @Test
    @DisplayName("회원은 오래된 콘텐츠 revision으로 후기를 덮어쓸 수 없고 최신 본문을 유지한다")
    void staleMemberUpdateKeepsLatestContent() {
        User owner = createUser("review-stale-edit@example.com", "01074000018", "수정 충돌 회원");
        ProductOrderSource source = createProductOrderSource(owner, true, "수정 충돌 상품");
        ReviewUseCase.ReviewItem created = reviewUseCase.createProductReview(
                owner.getId(), source.orderItem().getId(), 3, "최초 후기");

        ReviewUseCase.ReviewItem latest = reviewUseCase.updateReview(
                owner.getId(), created.id(), created.contentRevision(), 4, "최신 후기");

        assertThatThrownBy(() -> reviewUseCase.updateReview(
                owner.getId(), created.id(), created.contentRevision(), 1, "오래된 화면의 후기"))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.REVIEW_CONTENT_CHANGED));

        ReviewUseCase.ReviewItem current = reviewUseCase.getAdminReview(created.id());
        assertSoftly(softly -> {
            softly.assertThat(current.rating()).isEqualTo(4);
            softly.assertThat(current.content()).isEqualTo("최신 후기");
            softly.assertThat(current.contentRevision()).isEqualTo(latest.contentRevision());
        });
    }

    @Test
    @DisplayName("숨김 이력이 있는 후기 tombstone은 재공개 뒤에도 재작성을 차단하고 실제 전이만 감사 기록한다")
    void moderatedReviewTombstoneBlocksRecreationAndKeepsTransitionHistory() {
        User owner = createUser("review-block-owner@example.com", "01074000007", "차단 후기 회원");
        ProductOrderSource source = createProductOrderSource(owner, true, "차단 후기 상품");
        AdminUser admin = adminUserRepository.saveAndFlush(
                new AdminUser("review-block-admin", "password-hash"));
        createdAdminId = admin.getId();
        ReviewUseCase.ReviewItem review = reviewUseCase.createProductReview(
                owner.getId(), source.orderItem().getId(), 1, "운영 검토 대상 후기");

        ReviewUseCase.ReviewItem hidden = reviewUseCase.updateStatus(
                review.id(),
                ReviewStatus.HIDDEN,
                "운영 정책 위반",
                review.contentRevision(),
                review.version(),
                admin.getId());
        ReviewUseCase.ReviewItem duplicateHidden = reviewUseCase.updateStatus(
                review.id(),
                ReviewStatus.HIDDEN,
                "중복 요청",
                hidden.contentRevision(),
                hidden.version(),
                admin.getId());
        ReviewUseCase.ReviewItem published = reviewUseCase.updateStatus(
                review.id(),
                ReviewStatus.PUBLISHED,
                null,
                duplicateHidden.contentRevision(),
                duplicateHidden.version(),
                admin.getId());
        reviewUseCase.updateStatus(
                review.id(),
                ReviewStatus.PUBLISHED,
                null,
                published.contentRevision(),
                published.version(),
                admin.getId());
        reviewUseCase.deleteReview(owner.getId(), review.id());

        Review tombstone = reviewRepository.findById(review.id()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(tombstone.isDeleted()).isTrue();
            softly.assertThat(tombstone.isRecreationBlocked()).isTrue();
            softly.assertThat(reviewUseCase.listModerationActions(review.id()))
                    .extracting(ReviewUseCase.ModerationActionItem::action)
                    .containsExactly(
                            com.personal.happygallery.domain.review.ReviewModerationActionType.HIDE,
                            com.personal.happygallery.domain.review.ReviewModerationActionType.REPUBLISH);
            softly.assertThat(reviewUseCase.listMyReviews(owner.getId(), null, 20).content())
                    .isEmpty();
            softly.assertThat(reviewUseCase.listAdminReviews(null, null, null, 20).content())
                    .isEmpty();
            softly.assertThat(reviewUseCase.getProductReviewCreationState(
                            owner.getId(), source.orderItem().getId()).status())
                    .isEqualTo(ReviewCreationStatus.RECREATION_BLOCKED);
        });
        assertThatThrownBy(() -> reviewUseCase.createProductReview(
                owner.getId(), source.orderItem().getId(), 5, "재작성 우회"))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.REVIEW_RECREATION_BLOCKED));
    }

    @Test
    @DisplayName("관리자는 확인한 revision 이후 내용이 바뀐 후기를 상태 변경할 수 없다")
    void staleModerationRevisionLeavesNoActionEvidenceOrNotification() {
        User owner = createUser("review-stale-owner@example.com", "01074000017", "수정 후기 회원");
        ProductOrderSource source = createProductOrderSource(owner, true, "수정 감지 상품");
        ReviewUseCase.ReviewItem review = reviewUseCase.createProductReview(
                owner.getId(), source.orderItem().getId(), 3, "관리자가 확인한 본문");
        AdminUser admin = adminUserRepository.saveAndFlush(
                new AdminUser("review-stale-admin", "password-hash"));
        createdAdminId = admin.getId();

        ReviewUseCase.ReviewItem edited = reviewUseCase.updateReview(
                owner.getId(), review.id(), review.contentRevision(), 5, "작성자가 바꾼 본문");
        long actionCount = tableCount("review_moderation_actions");
        long evidenceCount = tableCount("review_evidence_snapshots");
        long notificationCount = tableCount("notification_outbox");

        assertThatThrownBy(() -> reviewUseCase.updateStatus(
                review.id(),
                ReviewStatus.HIDDEN,
                "이전 화면에서 숨김",
                review.contentRevision(),
                review.version(),
                admin.getId()))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.REVIEW_CONTENT_CHANGED));

        ReviewUseCase.ReviewItem current = reviewUseCase.getAdminReview(review.id());
        assertSoftly(softly -> {
            softly.assertThat(edited.contentRevision()).isEqualTo(review.contentRevision() + 1L);
            softly.assertThat(current.status()).isEqualTo(ReviewStatus.PUBLISHED);
            softly.assertThat(current.content()).isEqualTo("작성자가 바꾼 본문");
            softly.assertThat(current.contentRevision()).isEqualTo(edited.contentRevision());
            softly.assertThat(tableCount("review_moderation_actions")).isEqualTo(actionCount);
            softly.assertThat(tableCount("review_evidence_snapshots")).isEqualTo(evidenceCount);
            softly.assertThat(tableCount("notification_outbox")).isEqualTo(notificationCount);
        });
    }

    @Test
    @DisplayName("관리자 상태가 왕복해도 오래된 version 명령은 거부하고 감사 액션을 추가하지 않는다")
    void staleAdminVersionRejectsStatusAbaWithoutAction() {
        User owner = createUser("review-aba-owner@example.com", "01074000019", "상태 충돌 회원");
        ProductOrderSource source = createProductOrderSource(owner, true, "상태 충돌 상품");
        ReviewUseCase.ReviewItem original = reviewUseCase.createProductReview(
                owner.getId(), source.orderItem().getId(), 2, "상태 왕복 후기");
        AdminUser admin = adminUserRepository.saveAndFlush(
                new AdminUser("review-aba-admin", "password-hash"));
        createdAdminId = admin.getId();

        ReviewUseCase.ReviewItem hidden = reviewUseCase.updateStatus(
                original.id(),
                ReviewStatus.HIDDEN,
                "운영 확인",
                original.contentRevision(),
                original.version(),
                admin.getId());
        ReviewUseCase.ReviewItem republished = reviewUseCase.updateStatus(
                original.id(),
                ReviewStatus.PUBLISHED,
                null,
                hidden.contentRevision(),
                hidden.version(),
                admin.getId());
        int actionCount = reviewUseCase.listModerationActions(original.id()).size();

        assertThatThrownBy(() -> reviewUseCase.updateStatus(
                original.id(),
                ReviewStatus.HIDDEN,
                "오래된 화면에서 다시 숨김",
                original.contentRevision(),
                original.version(),
                admin.getId()))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CONFLICT));

        ReviewUseCase.ReviewItem current = reviewUseCase.getAdminReview(original.id());
        assertSoftly(softly -> {
            softly.assertThat(current.status()).isEqualTo(ReviewStatus.PUBLISHED);
            softly.assertThat(current.version()).isEqualTo(republished.version());
            softly.assertThat(reviewUseCase.listModerationActions(original.id()))
                    .hasSize(actionCount);
        });
    }

    @Test
    @DisplayName("작성 가능한 후기 목록은 완료 거래만 포함하고 일반 삭제 뒤 다시 노출한다")
    void reviewOpportunitiesTrackActiveAndDeletedReviews() {
        User owner = createUser("review-opportunity@example.com", "01074000008", "후기 기회 회원");
        ProductOrderSource productSource = createProductOrderSource(owner, true, "후기 기회 상품");
        ClassBookingSource classSource = createClassBookingSource(owner, "후기 기회 클래스");
        LocalDateTime productCompletedAt = LocalDateTime.of(2026, 4, 29, 15, 30);
        LocalDateTime classCompletedAt = LocalDateTime.of(2026, 4, 30, 11, 20);
        jdbcTemplate.update("""
                INSERT INTO order_approvals
                    (order_id, decision, decided_at)
                VALUES (?, 'PICKUP_COMPLETE', ?)
                """, productSource.order().getId(), productCompletedAt);
        jdbcTemplate.update("""
                INSERT INTO booking_history
                    (booking_id, action, actor, created_at)
                VALUES (?, 'COMPLETED', 'ADMIN', ?)
                """, classSource.booking().getId(), classCompletedAt);

        List<ReviewUseCase.ReviewOpportunity> initial =
                reviewUseCase.listMyReviewOpportunities(owner.getId(), null, 20).content();

        assertThat(initial)
                .extracting(ReviewUseCase.ReviewOpportunity::targetType)
                .containsExactlyInAnyOrder(ReviewTargetType.PRODUCT, ReviewTargetType.CLASS);
        assertThat(initial)
                .filteredOn(opportunity -> opportunity.targetType() == ReviewTargetType.PRODUCT)
                .singleElement()
                .extracting(ReviewUseCase.ReviewOpportunity::completedAt)
                .isEqualTo(productCompletedAt);
        assertThat(initial)
                .filteredOn(opportunity -> opportunity.targetType() == ReviewTargetType.CLASS)
                .singleElement()
                .extracting(ReviewUseCase.ReviewOpportunity::completedAt)
                .isEqualTo(classCompletedAt);

        ReviewUseCase.ReviewItem productReview = reviewUseCase.createProductReview(
                owner.getId(), productSource.orderItem().getId(), 5, "작성한 후기");
        assertThat(reviewUseCase.listMyReviewOpportunities(owner.getId(), null, 20).content())
                .filteredOn(opportunity -> opportunity.targetType() == ReviewTargetType.PRODUCT)
                .extracting(ReviewUseCase.ReviewOpportunity::sourceId)
                .doesNotContain(productSource.orderItem().getId());

        reviewUseCase.deleteReview(owner.getId(), productReview.id());
        List<ReviewUseCase.ReviewOpportunity> afterDelete =
                reviewUseCase.listMyReviewOpportunities(owner.getId(), null, 20).content();
        assertThat(afterDelete)
                .filteredOn(opportunity -> opportunity.targetType() == ReviewTargetType.PRODUCT)
                .extracting(ReviewUseCase.ReviewOpportunity::sourceId)
                .containsExactly(productSource.orderItem().getId());
        assertThat(afterDelete)
                .filteredOn(opportunity -> opportunity.targetType() == ReviewTargetType.CLASS)
                .extracting(ReviewUseCase.ReviewOpportunity::sourceId)
                .containsExactly(classSource.booking().getId());
    }

    @Test
    @DisplayName("같은 완료 시각의 후기 작성 기회는 대상 유형과 원본 ID 순서를 유지하며 커서로 빠짐없이 이어진다")
    void reviewOpportunityCursorKeepsCompositeOrderAcrossPages() {
        User owner = createUser(
                "review-opportunity-cursor@example.com",
                "01074000022",
                "후기 기회 커서 회원");
        LocalDateTime completedAt = LocalDateTime.of(2026, 5, 1, 12, 0);
        List<ProductOrderSource> productSources = new ArrayList<>();
        List<ClassBookingSource> classSources = new ArrayList<>();

        for (int sequence = 0; sequence < 10; sequence++) {
            ProductOrderSource source = createProductOrderSource(
                    owner, true, "후기 기회 커서 상품 " + sequence);
            productSources.add(source);
            jdbcTemplate.update("""
                    INSERT INTO order_approvals
                        (order_id, decision, decided_at)
                    VALUES (?, 'PICKUP_COMPLETE', ?)
                    """, source.order().getId(), completedAt);

            ClassBookingSource classSource = createClassBookingSource(
                    owner, "후기 기회 커서 클래스 " + sequence);
            classSources.add(classSource);
            jdbcTemplate.update("""
                    INSERT INTO booking_history
                        (booking_id, action, actor, created_at)
                    VALUES (?, 'COMPLETED', 'ADMIN', ?)
                    """, classSource.booking().getId(), completedAt);
        }

        List<String> expectedOrder = Stream.concat(
                        productSources.stream()
                                .sorted((left, right) -> right.orderItem().getId()
                                        .compareTo(left.orderItem().getId()))
                                .map(source -> ReviewTargetType.PRODUCT.name()
                                        + ":" + source.orderItem().getId()),
                        classSources.stream()
                                .sorted((left, right) -> right.booking().getId()
                                        .compareTo(left.booking().getId()))
                                .map(source -> ReviewTargetType.CLASS.name()
                                        + ":" + source.booking().getId()))
                .toList();
        List<ReviewUseCase.ReviewOpportunity> collected = new ArrayList<>();
        List<List<String>> pageKeys = new ArrayList<>();
        String cursor = null;

        for (int pageNumber = 0; pageNumber < 8; pageNumber++) {
            var page = reviewUseCase.listMyReviewOpportunities(owner.getId(), cursor, 3);
            collected.addAll(page.content());
            pageKeys.add(page.content().stream()
                    .map(opportunity -> opportunity.targetType().name()
                            + ":" + opportunity.sourceId())
                    .toList());
            if (!page.hasMore()) {
                break;
            }
            cursor = page.nextCursor();
        }

        List<String> actualOrder = collected.stream()
                .map(opportunity -> opportunity.targetType().name()
                        + ":" + opportunity.sourceId())
                .toList();
        assertSoftly(softly -> {
            softly.assertThat(pageKeys.getFirst())
                    .containsExactlyElementsOf(expectedOrder.subList(0, 3));
            softly.assertThat(pageKeys.get(1))
                    .containsExactlyElementsOf(expectedOrder.subList(3, 6));
            softly.assertThat(actualOrder).doesNotHaveDuplicates();
            softly.assertThat(actualOrder).containsExactlyElementsOf(expectedOrder);
            softly.assertThat(collected)
                    .extracting(ReviewUseCase.ReviewOpportunity::completedAt)
                    .containsOnly(completedAt);
        });
    }

    @Test
    @DisplayName("공개 후기 집계는 전체 별점 분포를 유지하고 필터와 정렬 커서를 안정적으로 적용한다")
    void publicReviewHistogramFilterAndRatingSortUseStableCursor() {
        Product product = productRepository.saveAndFlush(
                new Product("별점 정렬 상품", ProductType.READY_STOCK, 30_000L));
        User lowUser = createUser("review-rating-low@example.com", "01074000009", "낮은 별점 회원");
        User highUser = createUser("review-rating-high@example.com", "01074000010", "높은 별점 회원");
        User middleUser = createUser("review-rating-middle@example.com", "01074000011", "중간 별점 회원");
        ProductOrderSource low = createProductOrderSource(lowUser, true, product);
        ProductOrderSource high = createProductOrderSource(highUser, true, product);
        ProductOrderSource middle = createProductOrderSource(middleUser, true, product);
        reviewUseCase.createProductReview(lowUser.getId(), low.orderItem().getId(), 2, "별점 2점");
        reviewUseCase.createProductReview(highUser.getId(), high.orderItem().getId(), 5, "별점 5점");
        reviewUseCase.createProductReview(middleUser.getId(), middle.orderItem().getId(), 3, "별점 3점");

        ReviewUseCase.PublicReviewPage first = reviewUseCase.listProductReviews(
                product.getId(), null, ReviewSort.RATING_HIGH, null, 2);
        ReviewUseCase.PublicReviewPage second = reviewUseCase.listProductReviews(
                product.getId(), null, ReviewSort.RATING_HIGH, first.reviews().nextCursor(), 2);
        ReviewUseCase.PublicReviewPage filtered = reviewUseCase.listProductReviews(
                product.getId(), 3, ReviewSort.LATEST, null, 20);

        assertSoftly(softly -> {
            softly.assertThat(first.reviews().content())
                    .extracting(ReviewUseCase.ReviewItem::rating)
                    .containsExactly(5, 3);
            softly.assertThat(second.reviews().content())
                    .extracting(ReviewUseCase.ReviewItem::rating)
                    .containsExactly(2);
            softly.assertThat(filtered.filteredCount()).isEqualTo(1L);
            softly.assertThat(filtered.reviews().content())
                    .extracting(ReviewUseCase.ReviewItem::rating)
                    .containsExactly(3);
            softly.assertThat(filtered.summary().reviewCount()).isEqualTo(3L);
            softly.assertThat(filtered.summary().histogram())
                    .isEqualTo(new ReviewUseCase.RatingHistogram(0L, 1L, 1L, 0L, 1L));
        });
        assertThatThrownBy(() -> reviewUseCase.listProductReviews(
                product.getId(), 3, ReviewSort.RATING_HIGH, first.reviews().nextCursor(), 20))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("상품과 클래스 공개 후기는 정렬별 커서 순서를 끝까지 유지한다")
    void publicReviewSortQueriesPreserveProductAndClassCursorOrder() {
        Product product = productRepository.saveAndFlush(
                new Product("공개 정렬 상품", ProductType.READY_STOCK, 30_000L));
        BookingClass bookingClass = classRepository.saveAndFlush(
                new BookingClass("공개 정렬 클래스", "PERFUME", 120, 50_000L, 30));
        User lowUser = createUser("review-sort-low@example.com", "01074000024", "낮은 별점 회원");
        User highUser = createUser("review-sort-high@example.com", "01074000025", "높은 별점 회원");
        User middleUser = createUser(
                "review-sort-middle@example.com", "01074000026", "중간 별점 회원");

        ProductOrderSource lowProductSource = createProductOrderSource(lowUser, true, product);
        ProductOrderSource highProductSource = createProductOrderSource(highUser, true, product);
        ProductOrderSource middleProductSource = createProductOrderSource(
                middleUser, true, product);
        ClassBookingSource lowClassSource = createClassBookingSource(lowUser, bookingClass, 0);
        ClassBookingSource highClassSource = createClassBookingSource(highUser, bookingClass, 1);
        ClassBookingSource middleClassSource = createClassBookingSource(
                middleUser, bookingClass, 2);

        ReviewUseCase.ReviewItem lowProduct = reviewUseCase.createProductReview(
                lowUser.getId(), lowProductSource.orderItem().getId(), 2, "상품 별점 2점");
        ReviewUseCase.ReviewItem highProduct = reviewUseCase.createProductReview(
                highUser.getId(), highProductSource.orderItem().getId(), 5, "상품 별점 5점");
        ReviewUseCase.ReviewItem middleProduct = reviewUseCase.createProductReview(
                middleUser.getId(), middleProductSource.orderItem().getId(), 3, "상품 별점 3점");
        ReviewUseCase.ReviewItem lowClass = reviewUseCase.createClassReview(
                lowUser.getId(), lowClassSource.booking().getId(), 2, "클래스 별점 2점");
        ReviewUseCase.ReviewItem highClass = reviewUseCase.createClassReview(
                highUser.getId(), highClassSource.booking().getId(), 5, "클래스 별점 5점");
        ReviewUseCase.ReviewItem middleClass = reviewUseCase.createClassReview(
                middleUser.getId(), middleClassSource.booking().getId(), 3, "클래스 별점 3점");

        assertSoftly(softly -> {
            softly.assertThat(collectPublicReviewIds(
                            ReviewTargetType.PRODUCT, product.getId(), ReviewSort.LATEST))
                    .containsExactly(middleProduct.id(), highProduct.id(), lowProduct.id());
            softly.assertThat(collectPublicReviewIds(
                            ReviewTargetType.PRODUCT, product.getId(), ReviewSort.RATING_HIGH))
                    .containsExactly(highProduct.id(), middleProduct.id(), lowProduct.id());
            softly.assertThat(collectPublicReviewIds(
                            ReviewTargetType.PRODUCT, product.getId(), ReviewSort.RATING_LOW))
                    .containsExactly(lowProduct.id(), middleProduct.id(), highProduct.id());
            softly.assertThat(collectPublicReviewIds(
                            ReviewTargetType.CLASS, bookingClass.getId(), ReviewSort.LATEST))
                    .containsExactly(middleClass.id(), highClass.id(), lowClass.id());
            softly.assertThat(collectPublicReviewIds(
                            ReviewTargetType.CLASS, bookingClass.getId(), ReviewSort.RATING_HIGH))
                    .containsExactly(highClass.id(), middleClass.id(), lowClass.id());
            softly.assertThat(collectPublicReviewIds(
                            ReviewTargetType.CLASS, bookingClass.getId(), ReviewSort.RATING_LOW))
                    .containsExactly(lowClass.id(), middleClass.id(), highClass.id());
        });
    }

    @Test
    @DisplayName("신고는 원문 증거를 보존하고 도움돼요는 회원별로 멱등 처리한다")
    void reportSnapshotAndHelpfulVotePoliciesAreEnforced() {
        User owner = createUser("review-reaction-owner@example.com", "01074000012", "반응 후기 작성자");
        User actor = createUser("review-reaction-actor@example.com", "01074000013", "반응 회원");
        ProductOrderSource source = createProductOrderSource(owner, true, "신고 도움 상품");
        ReviewUseCase.ReviewItem review = reviewUseCase.createProductReview(
                owner.getId(), source.orderItem().getId(), 2, "신고 시점 원문");

        ReviewUseCase.HelpfulResult firstHelpful = reviewUseCase.markHelpful(actor.getId(), review.id());
        ReviewUseCase.HelpfulResult duplicateHelpful = reviewUseCase.markHelpful(actor.getId(), review.id());
        ReviewUseCase.ReviewReportItem report = reviewUseCase.createReport(
                actor.getId(), review.id(), ReviewReportReason.FALSE_INFORMATION, "사실과 달라요");
        reviewUseCase.updateReview(
                owner.getId(),
                review.id(),
                review.contentRevision(),
                5,
                "신고 뒤 수정된 본문");

        assertSoftly(softly -> {
            softly.assertThat(firstHelpful.helpfulCount()).isEqualTo(1L);
            softly.assertThat(duplicateHelpful.helpfulCount()).isEqualTo(1L);
            softly.assertThat(report.evidence().rating()).isEqualTo(2);
            softly.assertThat(report.evidence().content()).isEqualTo("신고 시점 원문");
            softly.assertThat(report.evidence().imagesComplete()).isTrue();
            softly.assertThat(reviewUseCase.listMyReviewReactions(
                            actor.getId(), List.of(review.id())))
                    .singleElement()
                    .satisfies(reaction -> {
                        softly.assertThat(reaction.helpfulByMe()).isTrue();
                        softly.assertThat(reaction.reportedByMe()).isTrue();
                    });
        });
        assertThatThrownBy(() -> reviewUseCase.markHelpful(owner.getId(), review.id()))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.REVIEW_SELF_INTERACTION_NOT_ALLOWED));
        assertThatThrownBy(() -> reviewUseCase.createReport(
                actor.getId(), review.id(), ReviewReportReason.SPAM, null))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.REVIEW_REPORT_ALREADY_EXISTS));

        ReviewUseCase.HelpfulResult removed = reviewUseCase.unmarkHelpful(actor.getId(), review.id());
        ReviewUseCase.HelpfulResult removedAgain = reviewUseCase.unmarkHelpful(actor.getId(), review.id());
        AdminUser admin = adminUserRepository.saveAndFlush(
                new AdminUser("review-report-admin", "password-hash"));
        createdAdminId = admin.getId();
        ReviewUseCase.ReviewReportItem decided = reviewUseCase.decideReport(
                report.id(), ReviewReportStatus.ACCEPTED, "신고 수용", admin.getId());
        reviewUseCase.deleteReview(owner.getId(), review.id());
        ReviewUseCase.ReviewReportSummaryItem summary = reviewUseCase.listAdminReports(
                ReviewReportStatus.ACCEPTED, null, 20).content().getFirst();
        ReviewUseCase.ReviewReportItem persisted = reviewUseCase.getAdminReport(report.id());

        assertSoftly(softly -> {
            softly.assertThat(removed.helpfulCount()).isZero();
            softly.assertThat(removedAgain.helpfulCount()).isZero();
            softly.assertThat(decided.status()).isEqualTo(ReviewReportStatus.ACCEPTED);
            softly.assertThat(summary.id()).isEqualTo(report.id());
            softly.assertThat(persisted.evidence().content()).isEqualTo("신고 시점 원문");
        });
    }

    @Test
    @DisplayName("신고와 운영 심사 증거는 후기 수정과 삭제 뒤에도 당시 이미지 순서대로 남는다")
    void reportAndModerationEvidenceSurvivesReviewChangesAndDeletion() {
        User owner = createUser("review-evidence-owner@example.com", "01074000018", "증거 후기 회원");
        User reporter = createUser("review-evidence-reporter@example.com", "01074000019", "증거 신고 회원");
        ProductOrderSource source = createProductOrderSource(owner, true, "증거 보존 상품");
        ReviewUseCase.ReviewItem review = reviewUseCase.createProductReview(
                owner.getId(), source.orderItem().getId(), 2, "신고 당시 본문");
        var firstImage = imageAttachmentService.attach(
                owner.getId(), review.id(), "/images/evidence-first.webp");
        imageAttachmentService.attach(owner.getId(), review.id(), "/images/evidence-second.webp");

        ReviewUseCase.ReviewReportItem report = reviewUseCase.createReport(
                reporter.getId(), review.id(), ReviewReportReason.FALSE_INFORMATION, "사실과 다릅니다");
        ReviewUseCase.ReviewItem beforeEdit = reviewUseCase.getAdminReview(review.id());
        reviewUseCase.updateReview(
                owner.getId(),
                review.id(),
                beforeEdit.contentRevision(),
                5,
                "심사 당시 수정 본문");
        reviewUseCase.deleteReviewImage(owner.getId(), review.id(), firstImage.getId());
        imageAttachmentService.attach(owner.getId(), review.id(), "/images/evidence-replacement.webp");
        ReviewUseCase.ReviewItem current = reviewUseCase.getAdminReview(review.id());
        AdminUser admin = adminUserRepository.saveAndFlush(
                new AdminUser("review-evidence-admin", "password-hash"));
        createdAdminId = admin.getId();

        reviewUseCase.updateStatus(
                review.id(),
                ReviewStatus.HIDDEN,
                "증거 확인 후 숨김",
                current.contentRevision(),
                current.version(),
                admin.getId());
        reviewUseCase.deleteReview(owner.getId(), review.id());

        ReviewUseCase.ReviewReportItem persistedReport = reviewUseCase.getAdminReport(report.id());
        ReviewUseCase.ModerationActionItem moderation = reviewUseCase
                .listModerationActions(review.id())
                .getFirst();

        assertSoftly(softly -> {
            softly.assertThat(report.evidence().contentRevision()).isEqualTo(3L);
            softly.assertThat(persistedReport.evidence().rating()).isEqualTo(2);
            softly.assertThat(persistedReport.evidence().content()).isEqualTo("신고 당시 본문");
            softly.assertThat(persistedReport.evidence().imageUrls())
                    .containsExactly(
                            "/images/evidence-first.webp",
                            "/images/evidence-second.webp");
            softly.assertThat(moderation.evidence().rating()).isEqualTo(5);
            softly.assertThat(moderation.evidence().content()).isEqualTo("심사 당시 수정 본문");
            softly.assertThat(moderation.evidence().imageUrls())
                    .containsExactly(
                            "/images/evidence-replacement.webp",
                            "/images/evidence-second.webp");
            softly.assertThat(moderation.evidence().imagesComplete()).isTrue();
        });
    }

    @Test
    @DisplayName("미결 신고 증거는 만료하지 않고 신고 결정 후 3년이 지나면 신고와 함께 삭제한다")
    void reportEvidenceRetentionStartsOnlyAfterDecision() {
        User owner = createUser("review-retention-owner@example.com", "01074000020", "보존 후기 회원");
        User reporter = createUser("review-retention-reporter@example.com", "01074000021", "보존 신고 회원");
        ProductOrderSource source = createProductOrderSource(owner, true, "보존 정책 상품");
        ReviewUseCase.ReviewItem review = reviewUseCase.createProductReview(
                owner.getId(), source.orderItem().getId(), 1, "보존 정책 검토 본문");
        ReviewUseCase.ReviewReportItem pending = reviewUseCase.createReport(
                reporter.getId(), review.id(), ReviewReportReason.OTHER, "운영 확인이 필요합니다");

        int pendingDeleted = evidenceRetentionService.deleteExpiredBatch(
                pending.evidence().capturedAt().plusYears(10), 100);

        assertSoftly(softly -> {
            softly.assertThat(pendingDeleted).isZero();
            softly.assertThat(tableCount("review_reports")).isEqualTo(1L);
            softly.assertThat(tableCount("review_evidence_snapshots")).isEqualTo(1L);
        });

        AdminUser admin = adminUserRepository.saveAndFlush(
                new AdminUser("review-retention-admin", "password-hash"));
        createdAdminId = admin.getId();
        ReviewUseCase.ReviewReportItem decided = reviewUseCase.decideReport(
                pending.id(), ReviewReportStatus.REJECTED, "정책 위반 아님", admin.getId());

        int beforeDeadline = evidenceRetentionService.deleteExpiredBatch(
                decided.decidedAt().plusYears(3).minusSeconds(1), 100);
        int atDeadline = evidenceRetentionService.deleteExpiredBatch(
                decided.decidedAt().plusYears(3), 100);

        assertSoftly(softly -> {
            softly.assertThat(beforeDeadline).isZero();
            softly.assertThat(atDeadline).isEqualTo(2);
            softly.assertThat(tableCount("review_reports")).isZero();
            softly.assertThat(tableCount("review_evidence_snapshots")).isZero();
        });
    }

    @Test
    @DisplayName("공식 답글은 수정 시 최초 시각을 유지하고 이미지 중간 삭제 뒤 빈 순서를 재사용한다")
    void replyAndImageAttachmentLifecycleIsStable() {
        User owner = createUser("review-media-owner@example.com", "01074000014", "미디어 후기 회원");
        ProductOrderSource source = createProductOrderSource(owner, true, "미디어 후기 상품");
        ReviewUseCase.ReviewItem review = reviewUseCase.createProductReview(
                owner.getId(), source.orderItem().getId(), 5, "이미지와 답글 후기");
        AdminUser admin = adminUserRepository.saveAndFlush(
                new AdminUser("review-reply-admin", "password-hash"));
        createdAdminId = admin.getId();

        ReviewUseCase.ReviewItem replied = reviewUseCase.upsertOfficialReply(
                review.id(), "첫 공식 답글", review.version(), admin.getId());
        ReviewUseCase.ReviewItem editedReply = reviewUseCase.upsertOfficialReply(
                review.id(), "수정 공식 답글", replied.version(), admin.getId());
        var firstImage = imageAttachmentService.attach(owner.getId(), review.id(), "/images/a.webp");
        var middleImage = imageAttachmentService.attach(owner.getId(), review.id(), "/images/b.webp");
        var lastImage = imageAttachmentService.attach(owner.getId(), review.id(), "/images/c.webp");
        reviewUseCase.deleteReviewImage(owner.getId(), review.id(), middleImage.getId());
        var replacement = imageAttachmentService.attach(
                owner.getId(), review.id(), "/images/replacement.webp");

        assertSoftly(softly -> {
            softly.assertThat(replied.officialReply().createdAt()).isNotNull();
            softly.assertThat(replied.officialReply().editedAt()).isNull();
            softly.assertThat(editedReply.officialReply().createdAt())
                    .isEqualTo(replied.officialReply().createdAt());
            softly.assertThat(editedReply.officialReply().editedAt()).isNotNull();
            softly.assertThat(replacement.getSortOrder()).isEqualTo(1);
            softly.assertThat(reviewImageRepository.findByReviewId(review.id()))
                    .extracting(com.personal.happygallery.domain.review.ReviewImage::getSortOrder)
                    .containsExactly(0, 1, 2);
            softly.assertThat(reviewRepository.findById(review.id()).orElseThrow()
                            .getContentRevision())
                    .isEqualTo(6L);
            softly.assertThat(firstImage.getId()).isNotNull();
            softly.assertThat(lastImage.getId()).isNotNull();
        });

        ReviewUseCase.ReviewItem current = reviewUseCase.getAdminReview(review.id());
        ReviewUseCase.ReviewItem withoutReply = reviewUseCase.deleteOfficialReply(
                review.id(), current.version());
        assertThat(withoutReply.officialReply()).isNull();
    }

    @Test
    @DisplayName("오래된 version의 공식 답글 수정과 삭제는 거부하고 최신 답글을 유지한다")
    void staleOfficialReplyCommandsKeepLatestReply() {
        User owner = createUser("review-reply-stale-owner@example.com", "01074000020", "답글 충돌 회원");
        ProductOrderSource source = createProductOrderSource(owner, true, "답글 충돌 상품");
        ReviewUseCase.ReviewItem review = reviewUseCase.createProductReview(
                owner.getId(), source.orderItem().getId(), 5, "답글 충돌 후기");
        AdminUser admin = adminUserRepository.saveAndFlush(
                new AdminUser("review-reply-stale-admin", "password-hash"));
        createdAdminId = admin.getId();

        ReviewUseCase.ReviewItem replied = reviewUseCase.upsertOfficialReply(
                review.id(), "첫 공식 답글", review.version(), admin.getId());
        ReviewUseCase.ReviewItem latest = reviewUseCase.upsertOfficialReply(
                review.id(), "최신 공식 답글", replied.version(), admin.getId());

        assertThatThrownBy(() -> reviewUseCase.upsertOfficialReply(
                review.id(), "오래된 화면의 수정", replied.version(), admin.getId()))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CONFLICT));
        assertThatThrownBy(() -> reviewUseCase.deleteOfficialReply(
                review.id(), replied.version()))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CONFLICT));

        ReviewUseCase.ReviewItem current = reviewUseCase.getAdminReview(review.id());
        assertSoftly(softly -> {
            softly.assertThat(current.version()).isEqualTo(latest.version());
            softly.assertThat(current.officialReply()).isNotNull();
            softly.assertThat(current.officialReply().content()).isEqualTo("최신 공식 답글");
            softly.assertThat(current.officialReply().editedAt()).isNotNull();
        });
    }

    private User createUser(String email, String phone, String name) {
        return userStore.save(new User(email, "password-hash", name, phone));
    }

    private ProductOrderSource createProductOrderSource(
            User user, boolean completed, String productName) {
        Product product = productRepository.saveAndFlush(
                new Product(productName, ProductType.READY_STOCK, 30_000L));
        return createProductOrderSource(user, completed, product);
    }

    private ProductOrderSource createProductOrderSource(
            User user, boolean completed, Product product) {
        LocalDateTime now = LocalDateTime.now(clock);
        Order order = Order.forMember(
                user.getId(), 30_000L, now.minusHours(1), now.plusHours(23));
        if (completed) {
            order.approve();
            order.markPickupReady();
            order.confirmPickup();
        }
        order = orderRepository.saveAndFlush(order);
        OrderItem orderItem = orderItemRepository.saveAndFlush(
                new OrderItem(order, product.getId(), product.getName(), 1, product.getPrice()));
        return new ProductOrderSource(product, order, orderItem);
    }

    private ClassBookingSource createClassBookingSource(User user, String className) {
        BookingClass bookingClass = classRepository.saveAndFlush(
                new BookingClass(className, "PERFUME", 120, 50_000L, 30));
        return createClassBookingSource(user, bookingClass, 0);
    }

    private ClassBookingSource createClassBookingSource(
            User user, BookingClass bookingClass, int slotSequence) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime endAt = now.minusHours(1L + slotSequence * 3L);
        Slot slot = slotRepository.saveAndFlush(
                new Slot(bookingClass, endAt.minusHours(2), endAt));
        Booking booking = Booking.forMemberDeposit(
                user, slot, 0L, 0L, DepositPaymentMethod.CARD);
        booking.complete(now);
        booking = bookingRepository.saveAndFlush(booking);
        return new ClassBookingSource(bookingClass, booking);
    }

    private List<Long> collectPublicReviewIds(
            ReviewTargetType targetType, Long targetId, ReviewSort sort) {
        List<Long> reviewIds = new ArrayList<>();
        String cursor = null;
        for (int pageNumber = 0; pageNumber < 10; pageNumber++) {
            ReviewUseCase.PublicReviewPage response = targetType == ReviewTargetType.PRODUCT
                    ? reviewUseCase.listProductReviews(targetId, null, sort, cursor, 1)
                    : reviewUseCase.listClassReviews(targetId, null, sort, cursor, 1);
            reviewIds.addAll(response.reviews().content().stream()
                    .map(ReviewUseCase.ReviewItem::id)
                    .toList());
            if (!response.reviews().hasMore()) {
                return reviewIds;
            }
            cursor = response.reviews().nextCursor();
        }
        throw new AssertionError("공개 후기 커서가 10페이지 안에 종료되지 않았습니다.");
    }

    private Throwable createReview(
            CountDownLatch ready,
            CountDownLatch start,
            Long userId,
            Long orderItemId,
            String content) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                return new AssertionError("동시 후기 작성 시작 신호를 기다리지 못했습니다.");
            }
            reviewUseCase.createProductReview(userId, orderItemId, 5, content);
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private long tableCount(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
    }

    private record ProductOrderSource(Product product, Order order, OrderItem orderItem) {}

    private record ClassBookingSource(BookingClass bookingClass, Booking booking) {}

    private record ConcurrentResult(Throwable firstFailure, Throwable secondFailure) {}
}
