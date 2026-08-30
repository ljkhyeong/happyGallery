package com.personal.happygallery.application.payment.port.in;

import com.personal.happygallery.application.batch.BatchResult;

public interface PaymentWebhookBatchUseCase {

    BatchResult processPendingReceipts();
}
