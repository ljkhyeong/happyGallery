package com.personal.happygallery.application.qna.port.out;

import java.time.LocalDateTime;
import java.util.List;

public interface SmartStoreInquiryProvider {

    boolean isEnabled();

    List<InquiryItem> findProductInquiries(LocalDateTime from, LocalDateTime to);

    void answer(long questionId, String content);

    record InquiryItem(
            long questionId,
            long channelProductId,
            String productName,
            String maskedWriterId,
            String question,
            String answer,
            boolean answered,
            LocalDateTime createdAt
    ) {}
}
