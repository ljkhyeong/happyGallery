package com.personal.happygallery.application.media;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UntrustedImageSanitizerTest {

    private final UntrustedImageSanitizer sanitizer =
            new UntrustedImageSanitizer(new ReviewImageProcessingProperties(2));

    @Test
    @DisplayName("회원 PNG 이미지는 디코딩한 뒤 원본 부가 데이터를 제거해 다시 인코딩한다")
    void sanitizePngAndRemoveOriginalPayload() throws Exception {
        BufferedImage source = new BufferedImage(8, 6, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        ImageIO.write(source, "png", original);
        original.write("EXIF_GPS_PRIVATE".getBytes(StandardCharsets.ISO_8859_1));

        UntrustedImageSanitizer.SanitizedImage sanitized =
                sanitizer.sanitize(original.toByteArray(), "image/png");

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(sanitized.bytes()));
        assertThat(sanitized.contentType()).isEqualTo("image/png");
        assertThat(decoded.getWidth()).isEqualTo(8);
        assertThat(decoded.getHeight()).isEqualTo(6);
        assertThat(new String(sanitized.bytes(), StandardCharsets.ISO_8859_1))
                .doesNotContain("EXIF_GPS_PRIVATE");
    }

    @Test
    @DisplayName("회원 JPEG 이미지는 RGB 파일로 다시 인코딩한다")
    void sanitizeJpeg() throws Exception {
        BufferedImage source = new BufferedImage(7, 5, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        ImageIO.write(source, "jpg", original);

        UntrustedImageSanitizer.SanitizedImage sanitized =
                sanitizer.sanitize(original.toByteArray(), "image/jpeg");

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(sanitized.bytes()));
        assertThat(sanitized.contentType()).isEqualTo("image/jpeg");
        assertThat(decoded.getWidth()).isEqualTo(7);
        assertThat(decoded.getHeight()).isEqualTo(5);
    }

    @Test
    @DisplayName("EXIF 방향이 지정된 JPEG는 눈에 보이는 방향으로 회전한 뒤 메타데이터 없이 저장한다")
    void correctExifOrientationBeforeEncoding() throws Exception {
        BufferedImage source = new BufferedImage(7, 5, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        ImageIO.write(source, "jpg", jpeg);
        byte[] oriented = withExifOrientation(jpeg.toByteArray(), 6);

        UntrustedImageSanitizer.SanitizedImage sanitized =
                sanitizer.sanitize(oriented, "image/jpeg");

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(sanitized.bytes()));
        assertThat(decoded.getWidth()).isEqualTo(5);
        assertThat(decoded.getHeight()).isEqualTo(7);
        Metadata metadata = ImageMetadataReader.readMetadata(
                new ByteArrayInputStream(sanitized.bytes()));
        assertThat(metadata.getFirstDirectoryOfType(ExifIFD0Directory.class)).isNull();
    }

    @Test
    @DisplayName("MIME 선언이 없거나 범용 바이너리여도 실제 디코딩 형식으로 정제한다")
    void useDecodedFormatWhenMimeIsUnspecified() throws Exception {
        BufferedImage source = new BufferedImage(4, 3, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(source, "png", png);

        UntrustedImageSanitizer.SanitizedImage blank =
                sanitizer.sanitize(png.toByteArray(), "  ");
        UntrustedImageSanitizer.SanitizedImage binary =
                sanitizer.sanitize(png.toByteArray(), "application/octet-stream");

        assertThat(blank.contentType()).isEqualTo("image/png");
        assertThat(binary.contentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("명시한 JPEG 또는 PNG MIME이 실제 디코딩 형식과 다르면 거절한다")
    void rejectExplicitMimeMismatch() throws Exception {
        BufferedImage source = new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        ImageIO.write(source, "jpg", jpeg);

        assertThatThrownBy(() -> sanitizer.sanitize(jpeg.toByteArray(), "image/png"))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("일치하지 않습니다");
    }

    @Test
    @DisplayName("동시 디코딩 슬롯이 모두 사용 중이면 기다리지 않고 처리율 제한으로 거절한다")
    void rejectImmediatelyWhenDecodeCapacityIsExhausted() throws Exception {
        Semaphore permits = new Semaphore(1);
        permits.acquire();
        UntrustedImageSanitizer saturated = new UntrustedImageSanitizer(permits);
        BufferedImage source = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(source, "png", png);

        try {
            assertThatThrownBy(() -> saturated.sanitize(png.toByteArray(), "image/png"))
                    .isInstanceOfSatisfying(
                            HappyGalleryException.class,
                            exception -> assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.TOO_MANY_REQUESTS));
        } finally {
            permits.release();
        }
    }

    @Test
    @DisplayName("후기 이미지는 검증 가능한 표준 디코더가 없는 WebP를 거절한다")
    void rejectWebpAtMemberBoundary() {
        byte[] webp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

        assertThatThrownBy(() -> sanitizer.sanitize(webp, "image/webp"))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("JPEG 또는 PNG");
    }

    @Test
    @DisplayName("압축 크기가 작아도 허용한 픽셀 경계를 넘는 이미지는 거절한다")
    void rejectOversizedDimensionsBeforeFullDecode() throws Exception {
        BufferedImage source = new BufferedImage(
                UntrustedImageSanitizer.MAX_DIMENSION + 1,
                1,
                BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        ImageIO.write(source, "png", original);

        assertThatThrownBy(() -> sanitizer.sanitize(original.toByteArray(), "image/png"))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("4096px");
    }

    private static byte[] withExifOrientation(byte[] jpeg, int orientation) throws Exception {
        byte[] exif = {
                'E', 'x', 'i', 'f', 0, 0,
                'M', 'M', 0, 42,
                0, 0, 0, 8,
                0, 1,
                1, 18,
                0, 3,
                0, 0, 0, 1,
                0, (byte) orientation, 0, 0,
                0, 0, 0, 0
        };
        int segmentLength = exif.length + 2;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(jpeg, 0, 2);
        output.write(0xFF);
        output.write(0xE1);
        output.write(segmentLength >>> 8);
        output.write(segmentLength);
        output.write(exif);
        output.write(jpeg, 2, jpeg.length - 2);
        return output.toByteArray();
    }
}
