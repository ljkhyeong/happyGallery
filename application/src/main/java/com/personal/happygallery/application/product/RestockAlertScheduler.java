package com.personal.happygallery.application.product;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.product.port.out.RestockAlertDeliveryPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RestockAlertScheduler {
    private final RestockAlertDeliveryPort delivery;
    private final RestockAlertTransactionService transactions;

    public RestockAlertScheduler(RestockAlertDeliveryPort delivery, RestockAlertTransactionService transactions) {
        this.delivery = delivery;
        this.transactions = transactions;
    }

    @Scheduled(fixedDelayString = "${app.product.restock-alert.poll-delay-ms:60000}")
    public BatchResult processPending() {
        return BatchExecutor.executeByIdCursor(afterId -> delivery.findCandidateIds(afterId, 100),
                id -> id, transactions::process, "상품 재입고 알림");
    }
}
