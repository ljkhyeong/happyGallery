package com.personal.happygallery.application.order.dto;

import java.util.List;

/** 스마트스토어 일괄 발주·발송 결과다. 외부 응답과 관리자 일괄 처리 결과가 함께 쓴다. */
public record SmartStoreOperationResult(
        List<String> successProductOrderIds,
        List<SmartStoreOperationFailure> failures
) {
    public SmartStoreOperationResult {
        successProductOrderIds = successProductOrderIds == null
                ? List.of() : List.copyOf(successProductOrderIds);
        failures = failures == null ? List.of() : List.copyOf(failures);
    }
}
