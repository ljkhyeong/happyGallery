package com.personal.happygallery.adapter.out.external.payment;

import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TossPaymentSettlementProviderTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    @DisplayName("정산 응답 본문이 누락되면 이전 페이지를 받았어도 조회를 실패시킨다")
    void findSettlements_missingPageFails(int missingPage) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.tosspayments.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentSettlementProvider provider = new TossPaymentSettlementProvider(builder.build());
        if (missingPage == 2) {
            expectFullPage(server);
        }
        server.expect(requestTo(containsString("/v1/settlements")))
                .andExpect(queryParam("page", Integer.toString(missingPage)))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> provider.findSettlements(
                LocalDate.of(2026, 8, 22), LocalDate.of(2026, 8, 28)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("토스 정산 응답이 비어 있습니다.");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    @DisplayName("정산의 빈 배열은 조회 완료로 처리하고 앞서 받은 페이지는 보존한다")
    void findSettlements_emptyArrayCompletes(int emptyPage) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.tosspayments.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentSettlementProvider provider = new TossPaymentSettlementProvider(builder.build());
        if (emptyPage == 2) {
            expectFullPage(server);
        }
        server.expect(requestTo(containsString("/v1/settlements")))
                .andExpect(queryParam("page", Integer.toString(emptyPage)))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var result = provider.findSettlements(
                LocalDate.of(2026, 8, 22), LocalDate.of(2026, 8, 28));

        assertThat(result).extracting(item -> item.transactionKey())
                .containsExactlyElementsOf(IntStream.range(0, emptyPage == 2 ? 5000 : 0)
                        .mapToObj(index -> "transaction-" + index).toList());
        server.verify();
    }

    private static void expectFullPage(MockRestServiceServer server) {
        String response = IntStream.range(0, 5000)
                .mapToObj(index -> """
                        {"transactionKey":"transaction-%d","paymentKey":"payment-%d",
                         "orderId":"order-%d","method":"카드","amount":10000,
                         "fees":[{"type":"BASE","fee":300}],"supplyAmount":300,
                         "vat":30,"payOutAmount":9670,"soldDate":"2026-08-28"}
                        """.formatted(index, index, index))
                .collect(Collectors.joining(",", "[", "]"));
        server.expect(requestTo(containsString("/v1/settlements")))
                .andExpect(queryParam("page", "1"))
                .andExpect(queryParam("size", "5000"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    @DisplayName("Toss 정산 조회는 기간과 페이지를 지정하고 수수료 합계를 계산한다")
    @Test
    void findSettlements_mapsSettlementResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.tosspayments.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentSettlementProvider provider = new TossPaymentSettlementProvider(builder.build());
        server.expect(requestTo("https://api.tosspayments.com/v1/settlements"
                        + "?startDate=2026-08-22&endDate=2026-08-28&dateType=soldDate&page=1&size=5000"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "transactionKey": "transaction-key",
                            "paymentKey": "payment-key",
                            "orderId": "order-id",
                            "method": "카드",
                            "amount": 10000,
                            "fees": [
                              {"type": "BASE", "fee": 300},
                              {"type": "ETC", "fee": 30}
                            ],
                            "supplyAmount": 300,
                            "vat": 30,
                            "payOutAmount": 9670,
                            "approvedAt": "2026-08-28T10:00:00+09:00",
                            "soldDate": "2026-08-28",
                            "paidOutDate": "2026-09-01"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        var result = provider.findSettlements(
                LocalDate.of(2026, 8, 22), LocalDate.of(2026, 8, 28));

        server.verify();
        assertThat(result).hasSize(1);
        assertSoftly(softly -> {
            var settlement = result.getFirst();
            softly.assertThat(settlement.transactionKey()).isEqualTo("transaction-key");
            softly.assertThat(settlement.amount()).isEqualTo(10_000L);
            softly.assertThat(settlement.feeAmount()).isEqualTo(330L);
            softly.assertThat(settlement.payOutAmount()).isEqualTo(9_670L);
            softly.assertThat(settlement.cancelTransaction()).isFalse();
        });
    }
}
