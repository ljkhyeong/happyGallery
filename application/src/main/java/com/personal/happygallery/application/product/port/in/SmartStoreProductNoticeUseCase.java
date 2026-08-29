package com.personal.happygallery.application.product.port.in;

import java.time.OffsetDateTime;
import java.util.List;

public interface SmartStoreProductNoticeUseCase {

    NoticePage list(int page, int size);

    Notice get(Long sellerNoticeId);

    Long create(SaveCommand command);

    Long update(Long sellerNoticeId, SaveCommand command);

    void delete(Long sellerNoticeId);

    void apply(Long sellerNoticeId, List<Long> channelProductNos);

    record NoticePage(
            List<NoticeSummary> notices,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    record NoticeSummary(
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

    record Notice(
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

    record SaveCommand(
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
}
