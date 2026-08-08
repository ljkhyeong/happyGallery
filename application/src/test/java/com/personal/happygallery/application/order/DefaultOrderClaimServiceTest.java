package com.personal.happygallery.application.order;

import com.personal.happygallery.application.customer.MemberAccountGuard;
import com.personal.happygallery.application.order.port.in.AdminOrderClaimUseCase.ResolveCommand;
import com.personal.happygallery.application.order.port.in.OrderClaimView;
import com.personal.happygallery.application.order.port.in.OrderClaimUseCase.RequestCommand;
import com.personal.happygallery.application.order.port.out.OrderClaimItemPort;
import com.personal.happygallery.application.order.port.out.OrderClaimPort;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.payment.RefundExecutionService;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.application.product.InventoryService;
import com.personal.happygallery.application.reward.RewardBenefitService;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderClaim;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultOrderClaimServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-08T01:00:00Z"), ZoneOffset.UTC);

    @DisplayName("회원 클레임 접수는 주문과 회원 순서로 잠근다")
    @Test
    void requestMemberClaim_locksOrderBeforeMember() {
        Dependencies dependencies = new Dependencies();
        Order order = mock(Order.class);
        when(dependencies.orderReader.findByIdForUpdate(11L)).thenReturn(Optional.of(order));
        when(order.getUserId()).thenReturn(99L);

        assertThatThrownBy(() -> dependencies.service().requestMemberClaim(
                11L, 23L, mock(RequestCommand.class)))
                .isInstanceOf(NotFoundException.class);

        InOrder lockOrder = inOrder(dependencies.orderReader, dependencies.memberAccountGuard);
        lockOrder.verify(dependencies.orderReader).findByIdForUpdate(11L);
        lockOrder.verify(dependencies.memberAccountGuard).requireActiveForUpdate(23L);
        verifyNoInteractions(dependencies.orderClaimPort);
    }

    @DisplayName("클레임 처리는 주문 식별자를 조회한 뒤 주문과 클레임 순서로 잠근다")
    @Test
    void resolve_locksOrderBeforeClaim() {
        Dependencies dependencies = new Dependencies();
        Order order = mock(Order.class);
        OrderClaim claim = mock(OrderClaim.class);
        OrderClaimView expected = mock(OrderClaimView.class);
        when(dependencies.orderClaimPort.findOrderIdById(7L)).thenReturn(Optional.of(11L));
        when(dependencies.orderReader.findByIdForUpdate(11L)).thenReturn(Optional.of(order));
        when(dependencies.orderClaimPort.findByIdForUpdate(7L)).thenReturn(Optional.of(claim));
        when(claim.getId()).thenReturn(7L);
        when(claim.getOrderId()).thenReturn(11L);
        when(order.getUserId()).thenReturn(23L);
        when(dependencies.orderClaimItemPort.findByClaimIdIn(List.of(7L)))
                .thenReturn(List.of());
        when(dependencies.orderClaimPort.save(claim)).thenReturn(claim);
        when(dependencies.viewAssembler.assemble(claim)).thenReturn(expected);

        OrderClaimView result = dependencies.service().resolve(
                7L, 3L, new ResolveCommand(false, null, false, "반품 대상이 아닙니다."));

        assertThat(result).isSameAs(expected);
        InOrder lockOrder = inOrder(dependencies.orderClaimPort, dependencies.orderReader);
        lockOrder.verify(dependencies.orderClaimPort).findOrderIdById(7L);
        lockOrder.verify(dependencies.orderReader).findByIdForUpdate(11L);
        lockOrder.verify(dependencies.orderClaimPort).findByIdForUpdate(7L);
    }

    @DisplayName("잠금 사이에 클레임의 주문 연결이 달라지면 처리를 중단한다")
    @Test
    void resolve_changedOrderLink_throwsConflict() {
        Dependencies dependencies = new Dependencies();
        Order order = mock(Order.class);
        OrderClaim claim = mock(OrderClaim.class);
        when(dependencies.orderClaimPort.findOrderIdById(7L)).thenReturn(Optional.of(11L));
        when(dependencies.orderReader.findByIdForUpdate(11L)).thenReturn(Optional.of(order));
        when(dependencies.orderClaimPort.findByIdForUpdate(7L)).thenReturn(Optional.of(claim));
        when(claim.getOrderId()).thenReturn(12L);

        assertThatThrownBy(() -> dependencies.service().resolve(
                7L, 3L, new ResolveCommand(false, null, false, "반품 대상이 아닙니다.")))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        verifyNoInteractions(dependencies.orderClaimItemPort);
    }

    private static final class Dependencies {
        private final OrderReaderPort orderReader = mock(OrderReaderPort.class);
        private final MemberAccountGuard memberAccountGuard = mock(MemberAccountGuard.class);
        private final OrderItemPort orderItemPort = mock(OrderItemPort.class);
        private final OrderClaimPort orderClaimPort = mock(OrderClaimPort.class);
        private final OrderClaimItemPort orderClaimItemPort = mock(OrderClaimItemPort.class);
        private final OrderClaimViewAssembler viewAssembler = mock(OrderClaimViewAssembler.class);
        private final RefundExecutionService refundExecutionService =
                mock(RefundExecutionService.class);
        private final RefundPort refundPort = mock(RefundPort.class);
        private final RewardBenefitService rewardBenefitService = mock(RewardBenefitService.class);
        private final InventoryService inventoryService = mock(InventoryService.class);
        private final GuestTokenService guestTokenService = mock(GuestTokenService.class);
        private final ApplicationEventPublisher eventPublisher =
                mock(ApplicationEventPublisher.class);

        private DefaultOrderClaimService service() {
            return new DefaultOrderClaimService(
                    orderReader,
                    memberAccountGuard,
                    orderItemPort,
                    orderClaimPort,
                    orderClaimItemPort,
                    viewAssembler,
                    refundExecutionService,
                    refundPort,
                    rewardBenefitService,
                    inventoryService,
                    guestTokenService,
                    eventPublisher,
                    CLOCK);
        }
    }
}
