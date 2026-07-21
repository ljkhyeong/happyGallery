package com.personal.happygallery.adapter.out.persistence.media;

import com.personal.happygallery.application.media.port.out.ImageMediaStoragePort;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class FileSystemImageMediaStorageAdapter implements ImageMediaStoragePort {

    private static final Pattern STORED_IMAGE_NAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");
    private static final String ORPHAN_MARKER_DIRECTORY = ".orphaned";

    private final Path storageDirectory;
    private final Path orphanMarkerDirectory;

    public FileSystemImageMediaStorageAdapter(MediaStorageProperties properties) {
        this.storageDirectory = Path.of(properties.storagePath()).toAbsolutePath().normalize();
        this.orphanMarkerDirectory = storageDirectory.resolve(ORPHAN_MARKER_DIRECTORY);
        try {
            Files.createDirectories(storageDirectory);
            Files.createDirectories(orphanMarkerDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("이미지 저장 디렉터리를 만들 수 없습니다.", e);
        }
    }

    @Override
    public void store(String fileName, byte[] bytes) {
        Path target = resolve(fileName);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(storageDirectory, "upload-", ".tmp");
            Files.write(temporary, bytes);
            moveIntoPlace(temporary, target);
        } catch (IOException e) {
            throw new UncheckedIOException("이미지를 저장할 수 없습니다.", e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // 원본 실패를 보존하고 임시 파일은 운영 정리 대상으로 남긴다.
                }
            }
        }
    }

    private static void moveIntoPlace(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, target);
        }
    }

    @Override
    public Optional<byte[]> read(String fileName) {
        Path target = resolve(fileName);
        if (!Files.isRegularFile(target)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(target));
        } catch (IOException e) {
            throw new UncheckedIOException("이미지를 읽을 수 없습니다.", e);
        }
    }

    @Override
    public boolean exists(String fileName) {
        return STORED_IMAGE_NAME.matcher(fileName).matches()
                && Files.isRegularFile(resolve(fileName), LinkOption.NOFOLLOW_LINKS);
    }

    @Override
    public List<String> findStoredImageNames() {
        try (Stream<Path> paths = Files.list(storageDirectory)) {
            return paths
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> STORED_IMAGE_NAME.matcher(path.getFileName().toString()).matches())
                    .map(path -> path.getFileName().toString())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("저장된 이미지 목록을 읽을 수 없습니다.", e);
        }
    }

    @Override
    public boolean markOrphanCandidate(String fileName, Instant observedAt, Duration gracePeriod) {
        Path marker = orphanMarker(fileName);
        try {
            try {
                Files.createFile(marker);
                Files.setLastModifiedTime(marker, FileTime.from(observedAt));
                return false;
            } catch (FileAlreadyExistsException ignored) {
                return !lastModified(marker).isAfter(observedAt.minus(gracePeriod));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("고아 이미지 관찰 시각을 기록할 수 없습니다.", e);
        }
    }

    @Override
    public void clearOrphanMarker(String fileName) {
        try {
            Files.deleteIfExists(orphanMarker(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException("고아 이미지 표시를 제거할 수 없습니다.", e);
        }
    }

    @Override
    public void delete(String fileName) {
        try {
            Files.deleteIfExists(resolve(fileName));
            Files.deleteIfExists(orphanMarker(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException("이미지를 삭제할 수 없습니다.", e);
        }
    }

    @Override
    public long usedBytes() {
        try (Stream<Path> paths = Files.list(storageDirectory)) {
            return paths
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .mapToLong(FileSystemImageMediaStorageAdapter::size)
                    .sum();
        } catch (IOException e) {
            throw new UncheckedIOException("이미지 저장소 사용량을 계산할 수 없습니다.", e);
        }
    }

    private static Instant lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toInstant();
        } catch (IOException e) {
            throw new UncheckedIOException("이미지 수정 시각을 읽을 수 없습니다.", e);
        }
    }

    private static long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new UncheckedIOException("이미지 크기를 읽을 수 없습니다.", e);
        }
    }

    private Path resolve(String fileName) {
        Path resolved = storageDirectory.resolve(fileName).normalize();
        if (!resolved.getParent().equals(storageDirectory)) {
            throw new IllegalArgumentException("허용되지 않은 이미지 경로입니다.");
        }
        return resolved;
    }

    private Path orphanMarker(String fileName) {
        if (!STORED_IMAGE_NAME.matcher(fileName).matches()) {
            throw new IllegalArgumentException("허용되지 않은 이미지 파일명입니다.");
        }
        return orphanMarkerDirectory.resolve(fileName);
    }
}
