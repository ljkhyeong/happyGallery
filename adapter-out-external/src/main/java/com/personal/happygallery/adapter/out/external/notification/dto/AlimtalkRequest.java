package com.personal.happygallery.adapter.out.external.notification.dto;

import java.util.List;
import java.util.Map;

public record AlimtalkRequest(
        String senderKey,
        String templateCode,
        List<Recipient> recipientList
) {
    public record Recipient(String recipientNo, Map<String, String> templateParameter) {}
}
