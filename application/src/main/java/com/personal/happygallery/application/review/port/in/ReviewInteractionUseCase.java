package com.personal.happygallery.application.review.port.in;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.HelpfulResult;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewReaction;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewReportItem;
import com.personal.happygallery.domain.review.ReviewReportReason;
import java.util.List;

/** 공개 후기에 대한 회원의 도움 표시·신고 유스케이스. */
public interface ReviewInteractionUseCase {

    ReviewReportItem createReport(
            Long userId, Long reviewId, ReviewReportReason reason, String detail);

    HelpfulResult markHelpful(Long userId, Long reviewId);

    HelpfulResult unmarkHelpful(Long userId, Long reviewId);

    List<ReviewReaction> listMyReviewReactions(Long userId, List<Long> reviewIds);
}
