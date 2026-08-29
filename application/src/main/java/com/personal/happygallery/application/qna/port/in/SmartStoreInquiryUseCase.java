package com.personal.happygallery.application.qna.port.in;

import java.time.LocalDateTime;
import java.util.List;

public interface SmartStoreInquiryUseCase {

    List<InquiryResult> list(boolean unansweredOnly, int limit);

    List<CustomerInquiryResult> listCustomerInquiries(boolean unansweredOnly, int limit);

    void answer(long questionId, String content);

    void answerCustomerInquiry(long inquiryNo, String content);

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

    record CustomerInquiryResult(
            long inquiryNo,
            String category,
            String title,
            String inquiryContent,
            String answerContent,
            boolean answered,
            String orderId,
            String channelProductId,
            String productOrderIds,
            String productName,
            String productOrderOption,
            String maskedCustomerId,
            String customerName,
            LocalDateTime createdAt,
            LocalDateTime answeredAt
    ) {}
}
