package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.application.batch.BatchResult;

public interface OrderAutoRefundBatchUseCase {

    BatchResult autoRefundExpired();
}
