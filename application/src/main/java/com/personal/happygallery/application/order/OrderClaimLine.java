package com.personal.happygallery.application.order;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.money.Proration;
import com.personal.happygallery.domain.order.OrderClaimItem;
import com.personal.happygallery.domain.order.OrderItem;

record OrderClaimLine(
        OrderClaimItem claimItem,
        OrderItem orderItem,
        long previousApprovedQuantity) {

    OrderClaimLine(OrderClaimItem claimItem, OrderItem orderItem) {
        this(claimItem, orderItem, 0L);
    }

    OrderClaimLine {
        if (claimItem == null || orderItem == null
                || previousApprovedQuantity < 0L
                || previousApprovedQuantity + claimItem.getQuantity() > orderItem.getQty()) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "클레임 상품 환불 누계가 주문 수량을 초과합니다.");
        }
    }

    long customerRefundableAmount() {
        return Math.addExact(pgRefundableAmount(), rewardRestorableAmount());
    }

    long pgRefundableAmount() {
        return componentShare(orderItem.getNetPaidAmount());
    }

    long rewardRestorableAmount() {
        return componentShare(orderItem.getRewardUsedAmount());
    }

    private long componentShare(long componentAmount) {
        long throughCurrent = previousApprovedQuantity + claimItem.getQuantity();
        return Proration.floor(componentAmount, throughCurrent, orderItem.getQty())
                - Proration.floor(componentAmount, previousApprovedQuantity, orderItem.getQty());
    }
}
