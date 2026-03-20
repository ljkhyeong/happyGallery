package com.personal.happygallery.app.order.port.in;

import com.personal.happygallery.app.batch.BatchResult;

public interface OrderAutoRefundBatchUseCase {

    BatchResult autoRefundExpired();
}
