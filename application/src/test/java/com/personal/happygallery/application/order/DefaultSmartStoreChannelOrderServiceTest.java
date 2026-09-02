package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.AdminActor;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ClaimDetail;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.OperationNotSentException;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.OperationRejectedException;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.OperationResultUnknownException;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderDetail;
import com.personal.happygallery.application.order.port.out.SmartStoreProductOrderPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.SmartStoreOrderAction;
import com.personal.happygallery.domain.order.SmartStoreProductOrder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
                mock(SmartStoreDeliveryInfoProtector.class),
                mock(SmartStoreOrderActionHistoryService.class));
        LocalDateTime changedAt = LocalDateTime.of(2026, 8, 31, 12, 0);
        SmartStoreProductOrder order = new SmartStoreProductOrder(
                "po-1", "order-1", 123L, null, "가죽 지갑", null, "PAYED",
                null, null, 1, 1, "PAYED", changedAt, changedAt);
        ClaimDetail claim = hasClaim ? new ClaimDetail(
                "claim-1", "RETURN", "RETURN_REQUEST", "PRODUCT_UNSATISFIED", null,
                1, changedAt, null, null, null, null, null, List.of()) : null;
        ProductOrderDetail detail = detail("po-1", claim, changedAt);
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

    @Test
    @DisplayName("네이버 현재 상태는 로컬 주문 원장이 없어도 외부 주문 번호로 조회한다")
    void currentStatus_withoutLocalOrder_returnsProviderStatus() {
        SmartStoreProductOrderPort orderPort = mock(SmartStoreProductOrderPort.class);
        SmartStoreOrderProvider provider = mock(SmartStoreOrderProvider.class);
        DefaultSmartStoreChannelOrderService service = new DefaultSmartStoreChannelOrderService(
                orderPort, mock(SmartStoreOrderTransactionService.class), provider,
                mock(SmartStoreDeliveryInfoProtector.class),
                mock(SmartStoreOrderActionHistoryService.class));
        LocalDateTime changedAt = LocalDateTime.of(2026, 9, 2, 12, 0);
        when(provider.isEnabled()).thenReturn(true);
        when(provider.fetchDetails(List.of("po-archived")))
                .thenReturn(List.of(detail("po-archived", null, changedAt)));

        var result = service.currentStatus("po-archived");

        assertThat(result.productOrderId()).isEqualTo("po-archived");
        assertThat(result.productOrderStatus()).isEqualTo("PAYED");
        verify(provider).fetchDetails(List.of("po-archived"));
    }

    @Test
    @DisplayName("스마트스토어 주문 처리가 성공하면 요청자와 성공 결과를 감사 이력에 남긴다")
    void confirm_success_recordsSucceededAudit() {
        SmartStoreOrderProvider provider = mock(SmartStoreOrderProvider.class);
        SmartStoreOrderActionHistoryService historyService = mock(SmartStoreOrderActionHistoryService.class);
        DefaultSmartStoreChannelOrderService service = service(provider, historyService);
        AdminActor actor = new AdminActor(7L, "주문 관리자");
        when(provider.isEnabled()).thenReturn(true);
        when(historyService.start(
                "po-1", SmartStoreOrderAction.ORDER_CONFIRMED, null, actor)).thenReturn(11L);

        service.confirm("po-1", actor);

        verify(historyService).succeed(11L);
    }

    @Test
    @DisplayName("스마트스토어가 주문 처리를 거절하면 거절 코드와 메시지를 감사 이력에 남긴다")
    void confirm_rejected_recordsRejectedAudit() {
        SmartStoreOrderProvider provider = mock(SmartStoreOrderProvider.class);
        SmartStoreOrderActionHistoryService historyService = mock(SmartStoreOrderActionHistoryService.class);
        DefaultSmartStoreChannelOrderService service = service(provider, historyService);
        AdminActor actor = new AdminActor(7L, "주문 관리자");
        when(provider.isEnabled()).thenReturn(true);
        when(historyService.start(
                "po-1", SmartStoreOrderAction.ORDER_CONFIRMED, null, actor)).thenReturn(12L);
        doThrow(new OperationRejectedException("INVALID_STATUS", "처리할 수 없는 상태"))
                .when(provider).confirm("po-1");

        assertThatThrownBy(() -> service.confirm("po-1", actor))
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.SMARTSTORE_OPERATION_REJECTED));

        verify(historyService).reject(12L, "INVALID_STATUS", "처리할 수 없는 상태");
    }

    @Test
    @DisplayName("스마트스토어 주문 처리 결과를 확인할 수 없으면 재시도 판단을 위해 결과 미확정으로 남긴다")
    void confirm_unknownResult_recordsUnknownAudit() {
        SmartStoreOrderProvider provider = mock(SmartStoreOrderProvider.class);
        SmartStoreOrderActionHistoryService historyService = mock(SmartStoreOrderActionHistoryService.class);
        DefaultSmartStoreChannelOrderService service = service(provider, historyService);
        AdminActor actor = new AdminActor(7L, "주문 관리자");
        when(provider.isEnabled()).thenReturn(true);
        when(historyService.start(
                "po-1", SmartStoreOrderAction.ORDER_CONFIRMED, null, actor)).thenReturn(13L);
        doThrow(new OperationResultUnknownException("응답 본문 없음"))
                .when(provider).confirm("po-1");

        assertThatThrownBy(() -> service.confirm("po-1", actor))
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.SMARTSTORE_OPERATION_RESULT_UNKNOWN));

        verify(historyService).markResultUnknown(13L, "응답 본문 없음");
    }

    @Test
    @DisplayName("인증 토큰 실패로 주문 요청을 보내지 못하면 안전하게 다시 시도할 수 있도록 미전송으로 남긴다")
    void confirm_notSent_recordsRetryableAudit() {
        SmartStoreOrderProvider provider = mock(SmartStoreOrderProvider.class);
        SmartStoreOrderActionHistoryService historyService = mock(SmartStoreOrderActionHistoryService.class);
        DefaultSmartStoreChannelOrderService service = service(provider, historyService);
        AdminActor actor = new AdminActor(7L, "주문 관리자");
        when(provider.isEnabled()).thenReturn(true);
        when(historyService.start(
                "po-1", SmartStoreOrderAction.ORDER_CONFIRMED, null, actor)).thenReturn(14L);
        doThrow(new OperationNotSentException(
                "ACCESS_TOKEN_UNAVAILABLE", "인증 토큰 준비 실패", new IllegalStateException()))
                .when(provider).confirm("po-1");

        assertThatThrownBy(() -> service.confirm("po-1", actor))
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.SMARTSTORE_OPERATION_NOT_SENT));

        verify(historyService).markNotSent(14L, "ACCESS_TOKEN_UNAVAILABLE", "인증 토큰 준비 실패");
    }

    @Test
    @DisplayName("스마트스토어 일괄 주문 요청이 거절되면 모든 주문 이력을 거절로 남긴다")
    void confirmAll_rejected_recordsRejectedAudits() {
        SmartStoreOrderProvider provider = mock(SmartStoreOrderProvider.class);
        SmartStoreOrderActionHistoryService historyService = mock(SmartStoreOrderActionHistoryService.class);
        DefaultSmartStoreChannelOrderService service = service(provider, historyService);
        AdminActor actor = new AdminActor(7L, "주문 관리자");
        List<String> productOrderIds = List.of("po-1", "po-2");
        when(provider.isEnabled()).thenReturn(true);
        when(historyService.start("po-1", SmartStoreOrderAction.ORDER_CONFIRMED, null, actor))
                .thenReturn(21L);
        when(historyService.start("po-2", SmartStoreOrderAction.ORDER_CONFIRMED, null, actor))
                .thenReturn(22L);
        when(provider.confirmAll(productOrderIds))
                .thenThrow(new OperationRejectedException("HTTP_400", "잘못된 일괄 요청"));

        assertThatThrownBy(() -> service.confirmAll(productOrderIds, actor))
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.SMARTSTORE_OPERATION_REJECTED));

        verify(historyService).reject(21L, "HTTP_400", "잘못된 일괄 요청");
        verify(historyService).reject(22L, "HTTP_400", "잘못된 일괄 요청");
    }

    private static ProductOrderDetail detail(
            String productOrderId,
            ClaimDetail claim,
            LocalDateTime changedAt) {
        return new ProductOrderDetail(
                productOrderId, "order-1", 123L, null, "가죽 지갑", null, null, "PAYED",
                null, null, null, claim, 1, 1, changedAt, null,
                null, null, null, null, null, null, null, null, null, List.of());
    }

    private static DefaultSmartStoreChannelOrderService service(
            SmartStoreOrderProvider provider,
            SmartStoreOrderActionHistoryService historyService) {
        return new DefaultSmartStoreChannelOrderService(
                mock(SmartStoreProductOrderPort.class),
                mock(SmartStoreOrderTransactionService.class),
                provider,
                mock(SmartStoreDeliveryInfoProtector.class),
                historyService);
    }
}
