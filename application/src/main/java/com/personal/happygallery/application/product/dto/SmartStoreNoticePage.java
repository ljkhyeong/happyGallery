package com.personal.happygallery.application.product.dto;

import java.util.List;

/** 스마트스토어 판매자 공지 목록 한 페이지다. */
public record SmartStoreNoticePage(
        List<SmartStoreNoticeSummary> notices,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public SmartStoreNoticePage {
        notices = notices == null ? List.of() : List.copyOf(notices);
    }
}
