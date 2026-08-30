package com.personal.happygallery.application.qna.port.in;

import com.personal.happygallery.application.shared.page.OffsetPage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SmartStoreInquiryUseCase {

    List<InquiryResult> list(boolean unansweredOnly, int limit);

    List<CustomerInquiryResult> listCustomerInquiries(boolean unansweredOnly, int limit);

    OffsetPage<InquiryResult> listPage(LocalDate from, LocalDate to, boolean unansweredOnly, int page, int size);

    OffsetPage<CustomerInquiryResult> listCustomerPage(
            LocalDate from, LocalDate to, boolean unansweredOnly, int page, int size);

    AnswerTemplateResult getProductInquiryAnswerTemplate();

    void answer(long questionId, String content);

    void answerCustomerInquiry(long inquiryNo, String content);

    void updateCustomerInquiryAnswer(long inquiryNo, long answerContentId, String content);

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

    record AnswerTemplateResult(
            String questionType,
            String subject,
            String content
    ) {}

    record CustomerInquiryResult(
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
