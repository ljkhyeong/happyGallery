package com.personal.happygallery.application.batch;

import com.personal.happygallery.application.batch.port.out.BatchExecutionLeasePort;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ScheduledExecutionGuardAspect {
    private final BatchExecutionLeasePort leases;
    private final boolean enabled;

    public ScheduledExecutionGuardAspect(BatchExecutionLeasePort leases,
            @Value("${app.deployment.guard-enabled:false}") boolean enabled) {
        this.leases = leases;
        this.enabled = enabled;
    }

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object execute(ProceedingJoinPoint invocation) throws Throwable {
        if (!enabled) {
            return invocation.proceed();
        }
        MethodSignature method = (MethodSignature) invocation.getSignature();
        var acquired = leases.tryAcquire(method.getDeclaringTypeName() + "." + method.getName());
        if (acquired.isEmpty()) {
            // 실행하지 않은 배치를 성공 지표로 기록하지 않도록 logging aspect 바깥에서 반환한다.
            return method.getReturnType() == BatchResult.class ? BatchResult.successOnly(0) : null;
        }
        try (var ignored = acquired.get()) {
            return invocation.proceed();
        }
    }
}
