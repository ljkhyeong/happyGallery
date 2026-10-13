package com.personal.happygallery.application.product.dto;

import java.time.OffsetDateTime;

/** 스마트스토어 판매자 공지 상세다. */
public record SmartStoreNotice(
        Long sellerNoticeId,
        String postCategoryType,
        String title,
        boolean importantNotice,
        OffsetDateTime importantNoticeStartDate,
        OffsetDateTime importantNoticeEndDate,
        boolean wholeNotice,
        OffsetDateTime displayStartDate,
        OffsetDateTime displayEndDate,
        boolean popup,
        OffsetDateTime popupStartDate,
        OffsetDateTime popupEndDate,
        String detailContents
) {}
