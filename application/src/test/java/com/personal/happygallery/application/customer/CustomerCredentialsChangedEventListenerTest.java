package com.personal.happygallery.application.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.personal.happygallery.application.customer.port.out.CustomerSessionRevocationPort;
import com.personal.happygallery.application.monitoring.AppMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomerCredentialsChangedEventListenerTest {

    @DisplayName("자격 증명 변경 후 세션 폐기가 실패하면 운영 경보용 카운터를 증가시킨다")
    @Test
    void revokeSessions_failure_incrementsMetric() {
        CustomerSessionRevocationPort sessionRevocation = mock(CustomerSessionRevocationPort.class);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(sessionRevocation)
                .revokeCredentialVersion(10L, 3L);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AppMetrics metrics = new AppMetrics(
                registry,
                Clock.fixed(Instant.parse("2026-07-30T00:00:00Z"), ZoneOffset.UTC));
        CustomerCredentialsChangedEventListener listener =
                new CustomerCredentialsChangedEventListener(sessionRevocation, metrics);

        listener.revokeSessions(new CustomerCredentialsChangedEvent(10L, 3L));

        assertThat(registry.get("happygallery.customer.session.revocation_failed")
                .counter()
                .count()).isEqualTo(1.0);
    }
}
