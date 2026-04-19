package com.personal.happygallery.adapter.out.external.notification.dto;

import java.util.List;

public record SmsRequest(
        String body,
        String sendNo,
        List<Recipient> recipientList
) {
    public record Recipient(String recipientNo) {}
}
