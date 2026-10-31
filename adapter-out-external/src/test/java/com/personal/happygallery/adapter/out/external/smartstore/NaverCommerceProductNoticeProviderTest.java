package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.application.product.dto.SmartStoreNoticeSaveCommand;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static com.personal.happygallery.adapter.out.external.smartstore.SmartStoreTestSupport.PROPERTIES;
import static com.personal.happygallery.adapter.out.external.smartstore.SmartStoreTestSupport.expectToken;
import static com.personal.happygallery.adapter.out.external.smartstore.SmartStoreTestSupport.mockApi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NaverCommerceProductNoticeProviderTest {

    private MockRestServiceServer server;
    private NaverCommerceProductNoticeProvider provider;

    @BeforeEach
    void setUp() {
        var api = mockApi();
        server = api.server();
        provider = new NaverCommerceProductNoticeProvider(api.client(), PROPERTIES, api.tokenProvider());
    }

    @Test
    @DisplayName("상품 공지를 조회하고 등록한 뒤 채널상품에 적용한다")
    void manageNotice_usesSellerNoticeContracts() {
        expectToken(server);
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
        Long sellerNoticeId = provider.create(new SmartStoreNoticeSaveCommand(
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
