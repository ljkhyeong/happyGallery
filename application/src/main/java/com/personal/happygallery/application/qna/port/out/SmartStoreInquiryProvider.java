package com.personal.happygallery.application.qna.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SmartStoreInquiryProvider {

    boolean isEnabled();

    List<InquiryItem> findProductInquiries(LocalDateTime from, LocalDateTime to);

    List<CustomerInquiryItem> findCustomerInquiries(
            LocalDate from, LocalDate to, boolean unansweredOnly, int limit);

    void answerProductInquiry(long questionId, String content);

    void answerCustomerInquiry(long inquiryNo, String content);

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

    record CustomerInquiryItem(
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
