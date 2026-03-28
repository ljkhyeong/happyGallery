package com.personal.happygallery.infra.notification.dto;

import java.util.Map;

public record KakaoAlimtalkRequest(
        String senderKey,
        String templateCode,
        String recipientNo,
        Map<String, String> templateArgs
) {}
