package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.adapter.out.external.payment.PaymentProvider;
import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import com.personal.happygallery.support.BookingTestHelper;
import com.personal.happygallery.support.BookingStateProbe;
import com.personal.happygallery.support.NotificationLogProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.personal.happygallery.support.BookingTestHelper.FUTURE;
import static com.personal.happygallery.support.NotificationLogTestHelper.awaitLogCount;
import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class BookingCancelUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired PhoneVerificationReaderPort phoneVerificationReaderPort;
    @Autowired BookingStateProbe bookingStateProbe;
    @Autowired NotificationLogProbe notificationLogProbe;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;
    @MockitoBean PaymentProvider paymentProvider;

    BookingClass cls;
    BookingTestHelper helper;

    @BeforeEach
    void setUp() {
        helper = new BookingTestHelper(mockMvc, phoneVerificationReaderPort);
        // 기본: PaymentProvider 성공
        when(paymentProvider.confirm(any(), any(), anyLong()))
                .thenReturn(PaymentConfirmResult.success("FAKE-TEST-PG", "CARD", "2026-04-26T12:00:00+09:00"));
        when(paymentProvider.refund(any(), anyLong()))
                .thenReturn(RefundResult.success("FAKE-TEST-REF"));
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearNotificationLogs();

        cls = classStorePort.save(defaultBookingClass());
    }

    // -----------------------------------------------------------------------
    // Proof: 취소 후 CANCELED 상태 + CANCELED 이력 + Refund REQUESTED 기록
    // -----------------------------------------------------------------------

    @DisplayName("환불 가능한 예약 취소 시 취소와 환불이 정상 처리된다")
    @Test
    void cancel_refundable_success() throws Exception {
        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        BookingTestHelper.CreatedBooking createdBooking = helper.createVerifiedCardBooking("01011110001", slot.getId(), 5_000L);
        Long bookingId = createdBooking.bookingId();
        awaitLogCount(notificationLogProbe, 1);
        cleanupSupport.clearNotificationLogs();

        // 취소 — 취소 보상 마감 이전 슬롯이므로 환불 가능
        mockMvc.perform(delete("/bookings/{id}", bookingId)
                        .header("X-Access-Token", createdBooking.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(true))
                .andExpect(jsonPath("$.refundAmount").value(5000));

        Booking booking = bookingStateProbe.getBooking(bookingId);
        Refund refund = awaitRefundStatus(RefundStatus.SUCCEEDED);
        Slot updatedSlot = bookingStateProbe.getSlot(slot.getId());
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 2);

        assertSoftly(softly -> {
            softly.assertThat(booking.getStatus().name()).isEqualTo("CANCELED");
            softly.assertThat(bookingStateProbe.bookingHistoryCountByBookingId(bookingId)).isEqualTo(2L);
            softly.assertThat(refund.getBookingId()).isEqualTo(bookingId);
            softly.assertThat(refund.getOrderId()).isNull();
            softly.assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
            softly.assertThat(refund.getPaymentKey()).isEqualTo("FAKE-TEST-PG");
            softly.assertThat(refund.getRefundTransactionKey()).isEqualTo("FAKE-TEST-REF");
            softly.assertThat(updatedSlot.getBookedCount()).isEqualTo(0);
            softly.assertThat(logs).extracting(NotificationLog::getEventType)
                    .containsExactlyInAnyOrder(
                            NotificationEventType.BOOKING_CANCELED,
                            NotificationEventType.DEPOSIT_REFUNDED);
        });
        verify(paymentProvider).refund(eq("FAKE-TEST-PG"), eq(5000L));
    }

    // -----------------------------------------------------------------------
    // Proof: PG 환불 실패 → refund FAILED 로 저장됨 (사라지지 않음)
    // -----------------------------------------------------------------------

    @DisplayName("PG 환불 실패 시 환불 이력이 FAILED로 저장된다")
    @Test
    void cancel_refundFailure_refundSavedAsFailed() throws Exception {
        when(paymentProvider.refund(any(), anyLong()))
                .thenReturn(RefundResult.failure("PG 타임아웃"));

        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01055550005", slot.getId(), 5_000L);
        awaitLogCount(notificationLogProbe, 1);
        cleanupSupport.clearNotificationLogs();

        // 취소 — 환불 가능 구간이지만 PG 실패
        mockMvc.perform(delete("/bookings/{id}", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(true));

        Refund refund = awaitRefundStatus(RefundStatus.FAILED);
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 1);
        assertSoftly(softly -> {
            softly.assertThat(refund.getBookingId()).isEqualTo(booking.bookingId());
            softly.assertThat(refund.getOrderId()).isNull();
            softly.assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
            softly.assertThat(refund.getFailReason()).isEqualTo("PG 타임아웃");
            softly.assertThat(refund.getPaymentKey()).isEqualTo("FAKE-TEST-PG");
            softly.assertThat(refund.getRefundTransactionKey()).isNull();
            softly.assertThat(logs).extracting(NotificationLog::getEventType)
                    .containsExactly(NotificationEventType.BOOKING_CANCELED);
        });
        verify(paymentProvider).refund(eq("FAKE-TEST-PG"), eq(5000L));
    }

    // -----------------------------------------------------------------------
    // 취소 보상 마감 이후 취소 — 환불 불가, refund 미생성
    // -----------------------------------------------------------------------

    @DisplayName("환불 불가 구간에서 예약을 취소하면 환불이 생성되지 않는다")
    @Test
    void cancel_notRefundable_noRefundCreated() throws Exception {
        // 오늘 14:00 시작하는 슬롯 — 취소 보상 마감(오늘 00:00)이 이미 지남 → 환불 불가
        LocalDateTime today14 = LocalDateTime.now(clock).toLocalDate().atTime(14, 0);
        Slot slot = slotStorePort.save(slot(cls, today14, today14.plusHours(2)));

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01022220002", slot.getId(), 5_000L);
        Long bookingId = booking.bookingId();

        mockMvc.perform(delete("/bookings/{id}", bookingId)
                        .header("X-Access-Token", booking.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(false))
                .andExpect(jsonPath("$.refundAmount").value(0));

        assertSoftly(softly -> {
            softly.assertThat(bookingStateProbe.refundCount()).isEqualTo(0L);
            softly.assertThat(bookingStateProbe.bookingHistoryCountByBookingId(bookingId)).isEqualTo(2L);
        });
    }

    // -----------------------------------------------------------------------
    // 404 — 잘못된 access_token
    // -----------------------------------------------------------------------

    @DisplayName("잘못된 토큰으로 예약 취소를 요청하면 404를 반환한다")
    @Test
    void cancel_wrongToken_returns404() throws Exception {
        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01033330003", slot.getId(), 5_000L);

        mockMvc.perform(delete("/bookings/{id}", booking.bookingId())
                        .header("X-Access-Token", "invalid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // -----------------------------------------------------------------------
    // 400 — 이미 취소된 예약 재취소 시도
    // -----------------------------------------------------------------------

    @DisplayName("이미 취소된 예약을 다시 취소하면 400을 반환한다")
    @Test
    void cancel_alreadyCanceled_returns400() throws Exception {
        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01044440004", slot.getId(), 5_000L);

        // 첫 번째 취소 — 성공
        mockMvc.perform(delete("/bookings/{id}", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken()))
                .andExpect(status().isOk());

        // 두 번째 취소 — 400
        mockMvc.perform(delete("/bookings/{id}", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    private Refund awaitRefundStatus(RefundStatus status) {
        await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(25, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var refunds = bookingStateProbe.refunds();
                    assertThat(refunds).hasSize(1);
                    assertThat(refunds.get(0).getStatus()).isEqualTo(status);
                });
        return bookingStateProbe.refunds().get(0);
    }

}
