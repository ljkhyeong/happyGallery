package com.personal.happygallery.support;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import java.time.LocalDateTime;

/**
 * 테스트 엔티티 생성 유틸.
 */
public final class TestFixtures {

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
        return new Guest(name, phone);
    }

    public static Booking booking(Guest guest,
                                  Slot slot,
                                  long depositAmount,
                                  long balanceAmount,
                                  DepositPaymentMethod paymentMethod,
                                  String accessToken) {
        return Booking.forGuestDeposit(guest, slot, depositAmount, balanceAmount, paymentMethod, accessToken);
    }

    public static PassPurchase passPurchase(Guest guest, LocalDateTime expiresAt, long totalPrice) {
        return PassPurchase.forGuest(guest, expiresAt, totalPrice);
    }

    public static Product readyStockProduct(String name, long price) {
        return new Product(name, ProductType.READY_STOCK, price);
    }

    public static Product madeToOrderProduct(String name, long price) {
        return new Product(name, ProductType.MADE_TO_ORDER, price);
    }

    public static Inventory inventory(Product product, int quantity) {
        return new Inventory(product, quantity);
    }
}
