package com.personal.happygallery.app.batch;

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

    @Around("@annotation(batchJob)")
    public Object logBatchExecution(ProceedingJoinPoint joinPoint, BatchJob batchJob) throws Throwable {
        String jobName = batchJob.value();
        String batchRequestId = "batch-" + jobName.replaceAll("\\s+", "-") + "-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", batchRequestId);

        long startedAt = System.nanoTime();
        log.info("[배치] {} 시작", jobName);

        try {
            Object result = joinPoint.proceed();
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

            if (result instanceof BatchResult batchResult) {
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
            } else if (result instanceof Number count) {
                log.info("[배치] {} 완료: {}건 ({}ms)", jobName, count.intValue(), durationMs);
            } else {
                log.info("[배치] {} 완료 ({}ms)", jobName, durationMs);
            }
            return result;
        } catch (Throwable t) {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            log.error("[배치] {} 실패 ({}ms)", jobName, durationMs, t);
            throw t;
        } finally {
            MDC.remove("requestId");
        }
    }
}
