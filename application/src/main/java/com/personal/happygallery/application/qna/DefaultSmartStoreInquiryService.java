package com.personal.happygallery.application.qna;

import com.personal.happygallery.application.qna.dto.SmartStoreAnswerTemplate;
import com.personal.happygallery.application.qna.dto.SmartStoreCustomerInquiry;
import com.personal.happygallery.application.qna.dto.SmartStoreProductInquiry;
import com.personal.happygallery.application.qna.port.in.SmartStoreInquiryUseCase;
import com.personal.happygallery.application.qna.port.out.SmartStoreInquiryProvider;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class DefaultSmartStoreInquiryService implements SmartStoreInquiryUseCase {

    private final SmartStoreInquiryProvider provider;

    public DefaultSmartStoreInquiryService(SmartStoreInquiryProvider provider) {
        this.provider = provider;
    }

    @Override
    public OffsetPage<SmartStoreProductInquiry> listPage(
            LocalDate from, LocalDate to, boolean unansweredOnly, int page, int size) {
        requireEnabled();
        requireDateRange(from, to);
        return provider.findProductInquiries(
                from.atStartOfDay(), to.atTime(23, 59, 59, 999_000_000), unansweredOnly, page, size);
    }

    @Override
    public void answer(long questionId, String content) {
        requireEnabled();
        provider.answerProductInquiry(questionId, content);
    }

    @Override
    public SmartStoreAnswerTemplate getProductInquiryAnswerTemplate() {
        requireEnabled();
        return provider.findProductInquiryAnswerTemplate();
    }

    @Override
    public OffsetPage<SmartStoreCustomerInquiry> listCustomerPage(
            LocalDate from, LocalDate to, boolean unansweredOnly, int page, int size) {
        requireEnabled();
        requireDateRange(from, to);
        return provider.findCustomerInquiries(from, to, unansweredOnly, page, size);
    }

    private static void requireDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "조회 시작일은 종료일보다 늦을 수 없습니다.");
        }
    }

    @Override
    public void answerCustomerInquiry(long inquiryNo, String content) {
        requireEnabled();
        provider.answerCustomerInquiry(inquiryNo, content);
    }

    @Override
    public void updateCustomerInquiryAnswer(long inquiryNo, long answerContentId, String content) {
        requireEnabled();
        provider.updateCustomerInquiryAnswer(inquiryNo, answerContentId, content);
    }

    private void requireEnabled() {
        if (!provider.isEnabled()) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "스마트스토어 연동이 비활성화되어 있습니다.");
        }
    }
}
