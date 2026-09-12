package com.personal.happygallery.adapter.out.persistence.media;

import com.personal.happygallery.application.batch.port.out.BatchExecutionLeasePort;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 단일 노드의 공유 PVC에서 프로세스 종료 시 자동 해제되는 작업별 잠금을 사용한다. */
@Component
public class FileSystemBatchExecutionLeaseAdapter implements BatchExecutionLeasePort {
    private final Path storage;

    public FileSystemBatchExecutionLeaseAdapter(MediaStorageProperties properties) {
        storage = Path.of(properties.storagePath()).toAbsolutePath().normalize();
    }

    @Override
    public Optional<Lease> tryAcquire(String job) {
        FileChannel channel = null;
        try {
            if (deploymentInProgress()) {
                return Optional.empty();
            }
            Path directory = Files.createDirectories(storage.resolve(".scheduler-locks"));
            String name = UUID.nameUUIDFromBytes(job.getBytes(StandardCharsets.UTF_8)).toString();
            channel = FileChannel.open(directory.resolve(name), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            var lock = channel.tryLock();
            if (lock == null || deploymentInProgress()) {
                channel.close();
                return Optional.empty();
            }
            FileChannel acquired = channel;
            return Optional.of(() -> {
                try {
                    acquired.close();
                } catch (IOException exception) {
                    throw new UncheckedIOException("배치 실행 잠금을 해제하지 못했습니다.", exception);
                }
            });
        } catch (OverlappingFileLockException exception) {
            close(channel);
            return Optional.empty();
        } catch (IOException exception) {
            close(channel);
            throw new UncheckedIOException("배치 실행 잠금을 확인하지 못했습니다.", exception);
        }
    }

    private boolean deploymentInProgress() throws IOException {
        try {
            Files.readAttributes(storage.resolve(".deployment-in-progress"),
                    BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            return true;
        } catch (NoSuchFileException exception) {
            return false;
        }
    }

    private static void close(FileChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException exception) {
                throw new UncheckedIOException("배치 잠금 채널을 닫지 못했습니다.", exception);
            }
        }
    }
}
