package com.personal.happygallery.adapter.out.external.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SmsRequest(
        String body,
        String sendNo,
        String userId,
        List<Recipient> recipientList
) {
    public record Recipient(String recipientNo) {}
}
