package com.personal.happygallery.application.order.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 스마트스토어 상품 주문의 현재 클레임 상세다. 네이버 응답과 관리자 조회가 같은 형태를 쓴다. */
public record SmartStoreClaimDetail(
        String claimId,
        String claimType,
        String claimStatus,
        String reason,
        String detailedReason,
        Integer requestQuantity,
        LocalDateTime requestedAt,
        String collectStatus,
        String collectDeliveryCompany,
        String collectTrackingNumber,
        Long claimDeliveryFeeDemandAmount,
        String holdbackStatus,
        List<String> imageUrls
) {
    public SmartStoreClaimDetail {
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }
}
