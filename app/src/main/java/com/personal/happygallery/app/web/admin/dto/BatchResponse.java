package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.app.batch.BatchResult;
import java.util.Map;

public record BatchResponse(
        int successCount,
        int failureCount,
        Map<String, Integer> failureReasons
) {

    public static BatchResponse from(BatchResult result) {
        return new BatchResponse(result.successCount(), result.failureCount(), result.failureReasons());
    }
}
