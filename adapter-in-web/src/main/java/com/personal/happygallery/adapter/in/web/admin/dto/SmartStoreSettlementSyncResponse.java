package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.batch.BatchResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

public record SmartStoreSettlementSyncResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int successCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int issueCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Map<String, Integer> issueReasons
) {
    public SmartStoreSettlementSyncResponse {
        issueReasons = Map.copyOf(issueReasons);
    }

    public static SmartStoreSettlementSyncResponse from(BatchResult result) {
        return new SmartStoreSettlementSyncResponse(
                result.successCount(), result.failureCount(), result.failureReasons());
    }
}
