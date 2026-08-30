package com.personal.happygallery.adapter.out.persistence.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemImageMediaStorageAdapterTest {

    private static final String FILE_NAME = "11111111-1111-1111-1111-111111111111.png";

    @TempDir
    Path temporaryDirectory;

    @DisplayName("고아 이미지 마커는 최초 관찰 후 7일이 지나야 삭제 가능 상태가 된다")
    @Test
    void orphanMarkerBecomesReadyAfterGracePeriod() {
        FileSystemImageMediaStorageAdapter storage = new FileSystemImageMediaStorageAdapter(
                new MediaStorageProperties(temporaryDirectory.toString()));
        Instant observedAt = Instant.parse("2026-07-21T00:00:00Z");
        Duration gracePeriod = Duration.ofDays(7);
        storage.store(FILE_NAME, new byte[] {1, 2, 3});

        assertThat(storage.findStoredImageNames()).containsExactly(FILE_NAME);
        assertThat(storage.markOrphanCandidate(FILE_NAME, observedAt, gracePeriod)).isFalse();
        assertThat(storage.markOrphanCandidate(
                FILE_NAME, observedAt.plus(gracePeriod).minusSeconds(1), gracePeriod)).isFalse();
        assertThat(storage.markOrphanCandidate(FILE_NAME, observedAt.plus(gracePeriod), gracePeriod)).isTrue();

        storage.clearOrphanMarker(FILE_NAME);

        assertThat(storage.markOrphanCandidate(FILE_NAME, observedAt.plus(gracePeriod), gracePeriod)).isFalse();
    }
}
