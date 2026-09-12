package com.personal.happygallery.application.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.personal.happygallery.application.batch.port.out.BatchExecutionLeasePort;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.scheduling.annotation.Scheduled;

class ScheduledExecutionGuardAspectTest {
    public static class ScheduledWork {
        int calls;
        boolean fail;

        @Scheduled(fixedDelay = 1000)
        public BatchResult run() {
            calls++;
            if (fail) throw new IllegalStateException("작업 실패");
            return BatchResult.successOnly(1);
        }
    }

    private ScheduledWork proxy(ScheduledWork target, BatchExecutionLeasePort leases, boolean enabled) {
        var factory = new AspectJProxyFactory(target);
        factory.addAspect(new ScheduledExecutionGuardAspect(leases, enabled));
        return factory.getProxy();
    }

    @Test
    @DisplayName("보호가 비활성인 개발 환경은 저장소 잠금을 조회하지 않는다")
    void disabledGuard() {
        var leases = mock(BatchExecutionLeasePort.class);
        var target = new ScheduledWork();
        proxy(target, leases, false).run();
        assertThat(target.calls).isEqualTo(1);
        verifyNoInteractions(leases);
    }

    @Test
    @DisplayName("잠금을 얻지 못한 정기 작업은 실행하지 않는다")
    void skipsUnavailableLease() {
        var leases = mock(BatchExecutionLeasePort.class);
        when(leases.tryAcquire(anyString())).thenReturn(Optional.empty());
        var target = new ScheduledWork();
        assertThat(proxy(target, leases, true).run()).isEqualTo(BatchResult.successOnly(0));
        assertThat(target.calls).isZero();
    }

    @Test
    @DisplayName("배치 예외를 전파하면서 실행 잠금을 해제한다")
    void releasesOnFailure() {
        var leases = mock(BatchExecutionLeasePort.class);
        var lease = mock(BatchExecutionLeasePort.Lease.class);
        when(leases.tryAcquire(anyString())).thenReturn(Optional.of(lease));
        var target = new ScheduledWork();
        target.fail = true;
        assertThatThrownBy(() -> proxy(target, leases, true).run()).hasMessage("작업 실패");
        assertThat(target.calls).isEqualTo(1);
        verify(lease).close();
    }
}
