package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.guest;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class OwnerConstraintUseCaseIT {

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired BookingStorePort bookingStorePort;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired OrderStorePort orderStorePort;
    @Autowired GuestStorePort guestStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired TestCleanupSupport cleanupSupport;

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("예약과 주문은 회원 또는 비회원 소유자 중 정확히 하나만 저장한다")
    @Test
    void ownerConstraints_rejectMissingOrDuplicateOwners() {
        User member = userStorePort.save(
                new User("owner-constraint@test.local", "password-hash", "소유자 테스트 회원", "01011110000"));
        Guest guestOwner = guestStorePort.save(guest("소유자 테스트 비회원", "01022220000"));

        LocalDateTime startAt = LocalDateTime.of(2030, 1, 2, 10, 0);
        BookingClass bookingClass = classStorePort.save(
                bookingClass("소유자 제약 클래스", "OWNER", 60, 50_000L, 30));
        Slot slot = slotStorePort.save(slot(bookingClass, startAt, startAt.plusHours(1)));
        Booking booking = bookingStorePort.save(Booking.forMemberDeposit(
                member, slot, 10_000L, 40_000L, DepositPaymentMethod.CARD));

        LocalDateTime paidAt = startAt.minusDays(1);
        Order order = orderStorePort.save(
                Order.forMember(member.getId(), 50_000L, paidAt, paidAt.plusHours(24)));

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> jdbcTemplate.update(
                            "UPDATE bookings SET user_id = NULL WHERE id = ?", booking.getId()))
                    .as("예약 소유자가 모두 비는 상태")
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("chk_bookings_exactly_one_owner");
            softly.assertThatThrownBy(() -> jdbcTemplate.update(
                            "UPDATE bookings SET guest_id = ? WHERE id = ?", guestOwner.getId(), booking.getId()))
                    .as("예약 소유자가 모두 채워지는 상태")
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("chk_bookings_exactly_one_owner");
            softly.assertThatThrownBy(() -> jdbcTemplate.update(
                            "UPDATE orders SET user_id = NULL WHERE id = ?", order.getId()))
                    .as("주문 소유자가 모두 비는 상태")
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("chk_orders_exactly_one_owner");
            softly.assertThatThrownBy(() -> jdbcTemplate.update(
                            "UPDATE orders SET guest_id = ? WHERE id = ?", guestOwner.getId(), order.getId()))
                    .as("주문 소유자가 모두 채워지는 상태")
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("chk_orders_exactly_one_owner");
        });
    }
}
