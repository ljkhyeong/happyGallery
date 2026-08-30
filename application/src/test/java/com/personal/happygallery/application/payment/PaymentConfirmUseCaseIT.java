package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.cart.port.out.CartItemStorePort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.ShippingAddressProtector;
import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmCommand;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderTextInput;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PassPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareCommand;
import com.personal.happygallery.application.payment.port.in.PaymentStatusQueryUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderItem;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderPayload;
import com.personal.happygallery.adapter.out.persistence.cart.CartItemRepository;
import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.persistence.policy.PolicyConsentRepository;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.application.product.port.out.ProductVariantReaderPort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.cart.CartItem;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.order.MadeToOrderConsent;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.domain.policy.PolicyConsentPurpose;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductOptionType;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import static com.personal.happygallery.support.BookingTestHelper.FUTURE;
import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.acceptedPolicies;
import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UseCaseIT
@TestPropertySource(properties = "app.order.shipping-fee=3000")
class PaymentConfirmUseCaseIT {

    @Autowired PaymentPrepareUseCase prepareUseCase;
    @Autowired PaymentConfirmUseCase confirmUseCase;
    @Autowired PaymentAttemptReaderPort attemptReader;
    @Autowired PaymentStatusQueryUseCase statusQueryUseCase;
    @Autowired RefundRepository refundRepository;
    @Autowired PolicyConsentRepository policyConsentRepository;
    @Autowired OrderRepository orderReader;
    @Autowired OrderItemPort orderItemPort;
    @Autowired FulfillmentPort fulfillmentPort;
    @Autowired ShippingAddressProtector shippingAddressProtector;
    @Autowired BookingReaderPort bookingReaderPort;
    @Autowired PassPurchaseReaderPort passPurchaseReaderPort;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired ProductStorePort productStorePort;
    @Autowired ProductAdminUseCase productAdminUseCase;
    @Autowired ProductVariantReaderPort productVariantReaderPort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired PhoneVerificationStorePort phoneVerificationStorePort;
    @Autowired CartItemStorePort cartItemStorePort;
    @Autowired CartItemRepository cartItemRepository;
    @Autowired NotificationOutboxRepository notificationOutboxRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;
    @Autowired FieldEncryptor fieldEncryptor;
    @Autowired Clock clock;
    @Autowired TestCleanupSupport cleanupSupport;
    @MockitoBean PaymentPort paymentProvider;

    @BeforeEach
    void setUp() {
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenReturn(PaymentConfirmResult.success(
                        "confirmed-payment-key", "CARD", "2026-07-12T10:00:00+09:00"));
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenReturn(RefundResult.success("compensation-refund-key"));
    }

    @AfterEach
    void tearDown() {
        cartItemRepository.deleteAllInBatch();
        cleanupSupport.clearOrderData();
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("장바구니 결제는 prepare 시점 항목으로 주문하고 결제 후 추가한 수량은 남긴다")
    @Test
    void confirm_cartCheckout_consumesOnlyPreparedQuantities() {
        User user = userStorePort.save(new User("cart-payment@example.com", "hashed", "회원", "01067896789"));
        Product product = productStorePort.save(readyStockProduct("장바구니 결제 상품", 31_000L));
        inventoryStorePort.save(inventory(product, 5));
        CartItem cartItem = cartItemStorePort.save(new CartItem(
                user.getId(), product.getId(), 2, LocalDateTime.of(2026, 7, 19, 10, 0)));
        AuthContext auth = AuthContext.member(user.getId());

        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(), true),
                auth));
        cartItem.addQty(1, LocalDateTime.of(2026, 7, 19, 10, 1));
        cartItemStorePort.save(cartItem);

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                customerCommand("cart-payment-key", prepared, auth));

        var order = orderReader.findById(result.domainId()).orElseThrow();
        var remainingCartItem = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId());
        assertSoftly(softly -> {
            softly.assertThat(prepared.amount()).isEqualTo(62_000L);
            softly.assertThat(order.getTotalAmount()).isEqualTo(62_000L);
            softly.assertThat(remainingCartItem).hasValueSatisfying(item ->
                    softly.assertThat(item.getQty()).isEqualTo(1));
        });
    }

    @DisplayName("장바구니 결제는 prepare 후 같은 상품을 다시 담아도 새 항목을 제거하지 않는다")
    @Test
    void confirm_cartCheckout_preservesRecreatedCartItem() {
        User user = userStorePort.save(new User(
                "cart-recreated@example.com", "hashed", "장바구니 회원", "01078907890"));
        Product product = productStorePort.save(readyStockProduct("다시 담은 상품", 28_000L));
        inventoryStorePort.save(inventory(product, 5));
        CartItem preparedCartItem = cartItemStorePort.save(new CartItem(
                user.getId(), product.getId(), 1, LocalDateTime.of(2026, 7, 19, 11, 0)));
        AuthContext auth = AuthContext.member(user.getId());

        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(), true),
                auth));
        cartItemRepository.deleteById(preparedCartItem.getId());
        cartItemRepository.flush();
        CartItem recreatedCartItem = cartItemStorePort.save(new CartItem(
                user.getId(), product.getId(), 1, LocalDateTime.of(2026, 7, 19, 11, 1)));

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                customerCommand("cart-recreated-key", prepared, auth));

        assertSoftly(softly -> {
            softly.assertThat(orderReader.findById(result.domainId())).isPresent();
            softly.assertThat(cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId()))
                    .hasValueSatisfying(item -> {
                        softly.assertThat(item.getId()).isEqualTo(recreatedCartItem.getId());
                        softly.assertThat(item.getQty()).isEqualTo(1);
                    });
        });
    }

    @DisplayName("confirm은 상품 정보가 바뀌어도 prepare 시점 상품명과 단가로 주문을 저장한다")
    @Test
    void confirm_productPriceChanged_usesPreparedPriceSnapshot() {
        User user = userStorePort.save(new User("payment-confirm@example.com", "hashed", "회원", "01056785678"));
        Product product = productStorePort.save(readyStockProduct("확정 상품", 31_000L));
        inventoryStorePort.save(inventory(product, 5));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(new OrderItemRef(product.getId(), 1))),
                auth));
        jdbcTemplate.update(
                "UPDATE products SET name = ?, price = ? WHERE id = ?",
                "변경된 상품명", 99_000L, product.getId());

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                customerCommand("payment-key-confirm", prepared, auth));

        var attempt = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow();
        var order = orderReader.findById(result.domainId()).orElseThrow();
        var orderItems = orderItemPort.findByOrder(order);
        assertSoftly(softly -> {
            softly.assertThat(result.context()).isEqualTo(PaymentContext.ORDER);
            softly.assertThat(prepared.amount()).isEqualTo(31_000L);
            softly.assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID_APPROVAL_PENDING);
            softly.assertThat(order.getTotalAmount()).isEqualTo(31_000L);
            softly.assertThat(order.getPaymentKey()).isEqualTo("confirmed-payment-key");
            softly.assertThat(orderItems).singleElement().satisfies(item -> {
                softly.assertThat(item.getProductName()).isEqualTo("확정 상품");
                softly.assertThat(item.getProductType()).isEqualTo(ProductType.READY_STOCK);
                softly.assertThat(item.getUnitPrice()).isEqualTo(31_000L);
            });
            softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
            softly.assertThat(attempt.getPaymentKey()).isEqualTo("payment-key-confirm");
            softly.assertThat(attempt.getConfirmedPaymentKey()).isEqualTo("confirmed-payment-key");
            softly.assertThat(notificationOutboxRepository.findAll())
                    .singleElement()
                    .satisfies(outbox -> {
                        softly.assertThat(outbox.getUserId()).isEqualTo(user.getId());
                        softly.assertThat(outbox.getEventType()).isEqualTo(NotificationEventType.ORDER_PAID);
                        softly.assertThat(outbox.getAggregateId()).isEqualTo(order.getId());
                    });
        });
        verify(paymentProvider).confirm(
                "payment-key-confirm", prepared.orderId(), prepared.amount(), prepared.orderId());
    }

    @DisplayName("주문제작 결제는 별도 동의 없이는 준비하지 않고 동의 문구와 시각을 주문에 보존한다")
    @Test
    void confirm_madeToOrder_requiresAndSnapshotsConsent() {
        User user = userStorePort.save(new User(
                "made-to-order-consent@example.com", "hashed", "제작 동의 회원", "01033335555"));
        ProductAdminUseCase.ProductResult registered = productAdminUseCase.register(
                new ProductAdminUseCase.SaveProductCommand(
                        "주문제작 가죽 소품", ProductType.MADE_TO_ORDER, null,
                        120_000L, 1, null, null,
                        "재료: 소가죽\n크기: 12 x 8 cm\n사양: 내추럴 브라운",
                        "물에 젖으면 마른 천으로 닦아 주세요.", 21,
                        List.of(new ProductAdminUseCase.OptionGroupDefinition(
                                "engraving", ProductOptionType.TEXT, "각인 문구", true, 0,
                                "영문 20자", 20, 3_000L, List.of())),
                        List.of()));
        Product product = registered.product();
        Long variantId = registered.options().variants().getFirst().id();
        AuthContext auth = AuthContext.member(user.getId());

        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        user.getId(), null, null, null,
                        List.of(new OrderItemRef(
                                product.getId(), variantId,
                                List.of(new OrderTextInput("engraving", "HAPPY")), 1))),
                auth)))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("청약철회 제한 안내");

        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        user.getId(), null, null, null,
                        List.of(new OrderItemRef(
                                product.getId(), variantId,
                                List.of(new OrderTextInput("engraving", "HAPPY")), 1)), false,
                        FulfillmentType.PICKUP, null,
                        MadeToOrderConsent.CURRENT_VERSION, true),
                auth));
        product.updateDetails(
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getDescription(),
                product.getImageUrl(),
                "재료: 소가죽\n크기: 변경된 규격\n사양: 변경된 색상",
                "변경된 관리 방법",
                45);
        productStorePort.save(product);
        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                customerCommand("made-to-order-consent-payment", prepared, auth));

        var order = orderReader.findById(result.domainId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(order.getMadeToOrderConsentVersion())
                    .isEqualTo(MadeToOrderConsent.CURRENT_VERSION);
            softly.assertThat(order.getMadeToOrderConsentDisclosure())
                    .isEqualTo(MadeToOrderConsent.CURRENT_DISCLOSURE);
            softly.assertThat(order.getMadeToOrderConsentAt()).isNotNull();
            softly.assertThat(orderItemPort.findByOrder(order))
                    .singleElement()
                    .satisfies(item -> {
                        softly.assertThat(item.getProductType()).isEqualTo(ProductType.MADE_TO_ORDER);
                        softly.assertThat(item.getSpecification())
                                .isEqualTo("재료: 소가죽\n크기: 12 x 8 cm\n사양: 내추럴 브라운");
                        softly.assertThat(item.getCareInstructions())
                                .isEqualTo("물에 젖으면 마른 천으로 닦아 주세요.");
                        softly.assertThat(item.getProductionLeadDays()).isEqualTo(21);
                        softly.assertThat(item.getProductVariantId()).isEqualTo(variantId);
                        softly.assertThat(item.getBasePrice()).isEqualTo(120_000L);
                        softly.assertThat(item.getTextOptionPriceAdjustment()).isEqualTo(3_000L);
                        softly.assertThat(item.getUnitPrice()).isEqualTo(123_000L);
                        softly.assertThat(item.getOptionSnapshots())
                                .singleElement()
                                .satisfies(option -> {
                                    softly.assertThat(option.getGroupName()).isEqualTo("각인 문구");
                                    softly.assertThat(option.getValue()).isEqualTo("HAPPY");
                                });
                    });
            softly.assertThat(productVariantReaderPort.findWithSelectionsById(variantId))
                    .hasValueSatisfying(variant ->
                            softly.assertThat(variant.getQuantity()).isZero());
        });
    }

    @DisplayName("V97 이전 기성품 payload는 READY_STOCK 주문 항목으로 확정한다")
    @Test
    void confirm_legacyReadyStockPayload_normalizesProductType() {
        User user = userStorePort.save(new User(
                "legacy-ready-stock@example.com", "hashed", "구형 기성품 회원", "01077772222"));
        Product product = productStorePort.save(
                readyStockProduct("구형 기성품 결제", 54_000L));
        inventoryStorePort.save(inventory(product, 1));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        user.getId(), null, null, null,
                        List.of(new OrderItemRef(product.getId(), 1)), false,
                        FulfillmentType.PICKUP, null),
                auth));

        PreparedPaymentPayload legacyPayload = new PreparedOrderPayload(
                user.getId(),
                null,
                null,
                null,
                List.of(new PreparedOrderItem(
                        product.getId(), product.getName(), 1, product.getPrice())),
                false,
                FulfillmentType.PICKUP,
                null,
                0L,
                null);
        Long attemptId = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow().getId();
        jdbcTemplate.update(
                "UPDATE payment_attempt SET payload_enc = ? WHERE id = ?",
                fieldEncryptor.encrypt(objectMapper.writeValueAsString(legacyPayload)),
                attemptId);

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                customerCommand("legacy-ready-stock-payment", prepared, auth));

        var order = orderReader.findById(result.domainId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(orderItemPort.findByOrder(order))
                    .singleElement()
                    .satisfies(item ->
                            softly.assertThat(item.getProductType())
                                    .isEqualTo(ProductType.READY_STOCK));
            softly.assertThat(attemptReader.findById(attemptId))
                    .hasValueSatisfying(attempt ->
                            softly.assertThat(attempt.getStatus())
                                    .isEqualTo(PaymentAttemptStatus.CONFIRMED));
        });
    }

    @DisplayName("구형 주문제작 payload는 구매 조건 없이 PG 승인이나 주문 생성으로 진행하지 않는다")
    @Test
    void confirm_legacyMadeToOrderPayloadWithoutPurchaseTerms_rejectedBeforePgCall() {
        User user = userStorePort.save(new User(
                "legacy-made-to-order@example.com", "hashed", "구형 결제 회원", "01077773333"));
        Product product = productAdminUseCase.register(
                new ProductAdminUseCase.SaveProductCommand(
                        "구형 주문제작 결제", ProductType.MADE_TO_ORDER, null,
                        95_000L, 1, null, null,
                        "재료: 월넛\n크기: 20 x 12 cm\n사양: 오일 마감",
                        null, 14, List.of(), List.of()))
                .product();
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        user.getId(), null, null, null,
                        List.of(new OrderItemRef(product.getId(), 1)), false,
                        FulfillmentType.PICKUP, null,
                        MadeToOrderConsent.CURRENT_VERSION, true),
                auth));

        PreparedPaymentPayload legacyPayload = new PreparedOrderPayload(
                user.getId(),
                null,
                null,
                null,
                List.of(new PreparedOrderItem(
                        product.getId(), product.getName(), 1, product.getPrice())),
                false,
                FulfillmentType.PICKUP,
                null,
                0L,
                MadeToOrderConsent.current(LocalDateTime.now(clock)));
        Long attemptId = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow().getId();
        jdbcTemplate.update(
                "UPDATE payment_attempt SET payload_enc = ? WHERE id = ?",
                fieldEncryptor.encrypt(objectMapper.writeValueAsString(legacyPayload)),
                attemptId);

        assertThatThrownBy(() -> confirmUseCase.confirm(
                customerCommand("legacy-made-to-order-payment", prepared, auth)))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("구매 조건이 저장되지 않은 이전 결제");

        assertSoftly(softly -> {
            softly.assertThat(orderReader.count()).isZero();
            softly.assertThat(attemptReader.findById(attemptId))
                    .hasValueSatisfying(attempt ->
                            softly.assertThat(attempt.getStatus())
                                    .isEqualTo(PaymentAttemptStatus.PENDING));
        });
        verify(paymentProvider, never()).confirm(any(), any(), anyLong(), any());
    }

    @DisplayName("배송 주문 confirm은 주문 시점 배송지를 암호문으로 고정한다")
    @Test
    void confirm_shippingOrder_storesEncryptedAddressSnapshot() {
        User user = userStorePort.save(new User(
                "shipping-snapshot@example.com", "hashed", "배송 회원", "01012345678"));
        Product product = productStorePort.save(readyStockProduct("배송 스냅샷 상품", 44_000L));
        inventoryStorePort.save(inventory(product, 1));
        ShippingAddress address = new ShippingAddress(
                "받는 사람", "010-9876-5432", "06236", "서울시 강남구 테헤란로 1", "2층");
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        user.getId(), null, null, null,
                        List.of(new OrderItemRef(product.getId(), 1)), false,
                        FulfillmentType.SHIPPING, address),
                auth));

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                customerCommand("shipping-payment-key", prepared, auth));

        var fulfillment = fulfillmentPort.findByOrderId(result.domainId()).orElseThrow();
        var order = orderReader.findById(result.domainId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(prepared.amount()).isEqualTo(47_000L);
            softly.assertThat(order.getShippingFee()).isEqualTo(3_000L);
            softly.assertThat(order.getTotalAmount()).isEqualTo(47_000L);
            softly.assertThat(fulfillment.getType()).isEqualTo(FulfillmentType.SHIPPING);
            softly.assertThat(fulfillment.getShippingAddressEnc())
                    .isNotBlank()
                    .doesNotContain(address.addressLine1())
                    .doesNotContain(address.phone());
            softly.assertThat(shippingAddressProtector.decrypt(fulfillment.getShippingAddressEnc()))
                    .isEqualTo(address);
        });
    }

    @DisplayName("비회원 주문은 원 인증 코드 만료 뒤에도 결제 귀속 증거로 생성되며 재호출은 멱등하다")
    @Test
    void confirm_completedGuestOrder_returnsStoredResultIdempotently() {
        String phone = "01090908080";
        String verificationCode = "654321";
        saveVerification(phone, verificationCode, PhoneVerificationPurpose.GUEST_ORDER);
        Product product = productStorePort.save(readyStockProduct("비회원 멱등 주문 상품", 47_000L));
        inventoryStorePort.save(inventory(product, 2));
        AuthContext auth = AuthContext.guest();
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                guestOrderPayload(phone, verificationCode, product.getId()),
                auth));
        jdbcTemplate.update(
                "UPDATE phone_verifications SET expires_at = ? WHERE verified = true",
                LocalDateTime.now(clock).minusMinutes(1));
        ConfirmCommand command = customerCommand("guest-idempotent-payment-key", prepared, auth);

        PaymentConfirmUseCase.ConfirmResult first = confirmUseCase.confirm(command);
        PaymentConfirmUseCase.ConfirmResult replay = confirmUseCase.confirm(command);

        User claimedUser = userStorePort.save(
                new User("claimed-payment@example.com", "hashed", "전환 회원", phone));
        var claimedOrder = orderReader.findById(first.domainId()).orElseThrow();
        claimedOrder.claimToUser(claimedUser.getId());
        orderReader.save(claimedOrder);
        PaymentConfirmUseCase.ConfirmResult afterClaim = confirmUseCase.confirm(command);
        var statusAfterClaim = statusQueryUseCase.getStatus(
                prepared.orderId(), auth, prepared.statusToken());

        var attempt = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(replay).isEqualTo(first);
            softly.assertThat(first.accessToken()).isNotBlank();
            softly.assertThat(first.accessRecoveryRequired()).isFalse();
            softly.assertThat(afterClaim.accessToken()).isNull();
            softly.assertThat(afterClaim.accessRecoveryRequired()).isTrue();
            softly.assertThat(statusAfterClaim.accessToken()).isNull();
            softly.assertThat(statusAfterClaim.accessRecoveryRequired()).isTrue();
            softly.assertThat(orderReader.count()).isOne();
            softly.assertThat(attempt.getFulfilledDomainId()).isEqualTo(first.domainId());
            softly.assertThat(attempt.getFulfilledAccessTokenEnc())
                    .isNotBlank()
                    .doesNotContain(first.accessToken());
        });
        verify(paymentProvider).confirm(
                "guest-idempotent-payment-key", prepared.orderId(), prepared.amount(), prepared.orderId());
    }

    @DisplayName("비회원 confirm은 prepare에서 발급한 상태 토큰이 없으면 결제 존재를 숨긴다")
    @Test
    void confirm_guestWithoutStatusToken_returnsNotFoundBeforePgCall() {
        Product product = productStorePort.save(readyStockProduct("비회원 소유권 검증 상품", 31_000L));
        inventoryStorePort.save(inventory(product, 1));
        saveVerification(
                "01010101234", "123456", PhoneVerificationPurpose.GUEST_ORDER);
        AuthContext auth = AuthContext.guest();
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                guestOrderPayload("01010101234", "123456", product.getId()),
                auth));

        assertThatThrownBy(() -> confirmUseCase.confirm(ConfirmCommand.customerRequest(
                "guest-owner-payment-key", prepared.orderId(), prepared.amount(), auth, null)))
                .isInstanceOf(NotFoundException.class);

        assertThat(attemptReader.findByOrderIdExternal(prepared.orderId()))
                .hasValueSatisfying(attempt -> assertThat(attempt.getStatus())
                        .isEqualTo(PaymentAttemptStatus.PENDING));
        verify(paymentProvider, never()).confirm(any(), any(), anyLong(), any());
    }

    @DisplayName("비회원 예약은 인증 귀속 증거와 PG가 확정한 실제 결제수단으로 생성한다")
    @Test
    void confirm_guestBooking_usesPrepareVerificationProof() {
        String phone = "01045456767";
        String verificationCode = "456789";
        saveVerification(phone, verificationCode, PhoneVerificationPurpose.GUEST_BOOKING);
        BookingClass bookingClass = classStorePort.save(
                bookingClass("비회원 증거 클래스", "CRAFT", 120, 60_000L, 30));
        Slot slot = slotStorePort.save(slot(bookingClass, FUTURE, FUTURE.plusHours(2)));
        AuthContext auth = AuthContext.guest();
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                new BookingPayload(
                        null, phone, verificationCode, "비회원 예약자",
                        slot.getId(), null, DepositPaymentMethod.EASY_PAY, acceptedPolicies()),
                auth));
        jdbcTemplate.update(
                "UPDATE phone_verifications SET expires_at = ? WHERE verified = true",
                LocalDateTime.now(clock).minusMinutes(1));

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                customerCommand("guest-booking-proof-key", prepared, auth));

        Long attemptId = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow().getId();
        assertThat(bookingReaderPort.findById(result.domainId()))
                .hasValueSatisfying(booking -> assertSoftly(softly -> {
                    softly.assertThat(booking.getGuest()).isNotNull();
                    softly.assertThat(booking.getDepositAmount()).isEqualTo(6_000L);
                    softly.assertThat(booking.getPaymentMethod()).isEqualTo(DepositPaymentMethod.CARD);
                    softly.assertThat(booking.getPaymentKey()).isEqualTo("confirmed-payment-key");
                    softly.assertThat(policyConsentRepository.findByPaymentAttemptIdOrderById(attemptId))
                            .hasSize(2)
                            .allSatisfy(consent -> assertThat(consent.getPurpose())
                                    .isEqualTo(PolicyConsentPurpose.GUEST_BOOKING_PAYMENT));
                }));
    }

    @DisplayName("confirm은 다인 예약의 prepare 시점 인원과 금액 스냅샷을 저장한다")
    @Test
    void confirm_bookingClassPriceChanged_usesPreparedPriceSnapshot() {
        User user = userStorePort.save(
                new User("booking-payment-confirm@example.com", "hashed", "예약 회원", "01034563456"));
        BookingClass bookingClass = classStorePort.save(
                bookingClass("예약금 확정 클래스", "CRAFT", 120, 50_000L, 30));
        Slot slot = slotStorePort.save(slot(bookingClass, FUTURE, FUTURE.plusHours(2)));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                new BookingPayload(
                        user.getId(), null, null, null, slot.getId(), null,
                        DepositPaymentMethod.CARD, 3, null),
                auth));
        jdbcTemplate.update("UPDATE classes SET price = ? WHERE id = ?", 90_000L, bookingClass.getId());

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                customerCommand("booking-payment-key", prepared, auth));

        var booking = bookingReaderPort.findById(result.domainId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(prepared.amount()).isEqualTo(15_000L);
            softly.assertThat(booking.getParticipantCount()).isEqualTo(3);
            softly.assertThat(booking.getDepositAmount()).isEqualTo(15_000L);
            softly.assertThat(booking.getDepositPaidAt()).isEqualTo(LocalDateTime.now(clock));
            softly.assertThat(booking.getBalanceAmount()).isEqualTo(135_000L);
            softly.assertThat(booking.getPaymentKey()).isEqualTo("confirmed-payment-key");
            softly.assertThat(jdbcTemplate.queryForObject(
                    "SELECT booked_count FROM slots WHERE id = ?",
                    Integer.class,
                    slot.getId())).isEqualTo(3);
        });
    }

    @DisplayName("confirm은 prepare에서 확정한 8회권 가격을 구매 내역에 저장한다")
    @Test
    void confirm_passPurchase_usesPreparedPriceSnapshot() {
        User user = userStorePort.save(
                new User("pass-payment-confirm@example.com", "hashed", "이용권 회원", "01045674567"));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.PASS,
                new PassPayload(user.getId()),
                auth));

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                customerCommand("pass-payment-key", prepared, auth));

        var passPurchase = passPurchaseReaderPort.findById(result.domainId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(passPurchase.getTotalPrice()).isEqualTo(prepared.amount());
            softly.assertThat(passPurchase.getPaymentKey()).isEqualTo("confirmed-payment-key");
        });
    }

    @DisplayName("confirm은 결제를 준비한 회원과 다른 회원이면 PG 호출 전에 거부한다")
    @Test
    void confirm_differentMember_rejectsBeforePgCall() {
        User owner = userStorePort.save(
                new User("payment-owner@example.com", "hashed", "준비 회원", "01011110000"));
        User other = userStorePort.save(
                new User("payment-other@example.com", "hashed", "다른 회원", "01022220000"));
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.PASS,
                new PassPayload(owner.getId()),
                AuthContext.member(owner.getId())));

        assertThatThrownBy(() -> confirmUseCase.confirm(ConfirmCommand.customerRequest(
                "payment-key-other", prepared.orderId(), prepared.amount(),
                AuthContext.member(other.getId()), null)))
                .isInstanceOf(NotFoundException.class);

        assertThat(attemptReader.findByOrderIdExternal(prepared.orderId()))
                .hasValueSatisfying(attempt -> assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PENDING));
        verify(paymentProvider, never()).confirm(any(), any(), anyLong(), any());
    }

    @DisplayName("confirm은 prepare 금액과 다른 금액이면 도메인 저장 전에 거부한다")
    @Test
    void confirm_amountTampered_rejectsBeforeFulfillment() {
        User user = userStorePort.save(new User("payment-tamper@example.com", "hashed", "회원", "01087654321"));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.PASS,
                new PassPayload(user.getId()),
                auth));

        assertThatThrownBy(() -> confirmUseCase.confirm(
                ConfirmCommand.customerRequest(
                        "payment-key-tampered", prepared.orderId(), prepared.amount() - 1,
                        auth, prepared.statusToken())))
                .isInstanceOfSatisfying(HappyGalleryException.class, e ->
                        assertSoftly(softly -> {
                            softly.assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                            softly.assertThat(e.getMessage()).contains("결제 금액");
                        }));

        assertThat(attemptReader.findByOrderIdExternal(prepared.orderId()))
                .hasValueSatisfying(attempt -> assertThat(attempt.getStatus())
                        .isEqualTo(PaymentAttemptStatus.PENDING));
    }

    @DisplayName("PG 확정 실패는 외부 호출 트랜잭션과 분리되어 FAILED 상태로 저장된다")
    @Test
    void confirm_pgFailure_persistsFailedAttemptOutsidePaymentTransaction() {
        User user = userStorePort.save(new User("payment-failure@example.com", "hashed", "회원", "01011112222"));
        Product product = productStorePort.save(readyStockProduct("확정 실패 상품", 41_000L));
        inventoryStorePort.save(inventory(product, 1));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(new OrderItemRef(product.getId(), 1))),
                auth));
        AtomicBoolean transactionActiveDuringPgCall = new AtomicBoolean(true);
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenAnswer(invocation -> {
                    transactionActiveDuringPgCall.set(
                            TransactionSynchronizationManager.isActualTransactionActive());
                    return PaymentConfirmResult.failure("PG 승인 거절");
                });
        ConfirmCommand command = customerCommand("payment-key-failure", prepared, auth);

        assertThatThrownBy(() -> confirmUseCase.confirm(command))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception -> assertSoftly(softly -> {
                    softly.assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_FAILED);
                    softly.assertThat(exception.getErrorCode().httpStatus).isEqualTo(502);
                }));
        assertThatThrownBy(() -> confirmUseCase.confirm(command))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception -> assertSoftly(softly -> {
                    softly.assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_FAILED);
                    softly.assertThat(exception.getErrorCode().httpStatus).isEqualTo(502);
                }));

        var attempt = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(transactionActiveDuringPgCall.get()).isFalse();
            softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
            softly.assertThat(attempt.getFailReason()).isEqualTo("PG 승인 거절");
            softly.assertThat(orderReader.count()).isZero();
        });
        verify(paymentProvider).confirm(
                "payment-key-failure", prepared.orderId(), prepared.amount(), prepared.orderId());
    }

    @DisplayName("PG 일시 실패는 재시도 가능 오류로 응답하고 같은 멱등키 재호출로 확정한다")
    @Test
    void confirm_retryablePgFailure_exposesRetryableErrorAndReusesIdempotencyKey() {
        User user = userStorePort.save(new User(
                "payment-retryable@example.com", "hashed", "회원", "01012123434"));
        Product product = productStorePort.save(readyStockProduct("재시도 상품", 42_000L));
        inventoryStorePort.save(inventory(product, 1));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        user.getId(), null, null, null,
                        List.of(new OrderItemRef(product.getId(), 1))),
                auth));
        ConfirmCommand command = customerCommand("payment-key-retryable", prepared, auth);
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenReturn(
                        PaymentConfirmResult.retryableFailure("PG 일시 장애"),
                        PaymentConfirmResult.success(
                                "payment-key-retryable", "CARD", "2026-07-20T10:00:00+09:00"));

        assertThatThrownBy(() -> confirmUseCase.confirm(command))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception -> assertSoftly(softly -> {
                    softly.assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.PAYMENT_CONFIRM_RETRYABLE);
                    softly.assertThat(exception.getErrorCode().httpStatus).isEqualTo(503);
                }));
        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(command);

        assertSoftly(softly -> {
            softly.assertThat(orderReader.findById(result.domainId())).isPresent();
            softly.assertThat(attemptReader.findByOrderIdExternal(prepared.orderId()))
                    .hasValueSatisfying(attempt -> softly.assertThat(attempt.getStatus())
                            .isEqualTo(PaymentAttemptStatus.CONFIRMED));
        });
        verify(paymentProvider, times(2)).confirm(
                "payment-key-retryable", prepared.orderId(), prepared.amount(), prepared.orderId());
    }

    @DisplayName("배치가 실행되지 않아도 30분이 지난 PENDING 결제는 confirm 시 만료된다")
    @Test
    void confirm_expiredPendingAttempt_cancelsBeforePgCall() {
        User user = userStorePort.save(new User(
                "payment-point-of-use-expiry@example.com", "hashed", "회원", "01022223333"));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.PASS,
                new PassPayload(user.getId()),
                auth));
        LocalDateTime expiredCreatedAt = LocalDateTime.ofInstant(
                clock.instant().minus(DefaultPaymentAttemptExpiryBatchService.PREPARE_TTL).minusSeconds(1),
                ZoneOffset.UTC);
        jdbcTemplate.update(
                "UPDATE payment_attempt SET created_at = ? WHERE order_id_external = ?",
                expiredCreatedAt,
                prepared.orderId());

        assertThatThrownBy(() -> confirmUseCase.confirm(
                customerCommand("expired-payment-key", prepared, auth)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ATTEMPT_EXPIRED));

        var attempt = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.CANCELED);
            softly.assertThat(attempt.getPayloadEnc()).isNull();
        });
        verify(paymentProvider, never()).confirm(any(), any(), anyLong(), any());
    }

    @DisplayName("PG 승인 후 도메인 생성이 실패하면 결제 시도 보상 환불을 실행한다")
    @Test
    void confirm_fulfillmentFailure_compensatesApprovedPayment() throws InterruptedException {
        CountDownLatch refundStarted = new CountDownLatch(1);
        CountDownLatch allowRefundCompletion = new CountDownLatch(1);
        when(paymentProvider.refund(any(), anyLong(), any())).thenAnswer(invocation -> {
            refundStarted.countDown();
            if (!allowRefundCompletion.await(3, TimeUnit.SECONDS)) {
                return RefundResult.retryableFailure("테스트 환불 대기 시간 초과");
            }
            return RefundResult.success("compensation-refund-key");
        });
        User user = userStorePort.save(new User("payment-compensation@example.com", "hashed", "회원", "01033334444"));
        Product product = productStorePort.save(readyStockProduct("보상 환불 상품", 52_000L));
        var availableInventory = inventoryStorePort.save(inventory(product, 1));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(new OrderItemRef(product.getId(), 1))),
                auth));
        availableInventory.deduct(1);
        inventoryStorePort.save(availableInventory);

        assertThatThrownBy(() -> confirmUseCase.confirm(
                customerCommand("payment-key-compensation", prepared, auth)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVENTORY_NOT_ENOUGH));

        try {
            assertThat(refundStarted.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(jdbcTemplate.update(
                    "UPDATE payment_attempt SET status = 'COMPENSATION_FAILED' WHERE order_id_external = ?",
                    prepared.orderId())).isOne();
            assertThat(statusQueryUseCase.getStatus(prepared.orderId(), auth, null).status())
                    .isEqualTo(PaymentStatusQueryUseCase.CustomerPaymentStatus.REFUNDING);
        } finally {
            allowRefundCompletion.countDown();
        }

        await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(25, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var attempt = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow();
                    var refunds = refundRepository.findAll();
                    assertSoftly(softly -> {
                        softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.COMPENSATED);
                        softly.assertThat(refunds).singleElement().satisfies(refund -> {
                            softly.assertThat(refund.getPaymentAttemptId()).isEqualTo(attempt.getId());
                            softly.assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
                            softly.assertThat(refund.getPaymentKey()).isEqualTo("confirmed-payment-key");
                        });
                    });
                });

        var refund = refundRepository.findAll().getFirst();
        var customerStatus = statusQueryUseCase.getStatus(prepared.orderId(), auth, null);
        assertThat(customerStatus.status())
                .isEqualTo(PaymentStatusQueryUseCase.CustomerPaymentStatus.REFUNDED);
        assertThatThrownBy(() -> statusQueryUseCase.getStatus(
                prepared.orderId(), AuthContext.member(user.getId() + 1), null))
                .isInstanceOf(NotFoundException.class);
        verify(paymentProvider).refund(
                "confirmed-payment-key", prepared.amount(), refund.getIdempotencyKey());
    }

    @DisplayName("PG 승인 직전에 회원이 탈퇴 상태가 되면 도메인을 만들지 않고 보상 환불한다")
    @Test
    void confirm_withdrawnMemberAfterPrepare_compensatesApprovedPayment() {
        User user = userStorePort.save(new User(
                "payment-withdraw-race@example.com", "hashed", "회원", "01044445555"));
        Product product = productStorePort.save(readyStockProduct("탈퇴 경합 상품", 54_000L));
        inventoryStorePort.save(inventory(product, 1));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        user.getId(), null, null, null,
                        List.of(new OrderItemRef(product.getId(), 1))),
                auth));
        assertThat(jdbcTemplate.update(
                "UPDATE users SET withdrawn_at = ? WHERE id = ?",
                LocalDateTime.now(clock),
                user.getId())).isOne();

        assertThatThrownBy(() -> confirmUseCase.confirm(
                customerCommand("payment-key-withdraw-race", prepared, auth)))
                .isInstanceOf(NotFoundException.class);

        await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(25, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var attempt = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow();
                    assertSoftly(softly -> {
                        softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.COMPENSATED);
                        softly.assertThat(orderReader.count()).isZero();
                        softly.assertThat(refundRepository.findAll())
                                .singleElement()
                                .satisfies(refund -> {
                                    softly.assertThat(refund.getPaymentAttemptId()).isEqualTo(attempt.getId());
                                    softly.assertThat(refund.getPaymentKey()).isEqualTo("confirmed-payment-key");
                                });
                    });
                });

        var refund = refundRepository.findAll().getFirst();
        verify(paymentProvider).refund(
                "confirmed-payment-key", prepared.amount(), refund.getIdempotencyKey());
    }

    @DisplayName("동시에 같은 결제를 확정하면 한 요청만 PG 호출과 주문 생성을 수행한다")
    @Test
    void confirm_concurrently_claimsSingleAttempt() throws Exception {
        User user = userStorePort.save(new User("payment-concurrent@example.com", "hashed", "회원", "01055556666"));
        Product product = productStorePort.save(readyStockProduct("동시 확정 상품", 63_000L));
        inventoryStorePort.save(inventory(product, 2));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(new OrderItemRef(product.getId(), 1))),
                auth));
        ConfirmCommand command = customerCommand("payment-key-concurrent", prepared, auth);
        CountDownLatch pgEntered = new CountDownLatch(1);
        CountDownLatch releasePg = new CountDownLatch(1);
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenAnswer(invocation -> {
                    pgEntered.countDown();
                    if (!releasePg.await(3, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("PG 호출 해제 대기 시간 초과");
                    }
                    return PaymentConfirmResult.success(
                            "confirmed-payment-key", "CARD", "2026-07-12T10:00:00+09:00");
                });
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(() -> confirmUseCase.confirm(command));
            assertThat(pgEntered.await(3, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> confirmUseCase.confirm(command));

            assertThatThrownBy(second::get)
                    .isInstanceOfSatisfying(ExecutionException.class, exception ->
                            assertThat(exception.getCause())
                                    .isInstanceOfSatisfying(HappyGalleryException.class, cause ->
                                            assertThat(cause.getErrorCode())
                                                    .isEqualTo(ErrorCode.PAYMENT_CONFIRM_IN_PROGRESS)));

            releasePg.countDown();
            PaymentConfirmUseCase.ConfirmResult result = first.get(3, TimeUnit.SECONDS);
            assertSoftly(softly -> {
                softly.assertThat(orderReader.findById(result.domainId())).isPresent();
                softly.assertThat(attemptReader.findByOrderIdExternal(prepared.orderId()))
                        .hasValueSatisfying(attempt -> softly.assertThat(attempt.getStatus())
                                .isEqualTo(PaymentAttemptStatus.CONFIRMED));
            });
            verify(paymentProvider).confirm(
                    "payment-key-concurrent", prepared.orderId(), prepared.amount(), prepared.orderId());
        } finally {
            releasePg.countDown();
            executor.shutdownNow();
        }
    }

    @DisplayName("stale confirm의 늦은 PG 실패는 새 실행권이 완료한 결제 결과를 덮지 않는다")
    @Test
    void confirm_stalePgFailure_returnsLatestCompletedResult() throws Exception {
        User user = userStorePort.save(new User(
                "payment-stale@example.com", "hashed", "회원", "01066667777"));
        Product product = productStorePort.save(readyStockProduct("stale 확정 상품", 64_000L));
        inventoryStorePort.save(inventory(product, 2));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(new OrderItemRef(product.getId(), 1))),
                auth));
        ConfirmCommand command = customerCommand("payment-key-stale", prepared, auth);
        CountDownLatch firstPgEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstPg = new CountDownLatch(1);
        AtomicInteger pgCalls = new AtomicInteger();
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenAnswer(invocation -> {
                    if (pgCalls.getAndIncrement() == 0) {
                        firstPgEntered.countDown();
                        if (!releaseFirstPg.await(3, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("첫 PG 호출 해제 대기 시간 초과");
                        }
                        return PaymentConfirmResult.failure("늦은 PG 실패");
                    }
                    return PaymentConfirmResult.success(
                            "confirmed-payment-key", "CARD", "2026-07-12T10:00:00+09:00");
                });
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var staleConfirm = executor.submit(() -> confirmUseCase.confirm(command));
            assertThat(firstPgEntered.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(jdbcTemplate.update(
                    "UPDATE payment_attempt SET processing_at = ? WHERE order_id_external = ?",
                    LocalDateTime.now(clock).minusMinutes(2), prepared.orderId())).isOne();

            var latestConfirm = executor.submit(() -> confirmUseCase.confirm(command));
            PaymentConfirmUseCase.ConfirmResult latestResult = latestConfirm.get(3, TimeUnit.SECONDS);
            releaseFirstPg.countDown();
            PaymentConfirmUseCase.ConfirmResult staleResult = staleConfirm.get(3, TimeUnit.SECONDS);

            var attempt = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow();
            assertSoftly(softly -> {
                softly.assertThat(staleResult).isEqualTo(latestResult);
                softly.assertThat(orderReader.count()).isOne();
                softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
            });
            verify(paymentProvider, times(2)).confirm(
                    "payment-key-stale", prepared.orderId(), prepared.amount(), prepared.orderId());
        } finally {
            releaseFirstPg.countDown();
            executor.shutdownNow();
        }
    }

    private OrderPayload guestOrderPayload(String phone, String verificationCode, Long productId) {
        return new OrderPayload(
                null,
                phone,
                verificationCode,
                "비회원",
                List.of(new OrderItemRef(productId, 1)),
                false,
                FulfillmentType.PICKUP,
                null,
                null,
                false,
                acceptedPolicies());
    }

    private void saveVerification(
            String phone,
            String code,
            PhoneVerificationPurpose purpose) {
        PhoneVerification verification = new PhoneVerification(
                phone, code, purpose, LocalDateTime.now(clock).plusMinutes(5));
        verification.markDelivered();
        phoneVerificationStorePort.save(verification);
    }

    private ConfirmCommand customerCommand(String paymentKey,
                                           PaymentPrepareUseCase.PrepareResult prepared,
                                           AuthContext auth) {
        return ConfirmCommand.customerRequest(
                paymentKey, prepared.orderId(), prepared.amount(), auth, prepared.statusToken());
    }
}
