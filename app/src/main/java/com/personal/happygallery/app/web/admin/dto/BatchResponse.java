package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.app.batch.BatchResult;
import java.util.LinkedHashMap;
import java.util.Map;

public record BatchResponse(
        int successCount,
        int failureCount,
        Map<String, Integer> failureReasons
) {

    private static final Map<String, String> REASON_LABELS = Map.of(
            "ObjectOptimisticLockingFailureException", "CONFLICT",
            "OptimisticLockingFailureException", "CONFLICT",
            "NotFoundException", "NOT_FOUND"
    );

    public static BatchResponse from(BatchResult result) {
        Map<String, Integer> sanitized = new LinkedHashMap<>();
        result.failureReasons().forEach((key, count) ->
                sanitized.merge(REASON_LABELS.getOrDefault(key, "INTERNAL_ERROR"), count, Integer::sum));
        return new BatchResponse(result.successCount(), result.failureCount(), sanitized);
    }
}
