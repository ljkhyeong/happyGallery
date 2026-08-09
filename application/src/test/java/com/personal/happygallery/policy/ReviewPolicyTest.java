package com.personal.happygallery.policy;

import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewReport;
import com.personal.happygallery.domain.review.ReviewReportReason;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("policy")
class ReviewPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 8, 12, 0);

    @Test
    @DisplayName("상품 후기는 주문 품목 원천만 저장하고 별점과 본문을 검증한다")
    void productReviewRequiresValidRatingAndContent() {
        Review review = Review.forProduct(1L, 2L, 3L, 5, "좋아요", NOW);

        assertThat(review.getTargetType()).isEqualTo(ReviewTargetType.PRODUCT);
        assertThat(review.getSourceId()).isEqualTo(2L);
        assertThat(review.getTargetId()).isEqualTo(3L);
        assertThat(review.getBookingId()).isNull();
        assertThat(review.getBookingClassId()).isNull();
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);

        assertThatThrownBy(() -> Review.forProduct(1L, 2L, 3L, 0, "내용", NOW))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> Review.forProduct(1L, 2L, 3L, 6, "내용", NOW))
                .isInstanceOf(HappyGalleryException.class);
        assertThatThrownBy(() -> Review.forProduct(1L, 2L, 3L, 5, " ", NOW))
                .isInstanceOf(HappyGalleryException.class);
    }

    @Test
    @DisplayName("클래스 후기는 예약 원천만 저장한다")
    void classReviewKeepsOnlyBookingSource() {
        Review review = Review.forClass(1L, 4L, 5L, 4, "유익해요", NOW);

        assertThat(review.getTargetType()).isEqualTo(ReviewTargetType.CLASS);
        assertThat(review.getSourceId()).isEqualTo(4L);
        assertThat(review.getTargetId()).isEqualTo(5L);
        assertThat(review.getOrderItemId()).isNull();
        assertThat(review.getProductId()).isNull();
    }

    @Test
    @DisplayName("관리자가 후기를 숨길 때 사유와 처리자를 기록하고 재공개하면 모두 지운다")
    void moderationMetadataMatchesStatus() {
        Review review = Review.forProduct(1L, 2L, 3L, 5, "좋아요", NOW);
        LocalDateTime hiddenAt = NOW.plusMinutes(1);

        review.changeStatus(ReviewStatus.HIDDEN, "운영 정책 위반", 7L, hiddenAt);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        assertThat(review.getHiddenReason()).isEqualTo("운영 정책 위반");
        assertThat(review.getHiddenAt()).isEqualTo(hiddenAt);
        assertThat(review.getHiddenByAdminId()).isEqualTo(7L);
        assertThat(review.isRecreationBlocked()).isTrue();

        LocalDateTime publishedAt = hiddenAt.plusMinutes(1);
        review.changeStatus(ReviewStatus.PUBLISHED, "무시되는 값", null, publishedAt);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
        assertThat(review.getHiddenReason()).isNull();
        assertThat(review.getHiddenAt()).isNull();
        assertThat(review.getHiddenByAdminId()).isNull();
        assertThat(review.getUpdatedAt()).isEqualTo(publishedAt);
        assertThat(review.isRecreationBlocked()).isTrue();
    }

    @Test
    @DisplayName("숨김 이력이 있는 후기는 재공개 뒤 삭제해도 원천 재작성 차단 표식을 유지한다")
    void hiddenReviewKeepsRecreationBlockAfterDelete() {
        Review review = Review.forProduct(1L, 2L, 3L, 5, "숨김 전 내용", NOW);

        review.changeStatus(ReviewStatus.HIDDEN, "정책 위반", 7L, NOW.plusMinutes(1));
        review.changeStatus(ReviewStatus.PUBLISHED, null, 7L, NOW.plusMinutes(2));
        review.softDelete(NOW.plusMinutes(3));

        assertThat(review.isRecreationBlocked()).isTrue();
        assertThat(review.isDeleted()).isTrue();
        assertThat(review.getRating()).isNull();
        assertThat(review.getContent()).isNull();
        assertThat(review.getDeletedAt()).isEqualTo(NOW.plusMinutes(3));
    }

    @Test
    @DisplayName("일반 삭제는 원천 재작성 차단 표식을 만들지 않고 본문만 비식별화한다")
    void ordinaryDeleteDoesNotBlockRecreation() {
        Review review = Review.forClass(1L, 4L, 5L, 4, "삭제할 내용", NOW);

        review.softDelete(NOW.plusMinutes(1));

        assertThat(review.isRecreationBlocked()).isFalse();
        assertThat(review.getRating()).isNull();
        assertThat(review.getContent()).isNull();
    }

    @Test
    @DisplayName("본문 수정 시각은 운영 상태와 공식 답글 변경 시각과 분리한다")
    void contentEditTimeIsIndependentFromModerationAndReply() {
        Review review = Review.forProduct(1L, 2L, 3L, 5, "최초 내용", NOW);

        review.update(4, "수정 내용", NOW.plusMinutes(1));
        review.changeStatus(ReviewStatus.HIDDEN, "검토", 7L, NOW.plusMinutes(2));
        boolean firstReply = review.upsertOfficialReply(
                "공식 답글", 7L, NOW.plusMinutes(3));
        boolean editedReply = review.upsertOfficialReply(
                "수정 공식 답글", 8L, NOW.plusMinutes(4));

        assertThat(review.getEditedAt()).isEqualTo(NOW.plusMinutes(1));
        assertThat(firstReply).isTrue();
        assertThat(editedReply).isFalse();
        assertThat(review.getReplyCreatedAt()).isEqualTo(NOW.plusMinutes(3));
        assertThat(review.getReplyEditedAt()).isEqualTo(NOW.plusMinutes(4));
    }

    @Test
    @DisplayName("후기 신고 결정은 대기 상태에서 승인 또는 기각으로 한 번만 전이한다")
    void reportDecisionIsSingleTransition() {
        ReviewReport report = new ReviewReport(
                1L,
                2L,
                ReviewReportReason.SPAM,
                "반복 광고",
                5,
                "신고 당시 내용",
                ReviewStatus.PUBLISHED,
                null,
                NOW);

        report.decide(ReviewReportStatus.ACCEPTED, "확인 완료", 7L, NOW.plusMinutes(1));

        assertThat(report.getStatus()).isEqualTo(ReviewReportStatus.ACCEPTED);
        assertThat(report.getSnapshotContent()).isEqualTo("신고 당시 내용");
        assertThatThrownBy(() -> report.decide(
                ReviewReportStatus.REJECTED, null, 7L, NOW.plusMinutes(2)))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.REVIEW_REPORT_DECISION_NOT_ALLOWED));
    }

    @Test
    @DisplayName("숨김 상태는 사유와 관리자 ID를 모두 요구한다")
    void hiddenReviewRequiresModerationMetadata() {
        Review review = Review.forProduct(1L, 2L, 3L, 5, "좋아요", NOW);

        assertThatThrownBy(() -> review.changeStatus(
                ReviewStatus.HIDDEN, " ", 7L, NOW.plusMinutes(1)))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> review.changeStatus(
                ReviewStatus.HIDDEN, "사유", null, NOW.plusMinutes(1)))
                .isInstanceOf(HappyGalleryException.class);
    }

    @Test
    @DisplayName("상품 후기는 배송 완료 픽업 완료 최종 완료 주문에만 허용한다")
    void orderStatusRequiresFulfilledOrderForReview() {
        Set<OrderStatus> allowed = Set.of(
                OrderStatus.DELIVERED,
                OrderStatus.PICKED_UP,
                OrderStatus.COMPLETED);

        for (OrderStatus status : OrderStatus.values()) {
            if (allowed.contains(status)) {
                assertThatCode(status::requireReviewable).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(status::requireReviewable)
                        .isInstanceOfSatisfying(
                                HappyGalleryException.class,
                                exception -> assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED));
            }
        }
    }

    @Test
    @DisplayName("클래스 후기는 완료 예약에만 허용한다")
    void bookingStatusRequiresCompletedBookingForReview() {
        for (BookingStatus status : BookingStatus.values()) {
            if (status == BookingStatus.COMPLETED) {
                assertThatCode(status::requireReviewable).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(status::requireReviewable)
                        .isInstanceOfSatisfying(
                                HappyGalleryException.class,
                                exception -> assertThat(exception.getErrorCode())
                                        .isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED));
            }
        }
    }
}
