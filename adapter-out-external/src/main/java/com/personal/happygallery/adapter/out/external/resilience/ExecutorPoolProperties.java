package com.personal.happygallery.adapter.out.external.resilience;

import jakarta.validation.constraints.Min;

/** 외부 호출 timeout executor의 고정 스레드 수와 제한 큐 크기. */
public record ExecutorPoolProperties(
        @Min(1) int poolSize,
        @Min(1) int queueCapacity
) {}
