package com.personal.happygallery.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.event.Event;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("policy")
class EventPolicyTest {

    private static final LocalDateTime START_AT = LocalDateTime.of(2026, 8, 10, 10, 0);
    private static final LocalDateTime END_AT = LocalDateTime.of(2026, 8, 20, 18, 0);

    @DisplayName("이벤트는 최대 길이의 제목과 요약과 평문 내용을 저장한다")
    @Test
    void event_acceptsMaximumTextLengths() {
        Event event = new Event(
                "가".repeat(Event.MAX_TITLE_LENGTH),
                "나".repeat(Event.MAX_SUMMARY_LENGTH),
                "다".repeat(Event.MAX_CONTENT_LENGTH),
                "  https://images.example.com/event.jpg  ",
                START_AT,
                END_AT,
                true,
                true,
                7L,
                Set.of(3L, 1L));

        assertSoftly(softly -> {
            softly.assertThat(event.getTitle()).hasSize(Event.MAX_TITLE_LENGTH);
            softly.assertThat(event.getSummary()).hasSize(Event.MAX_SUMMARY_LENGTH);
            softly.assertThat(event.getContent()).hasSize(Event.MAX_CONTENT_LENGTH);
            softly.assertThat(event.getImageUrl())
                    .isEqualTo("https://images.example.com/event.jpg");
            softly.assertThat(event.getRelatedProductIds()).containsExactly(1L, 3L);
            softly.assertThat(event.getCouponDefinitionId()).isEqualTo(7L);
        });
    }

    @DisplayName("이벤트는 시작 시각이 종료 시각보다 빠르지 않으면 거절한다")
    @Test
    void event_rejectsInvalidPeriod() {
        assertInvalidInput(() -> event(START_AT, START_AT, Set.of()));
        assertInvalidInput(() -> event(END_AT, START_AT, Set.of()));
        assertInvalidInput(() -> event(null, END_AT, Set.of()));
    }

    @DisplayName("이벤트는 비어 있거나 저장 한도를 넘는 텍스트를 거절한다")
    @Test
    void event_rejectsInvalidText() {
        assertInvalidInput(() -> new Event(
                " ", "요약", "내용", null,
                START_AT, END_AT, false, false, null, Set.of()));
        assertInvalidInput(() -> new Event(
                "제목", "가".repeat(Event.MAX_SUMMARY_LENGTH + 1), "내용", null,
                START_AT, END_AT, false, false, null, Set.of()));
        assertInvalidInput(() -> new Event(
                "제목", "요약", "가".repeat(Event.MAX_CONTENT_LENGTH + 1), null,
                START_AT, END_AT, false, false, null, Set.of()));
    }

    @DisplayName("이벤트의 연관 상품과 연결 쿠폰 ID는 양수만 허용한다")
    @Test
    void event_rejectsInvalidRelatedProductIds() {
        Set<Long> nullId = Collections.singleton(null);

        assertInvalidInput(() -> event(START_AT, END_AT, Set.of(0L)));
        assertInvalidInput(() -> event(START_AT, END_AT, Set.of(-1L)));
        assertInvalidInput(() -> event(START_AT, END_AT, nullId));
        assertThat(event(START_AT, END_AT, null).getRelatedProductIds()).isEmpty();
        assertInvalidInput(() -> new Event(
                "이벤트", "이벤트 요약", "이벤트 내용", null,
                START_AT, END_AT, false, false, 0L, Set.of()));
    }

    private static Event event(
            LocalDateTime startAt,
            LocalDateTime endAt,
            Set<Long> relatedProductIds
    ) {
        return new Event(
                "이벤트", "이벤트 요약", "이벤트 내용", null,
                startAt, endAt, false, false, null, relatedProductIds);
    }

    private static void assertInvalidInput(Runnable command) {
        assertThatThrownBy(command::run)
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
