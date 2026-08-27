package com.personal.happygallery.policy;

import com.personal.happygallery.domain.error.CapacityExceededException;
import com.personal.happygallery.domain.booking.SlotCapacity;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [PolicyTest] 슬롯 정원 정책 검증.
 *
 * <p>클래스에서 정한 슬롯 정원 미만이면 예약 가능,
 * 정원 이상이면 {@link CapacityExceededException}이 발생한다.
 */
@Tag("policy")
class SlotCapacityPolicyTest {

    @DisplayName("클래스 정원은 1명 이상이어야 한다")
    @Test
    void requireValidCapacity_rejectsZero() {
        assertThatThrownBy(() -> SlotCapacity.requireValidCapacity(0))
                .hasMessageContaining("클래스 정원은 1명 이상");
    }

    @DisplayName("슬롯 예약 가능 검사는 정원 미만일 때만 허용된다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("capacityCases")
    void checkAvailable_validatesCapacityLimit(
            String caseName, int capacity, int bookedCount, boolean expectedAvailable) {
        if (expectedAvailable) {
            assertThatCode(() -> SlotCapacity.checkAvailable(capacity, bookedCount, 1))
                    .doesNotThrowAnyException();
            return;
        }
        assertThatThrownBy(() -> SlotCapacity.checkAvailable(capacity, bookedCount, 1))
                .isInstanceOf(CapacityExceededException.class);
    }

    @DisplayName("슬롯 예약 가능 검사는 요청 인원 전체가 남은 정원에 들어와야 허용된다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("participantCapacityCases")
    void checkAvailable_validatesParticipantCount(
            String caseName, int capacity, int bookedCount,
            int participantCount, boolean expectedAvailable) {
        if (expectedAvailable) {
            assertThatCode(() -> SlotCapacity.checkAvailable(capacity, bookedCount, participantCount))
                    .doesNotThrowAnyException();
            return;
        }
        assertThatThrownBy(() -> SlotCapacity.checkAvailable(capacity, bookedCount, participantCount))
                .isInstanceOf(CapacityExceededException.class);
    }

    private static Stream<Arguments> capacityCases() {
        return Stream.of(
                Arguments.of("정원 미만 슬롯은 예약할 수 있다", 3, 2, true),
                Arguments.of("정원이 가득 찬 슬롯은 예약할 수 없다", 3, 3, false),
                Arguments.of("정원을 초과한 슬롯은 예약할 수 없다", 3, 4, false)
        );
    }

    private static Stream<Arguments> participantCapacityCases() {
        return Stream.of(
                Arguments.of("잔여 정원과 요청 인원이 같으면 예약할 수 있다", 8, 5, 3, true),
                Arguments.of("기존 기본값보다 큰 인원도 클래스 정원 안이면 예약할 수 있다", 12, 0, 9, true),
                Arguments.of("요청 인원이 잔여 정원보다 많으면 예약할 수 없다", 8, 6, 3, false)
        );
    }
}
