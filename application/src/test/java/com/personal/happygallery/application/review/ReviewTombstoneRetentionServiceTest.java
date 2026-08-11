package com.personal.happygallery.application.review;

import com.personal.happygallery.application.review.port.out.ReviewTombstoneRetentionPort;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewTombstoneRetentionServiceTest {

    @Test
    @DisplayName("증거와 재작성 차단이 없는 오래된 후기 tombstone만 배치 삭제한다")
    void deleteBatchBefore_deletesBoundedCandidates() {
        ReviewTombstoneRetentionPort port = mock(ReviewTombstoneRetentionPort.class);
        ReviewTombstoneRetentionService service = new ReviewTombstoneRetentionService(port);
        LocalDateTime cutoff = LocalDateTime.of(2026, 7, 1, 0, 0);
        when(port.deleteUnblockedBefore(cutoff, 100)).thenReturn(2);

        int deleted = service.deleteBatchBefore(cutoff, 100);

        assertThat(deleted).isEqualTo(2);
        verify(port).deleteUnblockedBefore(cutoff, 100);
    }

    @Test
    @DisplayName("후기 tombstone 배치 크기는 1 이상이어야 한다")
    void deleteBatchBefore_rejectsNonPositiveLimit() {
        ReviewTombstoneRetentionService service = new ReviewTombstoneRetentionService(
                mock(ReviewTombstoneRetentionPort.class));

        assertThatThrownBy(() -> service.deleteBatchBefore(LocalDateTime.now(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
