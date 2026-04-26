package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.pass.PassPriceProperties;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PassPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareCommand;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.BookingTestHelper.FUTURE;
import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class PaymentPrepareUseCaseTest {

    @Autowired PaymentPrepareUseCase prepareUseCase;
    @Autowired PaymentAttemptReaderPort attemptReader;
    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired PassPriceProperties passPriceProperties;
    @Autowired TestCleanupSupport cleanupSupport;

    @BeforeEach
    void setUp() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("prepare는 주문 예약 8회권 금액을 서버 기준으로 확정하고 결제 시도를 저장한다")
    @Test
    void prepare_calculatesServerOwnedAmountsAndStoresAttempts() {
        User user = userStorePort.save(new User("payment-prepare@example.com", "hashed", "회원", "01012341234"));
        Product product = productStorePort.save(readyStockProduct("서버 가격 상품", 29_000L));
        inventoryStorePort.save(inventory(product, 10));
        BookingClass cls = classStorePort.save(bookingClass("금액 클래스", "CRAFT", 120, 50_000L, 30));
        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        AuthContext auth = AuthContext.member(user.getId());

        PaymentPrepareUseCase.PrepareResult order = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(new OrderItemRef(product.getId(), 2))),
                auth));
        PaymentPrepareUseCase.PrepareResult booking = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                new BookingPayload(user.getId(), null, null, null, slot.getId(), null, DepositPaymentMethod.CARD),
                auth));
        PaymentPrepareUseCase.PrepareResult pass = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.PASS,
                new PassPayload(user.getId()),
                auth));

        assertSoftly(softly -> {
            softly.assertThat(order.amount()).isEqualTo(58_000L);
            softly.assertThat(booking.amount()).isEqualTo(5_000L);
            softly.assertThat(pass.amount()).isEqualTo(passPriceProperties.totalPrice());
            softly.assertThat(attemptReader.findByOrderIdExternal(order.orderId()))
                    .hasValueSatisfying(attempt -> {
                        softly.assertThat(attempt.getContext()).isEqualTo(PaymentContext.ORDER);
                        softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PENDING);
                    });
        });
    }
}
