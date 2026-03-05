package com.personal.happygallery.policy;

import com.personal.happygallery.common.error.CapacityExceededException;
import com.personal.happygallery.domain.booking.SlotCapacity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [PolicyTest] 슬롯 정원 정책 검증.
 *
 * <p>정원({@value SlotCapacity#MAX}명) 미만이면 예약 가능,
 * 정원 이상이면 {@link CapacityExceededException}이 발생한다.
 */
@Tag("policy")
class SlotCapacityPolicyTest {

    @Test
    void checkAvailable_whenUnderCapacity_noException() {
        assertThatCode(() -> SlotCapacity.checkAvailable(SlotCapacity.MAX - 1))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAvailable_whenAtCapacity_throws() {
        assertThatThrownBy(() -> SlotCapacity.checkAvailable(SlotCapacity.MAX))
                .isInstanceOf(CapacityExceededException.class);
    }

    @Test
    void checkAvailable_whenOverCapacity_throws() {
        assertThatThrownBy(() -> SlotCapacity.checkAvailable(SlotCapacity.MAX + 1))
                .isInstanceOf(CapacityExceededException.class);
    }
}
