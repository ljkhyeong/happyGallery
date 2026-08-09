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
                productReview.id(), ReviewStatus.HIDDEN, "운영 정책 위반", admin.getId());
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
                productReview.id(), ReviewStatus.PUBLISHED, null, admin.getId());

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
                owner.getId(), created.id(), 4, "수정한 후기");

        assertSoftly(softly -> {
            softly.assertThat(updated.rating()).isEqualTo(4);
            softly.assertThat(updated.content()).isEqualTo("수정한 후기");
            softly.assertThat(updated.updatedAt()).isAfterOrEqualTo(updated.createdAt());
            softly.assertThat(updated.edited()).isTrue();
        });
        assertThatThrownBy(() -> reviewUseCase.updateReview(
                other.getId(), created.id(), 5, "타인 수정"))
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
    @DisplayName("숨김 이력이 있는 후기 tombstone은 재공개 뒤에도 재작성을 차단하고 실제 전이만 감사 기록한다")
    void moderatedReviewTombstoneBlocksRecreationAndKeepsTransitionHistory() {
        User owner = createUser("review-block-owner@example.com", "01074000007", "차단 후기 회원");
        ProductOrderSource source = createProductOrderSource(owner, true, "차단 후기 상품");
        AdminUser admin = adminUserRepository.saveAndFlush(
                new AdminUser("review-block-admin", "password-hash"));
        createdAdminId = admin.getId();
        ReviewUseCase.ReviewItem review = reviewUseCase.createProductReview(
                owner.getId(), source.orderItem().getId(), 1, "운영 검토 대상 후기");

        reviewUseCase.updateStatus(
                review.id(), ReviewStatus.HIDDEN, "운영 정책 위반", admin.getId());
        reviewUseCase.updateStatus(
                review.id(), ReviewStatus.HIDDEN, "중복 요청", admin.getId());
        reviewUseCase.updateStatus(
                review.id(), ReviewStatus.PUBLISHED, null, admin.getId());
        reviewUseCase.updateStatus(
                review.id(), ReviewStatus.PUBLISHED, null, admin.getId());
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
                reviewUseCase.listMyReviewOpportunities(owner.getId());

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
        assertThat(reviewUseCase.listMyReviewOpportunities(owner.getId()))
                .filteredOn(opportunity -> opportunity.targetType() == ReviewTargetType.PRODUCT)
                .extracting(ReviewUseCase.ReviewOpportunity::sourceId)
                .doesNotContain(productSource.orderItem().getId());

        reviewUseCase.deleteReview(owner.getId(), productReview.id());
        List<ReviewUseCase.ReviewOpportunity> afterDelete =
                reviewUseCase.listMyReviewOpportunities(owner.getId());
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
        reviewUseCase.updateReview(owner.getId(), review.id(), 5, "신고 뒤 수정된 본문");

        assertSoftly(softly -> {
            softly.assertThat(firstHelpful.helpfulCount()).isEqualTo(1L);
            softly.assertThat(duplicateHelpful.helpfulCount()).isEqualTo(1L);
            softly.assertThat(report.snapshotRating()).isEqualTo(2);
            softly.assertThat(report.snapshotContent()).isEqualTo("신고 시점 원문");
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
        ReviewUseCase.ReviewReportItem persisted = reviewUseCase.listAdminReports(
                ReviewReportStatus.ACCEPTED, null, 20).content().getFirst();

        assertSoftly(softly -> {
            softly.assertThat(removed.helpfulCount()).isZero();
            softly.assertThat(removedAgain.helpfulCount()).isZero();
            softly.assertThat(decided.status()).isEqualTo(ReviewReportStatus.ACCEPTED);
            softly.assertThat(persisted.snapshotContent()).isEqualTo("신고 시점 원문");
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
                review.id(), "첫 공식 답글", admin.getId());
        ReviewUseCase.ReviewItem editedReply = reviewUseCase.upsertOfficialReply(
                review.id(), "수정 공식 답글", admin.getId());
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
            softly.assertThat(reviewImageRepository.findByReviewIdOrderBySortOrderAscIdAsc(review.id()))
                    .extracting(com.personal.happygallery.domain.review.ReviewImage::getSortOrder)
                    .containsExactly(0, 1, 2);
            softly.assertThat(firstImage.getId()).isNotNull();
            softly.assertThat(lastImage.getId()).isNotNull();
        });

        ReviewUseCase.ReviewItem withoutReply = reviewUseCase.deleteOfficialReply(
                review.id(), admin.getId());
        assertThat(withoutReply.officialReply()).isNull();
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
        LocalDateTime now = LocalDateTime.now(clock);
        BookingClass bookingClass = classRepository.saveAndFlush(
                new BookingClass(className, "PERFUME", 120, 50_000L, 30));
        Slot slot = slotRepository.saveAndFlush(
                new Slot(bookingClass, now.minusHours(3), now.minusHours(1)));
        Booking booking = Booking.forMemberDeposit(
                user, slot, 0L, 0L, DepositPaymentMethod.CARD);
        booking.complete(now);
        booking = bookingRepository.saveAndFlush(booking);
        return new ClassBookingSource(bookingClass, booking);
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

    private record ProductOrderSource(Product product, Order order, OrderItem orderItem) {}

    private record ClassBookingSource(BookingClass bookingClass, Booking booking) {}

    private record ConcurrentResult(Throwable firstFailure, Throwable secondFailure) {}
}
