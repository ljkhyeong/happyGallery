package com.personal.happygallery.app.monitoring;

public enum ClientMonitoringEventType {
    GUEST_LOOKUP_HUB_VIEWED("guest_lookup_hub_viewed"),
    GUEST_ORDER_DIRECT_ENTRY_CONTINUED("guest_order_direct_entry_continued"),
    GUEST_MEMBER_CTA_CLICKED("guest_member_cta_clicked"),
    GUEST_CLAIM_MODAL_OPENED("guest_claim_modal_opened"),
    GUEST_CLAIM_COMPLETED("guest_claim_completed");

    private final String logValue;

    ClientMonitoringEventType(String logValue) {
        this.logValue = logValue;
    }

    public String logValue() {
        return logValue;
    }
}
