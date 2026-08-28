package com.personal.happygallery.application.review;

import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.review.port.in.PublicReviewUseCase;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.PublicReviewPage;
import com.personal.happygallery.application.review.port.out.ReviewListView;
import com.personal.happygallery.application.review.port.out.ReviewReaderPort;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.review.ReviewSort;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class DefaultPublicReviewService implements PublicReviewUseCase {

    private final ReviewReaderPort reviewReader;
    private final ProductReaderPort productReader;
    private final ClassReaderPort classReader;
    private final ReviewViewAssembler viewAssembler;

    DefaultPublicReviewService(ReviewReaderPort reviewReader,
                               ProductReaderPort productReader,
                               ClassReaderPort classReader,
                               ReviewViewAssembler viewAssembler) {
        this.reviewReader = reviewReader;
        this.productReader = productReader;
        this.classReader = classReader;
        this.viewAssembler = viewAssembler;
    }

    @Override
    public PublicReviewPage listProductReviews(
            Long productId, Integer rating, ReviewSort sort, String cursor, int size) {
        productReader.findById(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        int pageSize = PageParams.requireSize(size);
        ReviewPublicCursor.CursorParam cursorParam = decodeCursor(cursor, sort, rating);
        List<ReviewListView> fetched = reviewReader.findPublishedByProduct(
                productId,
                rating,
                sort,
                cursorParam == null ? null : cursorParam.rating(),
                cursorParam == null ? null : cursorParam.createdAt(),
                cursorParam == null ? null : cursorParam.id(),
                pageSize + 1);
        return viewAssembler.publicPage(
                reviewReader.summarizePublishedProduct(productId),
                reviewReader.countPublishedProduct(productId, rating),
                fetched,
                pageSize,
                sort,
                rating);
    }

    @Override
    public PublicReviewPage listClassReviews(
            Long classId, Integer rating, ReviewSort sort, String cursor, int size) {
        classReader.findById(classId)
                .orElseThrow(NotFoundException.supplier("클래스"))
                .requireActive();
        int pageSize = PageParams.requireSize(size);
        ReviewPublicCursor.CursorParam cursorParam = decodeCursor(cursor, sort, rating);
        List<ReviewListView> fetched = reviewReader.findPublishedByClass(
                classId,
                rating,
                sort,
                cursorParam == null ? null : cursorParam.rating(),
                cursorParam == null ? null : cursorParam.createdAt(),
                cursorParam == null ? null : cursorParam.id(),
                pageSize + 1);
        return viewAssembler.publicPage(
                reviewReader.summarizePublishedClass(classId),
                reviewReader.countPublishedClass(classId, rating),
                fetched,
                pageSize,
                sort,
                rating);
    }

    private static ReviewPublicCursor.CursorParam decodeCursor(
            String cursor, ReviewSort sort, Integer ratingFilter) {
        return cursor == null ? null : ReviewPublicCursor.decode(cursor, sort, ratingFilter);
    }
}
