package com.personal.happygallery.adapter.out.external.payment;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TossPaymentSettlementProviderTest {

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
