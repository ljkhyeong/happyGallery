package com.personal.happygallery.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.personal.happygallery.adapter.out.persistence.coupon.CouponDefinitionRepository;
import com.personal.happygallery.adapter.out.persistence.event.EventRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;
import com.personal.happygallery.application.event.port.in.EventAdminUseCase;
import com.personal.happygallery.application.event.port.in.EventAdminUseCase.CreateCommand;
import com.personal.happygallery.application.event.port.in.EventAdminUseCase.UpdateCommand;
import com.personal.happygallery.application.event.port.in.EventQueryUseCase;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.CouponDiscountType;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.event.Event;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@UseCaseIT
class EventUseCaseIT {

    @Autowired EventAdminUseCase eventAdminUseCase;
    @Autowired EventQueryUseCase eventQueryUseCase;
    @Autowired EventRepository eventRepository;
    @Autowired CouponDefinitionRepository couponDefinitionRepository;
    @Autowired ProductRepository productRepository;
    @Autowired MockMvc mockMvc;
    @Autowired Clock clock;

    @AfterEach
    void tearDown() {
        eventRepository.deleteAllInBatch();
        couponDefinitionRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    @DisplayName("공개 이벤트 목록과 상세는 발행된 진행 및 예정 이벤트만 노출한다")
    @Test
    void publicEvents_exposeOnlyPublishedCurrentAndUpcomingEvents() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        Event current = createEvent(
                "진행 이벤트", now.minusHours(1), now.plusHours(1), true, true, Set.of());
        Event upcoming = createEvent(
                "예정 이벤트", now.plusDays(1), now.plusDays(2), true, true, Set.of());
        Event expired = createEvent(
                "종료 이벤트", now.minusDays(2), now.minusDays(1), true, true, Set.of());
        Event draft = createEvent(
                "미발행 이벤트", now.minusHours(1), now.plusDays(1), false, true, Set.of());

        List<Event> publicEvents = eventQueryUseCase.listPublicEvents();

        assertThat(publicEvents).extracting(Event::getId)
                .containsExactly(current.getId(), upcoming.getId());
        assertThatThrownBy(() -> eventQueryUseCase.getPublicEvent(expired.getId()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> eventQueryUseCase.getPublicEvent(draft.getId()))
                .isInstanceOf(NotFoundException.class);

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$[0].id").value(current.getId()))
                .andExpect(jsonPath("$[0].featured").value(true))
                .andExpect(jsonPath("$[1].id").value(upcoming.getId()))
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/events/{id}", current.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.title").value("진행 이벤트"));
        mockMvc.perform(get("/api/v1/events/{id}", expired.getId()))
                .andExpect(status().isNotFound());
    }

    @DisplayName("공개 이벤트는 종료 시각을 포함하지 않는다")
    @Test
    void publicEvents_excludeExactEndBoundary() {
        LocalDateTime boundary = LocalDateTime.now(clock).plusMinutes(1);
        Event endingAtBoundary = createEvent(
                "경계 종료 이벤트",
                boundary.minusDays(1),
                boundary,
                true,
                true,
                Set.of());

        assertSoftly(softly -> {
            softly.assertThat(eventRepository.findPublicEvents(boundary))
                    .extracting(Event::getId)
                    .doesNotContain(endingAtBoundary.getId());
            softly.assertThat(eventRepository.findPublicById(endingAtBoundary.getId(), boundary))
                    .isEmpty();
        });
    }

    @DisplayName("관리자는 연관 상품 ID와 버전을 포함해 이벤트를 생성 수정 삭제한다")
    @Test
    void adminEventCrud_persistsRelatedProductsAndRejectsStaleVersion() {
        LocalDateTime now = LocalDateTime.now(clock);
        Product firstProduct = productRepository.save(
                new Product("연관 상품 1", ProductType.READY_STOCK, 10_000L));
        Product secondProduct = productRepository.save(
                new Product("연관 상품 2", ProductType.READY_STOCK, 20_000L));
        CouponDefinition coupon = couponDefinitionRepository.saveAndFlush(new CouponDefinition(
                "이벤트 쿠폰",
                CouponDiscountType.FIXED,
                5_000L,
                10_000L,
                null,
                now,
                now.plusDays(30),
                true,
                true));

        Event created = createEvent(
                "여름 이벤트",
                now.plusDays(1),
                now.plusDays(10),
                false,
                true,
                Set.of(secondProduct.getId(), firstProduct.getId()),
                coupon.getId());
        long firstVersion = created.getVersion();

        assertThat(eventAdminUseCase.listAll())
                .singleElement()
                .satisfies(event -> assertThat(event.getRelatedProductIds())
                        .containsExactly(firstProduct.getId(), secondProduct.getId()));

        Event updated = eventAdminUseCase.update(created.getId(), new UpdateCommand(
                firstVersion,
                "여름 이벤트 수정",
                "수정한 요약",
                "수정한 평문 내용",
                "https://images.example.com/summer-event.jpg",
                now.plusDays(2),
                now.plusDays(11),
                true,
                false,
                coupon.getId(),
                Set.of(secondProduct.getId())));

        assertSoftly(softly -> {
            softly.assertThat(updated.getTitle()).isEqualTo("여름 이벤트 수정");
            softly.assertThat(updated.isPublished()).isTrue();
            softly.assertThat(updated.isFeatured()).isFalse();
            softly.assertThat(updated.getRelatedProductIds())
                    .containsExactly(secondProduct.getId());
            softly.assertThat(updated.getCouponDefinitionId()).isEqualTo(coupon.getId());
            softly.assertThat(updated.getVersion()).isEqualTo(firstVersion + 1);
            softly.assertThat(eventAdminUseCase.listAll())
                    .singleElement()
                    .satisfies(event -> softly.assertThat(event.getRelatedProductIds())
                            .containsExactly(secondProduct.getId()));
            softly.assertThat(eventQueryUseCase.listPublicEvents())
                    .singleElement()
                    .satisfies(event -> softly.assertThat(event.getRelatedProductIds())
                            .containsExactly(secondProduct.getId()));
        });
        assertThatThrownBy(() -> eventAdminUseCase.update(created.getId(), new UpdateCommand(
                firstVersion,
                "뒤늦은 수정",
                "요약",
                "내용",
                null,
                now.plusDays(2),
                now.plusDays(11),
                true,
                false,
                coupon.getId(),
                Set.of())))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

        eventAdminUseCase.delete(created.getId(), updated.getVersion());
        assertThat(eventRepository.findById(created.getId())).isEmpty();
    }

    @DisplayName("관리자는 존재하지 않는 상품을 이벤트에 연결할 수 없다")
    @Test
    void createEvent_rejectsMissingRelatedProduct() {
        LocalDateTime now = LocalDateTime.now(clock);

        assertThatThrownBy(() -> createEvent(
                "잘못된 이벤트",
                now.plusDays(1),
                now.plusDays(2),
                false,
                false,
                Set.of(Long.MAX_VALUE)))
                .isInstanceOf(NotFoundException.class);
        assertThat(eventRepository.count()).isZero();
    }

    @DisplayName("관리자는 존재하지 않는 쿠폰을 이벤트에 연결할 수 없다")
    @Test
    void createEvent_rejectsMissingCoupon() {
        LocalDateTime now = LocalDateTime.now(clock);

        assertThatThrownBy(() -> createEvent(
                "잘못된 쿠폰 이벤트",
                now.plusDays(1),
                now.plusDays(2),
                false,
                false,
                Set.of(),
                Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class);
        assertThat(eventRepository.count()).isZero();
    }

    private Event createEvent(
            String title,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean published,
            boolean featured,
            Set<Long> relatedProductIds
    ) {
        return createEvent(
                title, startAt, endAt, published, featured, relatedProductIds, null);
    }

    private Event createEvent(
            String title,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean published,
            boolean featured,
            Set<Long> relatedProductIds,
            Long couponDefinitionId
    ) {
        return eventAdminUseCase.create(new CreateCommand(
                title,
                title + " 요약",
                title + " 평문 내용",
                null,
                startAt,
                endAt,
                published,
                featured,
                couponDefinitionId,
                relatedProductIds));
    }
}
