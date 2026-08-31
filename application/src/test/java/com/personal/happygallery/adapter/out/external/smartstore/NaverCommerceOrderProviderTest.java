package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ChangeCursor;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.DelayCommand;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.DispatchCommand;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ExchangeDispatchCommand;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ExchangeRejectCommand;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ExchangeHoldCommand;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ReturnHoldCommand;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.SellerReturnCommand;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.SellerCancelCommand;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NaverCommerceOrderProviderTest {

    private static final SmartStoreProperties PROPERTIES = new SmartStoreProperties(
            true, "client-id", "$2a$10$abcdefghijklmnopqrstuv", "SELF", "",
            "https://api.commerce.naver.com", Duration.ofSeconds(5), Duration.ofSeconds(1),
            Duration.ofMillis(500), 5, Duration.ofSeconds(30));
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-29T03:00:00Z"), ZoneOffset.UTC);

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("완료 반품은 현재 클레임과 이력을 합쳐 한 번만 집계하고 취소와 반품 철회는 제외한다")
    void fetchDetails_collectsCompletedReturnsWithoutDuplicates(boolean hasHistory) {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverCommerceOrderProvider provider = new NaverCommerceOrderProvider(
                builder.build(), PROPERTIES,
                new NaverCommerceAccessTokenProvider(builder.build(), PROPERTIES, CLOCK));
        String history = hasHistory ? """
                [
                  {"claimId":"r-1","claimType":"RETURN","claimStatus":"RETURN_DONE",
                   "requestQuantity":1,"claimCompleteOperationDate":"2026-08-29T10:00:00+09:00"},
                  {"claimId":"r-2","claimType":"RETURN","claimStatus":"RETURN_DONE",
                   "requestQuantity":2,"claimCompleteOperationDate":"2026-08-29T11:00:00+09:00"},
                  {"claimId":"c-1","claimType":"CANCEL","claimStatus":"CANCEL_DONE","requestQuantity":1},
                  {"claimId":"r-3","claimType":"RETURN","claimStatus":"RETURN_REJECT","requestQuantity":1}
                ]
                """ : "null";
        expectToken(server);
        server.expect(requestTo(containsString("/product-orders/query")))
                .andRespond(withSuccess("""
                        {"data":[{
                          "order":{"orderId":"o-1"},
                          "productOrder":{"productOrderId":"po-1","originalProductId":"123",
                            "productName":"반품 상품","productOrderStatus":"DELIVERING",
                            "claimType":"RETURN","claimStatus":"RETURN_DONE",
                            "initialQuantity":5,"remainQuantity":%d},
                          "currentClaim":{"return":{"claimId":"r-2","claimStatus":"RETURN_DONE",
                            "requestQuantity":2,"returnCompletedDate":"2026-08-29T11:00:00+09:00"}},
                          "completedClaims":%s
                        }]}
                        """.formatted(hasHistory ? 1 : 3, history), MediaType.APPLICATION_JSON));

        var detail = provider.fetchDetails(List.of("po-1")).getFirst();

        assertThat(detail.completedReturnQuantity()).isEqualTo(hasHistory ? 3 : 2);
        assertThat(detail.completedReturnQuantityAt(LocalDateTime.of(2026, 8, 29, 10, 30)))
                .isEqualTo(hasHistory ? 1 : 0);
        assertThat(detail.completedReturns()).extracting(returned -> returned.claimId())
                .doesNotHaveDuplicates();
        assertThat(detail.deliveryCompany()).isNull();
        assertThat(detail.trackingNumber()).isNull();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("반품 완료 이력이나 수량이 없으면 잔여 수량 감소를 취소로 처리하지 않고 수집을 실패시킨다")
    void fetchDetails_rejectsIncompleteCompletedReturns(boolean hasReturnClaim) {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverCommerceOrderProvider provider = new NaverCommerceOrderProvider(
                builder.build(), PROPERTIES,
                new NaverCommerceAccessTokenProvider(builder.build(), PROPERTIES, CLOCK));
        String returned = hasReturnClaim ? """
                {"claimId":"r-1","claimStatus":"RETURN_DONE",
                 "returnCompletedDate":"2026-08-29T11:00:00+09:00"}
                """ : "null";
        expectToken(server);
        server.expect(requestTo(containsString("/product-orders/query")))
                .andRespond(withSuccess("""
                        {"data":[{
                          "order":{"orderId":"o-1"},
                          "productOrder":{"productOrderId":"po-1","originalProductId":"123",
                            "productName":"반품 상품","productOrderStatus":"DELIVERING",
                            "claimType":"RETURN","claimStatus":"RETURN_DONE",
                            "initialQuantity":2,"remainQuantity":1},
                          "currentClaim":{"return":%s}
                        }]}
                        """.formatted(returned), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.fetchDetails(List.of("po-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("반품");
        server.verify();
    }

    @Test
    @DisplayName("반품 택배사 계약번호와 우선순위를 v2 공식 응답에서 조회한다")
    void findReturnDeliveryCompanies_usesContractIds() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverCommerceOrderProvider provider = new NaverCommerceOrderProvider(
                builder.build(), PROPERTIES,
                new NaverCommerceAccessTokenProvider(builder.build(), PROPERTIES, CLOCK));
        expectToken(server);
        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v2/product-delivery-info/return-delivery-companies"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andRespond(withSuccess("""
                        {"returnDeliveryCompanies":[
                          {"id":1001,"name":"CJ대한통운","returnDeliveryCompanyPriorityType":"PRIMARY"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(provider.findReturnDeliveryCompanies()).singleElement().satisfies(company -> {
            assertThat(company.id()).isEqualTo(1001L);
            assertThat(company.name()).isEqualTo("CJ대한통운");
            assertThat(company.returnDeliveryCompanyPriorityType()).isEqualTo("PRIMARY");
        });
        server.verify();
    }

    @Test
    @DisplayName("변경 주문 식별자를 조회한 뒤 상품 주문 상세와 옵션 아이템 번호를 읽는다")
    void fetchChangedOrders_readsDetailAndItemNumber() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverCommerceAccessTokenProvider tokenProvider = new NaverCommerceAccessTokenProvider(
                builder.build(), PROPERTIES, CLOCK);
        NaverCommerceOrderProvider provider = new NaverCommerceOrderProvider(
                builder.build(), PROPERTIES, tokenProvider);

        expectToken(server);
        server.expect(requestTo(containsString(
                        "/external/v1/pay-order/seller/product-orders/last-changed-statuses")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(queryParam("lastChangedFrom", "2026-08-29T11:50:00+09:00"))
                .andExpect(queryParam("lastChangedTo", "2026-08-29T12:00:00+09:00"))
                .andExpect(queryParam("limitCount", "300"))
                .andRespond(withSuccess("""
                        {
                          "data": {
                            "count": 1,
                            "lastChangeStatuses": [{
                              "productOrderId": "2026082912345678",
                              "lastChangedType": "PAYED",
                              "lastChangedDate": "2026-08-29T11:59:00+09:00"
                            }]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v1/pay-order/seller/product-orders/query"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "productOrderIds": ["2026082912345678"],
                          "quantityClaimCompatibility": true
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "data": [{
                            "order": {
                              "orderId": "2026082911111111",
                              "paymentDate": "2026-08-29T11:58:00+09:00"
                            },
                            "productOrder": {
                              "productOrderId": "2026082912345678",
                              "originalProductId": "123456789",
                              "itemNo": "90001",
                              "productName": "각인 카드지갑",
                            "productOption": "색상: 브라운",
                              "shippingAddress": {
                                "name": "홍길동",
                                "tel1": "01012345678",
                                "zipCode": "04524",
                                "baseAddress": "서울 중구 세종대로 110",
                                "detailedAddress": "2층"
                              },
                              "shippingMemo": "문 앞에 놓아주세요",
                              "productOrderStatus": "PAYED",
                              "placeOrderStatus": "OK",
                              "shippingDueDate": "2026-08-30T18:00:00+09:00",
                              "expectedDeliveryMethod": "DELIVERY",
                              "initialQuantity": 2,
                              "remainQuantity": 2,
                              "unitPrice": 35000,
                              "remainPaymentAmount": 70000,
                              "paymentCommission": 1000,
                              "saleCommission": 2000,
                              "channelCommission": 300,
                              "expectedSettlementAmount": 66700
                            },
                            "delivery": {
                              "deliveryCompany": "CJ대한통운",
                              "trackingNumber": "1234567890"
                            },
                            "currentClaim": {
                              "return": {
                                "claimId":"claim-1",
                                "claimStatus":"RETURN_REQUEST",
                                "claimRequestDate":"2026-08-29T12:30:00+09:00",
                                "requestQuantity":1,
                                "returnReason":"PRODUCT_UNSATISFIED",
                                "returnDetailedReason":"색상이 달라요",
                                "collectStatus":"COLLECT_REQUEST",
                                "claimDeliveryFeeDemandAmount":3000,
                                "returnImageUrl":["https://example.com/claim.jpg"]
                              }
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        var page = provider.fetchChanges(
                new ChangeCursor(LocalDateTime.of(2026, 8, 29, 11, 50), null),
                LocalDateTime.of(2026, 8, 29, 12, 0));
        var details = provider.fetchDetails(List.of("2026082912345678"));

        server.verify();
        assertThat(page.changes()).hasSize(1);
        assertThat(details.getFirst().originProductNo()).isEqualTo(123456789L);
        assertThat(details.getFirst().itemNo()).isEqualTo(90001L);
        assertThat(details.getFirst().remainQuantity()).isEqualTo(2);
        assertThat(details.getFirst().deliveryInfo().recipientName()).isEqualTo("홍길동");
        assertThat(details.getFirst().deliveryInfo().shippingMemo()).isEqualTo("문 앞에 놓아주세요");
        assertThat(details.getFirst().trackingNumber()).isEqualTo("1234567890");
        assertThat(details.getFirst().expectedSettlementAmount()).isEqualTo(66700L);
        assertThat(details.getFirst().claimDetail().reason()).isEqualTo("PRODUCT_UNSATISFIED");
        assertThat(details.getFirst().claimDetail().requestQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("발송·지연·교환·반품 요청을 네이버 주문 API 계약에 맞춰 전송한다")
    void executeOrderOperations_sendsDocumentedPayloads() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverCommerceAccessTokenProvider tokenProvider = new NaverCommerceAccessTokenProvider(
                builder.build(), PROPERTIES, CLOCK);
        NaverCommerceOrderProvider provider = new NaverCommerceOrderProvider(
                builder.build(), PROPERTIES, tokenProvider);

        expectToken(server);
        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v1/pay-order/seller/product-orders/dispatch"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"dispatchProductOrders":[{
                          "productOrderId":"po-1",
                          "deliveryMethod":"DELIVERY",
                          "deliveryCompanyCode":"CJGLS",
                          "trackingNumber":"1234",
                          "dispatchDate":"2026-08-29T15:00:00+09:00"
                        }]}
                        """))
                .andRespond(operationSuccess());
        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v1/pay-order/seller/product-orders/po-1/delay"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "dispatchDueDate":"2026-08-31T18:00:00+09:00",
                          "delayedDispatchReason":"CUSTOM_BUILD",
                          "dispatchDelayedDetailedReason":"각인 제작 중"
                        }
                        """))
                .andRespond(operationSuccess());
        server.expect(requestTo(containsString(
                        "/external/v1/pay-order/seller/product-orders/po-1/claim/exchange/dispatch")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "reDeliveryMethod":"DELIVERY",
                          "reDeliveryCompany":"CJGLS",
                          "reDeliveryTrackingNumber":"5678"
                        }
                        """))
                .andRespond(operationSuccess());
        server.expect(requestTo(containsString(
                        "/external/v1/pay-order/seller/product-orders/po-1/claim/exchange/collect/approve")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(operationSuccess());
        server.expect(requestTo(containsString(
                        "/external/v1/pay-order/seller/product-orders/po-1/claim/exchange/reject")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"rejectExchangeReason\":\"교환 대상이 아닙니다.\"}"))
                .andRespond(operationSuccess());
        server.expect(requestTo(containsString(
                        "/external/v1/pay-order/seller/product-orders/po-1/claim/exchange/holdback")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "holdbackClassType":"EXCHANGE_DELIVERYFEE",
                          "holdbackExchangeDetailReason":"배송비 입금 대기",
                          "extraExchangeFeeAmount":3000
                        }
                        """))
                .andRespond(operationSuccess());
        server.expect(requestTo(containsString(
                        "/external/v1/pay-order/seller/product-orders/po-1/claim/exchange/holdback/release")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(operationSuccess());
        server.expect(requestTo(containsString(
                        "/external/v1/pay-order/seller/product-orders/po-1/claim/cancel/request")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "cancelReason":"SOLD_OUT",
                          "cancelDetailedReason":"부자재 품절",
                          "cancelQuantity":1
                        }
                        """))
                .andRespond(operationSuccess());
        server.expect(requestTo(containsString(
                        "/external/v1/pay-order/seller/product-orders/po-1/claim/return/holdback")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "holdbackClassType":"RETURN_DELIVERYFEE",
                          "holdbackReturnDetailReason":"반품 배송비 입금 대기",
                          "extraReturnFeeAmount":3000
                        }
                        """))
                .andRespond(operationSuccess());
        server.expect(requestTo(containsString(
                        "/external/v1/pay-order/seller/product-orders/po-1/claim/return/holdback/release")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(operationSuccess());
        server.expect(requestTo(containsString(
                        "/external/v1/pay-order/seller/product-orders/po-1/claim/return/request")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "returnReason":"PRODUCT_UNSATISFIED",
                          "collectDeliveryMethod":"RETURN_DESIGNATED",
                          "collectDeliveryCompany":"CJGLS",
                          "collectTrackingNumber":"9876",
                          "returnQuantity":1
                        }
                        """))
                .andRespond(operationSuccess());

        provider.dispatch(new DispatchCommand(
                "po-1", "DELIVERY", "CJGLS", "1234",
                LocalDateTime.of(2026, 8, 29, 15, 0)));
        provider.delay(new DelayCommand(
                "po-1", LocalDateTime.of(2026, 8, 31, 18, 0),
                "CUSTOM_BUILD", "각인 제작 중"));
        provider.dispatchExchange(new ExchangeDispatchCommand(
                "po-1", "DELIVERY", "CJGLS", "5678"));
        provider.completeExchangeCollect("po-1");
        provider.rejectExchange(new ExchangeRejectCommand("po-1", "교환 대상이 아닙니다."));
        provider.holdExchange(new ExchangeHoldCommand(
                "po-1", "EXCHANGE_DELIVERYFEE", "배송비 입금 대기", 3000L));
        provider.releaseExchangeHold("po-1");
        provider.requestSellerCancel(new SellerCancelCommand(
                "po-1", "SOLD_OUT", "부자재 품절", 1));
        provider.holdReturn(new ReturnHoldCommand(
                "po-1", "RETURN_DELIVERYFEE", "반품 배송비 입금 대기", 3000L));
        provider.releaseReturnHold("po-1");
        provider.requestSellerReturn(new SellerReturnCommand(
                "po-1", "PRODUCT_UNSATISFIED", "RETURN_DESIGNATED", "CJGLS", "9876", 1));

        server.verify();
    }

    @Test
    @DisplayName("발주와 발송 일괄 요청은 주문별 성공과 실패를 그대로 반환한다")
    void bulkOperations_preservePartialResults() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverCommerceAccessTokenProvider tokenProvider = new NaverCommerceAccessTokenProvider(
                builder.build(), PROPERTIES, CLOCK);
        NaverCommerceOrderProvider provider = new NaverCommerceOrderProvider(
                builder.build(), PROPERTIES, tokenProvider);

        expectToken(server);
        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v1/pay-order/seller/product-orders/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"productOrderIds\":[\"po-1\",\"po-2\"]}"))
                .andRespond(withSuccess("""
                        {"data":{
                          "successProductOrderInfos":[{"productOrderId":"po-1"}],
                          "failProductOrderInfos":[{
                            "productOrderId":"po-2","code":"INVALID_STATUS","message":"발주 상태가 아닙니다."
                          }]
                        }}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v1/pay-order/seller/product-orders/dispatch"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"dispatchProductOrders":[
                          {"productOrderId":"po-1","deliveryMethod":"DELIVERY",
                           "deliveryCompanyCode":"CJGLS","trackingNumber":"1111",
                           "dispatchDate":"2026-08-29T15:00:00+09:00"},
                          {"productOrderId":"po-2","deliveryMethod":"DELIVERY",
                           "deliveryCompanyCode":"CJGLS","trackingNumber":"2222",
                           "dispatchDate":"2026-08-29T15:00:00+09:00"}
                        ]}
                        """))
                .andRespond(withSuccess("""
                        {"data":{
                          "successProductOrderIds":["po-1"],
                          "failProductOrderInfos":[{
                            "productOrderId":"po-2","code":"INVALID_TRACKING","message":"운송장을 확인해 주세요."
                          }]
                        }}
                        """, MediaType.APPLICATION_JSON));

        var confirm = provider.confirmAll(List.of("po-1", "po-2"));
        var dispatch = provider.dispatchAll(List.of(
                new DispatchCommand("po-1", "DELIVERY", "CJGLS", "1111",
                        LocalDateTime.of(2026, 8, 29, 15, 0)),
                new DispatchCommand("po-2", "DELIVERY", "CJGLS", "2222",
                        LocalDateTime.of(2026, 8, 29, 15, 0))));

        server.verify();
        assertThat(confirm.successProductOrderIds()).containsExactly("po-1");
        assertThat(confirm.failures()).singleElement()
                .satisfies(failure -> assertThat(failure.productOrderId()).isEqualTo("po-2"));
        assertThat(dispatch.successProductOrderIds()).containsExactly("po-1");
        assertThat(dispatch.failures()).singleElement()
                .satisfies(failure -> assertThat(failure.code()).isEqualTo("INVALID_TRACKING"));
    }

    private static ResponseCreator operationSuccess() {
        return withSuccess("""
                {"data":{"successProductOrderIds":["po-1"],"failProductOrderInfos":[]}}
                """, MediaType.APPLICATION_JSON);
    }

    private static void expectToken(MockRestServiceServer server) {
        server.expect(requestTo("https://api.commerce.naver.com/external/v1/oauth2/token"))
                .andRespond(withSuccess("""
                        {
                          "access_token": "access-token",
                          "expires_in": 10800,
                          "token_type": "Bearer"
                        }
                        """, MediaType.APPLICATION_JSON));
    }
}
