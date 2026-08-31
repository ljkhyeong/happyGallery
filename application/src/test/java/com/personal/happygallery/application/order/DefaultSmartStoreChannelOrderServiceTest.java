package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ClaimDetail;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderDetail;
import com.personal.happygallery.application.order.port.out.SmartStoreProductOrderPort;
import com.personal.happygallery.domain.order.SmartStoreProductOrder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultSmartStoreChannelOrderServiceTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("연동 주문 상세는 클레임 유무와 관계없이 조회한다")
    void detail_withOrWithoutClaim_returnsOrder(boolean hasClaim) {
        SmartStoreProductOrderPort orderPort = mock(SmartStoreProductOrderPort.class);
        SmartStoreOrderProvider provider = mock(SmartStoreOrderProvider.class);
        DefaultSmartStoreChannelOrderService service = new DefaultSmartStoreChannelOrderService(
                orderPort, mock(SmartStoreOrderTransactionService.class), provider,
                mock(SmartStoreDeliveryInfoProtector.class));
        LocalDateTime changedAt = LocalDateTime.of(2026, 8, 31, 12, 0);
        SmartStoreProductOrder order = new SmartStoreProductOrder(
                "po-1", "order-1", 123L, null, "가죽 지갑", null, "PAYED",
                null, null, 1, 1, "PAYED", changedAt, changedAt);
        ClaimDetail claim = hasClaim ? new ClaimDetail(
                "claim-1", "RETURN", "RETURN_REQUEST", "PRODUCT_UNSATISFIED", null,
                1, changedAt, null, null, null, null, null, List.of()) : null;
        ProductOrderDetail detail = new ProductOrderDetail(
                "po-1", "order-1", 123L, null, "가죽 지갑", null, null, "PAYED",
                null, null, null, claim, 1, 1, changedAt, null,
                null, null, null, null, null, null, null, null, null);
        when(orderPort.findByProductOrderId("po-1")).thenReturn(Optional.of(order));
        when(provider.isEnabled()).thenReturn(true);
        when(provider.fetchDetails(List.of("po-1"))).thenReturn(List.of(detail));

        var result = service.detail("po-1");

        assertThat(result.order().productOrderId()).isEqualTo("po-1");
        if (hasClaim) {
            assertThat(result.claimDetail().claimId()).isEqualTo("claim-1");
            assertThat(result.claimDetail().reason()).isEqualTo("PRODUCT_UNSATISFIED");
        } else {
            assertThat(result.claimDetail()).isNull();
        }
    }
}
