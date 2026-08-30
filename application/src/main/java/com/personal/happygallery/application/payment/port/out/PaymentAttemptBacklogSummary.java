package com.personal.happygallery.application.payment.port.out;

import java.time.LocalDateTime;

public record PaymentAttemptBacklogSummary(
        long count,
        LocalDateTime oldestActionAt
) {}
