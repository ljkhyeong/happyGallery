package com.personal.happygallery.adapter.out.persistence.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemBatchExecutionLeaseAdapterTest {
    @TempDir Path directory;

    private FileSystemBatchExecutionLeaseAdapter adapter() {
        return new FileSystemBatchExecutionLeaseAdapter(new MediaStorageProperties(directory.toString()));
    }

    @Test
    @DisplayName("같은 작업의 중복 실행을 막고 잠금 해제 후 다음 실행을 허용한다")
    void excludesSameJobUntilClosed() {
        var first = adapter();
        var second = adapter();
        try (var lease = first.tryAcquire("reminder").orElseThrow()) {
            assertThat(second.tryAcquire("reminder")).isEmpty();
            try (var otherJob = second.tryAcquire("refund").orElseThrow()) {
                assertThat(otherJob).isNotNull();
            }
        }
        try (var next = second.tryAcquire("reminder").orElseThrow()) {
            assertThat(next).isNotNull();
        }
    }

    @Test
    @DisplayName("배포 표식이 있으면 새 배치를 막고 표식 제거 후 재개한다")
    void pausesDuringDeployment() throws Exception {
        Path marker = Files.createDirectory(directory.resolve(".deployment-in-progress"));
        assertThat(adapter().tryAcquire("reminder")).isEmpty();
        Files.delete(marker);
        try (var resumed = adapter().tryAcquire("reminder").orElseThrow()) {
            assertThat(resumed).isNotNull();
        }
    }

    @Test
    @DisplayName("공유 저장소에 오류가 있으면 보호 없이 배치를 실행하지 않는다")
    void storageFailureIsClosed() throws Exception {
        Files.createFile(directory.resolve(".scheduler-locks"));
        assertThatThrownBy(() -> adapter().tryAcquire("reminder")).isInstanceOf(UncheckedIOException.class);
    }
}
