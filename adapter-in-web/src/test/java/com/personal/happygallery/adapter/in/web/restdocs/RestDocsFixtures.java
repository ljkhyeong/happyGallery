package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.application.order.port.in.OrderQueryUseCase;
import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.domain.booking.BalanceStatus;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.inquiry.Inquiry;
import com.personal.happygallery.domain.notice.Notice;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.qna.ProductQna;
import com.personal.happygallery.domain.user.AuthProvider;
import com.personal.happygallery.domain.user.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class RestDocsFixtures {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 1, 21, 0);

    private RestDocsFixtures() {
    }

    static ProductQueryUseCase.ProductWithInventory productWithInventory() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        when(product.getName()).thenReturn("시그니처 캔들");
        when(product.getType()).thenReturn(ProductType.READY_STOCK);
        when(product.getCategory()).thenReturn("CANDLE");
        when(product.getPrice()).thenReturn(39000L);
        when(product.getStatus()).thenReturn(ProductStatus.ACTIVE);

        Inventory inventory = mock(Inventory.class);
        when(inventory.isAvailable()).thenReturn(true);
        when(inventory.getQuantity()).thenReturn(12);

        return new ProductQueryUseCase.ProductWithInventory(product, inventory);
    }

    static BookingClass bookingClass() {
        BookingClass bookingClass = mock(BookingClass.class);
        when(bookingClass.getId()).thenReturn(1L);
        when(bookingClass.getName()).thenReturn("향수 원데이");
        when(bookingClass.getCategory()).thenReturn("PERFUME");
        when(bookingClass.getDurationMin()).thenReturn(120);
        when(bookingClass.getPrice()).thenReturn(50000L);
        when(bookingClass.getBufferMin()).thenReturn(30);
        return bookingClass;
    }

    static Slot slot() {
        return slot(bookingClass());
    }

    static Slot slot(BookingClass bookingClass) {
        Slot slot = mock(Slot.class);
        when(slot.getId()).thenReturn(42L);
        when(slot.getBookingClass()).thenReturn(bookingClass);
        when(slot.getStartAt()).thenReturn(LocalDateTime.of(2026, 5, 7, 19, 0));
        when(slot.getEndAt()).thenReturn(LocalDateTime.of(2026, 5, 7, 21, 0));
        when(slot.getCapacity()).thenReturn(8);
        when(slot.getBookedCount()).thenReturn(2);
        when(slot.isActive()).thenReturn(true);
        return slot;
    }

    static Booking booking() {
        BookingClass bookingClass = bookingClass();
        Slot slot = slot(bookingClass);
        Guest guest = mock(Guest.class);
        when(guest.getName()).thenReturn("홍길동");
        when(guest.getPhone()).thenReturn("01012345678");

        Booking booking = mock(Booking.class);
        when(booking.getId()).thenReturn(100L);
        when(booking.getSlot()).thenReturn(slot);
        when(booking.getBookingClass()).thenReturn(bookingClass);
        when(booking.getStatus()).thenReturn(BookingStatus.BOOKED);
        when(booking.getDepositAmount()).thenReturn(5000L);
        when(booking.getBalanceAmount()).thenReturn(45000L);
        when(booking.getBalanceStatus()).thenReturn(BalanceStatus.UNPAID);
        when(booking.isPassBooking()).thenReturn(false);
        when(booking.getGuest()).thenReturn(guest);
        return booking;
    }

    static PhoneVerification phoneVerification() {
        PhoneVerification verification = mock(PhoneVerification.class);
        when(verification.getId()).thenReturn(7L);
        when(verification.getPhone()).thenReturn("01012345678");
        return verification;
    }

    static Order order() {
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(200L);
        when(order.getStatus()).thenReturn(OrderStatus.PAID_APPROVAL_PENDING);
        when(order.getTotalAmount()).thenReturn(39000L);
        when(order.getPaidAt()).thenReturn(NOW.minusMinutes(5));
        when(order.getApprovalDeadlineAt()).thenReturn(NOW.plusMinutes(15));
        when(order.getCreatedAt()).thenReturn(NOW.minusMinutes(10));
        return order;
    }

    static OrderItem orderItem() {
        OrderItem item = mock(OrderItem.class);
        when(item.getProductId()).thenReturn(1L);
        when(item.getQty()).thenReturn(1);
        when(item.getUnitPrice()).thenReturn(39000L);
        return item;
    }

    static Fulfillment fulfillment() {
        Fulfillment fulfillment = mock(Fulfillment.class);
        when(fulfillment.getType()).thenReturn(FulfillmentType.PICKUP);
        when(fulfillment.getExpectedShipDate()).thenReturn(LocalDate.of(2026, 5, 8));
        when(fulfillment.getPickupDeadlineAt()).thenReturn(NOW.plusDays(3));
        return fulfillment;
    }

    static OrderQueryUseCase.OrderDetail orderDetail() {
        return new OrderQueryUseCase.OrderDetail(order(), List.of(orderItem()), fulfillment());
    }

    static PassPurchase passPurchase() {
        PassPurchase pass = mock(PassPurchase.class);
        when(pass.getId()).thenReturn(300L);
        when(pass.getPurchasedAt()).thenReturn(NOW.minusDays(1));
        when(pass.getExpiresAt()).thenReturn(NOW.plusDays(89));
        when(pass.getTotalCredits()).thenReturn(8);
        when(pass.getRemainingCredits()).thenReturn(7);
        when(pass.getTotalPrice()).thenReturn(240000L);
        return pass;
    }

    static User user() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(RestDocsTestSupport.CUSTOMER_USER_ID);
        when(user.getEmail()).thenReturn("member@example.com");
        when(user.getName()).thenReturn("회원");
        when(user.getPhone()).thenReturn("01012345678");
        when(user.isPhoneVerified()).thenReturn(true);
        when(user.getProvider()).thenReturn(AuthProvider.LOCAL);
        return user;
    }

    static Notice notice() {
        Notice notice = mock(Notice.class);
        when(notice.getId()).thenReturn(1L);
        when(notice.getTitle()).thenReturn("운영 안내");
        when(notice.getContent()).thenReturn("5월 클래스 운영 안내입니다.");
        when(notice.isPinned()).thenReturn(true);
        when(notice.getViewCount()).thenReturn(12);
        when(notice.getCreatedAt()).thenReturn(NOW.minusDays(2));
        return notice;
    }

    static ProductQna productQna() {
        ProductQna qna = mock(ProductQna.class);
        when(qna.getId()).thenReturn(5L);
        when(qna.getProductId()).thenReturn(1L);
        when(qna.getTitle()).thenReturn("배송 문의");
        when(qna.getContent()).thenReturn("언제 받을 수 있나요?");
        when(qna.getReplyContent()).thenReturn("주문 승인 후 안내드립니다.");
        when(qna.getRepliedAt()).thenReturn(NOW.plusHours(1));
        when(qna.isSecret()).thenReturn(false);
        when(qna.hasReply()).thenReturn(true);
        when(qna.getCreatedAt()).thenReturn(NOW);
        return qna;
    }

    static Inquiry inquiry() {
        Inquiry inquiry = mock(Inquiry.class);
        when(inquiry.getId()).thenReturn(9L);
        when(inquiry.getTitle()).thenReturn("예약 문의");
        when(inquiry.getContent()).thenReturn("예약 변경이 가능한가요?");
        when(inquiry.hasReply()).thenReturn(true);
        when(inquiry.getReplyContent()).thenReturn("마이페이지에서 변경할 수 있습니다.");
        when(inquiry.getRepliedAt()).thenReturn(NOW.plusHours(2));
        when(inquiry.getCreatedAt()).thenReturn(NOW);
        return inquiry;
    }
}
