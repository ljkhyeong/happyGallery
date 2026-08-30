package com.personal.happygallery.application.qna.port.out;

import com.personal.happygallery.application.shared.page.OffsetPage;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface SmartStoreInquiryProvider {

    boolean isEnabled();

    OffsetPage<InquiryItem> findProductInquiries(
            LocalDateTime from, LocalDateTime to, boolean unansweredOnly, int page, int size);

    OffsetPage<CustomerInquiryItem> findCustomerInquiries(
            LocalDate from, LocalDate to, boolean unansweredOnly, int page, int size);

    AnswerTemplate findProductInquiryAnswerTemplate();

    void answerProductInquiry(long questionId, String content);

    void answerCustomerInquiry(long inquiryNo, String content);

    void updateCustomerInquiryAnswer(long inquiryNo, long answerContentId, String content);

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

    record AnswerTemplate(
            String questionType,
            String subject,
            String content
    ) {}

    record CustomerInquiryItem(
            long inquiryNo,
            Long answerContentId,
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
