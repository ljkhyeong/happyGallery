package com.personal.happygallery.application.media;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import com.personal.happygallery.application.media.port.out.ImageMediaStoragePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DefaultImageMediaService implements ImageMediaUseCase {

    public static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    private static final Set<FileType> SUPPORTED_FILE_TYPES = Set.of(
            FileType.Jpeg,
            FileType.Png,
            FileType.WebP);
    private static final Pattern FILE_NAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    private final ImageMediaStoragePort storagePort;

    public DefaultImageMediaService(ImageMediaStoragePort storagePort) {
        this.storagePort = storagePort;
    }

    @Override
    public StoredImage upload(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이미지는 5MB 이하여야 합니다.");
        }
        String normalizedType = Objects.requireNonNullElse(contentType, "")
                .toLowerCase(Locale.ROOT);
        FileType detectedType = detectFileType(bytes);
        if (!SUPPORTED_FILE_TYPES.contains(detectedType)
                || !detectedType.getMimeType().equals(normalizedType)) {
            throw invalidImage();
        }
        String fileName = UUID.randomUUID() + "." + detectedType.getCommonExtension();
        storagePort.store(fileName, bytes);
        return new StoredImage(fileName, "/api/v1/media/images/" + fileName);
    }

    @Override
    public ImageContent get(String fileName) {
        if (fileName == null || !FILE_NAME.matcher(fileName).matches()) {
            throw new NotFoundException("이미지");
        }
        byte[] bytes = storagePort.read(fileName).orElseThrow(NotFoundException.supplier("이미지"));
        return new ImageContent(bytes, contentType(fileName));
    }

    private static FileType detectFileType(byte[] bytes) {
        try {
            return FileTypeDetector.detectFileType(new ByteArrayInputStream(bytes));
        } catch (IOException ignored) {
            throw invalidImage();
        }
    }

    private static String contentType(String fileName) {
        if (fileName.endsWith(".jpg")) return FileType.Jpeg.getMimeType();
        if (fileName.endsWith(".png")) return FileType.Png.getMimeType();
        return FileType.WebP.getMimeType();
    }

    private static HappyGalleryException invalidImage() {
        return new HappyGalleryException(
                ErrorCode.INVALID_INPUT, "JPEG, PNG, WebP 이미지 파일만 업로드할 수 있습니다.");
    }
}
