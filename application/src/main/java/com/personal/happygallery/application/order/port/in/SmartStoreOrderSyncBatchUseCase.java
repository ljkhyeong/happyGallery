package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.application.batch.BatchResult;

public interface SmartStoreOrderSyncBatchUseCase {

    BatchResult syncChangedOrders();

    /** 이번 수집이 현재 조회 구간의 마지막 페이지까지 완료된 경우에만 재고 전송을 허용한다. */
    boolean synchronizeBeforeStock();
}
