package com.personal.happygallery.adapter.out.external.notification.dto;

import java.util.Map;

public record KakaoAlimtalkRequest(
        String senderKey,
        String templateCode,
        String recipientNo,
        Map<String, String> templateArgs
) {}
