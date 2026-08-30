package com.personal.happygallery.application.review.port.out;

import com.personal.happygallery.domain.review.ReviewTargetType;
import java.time.LocalDateTime;

public record ReviewOpportunityView(
        ReviewTargetType targetType,
        Long sourceId,
        Long targetId,
        String targetName,
        Long orderId,
        Long bookingId,
        LocalDateTime completedAt
) {}
