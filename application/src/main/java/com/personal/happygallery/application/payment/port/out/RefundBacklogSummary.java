package com.personal.happygallery.application.payment.port.out;

import com.personal.happygallery.domain.payment.RefundStatus;
import java.time.LocalDateTime;

public record RefundBacklogSummary(
        RefundStatus status,
        long count,
        LocalDateTime oldestActionAt
) {}
