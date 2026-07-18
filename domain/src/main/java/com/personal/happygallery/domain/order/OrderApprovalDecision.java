package com.personal.happygallery.domain.order;

public enum OrderApprovalDecision {
    APPROVE,
    REJECT,
    DELAY,
    DELAY_CANCEL,
    AUTO_REFUND,
    PRODUCTION_COMPLETE,
    RESUME_PRODUCTION,
    PICKUP_READY,
    PICKUP_COMPLETE,
    PICKUP_EXPIRED,
    PICKUP_FORFEITED,
    PREPARE_SHIPPING,
    SHIP,
    DELIVER
}
