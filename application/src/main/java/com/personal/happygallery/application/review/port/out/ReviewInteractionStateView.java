package com.personal.happygallery.application.review.port.out;

import com.personal.happygallery.domain.review.ReviewStatus;

public record ReviewInteractionStateView(
        Long reviewId,
        Long ownerUserId,
        ReviewStatus status
) {}
