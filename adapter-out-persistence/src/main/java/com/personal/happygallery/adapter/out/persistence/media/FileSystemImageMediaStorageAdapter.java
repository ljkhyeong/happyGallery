package com.personal.happygallery.adapter.out.persistence.media;

import com.personal.happygallery.application.media.port.out.ImageMediaStoragePort;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class FileSystemImageMediaStorageAdapter implements ImageMediaStoragePort {

    private final Path storageDirectory;

    public FileSystemImageMediaStorageAdapter(MediaStorageProperties properties) {
        this.storageDirectory = Path.of(properties.storagePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageDirectory);
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

    private Path resolve(String fileName) {
        Path resolved = storageDirectory.resolve(fileName).normalize();
        if (!resolved.getParent().equals(storageDirectory)) {
            throw new IllegalArgumentException("허용되지 않은 이미지 경로입니다.");
        }
        return resolved;
    }
}
