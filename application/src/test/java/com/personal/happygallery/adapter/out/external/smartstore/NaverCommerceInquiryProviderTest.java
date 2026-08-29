package com.personal.happygallery.adapter.out.external.smartstore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NaverCommerceInquiryProviderTest {

    private static final SmartStoreProperties PROPERTIES = new SmartStoreProperties(
            true, "client-id", "$2a$10$abcdefghijklmnopqrstuv", "SELF", "",
            "https://api.commerce.naver.com", Duration.ofSeconds(5), Duration.ofSeconds(1),
            Duration.ofMillis(500), 5, Duration.ofSeconds(30));
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-29T03:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("상품 문의 목록을 페이지 응답에서 읽고 답변 본문을 공식 필드명으로 전송한다")
    void findAndAnswer_usesOfficialContract() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverCommerceInquiryProvider provider = new NaverCommerceInquiryProvider(
                builder.build(), PROPERTIES,
                new NaverCommerceAccessTokenProvider(builder.build(), PROPERTIES, CLOCK));

        server.expect(requestTo("https://api.commerce.naver.com/external/v1/oauth2/token"))
                .andRespond(withSuccess("""
                        {"access_token":"access-token","expires_in":10800,"token_type":"Bearer"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/external/v1/contents/qnas")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("page", "1"))
                .andExpect(queryParam("size", "100"))
                .andRespond(withSuccess("""
                        {
                          "contents":[{
                            "createDate":"2026-08-29T10:00:00+09:00",
                            "question":"각인 가능한가요?",
                            "answer":null,
                            "answered":false,
                            "productId":123,
                            "productName":"가죽 지갑",
                            "maskedWriterId":"cust***",
                            "questionId":456
                          }],
                          "page":1,
                          "size":100,
                          "totalElements":1,
                          "totalPages":1,
                          "first":true,
                          "last":true
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v1/contents/qnas/456"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("""
                        {"commentContent":"원하시는 문구로 가능합니다."}
                        """))
                .andRespond(withSuccess());

        var inquiries = provider.findProductInquiries(
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 29, 12, 0));
        provider.answer(456L, "원하시는 문구로 가능합니다.");

        server.verify();
        assertThat(inquiries).singleElement().satisfies(inquiry -> {
            assertThat(inquiry.questionId()).isEqualTo(456L);
            assertThat(inquiry.answered()).isFalse();
            assertThat(inquiry.question()).isEqualTo("각인 가능한가요?");
        });
    }
}
