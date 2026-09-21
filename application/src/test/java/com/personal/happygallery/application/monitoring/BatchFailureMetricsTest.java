package com.personal.happygallery.application.monitoring;

import com.personal.happygallery.application.batch.BatchResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BatchFailureMetricsTest {
    @Test
    @DisplayName("부분 실패는 항목별 시각을 유지하고 전체 정상 완료만 마지막 성공을 갱신한다")
    void recordsReasonsUntilSuccessfulRun() {
        var registry = new SimpleMeterRegistry();
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.ofEpochSecond(1000));
        var metrics = new AppMetrics(registry, clock);
        String job = "personal_data_retention";
        metrics.recordBatchResult(job, BatchResult.of(2, Map.of("image_media", 1, "notification_log", 1)), 100);
        assertThat(registry.get("happygallery.batch.last_failure").tag("reason", "image_media").gauge().value()).isEqualTo(1000);
        assertThat(registry.get("happygallery.batch.last_failure").tag("reason", "notification_log").gauge().value()).isEqualTo(1000);
        assertThat(registry.get("happygallery.batch.last_success").tag("job", job).gauge().value()).isZero();
        when(clock.instant()).thenReturn(Instant.ofEpochSecond(2000));
        metrics.recordBatchResult(job, BatchResult.successOnly(0), 100);
        assertThat(registry.get("happygallery.batch.last_success").tag("job", job).gauge().value()).isEqualTo(2000);
        assertThat(registry.get("happygallery.batch.last_failure").tag("reason", "image_media").gauge().value()).isEqualTo(1000);
    }

    @Test
    @DisplayName("배치 전체 예외도 실패 시각과 고정된 실행 실패 사유를 남긴다")
    void recordsExecutionFailure() {
        var registry = new SimpleMeterRegistry();
        var metrics = new AppMetrics(registry, Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC));
        metrics.recordBatchFailure("personal_data_retention", 100);
        assertThat(registry.get("happygallery.batch.last_failure").tag("reason", "execution").gauge().value()).isEqualTo(1000);
        assertThat(registry.get("happygallery.batch.last_success").tag("job", "personal_data_retention").gauge().value()).isZero();
    }
}
