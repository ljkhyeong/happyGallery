package com.personal.happygallery.application.qna;

import com.personal.happygallery.application.qna.port.in.SmartStoreInquiryUseCase;
import com.personal.happygallery.application.qna.port.out.SmartStoreInquiryProvider;
import com.personal.happygallery.application.qna.port.out.SmartStoreInquiryProvider.InquiryItem;
import com.personal.happygallery.application.qna.port.out.SmartStoreInquiryProvider.CustomerInquiryItem;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.ArrayList;
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
        int size = Math.min(limit, 100);
        var first = provider.findProductInquiries(now.minusDays(30), now, unansweredOnly, 0, size);
        List<InquiryItem> items = new ArrayList<>(first.content());
        if (first.totalPages() > 1 && items.size() < limit) {
            items.addAll(provider.findProductInquiries(now.minusDays(30), now, unansweredOnly, 1, size).content());
        }
        return items.stream()
                .sorted(Comparator.comparing(InquiryItem::createdAt).reversed())
                .limit(limit)
                .map(DefaultSmartStoreInquiryService::toResult)
                .toList();
    }

    @Override
    public OffsetPage<InquiryResult> listPage(
            LocalDate from, LocalDate to, boolean unansweredOnly, int page, int size) {
        requireEnabled();
        requireDateRange(from, to);
        var result = provider.findProductInquiries(
                from.atStartOfDay(), to.atTime(23, 59, 59, 999_000_000), unansweredOnly, page, size);
        return new OffsetPage<>(result.content().stream().map(DefaultSmartStoreInquiryService::toResult).toList(),
                result.page(), result.size(), result.totalCount(), result.totalPages());
    }

    @Override
    public void answer(long questionId, String content) {
        requireEnabled();
        provider.answerProductInquiry(questionId, content);
    }

    @Override
    public AnswerTemplateResult getProductInquiryAnswerTemplate() {
        requireEnabled();
        var template = provider.findProductInquiryAnswerTemplate();
        return new AnswerTemplateResult(
                template.questionType(), template.subject(), template.content());
    }

    @Override
    public List<CustomerInquiryResult> listCustomerInquiries(boolean unansweredOnly, int limit) {
        requireEnabled();
        LocalDate today = LocalDate.now(clock);
        return provider.findCustomerInquiries(today.minusDays(30), today, unansweredOnly, 0, Math.max(limit, 10))
                .content().stream().limit(limit)
                .map(DefaultSmartStoreInquiryService::toResult)
                .toList();
    }

    @Override
    public OffsetPage<CustomerInquiryResult> listCustomerPage(
            LocalDate from, LocalDate to, boolean unansweredOnly, int page, int size) {
        requireEnabled();
        requireDateRange(from, to);
        var result = provider.findCustomerInquiries(from, to, unansweredOnly, page, size);
        return new OffsetPage<>(result.content().stream().map(DefaultSmartStoreInquiryService::toResult).toList(),
                result.page(), result.size(), result.totalCount(), result.totalPages());
    }

    private static InquiryResult toResult(InquiryItem item) {
        return new InquiryResult(item.questionId(), item.channelProductId(), item.productName(),
                item.maskedWriterId(), item.question(), item.answer(), item.answered(), item.createdAt());
    }

    private static CustomerInquiryResult toResult(CustomerInquiryItem item) {
        return new CustomerInquiryResult(
                item.inquiryNo(), item.answerContentId(), item.category(), item.title(), item.inquiryContent(),
                item.answerContent(), item.answered(), item.orderId(), item.channelProductId(), item.productOrderIds(),
                item.productName(), item.productOrderOption(), item.maskedCustomerId(), item.customerName(),
                item.createdAt(), item.answeredAt());
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
