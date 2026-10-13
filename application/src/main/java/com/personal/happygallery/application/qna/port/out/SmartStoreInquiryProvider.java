package com.personal.happygallery.application.qna.port.out;

import com.personal.happygallery.application.qna.dto.SmartStoreAnswerTemplate;
import com.personal.happygallery.application.qna.dto.SmartStoreCustomerInquiry;
import com.personal.happygallery.application.qna.dto.SmartStoreProductInquiry;
import com.personal.happygallery.application.shared.page.OffsetPage;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface SmartStoreInquiryProvider {

    boolean isEnabled();

    OffsetPage<SmartStoreProductInquiry> findProductInquiries(
            LocalDateTime from, LocalDateTime to, boolean unansweredOnly, int page, int size);

    OffsetPage<SmartStoreCustomerInquiry> findCustomerInquiries(
            LocalDate from, LocalDate to, boolean unansweredOnly, int page, int size);

    SmartStoreAnswerTemplate findProductInquiryAnswerTemplate();

    void answerProductInquiry(long questionId, String content);

    void answerCustomerInquiry(long inquiryNo, String content);

    void updateCustomerInquiryAnswer(long inquiryNo, long answerContentId, String content);
}
