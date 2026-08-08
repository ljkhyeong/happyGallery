package com.personal.happygallery.policy;

import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.media.ImageReferencePolicy;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("policy")
class ImageReferencePolicyTest {

    @DisplayName("이미지 참조는 서비스 경로와 호스트가 있는 HTTP 주소를 허용하고 공백을 정규화한다")
    @Test
    void imageReference_acceptsServicePathAndHttpUrl() {
        assertSoftly(softly -> {
            softly.assertThat(ImageReferencePolicy.optional("  /api/v1/media/images/class.jpg  "))
                    .isEqualTo("/api/v1/media/images/class.jpg");
            softly.assertThat(ImageReferencePolicy.optional("https://images.example.com/product.jpg"))
                    .isEqualTo("https://images.example.com/product.jpg");
            softly.assertThat(ImageReferencePolicy.optional("   ")).isNull();
        });
    }

    @DisplayName("상품과 클래스는 스킴이 없거나 호스트가 없는 외부 이미지 주소를 거절한다")
    @Test
    void catalogAggregates_rejectInvalidExternalImageReferences() {
        assertInvalidInput(() -> new Product(
                "상품", ProductType.READY_STOCK, "소품", 10_000L,
                null, "javascript:alert(1)"));
        assertInvalidInput(() -> new BookingClass(
                "클래스", "CRAFT", 60, 30_000L, 30,
                true, null, "https:/missing-host/image.jpg", null, null));
        assertInvalidInput(() -> ImageReferencePolicy.optional("//images.example.com/image.jpg"));
        assertInvalidInput(() -> ImageReferencePolicy.optional("///images.example.com/image.jpg"));
    }

    @DisplayName("상품과 클래스는 같은 이미지 참조 정책으로 서비스 경로를 저장한다")
    @Test
    void catalogAggregates_shareImageReferencePolicy() {
        Product product = new Product(
                "상품", ProductType.READY_STOCK, "소품", 10_000L,
                null, "/api/v1/media/images/product.jpg");
        BookingClass bookingClass = new BookingClass(
                "클래스", "CRAFT", 60, 30_000L, 30,
                true, null, "/api/v1/media/images/class.jpg", null, null);

        assertThat(product.getImageUrl()).isEqualTo("/api/v1/media/images/product.jpg");
        assertThat(bookingClass.getImageUrl()).isEqualTo("/api/v1/media/images/class.jpg");
    }

    private void assertInvalidInput(Runnable command) {
        assertThatThrownBy(command::run)
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
