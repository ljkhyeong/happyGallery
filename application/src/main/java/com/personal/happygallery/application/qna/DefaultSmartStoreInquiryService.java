package com.personal.happygallery.application.qna;

import com.personal.happygallery.application.qna.port.in.SmartStoreInquiryUseCase;
import com.personal.happygallery.application.qna.port.out.SmartStoreInquiryProvider;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultSmartStoreInquiryService implements SmartStoreInquiryUseCase {

    private final SmartStoreInquiryProvider provider;
    private final Clock clock;

    public DefaultSmartStoreInquiryService(SmartStoreInquiryProvider provider, Clock clock) {
        this.provider = provider;
        this.clock = clock;
    }

    @Override
    public List<InquiryResult> list(boolean unansweredOnly, int limit) {
        requireEnabled();
        LocalDateTime now = LocalDateTime.now(clock);
        return provider.findProductInquiries(now.minusDays(30), now).stream()
                .filter(item -> !unansweredOnly || !item.answered())
                .sorted(Comparator.comparing(
                        SmartStoreInquiryProvider.InquiryItem::createdAt).reversed())
                .limit(limit)
                .map(item -> new InquiryResult(
                        item.questionId(), item.channelProductId(), item.productName(),
                        item.maskedWriterId(), item.question(), item.answer(), item.answered(),
                        item.createdAt()))
                .toList();
    }

    @Override
    public void answer(long questionId, String content) {
        requireEnabled();
        provider.answerProductInquiry(questionId, content);
    }

    @Override
    public List<CustomerInquiryResult> listCustomerInquiries(boolean unansweredOnly, int limit) {
        requireEnabled();
        LocalDate today = LocalDate.now(clock);
        return provider.findCustomerInquiries(today.minusDays(30), today, unansweredOnly, limit)
                .stream()
                .map(item -> new CustomerInquiryResult(
                        item.inquiryNo(), item.category(), item.title(), item.inquiryContent(),
                        item.answerContent(), item.answered(), item.orderId(),
                        item.channelProductId(), item.productOrderIds(), item.productName(),
                        item.productOrderOption(), item.maskedCustomerId(), item.customerName(),
                        item.createdAt(), item.answeredAt()))
                .toList();
    }

    @Override
    public void answerCustomerInquiry(long inquiryNo, String content) {
        requireEnabled();
        provider.answerCustomerInquiry(inquiryNo, content);
    }

    private void requireEnabled() {
        if (!provider.isEnabled()) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "스마트스토어 연동이 비활성화되어 있습니다.");
        }
    }
}
