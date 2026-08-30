package com.personal.happygallery.policy;

import com.personal.happygallery.domain.product.SmartStoreStockSync;
import com.personal.happygallery.domain.product.SmartStoreStockSyncStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("policy")
class SmartStoreStockSyncPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 29, 12, 0);

    @Test
    @DisplayName("처리 중 재고가 다시 바뀌면 완료 결과로 덮지 않고 최신 버전을 대기시킨다")
    void complete_whenNewRequestExists_keepsLatestRequestPending() {
        SmartStoreStockSync sync = new SmartStoreStockSync(1L, NOW);
        long claimedVersion = sync.claim(NOW, NOW.minusMinutes(5));

        sync.request(NOW.plusSeconds(1));
        sync.complete(sync.getGeneration(), claimedVersion, NOW.plusSeconds(2));

        assertSoftly(softly -> {
            softly.assertThat(sync.getRequestVersion()).isEqualTo(2);
            softly.assertThat(sync.getStatus()).isEqualTo(SmartStoreStockSyncStatus.PENDING);
            softly.assertThat(sync.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(2));
        });
    }

    @Test
    @DisplayName("외부 연동 실패는 재시도 간격을 늘리고 열 번째 실패에서 운영 확인 대상으로 남긴다")
    void fail_retriesWithBackoff_thenStopsAfterMaximumAttempts() {
        SmartStoreStockSync sync = new SmartStoreStockSync(1L, NOW);

        for (int attempt = 1; attempt <= SmartStoreStockSync.MAX_ATTEMPTS; attempt++) {
            long version = sync.claim(NOW.plusMinutes(attempt), NOW.minusMinutes(5));
            sync.fail(sync.getGeneration(), version, "스마트스토어 연결 실패", NOW.plusMinutes(attempt));
        }

        assertThat(sync.getStatus()).isEqualTo(SmartStoreStockSyncStatus.FAILED);
        assertThat(sync.getAttemptCount()).isEqualTo(SmartStoreStockSync.MAX_ATTEMPTS);
        assertThat(sync.getLastError()).isEqualTo("스마트스토어 연결 실패");
    }
}
