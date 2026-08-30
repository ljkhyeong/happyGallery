package com.personal.happygallery.application.order.port.out;

import java.time.LocalDateTime;

public record OrderApprovalBacklogSummary(long count, LocalDateTime oldestPaidAt) {
}
