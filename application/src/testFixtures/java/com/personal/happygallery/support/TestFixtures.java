package com.personal.happygallery.support;

import com.personal.happygallery.application.crypto.VersionedFieldEncryptor;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderItem;
import com.personal.happygallery.application.policy.PolicyAcceptance;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.order.OrderItemPricing;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.pass.PassPlan;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 테스트 엔티티 생성 유틸.
 */
public final class TestFixtures {

    private static final String TEST_ENCRYPT_KEY =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String TEST_HMAC_KEY =
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
    private static final FieldEncryptor TEST_FIELD_ENCRYPTOR =
            new VersionedFieldEncryptor("v1", Map.of("v1", HexFormat.of().parseHex(TEST_ENCRYPT_KEY)));
    private static final BlindIndexer TEST_BLIND_INDEXER =
            new BlindIndexer(HexFormat.of().parseHex(TEST_HMAC_KEY));

    private TestFixtures() {
    }

    public static BookingClass defaultBookingClass() {
        return new BookingClass("향수 클래스", "PERFUME", 120, 50_000L, 30);
    }

    public static BookingClass bookingClass(String name, String category, int durationMin, long price, int bufferMin) {
        return new BookingClass(name, category, durationMin, price, bufferMin);
    }

    public static Slot slot(BookingClass bookingClass, LocalDateTime startAt, LocalDateTime endAt) {
        return new Slot(bookingClass, startAt, endAt);
    }

    public static Guest guest(String name, String phone) {
        return new Guest(
                TEST_FIELD_ENCRYPTOR.encrypt(name), TEST_BLIND_INDEXER.index(name),
                TEST_FIELD_ENCRYPTOR.encrypt(phone), TEST_BLIND_INDEXER.index(phone));
    }

    public static Booking booking(Guest guest,
                                  Slot slot,
                                  long depositAmount,
                                  long balanceAmount,
                                  DepositPaymentMethod paymentMethod,
                                  String accessToken) {
        return Booking.forGuestDeposit(guest, slot, depositAmount, balanceAmount, paymentMethod, accessToken);
    }

    public static PassPurchase passPurchase(Long userId, LocalDateTime expiresAt, long totalPrice) {
        return PassPurchase.forMember(
                userId,
                expiresAt.minusDays(90),
                expiresAt,
                totalPrice,
                PassPlan.REGULAR_CRAFT_4);
    }

    public static Product readyStockProduct(String name, long price) {
        return new Product(name, ProductType.READY_STOCK, price);
    }

    /** 할인 없는 기성품 주문 상품 라인. */
    public static OrderItem readyStockOrderItem(
            Order order, Long productId, String productName, int qty, long unitPrice) {
        return new OrderItem(
                order, productId, null, productName, ProductType.READY_STOCK, qty, unitPrice, unitPrice,
                0L, 0L, List.of(), null, null, null, OrderItemPricing.fullPrice(qty, unitPrice));
    }

    /** prepare가 저장하는 형식과 같은 할인 없는 기성품 결제 품목 스냅샷. */
    public static PreparedOrderItem preparedReadyStockItem(
            Long productId, String productName, int qty, long unitPrice) {
        return new PreparedOrderItem(
                null, productId, null, productName, qty, unitPrice, unitPrice, 0L, 0L,
                List.of(), null, null, null, ProductType.READY_STOCK,
                OrderItemPricing.fullPrice(qty, unitPrice));
    }

    public static Inventory inventory(Product product, int quantity) {
        return new Inventory(product, quantity);
    }

    public static PolicyAcceptance acceptedPolicies() {
        return new PolicyAcceptance("2026-09-11-v1", true, "2026-09-12-v1", true);
    }

    /** 32자 hex access token (테스트용). */
    public static String accessToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
