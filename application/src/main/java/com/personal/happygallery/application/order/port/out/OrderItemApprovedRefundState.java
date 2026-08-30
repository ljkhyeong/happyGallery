package com.personal.happygallery.application.order.port.out;

/** 이전 승인 클레임이 이미 점유한 수량과 혜택 반환 누계. */
public record OrderItemApprovedRefundState(
        Long orderItemId,
        long quantity,
        long customerRefundAmount,
        long rewardRestoreAmount) {

    public long pgRefundAmount() {
        return customerRefundAmount - rewardRestoreAmount;
    }
}
