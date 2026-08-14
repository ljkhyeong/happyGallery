package com.personal.happygallery.adapter.out.persistence.media;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.personal.happygallery.adapter.out.persistence.booking.ClassRepository;
import com.personal.happygallery.adapter.out.persistence.event.EventRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;
import com.personal.happygallery.application.media.port.out.ImageMediaReferenceReaderPort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingClassStatus;
import com.personal.happygallery.domain.event.Event;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@UseCaseIT
class ImagePublicReferenceUseCaseIT {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 14, 21, 0);
    private static final String ACTIVE_PRODUCT_IMAGE = imageUrl("000000000001");
    private static final String INACTIVE_PRODUCT_IMAGE = imageUrl("000000000002");
    private static final String ACTIVE_CLASS_IMAGE = imageUrl("000000000003");
    private static final String INACTIVE_CLASS_IMAGE = imageUrl("000000000004");
    private static final String UPCOMING_EVENT_IMAGE = imageUrl("000000000005");
    private static final String DRAFT_EVENT_IMAGE = imageUrl("000000000006");
    private static final String EXPIRED_EVENT_IMAGE = imageUrl("000000000007");

    @Autowired ImageMediaReferenceReaderPort referenceReader;
    @Autowired ProductRepository productRepository;
    @Autowired ClassRepository classRepository;
    @Autowired EventRepository eventRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        eventRepository.deleteAllInBatch();
        classRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("카탈로그 이미지는 기존 query 참조를 포함해 공개 상태와 서울 종료 경계를 따른다")
    void classifyCatalogReferencesAtApplicationClockBoundary() {
        Product activeProduct =
                productRepository.saveAndFlush(product("공개 상품", ACTIVE_PRODUCT_IMAGE, true));
        productRepository.saveAndFlush(product("비활성 상품", INACTIVE_PRODUCT_IMAGE, false));
        jdbcTemplate.update(
                "UPDATE products SET image_url = ? WHERE id = ?",
                ACTIVE_PRODUCT_IMAGE + "?v=legacy#preview",
                activeProduct.getId());

        classRepository.saveAndFlush(bookingClass("공개 클래스", ACTIVE_CLASS_IMAGE, true));
        classRepository.saveAndFlush(bookingClass("비활성 클래스", INACTIVE_CLASS_IMAGE, false));

        eventRepository.saveAndFlush(event(
                "예정 이벤트", UPCOMING_EVENT_IMAGE, NOW.plusDays(1), NOW.plusDays(2), true));
        eventRepository.saveAndFlush(event(
                "미게시 이벤트", DRAFT_EVENT_IMAGE, NOW.minusDays(1), NOW.plusDays(1), false));
        eventRepository.saveAndFlush(event(
                "종료 이벤트", EXPIRED_EVENT_IMAGE, NOW.minusDays(2), NOW, true));

        assertSoftly(softly -> {
            softly.assertThat(isPublic(ACTIVE_PRODUCT_IMAGE, NOW)).isTrue();
            softly.assertThat(isPublic(INACTIVE_PRODUCT_IMAGE, NOW)).isFalse();
            softly.assertThat(isPublic(ACTIVE_CLASS_IMAGE, NOW)).isTrue();
            softly.assertThat(isPublic(INACTIVE_CLASS_IMAGE, NOW)).isFalse();
            softly.assertThat(isPublic(UPCOMING_EVENT_IMAGE, NOW)).isTrue();
            softly.assertThat(isPublic(DRAFT_EVENT_IMAGE, NOW)).isFalse();
            softly.assertThat(isPublic(EXPIRED_EVENT_IMAGE, NOW.minusNanos(1_000))).isTrue();
            softly.assertThat(isPublic(EXPIRED_EVENT_IMAGE, NOW)).isFalse();
            softly.assertThat(isPublic(EXPIRED_EVENT_IMAGE, NOW.plusNanos(1_000))).isFalse();
            softly.assertThat(isPublic(imageUrl("000000000099"), NOW)).isFalse();
        });
    }

    private boolean isPublic(String imageUrl, LocalDateTime now) {
        return referenceReader.isPubliclyReferenced(imageUrl, now);
    }

    private static Product product(String name, String imageUrl, boolean active) {
        Product product = new Product(
                name, ProductType.READY_STOCK, "WOOD", 30_000L, null, imageUrl);
        if (!active) {
            product.deactivate();
        }
        return product;
    }

    private static BookingClass bookingClass(String name, String imageUrl, boolean active) {
        BookingClass bookingClass = new BookingClass(
                name,
                "WOOD",
                120,
                50_000L,
                30,
                true,
                null,
                imageUrl,
                null,
                null);
        if (!active) {
            bookingClass.changeStatus(BookingClassStatus.INACTIVE);
        }
        return bookingClass;
    }

    private static Event event(
            String title,
            String imageUrl,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean published
    ) {
        return new Event(
                title,
                title + " 요약",
                title + " 내용",
                imageUrl,
                startAt,
                endAt,
                published,
                false,
                Set.of());
    }

    private static String imageUrl(String suffix) {
        return "/api/v1/media/images/11111111-1111-4111-8111-" + suffix + ".jpg";
    }
}
