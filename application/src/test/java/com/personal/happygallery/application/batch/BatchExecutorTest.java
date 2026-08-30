package com.personal.happygallery.application.batch;

import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BatchExecutorTest {

    @Test
    @DisplayName("앞 페이지가 모두 실패해도 다음 ID 페이지를 계속 처리한다")
    void continuesAfterFullyFailedPage() {
        BatchResult result = BatchExecutor.executeByIdCursor(
                afterId -> fetchPage(afterId),
                Candidate::id,
                candidate -> {
                    if (candidate.id() <= 100) {
                        throw new IllegalStateException("처리 실패");
                    }
                    return true;
                },
                "테스트 배치");

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(100);
    }

    private static List<Candidate> fetchPage(long afterId) {
        if (afterId == 0) {
            return LongStream.rangeClosed(1, 100)
                    .mapToObj(Candidate::new)
                    .toList();
        }
        if (afterId == 100) {
            return List.of(new Candidate(101));
        }
        return List.of();
    }

    private record Candidate(long id) {}
}
