package com.personal.happygallery.application.order;

import com.personal.happygallery.domain.order.OrderClaimItem;
import com.personal.happygallery.domain.order.OrderItem;

record OrderClaimLine(OrderClaimItem claimItem, OrderItem orderItem) {

    long amount() {
        return Math.multiplyExact(orderItem.getUnitPrice(), claimItem.getQuantity());
    }
}
