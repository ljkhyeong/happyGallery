package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.application.qna.port.out.SmartStoreInquiryProvider;
import com.personal.happygallery.domain.time.Clocks;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NaverCommerceInquiryProvider implements SmartStoreInquiryProvider {

    private static final int PAGE_SIZE = 100;

    private final RestClient restClient;
    private final SmartStoreProperties properties;
    private final NaverCommerceAccessTokenProvider accessTokenProvider;

    public NaverCommerceInquiryProvider(
            RestClient smartStoreRestClient,
            SmartStoreProperties properties,
            NaverCommerceAccessTokenProvider accessTokenProvider) {
        this.restClient = smartStoreRestClient;
        this.properties = properties;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public boolean isEnabled() {
        return properties.enabled();
    }

    @Override
    public List<InquiryItem> findProductInquiries(LocalDateTime from, LocalDateTime to) {
        List<InquiryItem> inquiries = new ArrayList<>();
        int page = 1;
        int totalPages;
        do {
            InquiryResponse response = fetch(from, to, page);
            if (response.contents() != null) {
                response.contents().stream()
                        .map(item -> new InquiryItem(
                                item.questionId(), item.productId(), item.productName(),
                                item.maskedWriterId(), item.question(), item.answer(), item.answered(),
                                toLocalDateTime(item.createDate())))
                        .forEach(inquiries::add);
            }
            totalPages = response.totalPages();
            page++;
        } while (page <= totalPages);
        return List.copyOf(inquiries);
    }

    private InquiryResponse fetch(LocalDateTime from, LocalDateTime to, int page) {
        InquiryResponse response = accessTokenProvider.authorized(token -> restClient.get()
                .uri(builder -> builder.path("/external/v1/contents/qnas")
                        .queryParam("fromDate", format(from))
                        .queryParam("toDate", format(to))
                        .queryParam("page", page)
                        .queryParam("size", PAGE_SIZE)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(InquiryResponse.class));
        if (response == null) {
            throw new IllegalStateException("스마트스토어 상품 문의 응답이 비어 있습니다.");
        }
        return response;
    }

    @Override
    public List<CustomerInquiryItem> findCustomerInquiries(
            LocalDate from, LocalDate to, boolean unansweredOnly, int limit) {
        List<CustomerInquiryItem> inquiries = new ArrayList<>();
        int page = 1;
        int totalPages;
        do {
            CustomerInquiryResponse response = fetchCustomerInquiries(
                    from, to, unansweredOnly, page, Math.min(limit, 200));
            if (response.content() != null) {
                response.content().stream()
                        .map(item -> new CustomerInquiryItem(
                                item.inquiryNo(), item.category(), item.title(),
                                item.inquiryContent(), item.answerContent(), item.answered(),
                                item.orderId(), item.productNo(), item.productOrderIdList(),
                                item.productName(), item.productOrderOption(), item.customerId(),
                                item.customerName(), toLocalDateTime(item.inquiryRegistrationDateTime()),
                                toNullableLocalDateTime(item.answerRegistrationDateTime())))
                        .limit(limit - inquiries.size())
                        .forEach(inquiries::add);
            }
            totalPages = response.totalPages();
            page++;
        } while (page <= totalPages && inquiries.size() < limit);
        return List.copyOf(inquiries);
    }

    private CustomerInquiryResponse fetchCustomerInquiries(
            LocalDate from, LocalDate to, boolean unansweredOnly, int page, int size) {
        CustomerInquiryResponse response = accessTokenProvider.authorized(token -> restClient.get()
                .uri(builder -> {
                    builder.path("/external/v1/pay-user/inquiries")
                            .queryParam("startSearchDate", from)
                            .queryParam("endSearchDate", to)
                            .queryParam("page", page)
                            .queryParam("size", size);
                    if (unansweredOnly) {
                        builder.queryParam("answered", false);
                    }
                    return builder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(CustomerInquiryResponse.class));
        if (response == null) {
            throw new IllegalStateException("스마트스토어 고객 문의 응답이 비어 있습니다.");
        }
        return response;
    }

    @Override
    public void answerProductInquiry(long questionId, String content) {
        accessTokenProvider.authorized(token -> {
            restClient.put()
                    .uri("/external/v1/contents/qnas/{questionId}", questionId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AnswerRequest(content))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        });
    }

    @Override
    public void answerCustomerInquiry(long inquiryNo, String content) {
        accessTokenProvider.authorized(token -> {
            restClient.post()
                    .uri("/external/v1/pay-merchant/inquiries/{inquiryNo}/answer", inquiryNo)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CustomerAnswerRequest(content))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        });
    }

    private static String format(LocalDateTime value) {
        return value.atZone(Clocks.SEOUL).toOffsetDateTime()
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private static LocalDateTime toLocalDateTime(OffsetDateTime value) {
        return value.atZoneSameInstant(Clocks.SEOUL).toLocalDateTime();
    }

    private static LocalDateTime toNullableLocalDateTime(OffsetDateTime value) {
        return value == null ? null : toLocalDateTime(value);
    }

    private record InquiryResponse(
            List<InquiryContent> contents,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {}

    private record InquiryContent(
            OffsetDateTime createDate,
            String question,
            String answer,
            boolean answered,
            long productId,
            String productName,
            String maskedWriterId,
            long questionId
    ) {}

    private record AnswerRequest(String commentContent) {}

    private record CustomerInquiryResponse(List<CustomerInquiryContent> content, int totalPages) {}

    private record CustomerInquiryContent(
            long inquiryNo,
            String category,
            String title,
            String inquiryContent,
            OffsetDateTime inquiryRegistrationDateTime,
            String answerContent,
            OffsetDateTime answerRegistrationDateTime,
            boolean answered,
            String orderId,
            String productNo,
            String productOrderIdList,
            String productName,
            String productOrderOption,
            String customerId,
            String customerName
    ) {}

    private record CustomerAnswerRequest(String answerComment) {}
}
