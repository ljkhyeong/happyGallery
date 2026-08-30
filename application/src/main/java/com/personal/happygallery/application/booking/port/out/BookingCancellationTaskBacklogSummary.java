package com.personal.happygallery.application.booking.port.out;

import java.time.LocalDateTime;

public record BookingCancellationTaskBacklogSummary(
        long count,
        LocalDateTime oldestCreatedAt
) {}
