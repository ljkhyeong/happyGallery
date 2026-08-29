package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.application.product.port.out.SmartStoreProductNoticeProvider.SaveCommand;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NaverCommerceProductNoticeProviderTest {

    private static final SmartStoreProperties PROPERTIES = new SmartStoreProperties(
            true, "client-id", "$2a$10$abcdefghijklmnopqrstuv", "SELF", "",
            "https://api.commerce.naver.com", Duration.ofSeconds(5), Duration.ofSeconds(1),
            Duration.ofMillis(500), 5, Duration.ofSeconds(30));
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-29T03:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("상품 공지를 조회하고 등록한 뒤 채널상품에 적용한다")
    void manageNotice_usesSellerNoticeContracts() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverCommerceProductNoticeProvider provider = new NaverCommerceProductNoticeProvider(
                builder.build(), PROPERTIES,
                new NaverCommerceAccessTokenProvider(builder.build(), PROPERTIES, CLOCK));

        server.expect(requestTo("https://api.commerce.naver.com/external/v1/oauth2/token"))
                .andRespond(withSuccess("""
                        {"access_token":"access-token","expires_in":10800,"token_type":"Bearer"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString(
                        "/external/v1/contents/seller-notices?page=1&size=100")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"contents":[{
                          "sellerNoticeId":77,"postCategoryType":"DELIVERY",
                          "title":"추석 배송 안내","importantNotice":true,
                          "importantNoticeStartDate":"2026-09-20T00:00:00+09:00",
                          "importantNoticeEndDate":"2026-09-30T23:59:59+09:00",
                          "wholeNotice":false,"displayStartDate":"2026-09-20T00:00:00+09:00",
                          "displayEndDate":"2026-09-30T23:59:59+09:00"
                        }],"page":1,"size":100,"totalElements":1,"totalPages":1}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v1/contents/seller-notices/77"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"sellerNoticeId":77,"postCategoryType":"DELIVERY",
                         "title":"추석 배송 안내","importantNotice":true,"wholeNotice":false,
                         "displayStartDate":"2026-09-20T00:00:00+09:00",
                         "displayEndDate":"2026-09-30T23:59:59+09:00",
                         "popup":false,"detailContents":"연휴 뒤 순차 발송합니다."}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v1/contents/seller-notices"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"postCategoryType":"DELIVERY","title":"추석 배송 안내",
                         "importantNotice":false,"wholeNotice":false,"popup":false,
                         "detailContents":"연휴 뒤 순차 발송합니다."}
                        """))
                .andRespond(withSuccess("{\"sellerNoticeId\":77}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v1/products/channel-products/notice/apply"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("""
                        {"sellerNoticeId":77,"channelProductNos":[987654321]}
                        """))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        var page = provider.list(1, 100);
        var notice = provider.get(77L);
        Long sellerNoticeId = provider.create(new SaveCommand(
                "DELIVERY", "추석 배송 안내", false, null, null, false,
                null, null, false, null, null, "연휴 뒤 순차 발송합니다."));
        provider.apply(sellerNoticeId, List.of(987654321L));

        server.verify();
        assertThat(page.notices()).singleElement()
                .satisfies(item -> assertThat(item.sellerNoticeId()).isEqualTo(77L));
        assertThat(notice.detailContents()).isEqualTo("연휴 뒤 순차 발송합니다.");
        assertThat(sellerNoticeId).isEqualTo(77L);
    }
}
