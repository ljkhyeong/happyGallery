package com.personal.happygallery.application.qna.port.in;

import java.time.LocalDateTime;
import java.util.List;

public interface SmartStoreInquiryUseCase {

    List<InquiryResult> list(boolean unansweredOnly, int limit);

    void answer(long questionId, String content);

    record InquiryResult(
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
