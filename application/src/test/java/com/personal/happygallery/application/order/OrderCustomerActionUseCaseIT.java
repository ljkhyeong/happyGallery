package com.personal.happygallery.application.order;

import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.order.port.in.OrderApprovalUseCase;
import com.personal.happygallery.application.order.port.in.OrderCustomerActionUseCase;
import com.personal.happygallery.application.order.port.in.OrderProductionUseCase;
import com.personal.happygallery.application.order.port.in.OrderProductionUseCase.ProposeDelayCommand;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderDelayDecision;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.MadeToOrderConsent;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.support.OrderStateProbe;
import com.personal.happygallery.support.OrderTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.TestFixtures;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class OrderCustomerActionUseCaseIT {

    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired InventoryReaderPort inventoryReaderPort;
    @Autowired OrderStorePort orderStorePort;
    @Autowired OrderItemPort orderItemPort;
    @Autowired UserStorePort userStorePort;
    @Autowired GuestStorePort guestStorePort;
    @Autowired OrderService orderService;
    @Autowired OrderApprovalUseCase orderApprovalUseCase;
    @Autowired OrderProductionUseCase orderProductionUseCase;
    @Autowired OrderCustomerActionUseCase orderCustomerActionUseCase;
    @Autowired OrderStateProbe orderStateProbe;
    @Autowired TestCleanupSupport cleanupSupport;

    OrderTestHelper orderHelper;

    @BeforeEach
    void setUp() {
        cleanup();
        orderHelper = new OrderTestHelper(
                productStorePort,
                inventoryStorePort,
                inventoryReaderPort,
                orderStorePort,
                orderItemPort,
                userStorePort,
                orderService);
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("회원은 배송비를 포함한 승인 대기 주문 전액을 취소하고 재고를 복구한다")
    @Test
    void cancelMemberOrder_restoresInventoryAndRequestsRefund() {
        OrderTestHelper.OrderFixture fixture =
                orderHelper.createReadyStockPaidShippingOrder("고객 취소 상품", 40_000L, 3_000L);

        var result = orderCustomerActionUseCase.cancelMemberOrder(
                fixture.order().getId(), fixture.order().getUserId());

        assertSoftly(softly -> {
            softly.assertThat(result.order().getStatus()).isEqualTo(OrderStatus.CUSTOMER_CANCELED);
            softly.assertThat(result.order().getTotalAmount()).isEqualTo(43_000L);
            softly.assertThat(result.order().getShippingFee()).isEqualTo(3_000L);
            softly.assertThat(result.refund().getAmount()).isEqualTo(43_000L);
            softly.assertThat(result.refund().getStatus()).isEqualTo(RefundStatus.REQUESTED);
            softly.assertThat(orderStateProbe.getInventoryByProductId(fixture.product().getId()).getQuantity())
                    .isEqualTo(1);
            softly.assertThat(orderStateProbe.orderApprovalHistory(fixture.order().getId()))
                    .extracting("decision")
                    .containsExactly(OrderApprovalDecision.CUSTOMER_CANCEL);
        });
    }

    @DisplayName("다른 회원과 잘못된 비회원 토큰은 주문을 변경하지 못한다")
    @Test
    void customerAction_requiresOrderOwnership() {
        OrderTestHelper.OrderFixture memberFixture =
                orderHelper.createReadyStockPaidOrder("회원 소유권 상품", 30_000L);
        GuestOrderFixture guestFixture = createGuestMadeToOrder("비회원 소유권 상품", 70_000L);

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> orderCustomerActionUseCase.cancelMemberOrder(
                            memberFixture.order().getId(), Long.MAX_VALUE))
                    .isInstanceOf(NotFoundException.class);
            softly.assertThatThrownBy(() -> orderCustomerActionUseCase.cancelGuestOrder(
                            guestFixture.order().getId(), "wrong-token"))
                    .isInstanceOf(NotFoundException.class);
            softly.assertThat(orderStateProbe.getOrder(memberFixture.order().getId()).getStatus())
                    .isEqualTo(OrderStatus.PAID_APPROVAL_PENDING);
            softly.assertThat(orderStateProbe.getOrder(guestFixture.order().getId()).getStatus())
                    .isEqualTo(OrderStatus.PAID_APPROVAL_PENDING);
            softly.assertThat(orderStateProbe.refunds()).isEmpty();
        });
    }

    @DisplayName("회원이 제작 지연 제안을 수락하면 지연 수락 상태와 이력이 기록된다")
    @Test
    void acceptDelay_transitionsToDelayRequested() {
        Order order = orderHelper.createMadeToOrderPaidOrder("지연 수락 상품", 120_000L).order();
        orderApprovalUseCase.approve(order.getId(), 1L);
        orderProductionUseCase.proposeDelay(new ProposeDelayCommand(order.getId(), 1L));

        var result = orderCustomerActionUseCase.respondToMemberDelay(
                order.getId(), order.getUserId(), OrderDelayDecision.ACCEPT);

        assertSoftly(softly -> {
            softly.assertThat(result.order().getStatus()).isEqualTo(OrderStatus.DELAY_ACCEPTED);
            softly.assertThat(result.refund()).isNull();
            softly.assertThat(orderStateProbe.orderApprovalHistory(order.getId()))
                    .extracting("decision")
                    .containsExactly(
                            OrderApprovalDecision.APPROVE,
                            OrderApprovalDecision.DELAY,
                            OrderApprovalDecision.DELAY_ACCEPT);
        });
    }

    @DisplayName("비회원이 제작 지연 제안을 거절하면 주문 취소와 환불 및 재고 복구가 수행된다")
    @Test
    void rejectGuestDelay_cancelsAndRequestsRefund() {
        GuestOrderFixture fixture = createGuestMadeToOrder("지연 거절 상품", 150_000L);
        orderApprovalUseCase.approve(fixture.order().getId(), 1L);
        orderProductionUseCase.proposeDelay(new ProposeDelayCommand(fixture.order().getId(), 1L));

        var result = orderCustomerActionUseCase.respondToGuestDelay(
                fixture.order().getId(), fixture.accessToken(), OrderDelayDecision.REJECT);

        assertSoftly(softly -> {
            softly.assertThat(result.order().getStatus())
                    .isEqualTo(OrderStatus.DELAY_REJECTED_CANCELED);
            softly.assertThat(result.refund().getStatus()).isEqualTo(RefundStatus.REQUESTED);
            softly.assertThat(orderStateProbe.getInventoryByProductId(fixture.product().getId()).getQuantity())
                    .isEqualTo(1);
            softly.assertThat(orderStateProbe.orderApprovalHistory(fixture.order().getId()))
                    .extracting("decision")
                    .containsExactly(
                            OrderApprovalDecision.APPROVE,
                            OrderApprovalDecision.DELAY,
                            OrderApprovalDecision.DELAY_REJECT);
        });
    }

    private GuestOrderFixture createGuestMadeToOrder(String name, long price) {
        Product product = productStorePort.save(new Product(name, ProductType.MADE_TO_ORDER, price));
        inventoryStorePort.save(new Inventory(product, 1));
        Guest guest = guestStorePort.save(TestFixtures.guest("비회원 주문자", "01055556666"));
        OrderService.OrderCreationResult result = orderService.createPaidOrder(
                guest.getId(),
                List.of(new OrderService.OrderItemRequest(
                        product.getId(), product.getName(), 1, price)),
                FulfillmentType.PICKUP,
                null,
                0L,
                MadeToOrderConsent.current(LocalDateTime.of(2026, 1, 1, 0, 0)));
        return new GuestOrderFixture(product, result.order(), result.rawAccessToken());
    }

    private record GuestOrderFixture(Product product, Order order, String accessToken) {}
}
