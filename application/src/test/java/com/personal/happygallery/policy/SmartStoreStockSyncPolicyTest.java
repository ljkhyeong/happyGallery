package com.personal.happygallery.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.personal.happygallery.domain.product.SmartStoreStockSync;
import com.personal.happygallery.domain.product.SmartStoreStockSyncStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
            softly.assertThat(sync.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(1));
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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("이전 세대와 요청 버전의 응답이 교차해도 마지막 보정 전송을 남긴다")
    void supersededResponses_keepFinalCorrectionPending(boolean success) {
        SmartStoreStockSync sync = new SmartStoreStockSync(1L, NOW);
        String currentGeneration = sync.getGeneration();
        long firstVersion = sync.claim(NOW, NOW.minusMinutes(5));

        sync.complete("previous-generation", firstVersion, NOW.plusSeconds(1));
        long correctionVersion = sync.claim(NOW.plusSeconds(2), NOW.minusMinutes(5));
        finish(sync, currentGeneration, firstVersion, success, NOW.plusSeconds(3));
        sync.complete("previous-generation", firstVersion, NOW.plusSeconds(4));
        finish(sync, currentGeneration, correctionVersion, success, NOW.plusSeconds(5));

        assertSoftly(softly -> {
            softly.assertThat(sync.getRequestVersion()).isEqualTo(3);
            softly.assertThat(sync.getStatus()).isEqualTo(SmartStoreStockSyncStatus.PENDING);
            softly.assertThat(sync.getAttemptCount()).isZero();
            softly.assertThat(sync.getLastError()).isNull();
        });
        long finalVersion = sync.claim(NOW.plusSeconds(6), NOW.minusMinutes(5));
        sync.complete(currentGeneration, finalVersion, NOW.plusSeconds(7));
        assertThat(sync.getStatus()).isEqualTo(SmartStoreStockSyncStatus.SYNCED);
    }

    private static void finish(
            SmartStoreStockSync sync,
            String generation,
            long version,
            boolean success,
            LocalDateTime now) {
        if (success) {
            sync.complete(generation, version, now);
        } else {
            sync.fail(generation, version, "이전 전송 실패", now);
        }
    }
}
