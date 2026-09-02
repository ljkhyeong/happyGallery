package com.personal.happygallery.application.order.port.out;

import java.time.LocalDateTime;

public record SmartStoreOrderActionBacklogSummary(long count, LocalDateTime oldestRequestedAt) {}
