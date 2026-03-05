package com.personal.happygallery.app.batch;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class BatchLoggingAspectTest {

    private final BatchLoggingAspect aspect = new BatchLoggingAspect();

    @Test
    void logBatchExecution_returnsBatchResultAsIs() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        BatchJob batchJob = mock(BatchJob.class);
        BatchResult result = BatchResult.successOnly(3);

        when(batchJob.value()).thenReturn("주문 자동환불");
        when(joinPoint.proceed()).thenReturn(result);

        Object actual = aspect.logBatchExecution(joinPoint, batchJob);

        assertThat(actual).isEqualTo(result);
        verify(joinPoint).proceed();
    }

    @Test
    void logBatchExecution_rethrowsException() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        BatchJob batchJob = mock(BatchJob.class);
        RuntimeException failure = new RuntimeException("boom");

        when(batchJob.value()).thenReturn("픽업 만료");
        when(joinPoint.proceed()).thenThrow(failure);

        assertThatThrownBy(() -> aspect.logBatchExecution(joinPoint, batchJob))
                .isSameAs(failure);
    }

    @Test
    void logBatchExecution_blankBatchName_usesMethodSignature(CapturedOutput output) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        BatchJob batchJob = mock(BatchJob.class);

        when(batchJob.value()).thenReturn("  ");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("BatchScheduler.runOrderAutoRefund()");
        when(joinPoint.proceed()).thenReturn(BatchResult.successOnly(1));

        aspect.logBatchExecution(joinPoint, batchJob);

        assertThat(output).contains("BatchScheduler.runOrderAutoRefund()");
    }
}
