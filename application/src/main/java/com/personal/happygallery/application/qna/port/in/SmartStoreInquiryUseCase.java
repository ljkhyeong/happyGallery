package com.personal.happygallery.application.qna.port.in;

import com.personal.happygallery.application.qna.dto.SmartStoreAnswerTemplate;
import com.personal.happygallery.application.qna.dto.SmartStoreCustomerInquiry;
import com.personal.happygallery.application.qna.dto.SmartStoreProductInquiry;
import com.personal.happygallery.application.shared.page.OffsetPage;
import java.time.LocalDate;

public interface SmartStoreInquiryUseCase {

    OffsetPage<SmartStoreProductInquiry> listPage(LocalDate from, LocalDate to, boolean unansweredOnly, int page, int size);

    OffsetPage<SmartStoreCustomerInquiry> listCustomerPage(
            LocalDate from, LocalDate to, boolean unansweredOnly, int page, int size);

    SmartStoreAnswerTemplate getProductInquiryAnswerTemplate();

    void answer(long questionId, String content);

    void answerCustomerInquiry(long inquiryNo, String content);

    void updateCustomerInquiryAnswer(long inquiryNo, long answerContentId, String content);
}
