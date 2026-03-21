package com.personal.happygallery.app.booking;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.ClassRepository;
import com.personal.happygallery.infra.booking.GuestRepository;
import com.personal.happygallery.infra.booking.PhoneVerificationRepository;
import com.personal.happygallery.infra.booking.RefundRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import com.personal.happygallery.infra.notification.NotificationLogRepository;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import com.personal.happygallery.infra.payment.PaymentProvider;
import com.personal.happygallery.app.payment.port.out.RefundResult;
import com.personal.happygallery.support.BookingTestHelper;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.personal.happygallery.support.BookingTestHelper.FUTURE;
import static com.personal.happygallery.support.NotificationLogTestHelper.awaitLogCount;
import static com.personal.happygallery.support.TestDataCleaner.clearBookingWithPassAndRefundData;
import static com.personal.happygallery.support.TestFixtures.booking;
import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static com.personal.happygallery.support.TestFixtures.guest;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class BookingCancelUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired ClassRepository classRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired GuestRepository guestRepository;
    @Autowired PhoneVerificationRepository phoneVerificationRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired PassLedgerRepository passLedgerRepository;
    @Autowired PassPurchaseRepository passPurchaseRepository;
    @Autowired NotificationLogRepository notificationLogRepository;
    @Autowired Clock clock;
    @MockitoBean PaymentProvider paymentProvider;

    BookingClass cls;
    BookingTestHelper helper;

    @BeforeEach
    void setUp() {
        helper = new BookingTestHelper(mockMvc, phoneVerificationRepository);
        // 기본: PaymentProvider 성공
        when(paymentProvider.refund(any(), anyLong()))
                .thenReturn(RefundResult.success("FAKE-TEST-REF"));
        clearBookingWithPassAndRefundData(
                passLedgerRepository,
                refundRepository,
                bookingHistoryRepository,
                bookingRepository,
                passPurchaseRepository,
                phoneVerificationRepository,
                guestRepository,
                slotRepository,
                classRepository);
        notificationLogRepository.deleteAllInBatch();

        cls = classRepository.save(defaultBookingClass());
    }

    // -----------------------------------------------------------------------
    // Proof: 취소 후 CANCELED 상태 + CANCELED 이력 + Refund REQUESTED 기록
    // -----------------------------------------------------------------------

    @DisplayName("환불 가능한 예약 취소 시 취소와 환불이 정상 처리된다")
    @Test
    void cancel_refundable_success() throws Exception {
        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        BookingTestHelper.CreatedBooking createdBooking = helper.createVerifiedCardBooking("01011110001", slot.getId(), 5_000L);
        Long bookingId = createdBooking.bookingId();
        awaitLogCount(notificationLogRepository, 1);
        notificationLogRepository.deleteAllInBatch();

        // 취소 — D-1 이전 슬롯이므로 환불 가능
        mockMvc.perform(delete("/bookings/{id}", bookingId)
                        .header("X-Access-Token", createdBooking.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(true))
                .andExpect(jsonPath("$.refundAmount").value(5000));

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        var refunds = refundRepository.findAll();
        Slot updatedSlot = slotRepository.findById(slot.getId()).orElseThrow();
        List<NotificationLog> logs = awaitLogCount(notificationLogRepository, 2);

        assertSoftly(softly -> {
            softly.assertThat(booking.getStatus().name()).isEqualTo("CANCELED");
            softly.assertThat(bookingHistoryRepository.countByBookingId(bookingId)).isEqualTo(2L);
            softly.assertThat(refunds).hasSize(1);
            if (!refunds.isEmpty()) {
                softly.assertThat(refunds.get(0).getStatus().name()).isEqualTo("SUCCEEDED");
            }
            softly.assertThat(updatedSlot.getBookedCount()).isEqualTo(0);
            softly.assertThat(logs).extracting(NotificationLog::getEventType)
                    .containsExactlyInAnyOrder(
                            NotificationEventType.BOOKING_CANCELED,
                            NotificationEventType.DEPOSIT_REFUNDED);
        });
    }

    // -----------------------------------------------------------------------
    // Proof: PG 환불 실패 → refund FAILED 로 저장됨 (사라지지 않음)
    // -----------------------------------------------------------------------

    @DisplayName("PG 환불 실패 시 환불 이력이 FAILED로 저장된다")
    @Test
    void cancel_refundFailure_refundSavedAsFailed() throws Exception {
        when(paymentProvider.refund(any(), anyLong()))
                .thenReturn(RefundResult.failure("PG 타임아웃"));

        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01055550005", slot.getId(), 5_000L);
        awaitLogCount(notificationLogRepository, 1);
        notificationLogRepository.deleteAllInBatch();

        // 취소 — 환불 가능 구간이지만 PG 실패
        mockMvc.perform(delete("/bookings/{id}", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(true));

        var refunds = refundRepository.findAll();
        List<NotificationLog> logs = awaitLogCount(notificationLogRepository, 1);
        assertSoftly(softly -> {
            softly.assertThat(refunds).hasSize(1);
            if (!refunds.isEmpty()) {
                softly.assertThat(refunds.get(0).getStatus().name()).isEqualTo("FAILED");
                softly.assertThat(refunds.get(0).getFailReason()).isEqualTo("PG 타임아웃");
            }
            softly.assertThat(logs).extracting(NotificationLog::getEventType)
                    .containsExactly(NotificationEventType.BOOKING_CANCELED);
        });
    }

    @DisplayName("환불 실패 목록 관리자 API는 DTO 응답을 반환한다")
    @Test
    void list_failed_refunds_adminApi_returnsDtoResponse() throws Exception {
        Guest guest = guestRepository.save(guest("환불실패", "01077770007"));
        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        Booking booking = bookingRepository.save(booking(
                guest, slot, 5_000L, 45_000L, DepositPaymentMethod.CARD, "refund-token"));

        Refund refund = Refund.forBooking(booking, 5_000L);
        refund.markFailed(null);
        refundRepository.save(refund);

        mockMvc.perform(get("/admin/refunds/failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].refundId").value(refund.getId()))
                .andExpect(jsonPath("$[0].bookingId").value(booking.getId()))
                .andExpect(jsonPath("$[0].orderId", nullValue()))
                .andExpect(jsonPath("$[0].amount").value(5000))
                .andExpect(jsonPath("$[0].failReason").value(""))
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty());
    }

    // -----------------------------------------------------------------------
    // D-1 이후 취소 — 환불 불가, refund 미생성
    // -----------------------------------------------------------------------

    @DisplayName("환불 불가 구간에서 예약을 취소하면 환불이 생성되지 않는다")
    @Test
    void cancel_notRefundable_noRefundCreated() throws Exception {
        // 오늘 14:00 시작하는 슬롯 — 체험일(오늘)의 D-1 deadline(오늘 00:00)이 이미 지남 → 환불 불가
        LocalDateTime today14 = LocalDateTime.now(clock).toLocalDate().atTime(14, 0);
        Slot slot = slotRepository.save(slot(cls, today14, today14.plusHours(2)));

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01022220002", slot.getId(), 5_000L);
        Long bookingId = booking.bookingId();

        mockMvc.perform(delete("/bookings/{id}", bookingId)
                        .header("X-Access-Token", booking.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(false))
                .andExpect(jsonPath("$.refundAmount").value(0));

        assertSoftly(softly -> {
            softly.assertThat(refundRepository.count()).isEqualTo(0L);
            softly.assertThat(bookingHistoryRepository.countByBookingId(bookingId)).isEqualTo(2L);
        });
    }

    // -----------------------------------------------------------------------
    // 404 — 잘못된 access_token
    // -----------------------------------------------------------------------

    @DisplayName("잘못된 토큰으로 예약 취소를 요청하면 404를 반환한다")
    @Test
    void cancel_wrongToken_returns404() throws Exception {
        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

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
        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

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

}
