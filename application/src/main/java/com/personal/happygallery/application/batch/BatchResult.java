package com.personal.happygallery.application.batch;

import java.util.LinkedHashMap;
import java.util.Map;

public record BatchResult(int successCount, int failureCount, Map<String, Integer> failureReasons) {

    public BatchResult {
        failureReasons = Map.copyOf(failureReasons);
    }

    public static BatchResult successOnly(int successCount) {
        return new BatchResult(successCount, 0, Map.of());
    }

    public static BatchResult of(int successCount, Map<String, Integer> failureReasons) {
        int failureCount = failureReasons.values().stream().mapToInt(Integer::intValue).sum();
        return new BatchResult(successCount, failureCount, failureReasons);
    }

    public BatchResult merge(BatchResult other) {
        Map<String, Integer> merged = new LinkedHashMap<>(failureReasons);
        other.failureReasons.forEach((k, v) -> merged.merge(k, v, Integer::sum));
        return new BatchResult(
                successCount + other.successCount,
                failureCount + other.failureCount,
                merged);
    }

    public static Map<String, Integer> failureReasonsOf(Throwable... errors) {
        Map<String, Integer> reasons = new LinkedHashMap<>();
        for (Throwable error : errors) {
            reasons.merge(error.getClass().getSimpleName(), 1, Integer::sum);
        }
        return reasons;
    }
}
