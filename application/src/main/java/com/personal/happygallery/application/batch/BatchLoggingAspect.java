package com.personal.happygallery.application.batch;

import com.personal.happygallery.application.monitoring.AppMetrics;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class BatchLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(BatchLoggingAspect.class);
    private final AppMetrics appMetrics;

    public BatchLoggingAspect(AppMetrics appMetrics) {
        this.appMetrics = appMetrics;
    }

    @Around("@annotation(batchJob)")
    public Object logBatchExecution(ProceedingJoinPoint joinPoint, BatchJob batchJob) throws Throwable {
        String jobName = batchJob.value();
        String jobId = batchJob.id();
        String batchRequestId = "batch-" + jobName.replaceAll("\\s+", "-") + "-" + UUID.randomUUID().toString().substring(0, 8);

        try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", batchRequestId)) {
            long startedAt = System.nanoTime();
            log.info("[배치] {} 시작", jobName);
            try {
                Object result = joinPoint.proceed();
                long elapsedNanos = System.nanoTime() - startedAt;
                long durationMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);

                if (result instanceof BatchResult batchResult) {
                    appMetrics.recordBatchResult(jobId, batchResult, elapsedNanos);
                    if (batchResult.failureCount() > 0) {
                        log.warn("[배치] {} 완료: 성공 {}건, 실패 {}건, 사유 {} ({}ms)",
                                jobName,
                                batchResult.successCount(),
                                batchResult.failureCount(),
                                batchResult.failureReasons(),
                                durationMs);
                    } else {
                        log.info("[배치] {} 완료: 성공 {}건, 실패 0건 ({}ms)",
                                jobName,
                                batchResult.successCount(),
                                durationMs);
                    }
                } else {
                    appMetrics.recordBatchSuccess(jobId, elapsedNanos);
                    log.info("[배치] {} 완료 ({}ms)", jobName, durationMs);
                }
                return result;
            } catch (Throwable t) {
                long elapsedNanos = System.nanoTime() - startedAt;
                long durationMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
                appMetrics.recordBatchFailure(jobId, elapsedNanos);
                log.error("[배치] {} 실패 ({}ms) [type={}]",
                        jobName, durationMs, t.getClass().getSimpleName(), t);
                throw t;
            }
        }
    }
}
