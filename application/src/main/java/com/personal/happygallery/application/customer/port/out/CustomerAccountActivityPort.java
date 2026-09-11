package com.personal.happygallery.application.customer.port.out;

import java.time.LocalDateTime;
import java.util.List;

/** 회원 탈퇴를 막는 미완료 거래를 조회한다. */
public interface CustomerAccountActivityPort {

    List<BlockingActivity> findBlockingActivities(Long userId, LocalDateTime now);

    enum BlockingActivity {
        ORDER("처리 중인 주문이 있습니다."),
        CLAIM("진행 중인 반품·교환이 있습니다."),
        BOOKING("예정된 예약이 있습니다."),
        CANCELLATION_TASK("예약 취소 후 공방 확인이 필요한 내역이 있습니다."),
        PASS("사용 가능한 이용권이 있습니다."),
        REFUND("처리 중인 환불이 있습니다."),
        PAYMENT("처리 중인 결제가 있습니다."),
        REWARD("결제에 사용 중이거나 회수할 적립금이 있습니다.");

        private final String message;

        BlockingActivity(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }
}
