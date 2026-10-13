package com.personal.happygallery.application.product.dto;

import java.time.OffsetDateTime;

/** 스마트스토어 판매자 공지 목록 항목이다. */
public record SmartStoreNoticeSummary(
        Long sellerNoticeId,
        String postCategoryType,
        String title,
        boolean importantNotice,
        OffsetDateTime importantNoticeStartDate,
        OffsetDateTime importantNoticeEndDate,
        boolean wholeNotice,
        OffsetDateTime displayStartDate,
        OffsetDateTime displayEndDate
) {}
