package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.batch.BatchResult;
import java.util.LinkedHashMap;
import java.util.Map;

public record BatchResponse(
        int successCount,
        int failureCount,
        Map<String, Integer> failureReasons
) {

    private static final Map<String, String> REASON_LABELS = Map.ofEntries(
            Map.entry("ObjectOptimisticLockingFailureException", "CONFLICT"),
            Map.entry("OptimisticLockingFailureException", "CONFLICT"),
            Map.entry("NotFoundException", "NOT_FOUND"),
            Map.entry("AlreadyRefundedException", "ALREADY_PROCESSED"),
            Map.entry("HappyGalleryException", "BUSINESS_ERROR")
    );

    public static BatchResponse from(BatchResult result) {
        Map<String, Integer> sanitized = new LinkedHashMap<>();
        result.failureReasons().forEach((key, count) ->
                sanitized.merge(REASON_LABELS.getOrDefault(key, "INTERNAL_ERROR"), count, Integer::sum));
        return new BatchResponse(result.successCount(), result.failureCount(), sanitized);
    }
}
