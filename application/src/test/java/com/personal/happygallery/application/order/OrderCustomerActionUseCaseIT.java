package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.OrderShippingAddressUseCase;
import com.personal.happygallery.application.order.port.in.OrderShippingUseCase;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
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

    @Autowired OrderShippingAddressUseCase addressUseCase;
    @Autowired OrderShippingUseCase shippingUseCase;
    @Autowired FulfillmentPort fulfillmentPort;
    @Autowired ShippingAddressProtector addressProtector;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    OrderTestHelper orderHelper;

    private static final ShippingAddress UPDATED_ADDRESS = new ShippingAddress(
            "새 수령인", "01012345678", "12345", "서울시 변경 주소 12", "201호");

    @Test
    @DisplayName("본인 주문의 배송지를 암호화해 변경하고 이전 버전과 다른 고객의 수정은 거절한다")
    void updateShippingAddress_preservesAuditAndRejectsStaleVersion() {
        var fixture = orderHelper.createReadyStockPaidShippingOrder("주소 변경 상품", 40_000L, 3_000L);
        var order = fixture.order();
        var before = fulfillmentPort.findByOrderId(order.getId()).orElseThrow();
        assertThatThrownBy(() -> addressUseCase.updateMember(order.getId(), Long.MAX_VALUE,
                before.getVersion(), UPDATED_ADDRESS)).isInstanceOf(NotFoundException.class);
        addressUseCase.updateMember(order.getId(), order.getUserId(), before.getVersion(), UPDATED_ADDRESS);
        var after = fulfillmentPort.findByOrderId(order.getId()).orElseThrow();
        assertThat(addressProtector.decrypt(after.getShippingAddressEnc())).isEqualTo(UPDATED_ADDRESS);
        var audit = jdbc.queryForMap("SELECT * FROM shipping_address_changes WHERE order_id = ?", order.getId());
        assertThat(audit.get("before_address_enc")).isEqualTo(before.getShippingAddressEnc());
        assertThat(audit.get("after_address_enc")).isEqualTo(after.getShippingAddressEnc());
        assertThat(audit.get("user_id")).isEqualTo(order.getUserId());
        assertThat(after.getShippingAddressEnc()).doesNotContain(UPDATED_ADDRESS.addressLine1());
        assertThat(orderStateProbe.getOrder(order.getId()).getTotalAmount()).isEqualTo(43_000L);
        assertThatThrownBy(() -> addressUseCase.updateMember(order.getId(), order.getUserId(),
                before.getVersion(), UPDATED_ADDRESS)).isInstanceOfSatisfying(HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        var pickup = orderHelper.createReadyStockPaidOrder("픽업 변경 거절 상품", 20_000L).order();
        assertThatThrownBy(() -> addressUseCase.updateMember(pickup.getId(), pickup.getUserId(),
                0L, UPDATED_ADDRESS)).isInstanceOf(HappyGalleryException.class);
    }

    @Test
    @DisplayName("비회원은 주문 조회 코드로 배송지를 변경하고 잘못된 코드는 거절된다")
    void updateGuestAddress_requiresAccessToken() {
        var fixture = createGuestMadeToOrder("비회원 주소 상품", 50_000L, FulfillmentType.SHIPPING);
        var order = fixture.order();
        long version = fulfillmentPort.findByOrderId(order.getId()).orElseThrow().getVersion();
        assertThatThrownBy(() -> addressUseCase.updateGuest(order.getId(), "wrong-token", version,
                UPDATED_ADDRESS)).isInstanceOf(NotFoundException.class);
        addressUseCase.updateGuest(order.getId(), fixture.accessToken(), version, UPDATED_ADDRESS);
        assertThat(jdbc.queryForObject("SELECT guest_id FROM shipping_address_changes WHERE order_id = ?",
                Long.class, order.getId())).isEqualTo(order.getGuestId());
    }

    @Test
    @DisplayName("배송 준비가 주문 잠금을 먼저 확보하면 동시에 요청된 배송지 수정은 거절된다")
    void shippingPreparation_serializesAddressChange() throws Exception {
        var order = orderHelper.createReadyStockPaidShippingOrder("동시 주소 상품", 30_000L, 3_000L).order();
        orderApprovalUseCase.approve(order.getId(), 1L);
        var original = fulfillmentPort.findByOrderId(order.getId()).orElseThrow();
        var prepared = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var preparing = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                shippingUseCase.prepareShipping(order.getId(), 1L);
                prepared.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("테스트 대기 시간 초과");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }));
            try {
                assertThat(prepared.await(10, TimeUnit.SECONDS)).isTrue();
                var updating = executor.submit(() -> assertThatThrownBy(() -> addressUseCase.updateMember(
                        order.getId(), order.getUserId(), original.getVersion(), UPDATED_ADDRESS))
                        .isInstanceOf(HappyGalleryException.class));
                release.countDown();
                preparing.get(10, TimeUnit.SECONDS);
                updating.get(10, TimeUnit.SECONDS);
            } finally {
                release.countDown();
            }
        }
        assertThat(fulfillmentPort.findByOrderId(order.getId()).orElseThrow().getShippingAddressEnc())
                .isEqualTo(original.getShippingAddressEnc());
    }


    @BeforeEach
    void setUp() {
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
        return createGuestMadeToOrder(name, price, FulfillmentType.PICKUP);
    }

    private GuestOrderFixture createGuestMadeToOrder(String name, long price, FulfillmentType type) {
        Product product = productStorePort.save(new Product(
                name,
                ProductType.MADE_TO_ORDER,
                null,
                price,
                null,
                null,
                "재료: 가죽\n크기: 고정 규격\n사양: 기본 색상",
                "물에 젖지 않게 보관하세요.",
                14));
        inventoryStorePort.save(new Inventory(product, 1));
        Guest guest = guestStorePort.save(TestFixtures.guest("비회원 주문자", "01055556666"));
        OrderService.OrderCreationResult result = orderService.createPaidOrder(
                guest.getId(),
                List.of(new OrderService.OrderItemRequest(
                        product.getId(), product.getName(), product.getType(), 1, price,
                        product.getSpecification(), product.getCareInstructions(),
                        product.getProductionLeadDays())),
                type,
                type == FulfillmentType.SHIPPING ? UPDATED_ADDRESS : null,
                0L,
                MadeToOrderConsent.current(LocalDateTime.of(2026, 1, 1, 0, 0)));
        return new GuestOrderFixture(product, result.order(), result.rawAccessToken());
    }

    private record GuestOrderFixture(Product product, Order order, String accessToken) {}
}
