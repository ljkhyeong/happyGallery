package com.personal.happygallery.application.media;

import com.personal.happygallery.domain.error.HappyGalleryException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UntrustedImageSanitizerTest {

    private final UntrustedImageSanitizer sanitizer = new UntrustedImageSanitizer();

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
}
