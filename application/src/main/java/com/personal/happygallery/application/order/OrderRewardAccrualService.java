package com.personal.happygallery.application.order;

import com.personal.happygallery.application.reward.RewardBenefitService;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.reward.RewardAccrualPolicy;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 주문 이행 완료 시 회원 적립금을 한 번만 계산하고 기록하는 애플리케이션 협력 객체. */
@Service
class OrderRewardAccrualService {

    private final RewardBenefitService rewardBenefitService;
    private final Clock clock;

    OrderRewardAccrualService(RewardBenefitService rewardBenefitService, Clock clock) {
        this.rewardBenefitService = rewardBenefitService;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void accrueForCompletion(Order order) {
        if (order.getUserId() == null) {
            return;
        }
        rewardBenefitService.accrue(
                order.getUserId(),
                order.getId(),
                RewardAccrualPolicy.calculate(order.getRewardEarnBase()),
                LocalDateTime.now(clock));
    }
}
