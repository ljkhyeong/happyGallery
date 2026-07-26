package com.personal.happygallery.application.booking;

import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.AdminCancelCommand;
import com.personal.happygallery.application.booking.port.in.AdminBookingCreateUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingCreateUseCase.CreateAdminBookingCommand;
import com.personal.happygallery.application.booking.port.in.AdminBookingResponse;
import com.personal.happygallery.application.booking.port.in.BookingCancellationTaskUseCase;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingCancellationTaskType;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingSource;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.support.BookingStateProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@UseCaseIT
class AdminBookingCreateUseCaseIT {

    @Autowired AdminBookingCreateUseCase adminBookingCreateUseCase;
    @Autowired AdminBookingCancelUseCase adminBookingCancelUseCase;
    @Autowired BookingCancellationTaskUseCase bookingCancellationTaskUseCase;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired BookingStateProbe bookingStateProbe;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearBookingWithPassAndRefundData();
    }

    @DisplayName("운영자가 받은 다인 예약금 예약은 정원을 점유하고 취소 시 수동 환불 작업을 남긴다")
    @Test
    void createPaidOfflineBooking_thenCancel_tracksCapacityAndManualCompensation() {
        BookingClass bookingClass = classStorePort.save(
                bookingClass("전화 접수 클래스", "PHONE_BOOKING", 120, 50_000L, 30));
        LocalDateTime startAt = LocalDateTime.now(clock).plusDays(3);
        Slot slot = slotStorePort.save(
                slot(bookingClass, startAt, startAt.plusHours(2)));

        AdminBookingResponse created = adminBookingCreateUseCase.create(
                new CreateAdminBookingCommand(
                        slot.getId(),
                        "전화예약자",
                        "010-2345-6789",
                        3,
                        BookingSource.PHONE,
                        true,
                        7L));
        AdminBookingCancelUseCase.AdminCancelResult canceled =
                adminBookingCancelUseCase.cancel(
                        new AdminCancelCommand(created.bookingId(), 7L, "공방 일정 변경"));

        Booking booking = bookingStateProbe.getBooking(created.bookingId());
        var pendingTasks = bookingCancellationTaskUseCase.listPending();

        assertSoftly(softly -> {
            softly.assertThat(created.source()).isEqualTo("PHONE");
            softly.assertThat(created.participantCount()).isEqualTo(3);
            softly.assertThat(created.depositAmount()).isEqualTo(15_000L);
            softly.assertThat(created.balanceAmount()).isEqualTo(135_000L);
            softly.assertThat(created.depositPaidAt()).isNotNull();
            softly.assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELED);
            softly.assertThat(booking.getSource()).isEqualTo(BookingSource.PHONE);
            softly.assertThat(bookingStateProbe.getSlot(slot.getId()).getBookedCount()).isZero();
            softly.assertThat(bookingStateProbe.refundCount()).isZero();
            softly.assertThat(canceled.manualCompensationRequired()).isTrue();
            softly.assertThat(canceled.refund()).isNull();
            softly.assertThat(pendingTasks)
                    .singleElement()
                    .satisfies(task -> {
                        softly.assertThat(task.bookingId()).isEqualTo(created.bookingId());
                        softly.assertThat(task.type())
                                .isEqualTo(BookingCancellationTaskType.MANUAL_COMPENSATION);
                        softly.assertThat(task.balanceAmount()).isZero();
                        softly.assertThat(task.compensationAmount()).isEqualTo(15_000L);
                    });
        });
    }
}
