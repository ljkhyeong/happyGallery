package com.personal.happygallery.application.media;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import com.personal.happygallery.application.media.port.out.ImageMediaStoragePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DefaultImageMediaService implements ImageMediaUseCase {

    public static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");
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
        String extension = EXTENSIONS.get(normalizedType);
        if (extension == null || !matchesSignature(bytes, normalizedType)) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "JPEG, PNG, WebP 이미지 파일만 업로드할 수 있습니다.");
        }
        String fileName = UUID.randomUUID() + "." + extension;
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

    private static boolean matchesSignature(byte[] bytes, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && Byte.toUnsignedInt(bytes[0]) == 0xFF
                    && Byte.toUnsignedInt(bytes[1]) == 0xD8
                    && Byte.toUnsignedInt(bytes[2]) == 0xFF;
            case "image/png" -> bytes.length >= 8
                    && Byte.toUnsignedInt(bytes[0]) == 0x89
                    && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                    && Byte.toUnsignedInt(bytes[4]) == 0x0D
                    && Byte.toUnsignedInt(bytes[5]) == 0x0A
                    && Byte.toUnsignedInt(bytes[6]) == 0x1A
                    && Byte.toUnsignedInt(bytes[7]) == 0x0A;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    private static String contentType(String fileName) {
        if (fileName.endsWith(".jpg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        return "image/webp";
    }
}
