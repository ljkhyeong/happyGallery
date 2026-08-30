package com.personal.happygallery.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.personal.happygallery.domain.reward.RewardAccrualPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("policy")
class RewardAccrualPolicyTest {

    @Test
    @DisplayName("배송비와 혜택을 제외한 상품 실결제액의 1퍼센트를 원 단위로 내림해 적립한다")
    void calculateOnePercentWithFloor() {
        assertThat(RewardAccrualPolicy.calculate(12_399L)).isEqualTo(123L);
        assertThat(RewardAccrualPolicy.calculate(99L)).isZero();
    }
}
