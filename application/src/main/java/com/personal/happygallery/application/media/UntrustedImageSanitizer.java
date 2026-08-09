package com.personal.happygallery.application.media;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.stereotype.Component;

/** 회원이 올린 이미지를 디코딩하고 메타데이터 없는 안전한 이미지로 다시 만든다. */
@Component
public class UntrustedImageSanitizer {

    static final int MAX_DIMENSION = 4_096;
    static final long MAX_PIXELS = 16_000_000L;

    public SanitizedImage sanitize(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length == 0
                || bytes.length > DefaultImageMediaService.MAX_IMAGE_BYTES) {
            throw invalid("이미지는 5MB 이하여야 합니다.");
        }

        String normalizedType = contentType == null
                ? ""
                : contentType.toLowerCase(Locale.ROOT);
        ImageFormat format = ImageFormat.fromContentType(normalizedType);

        try (ImageInputStream input = new MemoryCacheImageInputStream(
                new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalid("손상되었거나 지원하지 않는 이미지입니다.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                if (!format.matches(reader.getFormatName())) {
                    throw invalid("이미지 형식과 MIME 타입이 일치하지 않습니다.");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height);
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw invalid("이미지를 디코딩할 수 없습니다.");
                }
                return new SanitizedImage(
                        encode(decoded, format),
                        format.contentType);
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw invalid("손상되었거나 지원하지 않는 이미지입니다.");
        }
    }

    private static void validateDimensions(int width, int height) {
        long pixels = (long) width * height;
        if (width < 1 || height < 1
                || width > MAX_DIMENSION || height > MAX_DIMENSION
                || pixels > MAX_PIXELS) {
            throw invalid("이미지 크기는 가로·세로 4096px, 총 1600만 픽셀 이하여야 합니다.");
        }
    }

    private static byte[] encode(BufferedImage decoded, ImageFormat format) throws IOException {
        BufferedImage image = format == ImageFormat.JPEG ? rgbImage(decoded) : decoded;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, format.writerName, output)) {
            throw invalid("이미지를 안전한 형식으로 변환할 수 없습니다.");
        }
        return output.toByteArray();
    }

    private static BufferedImage rgbImage(BufferedImage source) {
        BufferedImage target = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, target.getWidth(), target.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private static HappyGalleryException invalid(String message) {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT, message);
    }

    public record SanitizedImage(byte[] bytes, String contentType) {
        public SanitizedImage {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }

    private enum ImageFormat {
        JPEG("image/jpeg", "JPEG", "jpg"),
        PNG("image/png", "PNG", "png");

        private final String contentType;
        private final String readerName;
        private final String writerName;

        ImageFormat(String contentType, String readerName, String writerName) {
            this.contentType = contentType;
            this.readerName = readerName;
            this.writerName = writerName;
        }

        private static ImageFormat fromContentType(String contentType) {
            for (ImageFormat format : values()) {
                if (format.contentType.equals(contentType)) {
                    return format;
                }
            }
            throw invalid("후기 사진은 JPEG 또는 PNG만 업로드할 수 있습니다.");
        }

        private boolean matches(String actualFormatName) {
            return readerName.equalsIgnoreCase(actualFormatName);
        }
    }
}
