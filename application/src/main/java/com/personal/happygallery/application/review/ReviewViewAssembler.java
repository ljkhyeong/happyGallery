package com.personal.happygallery.application.review;

import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.OfficialReplyItem;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.PublicReviewPage;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.RatingHistogram;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewImageItem;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewItem;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewSummary;
import com.personal.happygallery.application.review.port.out.ReviewHelpfulPort;
import com.personal.happygallery.application.review.port.out.ReviewImagePort;
import com.personal.happygallery.application.review.port.out.ReviewListView;
import com.personal.happygallery.application.review.port.out.ReviewReaderPort;
import com.personal.happygallery.application.review.port.out.ReviewSummaryView;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewImage;
import com.personal.happygallery.domain.review.ReviewSort;
import com.personal.happygallery.domain.user.User;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class ReviewViewAssembler {

    private final ReviewReaderPort reviewReader;
    private final UserReaderPort userReader;
    private final ReviewHelpfulPort helpfulPort;
    private final ReviewImagePort imagePort;

    ReviewViewAssembler(ReviewReaderPort reviewReader,
                        UserReaderPort userReader,
                        ReviewHelpfulPort helpfulPort,
                        ReviewImagePort imagePort) {
        this.reviewReader = reviewReader;
        this.userReader = userReader;
        this.helpfulPort = helpfulPort;
        this.imagePort = imagePort;
    }

    ReviewItem savedView(Review review, boolean includeWithdrawnUsers) {
        return reviewReader.findViewById(review.getId())
                .map(view -> toItems(List.of(view), includeWithdrawnUsers).getFirst())
                .orElseThrow(NotFoundException.supplier("후기"));
    }

    PublicReviewPage publicPage(
            ReviewSummaryView summary,
            long filteredCount,
            List<ReviewListView> fetched,
            int pageSize,
            ReviewSort sort,
            Integer ratingFilter) {
        CursorPage<ReviewItem> page = CursorPage.of(
                toItems(fetched, false),
                pageSize,
                item -> ReviewPublicCursor.encode(
                        sort, ratingFilter, item.rating(), item.createdAt(), item.id()));
        return new PublicReviewPage(toSummary(summary), filteredCount, page);
    }

    CursorPage<ReviewItem> standardPage(
            List<ReviewListView> fetched, int pageSize, boolean includeWithdrawnUsers) {
        return CursorPage.of(
                toItems(fetched, includeWithdrawnUsers),
                pageSize,
                item -> CursorUtils.encode(item.createdAt(), item.id()));
    }

    List<ReviewItem> toItems(List<ReviewListView> views, boolean includeWithdrawnUsers) {
        if (views.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = views.stream().map(ReviewListView::userId).distinct().toList();
        List<Long> reviewIds = views.stream().map(ReviewListView::id).toList();
        List<User> users = includeWithdrawnUsers
                ? userReader.findAllByIdForAdminHistory(userIds)
                : userReader.findAllById(userIds);
        Map<Long, String> authorNames = users.stream()
                .collect(Collectors.toMap(User::getId, User::getName));
        Map<Long, Long> helpfulCounts = helpfulPort.countByReviewIds(reviewIds).stream()
                .collect(Collectors.toMap(
                        ReviewHelpfulPort.ReviewHelpfulCountView::reviewId,
                        ReviewHelpfulPort.ReviewHelpfulCountView::helpfulCount));
        Map<Long, List<ReviewImageItem>> images = imagePort.findByReviewIds(reviewIds).stream()
                .collect(Collectors.groupingBy(
                        ReviewImage::getReviewId,
                        Collectors.mapping(ReviewViewAssembler::toImageItem, Collectors.toList())));
        return views.stream()
                .map(view -> toItem(
                        view,
                        authorNames.getOrDefault(view.userId(), "탈퇴회원"),
                        helpfulCounts.getOrDefault(view.id(), 0L),
                        images.getOrDefault(view.id(), List.of())))
                .toList();
    }

    static ReviewImageItem toImageItem(ReviewImage image) {
        return new ReviewImageItem(
                image.getId(), image.getImageUrl(), image.getSortOrder(), image.getCreatedAt());
    }

    private static ReviewItem toItem(
            ReviewListView view,
            String authorName,
            long helpfulCount,
            List<ReviewImageItem> images) {
        OfficialReplyItem reply = view.replyContent() == null
                ? null
                : new OfficialReplyItem(
                        view.replyContent(),
                        view.replyAdminId(),
                        view.replyCreatedAt(),
                        view.replyEditedAt());
        return new ReviewItem(
                view.id(),
                view.userId(),
                authorName,
                view.targetType(),
                view.sourceId(),
                view.targetId(),
                view.targetName(),
                view.rating(),
                view.content(),
                view.status(),
                view.contentRevision(),
                view.version(),
                view.hiddenReason(),
                view.hiddenAt(),
                view.hiddenByAdminId(),
                view.createdAt(),
                view.updatedAt(),
                view.editedAt(),
                view.editedAt() != null,
                true,
                reply,
                helpfulCount,
                images);
    }

    private static ReviewSummary toSummary(ReviewSummaryView view) {
        return new ReviewSummary(
                view.reviewCount(),
                view.averageRating(),
                new RatingHistogram(
                        view.rating1(),
                        view.rating2(),
                        view.rating3(),
                        view.rating4(),
                        view.rating5()));
    }
}
