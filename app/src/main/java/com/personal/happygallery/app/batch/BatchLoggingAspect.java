package com.personal.happygallery.app.batch;

import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class BatchLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(BatchLoggingAspect.class);

    @Around("@annotation(batchJob)")
    public Object logBatchExecution(ProceedingJoinPoint joinPoint, BatchJob batchJob) throws Throwable {
        long startedAt = System.nanoTime();
        String jobName = batchJob.value();
        log.info("[배치] {} 시작", jobName);

        try {
            Object result = joinPoint.proceed();
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

            if (result instanceof Number count) {
                log.info("[배치] {} 완료: {}건 ({}ms)", jobName, count.intValue(), durationMs);
            } else {
                log.info("[배치] {} 완료 ({}ms)", jobName, durationMs);
            }
            return result;
        } catch (Throwable t) {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            log.error("[배치] {} 실패 ({}ms)", jobName, durationMs, t);
            throw t;
        }
    }
}
