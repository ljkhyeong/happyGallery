package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.booking.port.in.BookingSettlementUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.AdminCancelCommand;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@UseCaseIT
class BookingCancelUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired PhoneVerificationReaderPort phoneVerificationReaderPort;
    @Autowired BookingStateProbe bookingStateProbe;
    @Autowired NotificationLogProbe notificationLogProbe;
    @Autowired BookingSettlementUseCase bookingSettlementUseCase;
    @Autowired AdminBookingCancelUseCase adminBookingCancelUseCase;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean PaymentPort paymentProvider;

    BookingClass cls;
    BookingTestHelper helper;

    @BeforeEach
    void setUp() {
        helper = new BookingTestHelper(mockMvc, phoneVerificationReaderPort, objectMapper);
        // 기본: PaymentPort 성공
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenReturn(PaymentConfirmResult.success("FAKE-TEST-PG", "CARD", "2026-04-26T12:00:00+09:00"));
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenReturn(RefundResult.success("FAKE-TEST-REF"));

        cls = classStorePort.save(defaultBookingClass());
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearNotificationLogs();
    }

    // -----------------------------------------------------------------------
    // Proof: 취소 후 CANCELED 상태 + CANCELED 이력 + Refund REQUESTED 기록
    // -----------------------------------------------------------------------

    @DisplayName("환불 가능한 예약 취소 시 취소와 환불이 정상 처리된다")
    @Test
    void cancel_refundable_success() throws Exception {
        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        BookingTestHelper.CreatedBooking createdBooking =
                helper.createVerifiedCardBooking("01011110001", slot.getId(), 3);
        Long bookingId = createdBooking.bookingId();
        awaitLogCount(notificationLogProbe, 1);
        cleanupSupport.clearNotificationLogs();

        // 취소 — 취소 보상 마감 이전 슬롯이므로 환불 가능
        mockMvc.perform(delete("/api/v1/bookings/{id}", bookingId)
                        .header("X-Access-Token", createdBooking.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.participantCount").value(3))
                .andExpect(jsonPath("$.refundable").value(true))
                .andExpect(jsonPath("$.refundAmount").value(15000))
                .andExpect(jsonPath("$.refund.amount").value(15000))
                .andExpect(jsonPath("$.refund.status").value("REQUESTED"));

        Booking booking = bookingStateProbe.getBooking(bookingId);
        Refund refund = awaitRefundStatus(RefundStatus.SUCCEEDED);
        mockMvc.perform(get("/api/v1/bookings/{id}", bookingId)
                        .header("X-Access-Token", createdBooking.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(3))
                .andExpect(jsonPath("$.refund.amount").value(15000))
                .andExpect(jsonPath("$.refund.status").value("SUCCEEDED"));
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
        verify(paymentProvider).refund(eq("FAKE-TEST-PG"), eq(15_000L), any());
    }

    @DisplayName("취소한 예약과 같은 전화번호로 같은 슬롯을 다시 예약할 수 있다")
    @Test
    void cancel_thenRebookSameSlot_success() throws Exception {
        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        String phone = "01077770007";

        BookingTestHelper.CreatedBooking canceledBooking =
                helper.createVerifiedCardBooking(phone, slot.getId());
        mockMvc.perform(delete("/api/v1/bookings/{id}", canceledBooking.bookingId())
                        .header("X-Access-Token", canceledBooking.accessToken()))
                .andExpect(status().isOk());

        BookingTestHelper.CreatedBooking rebooked =
                helper.createVerifiedCardBooking(phone, slot.getId());

        assertSoftly(softly -> {
            softly.assertThat(rebooked.bookingId()).isNotEqualTo(canceledBooking.bookingId());
            softly.assertThat(bookingStateProbe.getBooking(canceledBooking.bookingId()).getStatus().name())
                    .isEqualTo("CANCELED");
            softly.assertThat(bookingStateProbe.getBooking(rebooked.bookingId()).getStatus().name())
                    .isEqualTo("BOOKED");
            softly.assertThat(bookingStateProbe.getSlot(slot.getId()).getBookedCount()).isEqualTo(1);
        });
    }

    @DisplayName("다인 예약은 인원을 단계적으로 줄이고 줄인 인원만큼 각각 환불한다")
    @Test
    void reduceParticipants_twice_refundsEachReduction() throws Exception {
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenReturn(
                        RefundResult.success("FAKE-PARTIAL-REF-1"),
                        RefundResult.success("FAKE-PARTIAL-REF-2"));
        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        BookingTestHelper.CreatedBooking booking =
                helper.createVerifiedCardBooking("01012121212", slot.getId(), 3);

        mockMvc.perform(patch("/api/v1/bookings/{id}/participants", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"participantCount\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.canceledParticipantCount").value(1))
                .andExpect(jsonPath("$.depositAmount").value(10_000))
                .andExpect(jsonPath("$.balanceAmount").value(90_000))
                .andExpect(jsonPath("$.refundAmount").value(5_000));
        awaitRefundCount(1);

        mockMvc.perform(patch("/api/v1/bookings/{id}/participants", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"participantCount\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.depositAmount").value(5_000))
                .andExpect(jsonPath("$.balanceAmount").value(45_000))
                .andExpect(jsonPath("$.refundAmount").value(5_000));

        List<Refund> refunds = awaitRefundCount(2);
        assertSoftly(softly -> {
            softly.assertThat(refunds).extracting(Refund::getAmount)
                    .containsExactlyInAnyOrder(5_000L, 5_000L);
            softly.assertThat(refunds).extracting(Refund::getStatus)
                    .containsOnly(RefundStatus.SUCCEEDED);
            softly.assertThat(bookingStateProbe.getBooking(booking.bookingId()).getStatus().name())
                    .isEqualTo("BOOKED");
            softly.assertThat(bookingStateProbe.getSlot(slot.getId()).getBookedCount()).isEqualTo(1);
            softly.assertThat(bookingStateProbe.bookingHistoryCountByBookingId(booking.bookingId()))
                    .isEqualTo(3L);
        });
    }

    @DisplayName("취소 보상 마감이 지난 예약은 인원 부분취소를 거절한다")
    @Test
    void reduceParticipants_afterRefundDeadline_rejectedWithoutStateChange() throws Exception {
        LocalDateTime today14 = LocalDateTime.now(clock).toLocalDate().atTime(14, 0);
        Slot slot = slotStorePort.save(slot(cls, today14, today14.plusHours(2)));
        BookingTestHelper.CreatedBooking booking =
                helper.createVerifiedCardBooking("01013131313", slot.getId(), 3);

        mockMvc.perform(patch("/api/v1/bookings/{id}/participants", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"participantCount\":2}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CHANGE_NOT_ALLOWED"));

        assertSoftly(softly -> {
            softly.assertThat(bookingStateProbe.getBooking(booking.bookingId()).getParticipantCount())
                    .isEqualTo(3);
            softly.assertThat(bookingStateProbe.getSlot(slot.getId()).getBookedCount()).isEqualTo(3);
            softly.assertThat(bookingStateProbe.refundCount()).isZero();
        });
    }

    @DisplayName("만석 회차에서 부분취소로 자리가 생기면 신청자에게 한 번 알린다")
    @Test
    void reduceParticipants_fullSlot_notifiesVacancyAlertApplicant() throws Exception {
        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        BookingTestHelper.CreatedBooking booking =
                helper.createVerifiedCardBooking("01014141414", slot.getId(), slot.getCapacity());
        awaitLogCount(notificationLogProbe, 1);
        cleanupSupport.clearNotificationLogs();

        String alertPhone = "01015151515";
        String verificationCode = helper.sendVerificationAndGetCode(alertPhone);
        mockMvc.perform(post("/api/v1/slots/{slotId}/vacancy-alerts", slot.getId())
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "빈자리 신청자",
                                  "phone": "%s",
                                  "verificationCode": "%s"
                                }
                                """.formatted(alertPhone, verificationCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value(slot.getId()))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(patch("/api/v1/bookings/{id}/participants", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken())
                        .contentType(APPLICATION_JSON)
                        .content("{\"participantCount\":%d}".formatted(slot.getCapacity() - 1)))
                .andExpect(status().isOk());

        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 3);
        assertThat(logs).extracting(NotificationLog::getEventType)
                .containsExactlyInAnyOrder(
                        NotificationEventType.BOOKING_RESCHEDULED,
                        NotificationEventType.BOOKING_VACANCY_AVAILABLE,
                        NotificationEventType.DEPOSIT_REFUNDED);
    }

    @DisplayName("자리가 남은 회차에는 빈자리 알림을 신청할 수 없다")
    @Test
    void registerVacancyAlert_availableSlot_rejected() throws Exception {
        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        String phone = "01016161616";
        String verificationCode = helper.sendVerificationAndGetCode(phone);

        mockMvc.perform(post("/api/v1/slots/{slotId}/vacancy-alerts", slot.getId())
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "빈자리 신청자",
                                  "phone": "%s",
                                  "verificationCode": "%s"
                                }
                                """.formatted(phone, verificationCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    // -----------------------------------------------------------------------
    // Proof: PG 환불 결과 불명 → 상태 확인 필요로 저장됨 (사라지지 않음)
    // -----------------------------------------------------------------------

    @DisplayName("PG 환불 타임아웃 시 환불 이력이 상태 확인 필요로 저장된다")
    @Test
    void cancel_refundTimeout_refundSavedAsReconciliationRequired() throws Exception {
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenReturn(RefundResult.reconciliationRequired("PG 타임아웃"));

        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01055550005", slot.getId());
        awaitLogCount(notificationLogProbe, 1);
        cleanupSupport.clearNotificationLogs();

        // 취소 — 환불 가능 구간이지만 PG 실패
        mockMvc.perform(delete("/api/v1/bookings/{id}", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(true));

        Refund refund = awaitRefundStatus(RefundStatus.RECONCILIATION_REQUIRED);
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 1);
        assertSoftly(softly -> {
            softly.assertThat(refund.getBookingId()).isEqualTo(booking.bookingId());
            softly.assertThat(refund.getOrderId()).isNull();
            softly.assertThat(refund.getStatus()).isEqualTo(RefundStatus.RECONCILIATION_REQUIRED);
            softly.assertThat(refund.getFailReason()).isEqualTo("PG 타임아웃");
            softly.assertThat(refund.getPaymentKey()).isEqualTo("FAKE-TEST-PG");
            softly.assertThat(refund.getRefundTransactionKey()).isNull();
            softly.assertThat(logs).extracting(NotificationLog::getEventType)
                    .containsExactly(NotificationEventType.BOOKING_CANCELED);
        });
        verify(paymentProvider).refund(eq("FAKE-TEST-PG"), eq(5000L), any());
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

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01022220002", slot.getId());
        Long bookingId = booking.bookingId();

        mockMvc.perform(delete("/api/v1/bookings/{id}", bookingId)
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

    @DisplayName("잔금 결제가 완료된 예약은 고객 취소를 거절한다")
    @Test
    void cancel_balancePaid_returns422WithoutStateChange() throws Exception {
        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        BookingTestHelper.CreatedBooking booking =
                helper.createVerifiedCardBooking("01088880008", slot.getId());
        bookingSettlementUseCase.markBalancePaid(booking.bookingId(), 1L);

        mockMvc.perform(delete("/api/v1/bookings/{id}", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CHANGE_NOT_ALLOWED"));

        assertSoftly(softly -> {
            softly.assertThat(bookingStateProbe.getBooking(booking.bookingId()).getStatus().name())
                    .isEqualTo("BOOKED");
            softly.assertThat(bookingStateProbe.getSlot(slot.getId()).getBookedCount()).isEqualTo(1);
            softly.assertThat(bookingStateProbe.refundCount()).isZero();
        });
    }

    @DisplayName("공방 사정 취소는 마감과 잔금 결제 여부와 관계없이 예약금 환불을 시작한다")
    @Test
    void adminCancel_afterDeadlineAndBalancePaid_startsDepositRefund() throws Exception {
        LocalDateTime today14 = LocalDateTime.now(clock).toLocalDate().atTime(14, 0);
        Slot slot = slotStorePort.save(slot(cls, today14, today14.plusHours(2)));
        BookingTestHelper.CreatedBooking booking =
                helper.createVerifiedCardBooking("01099990009", slot.getId());
        bookingSettlementUseCase.markBalancePaid(booking.bookingId(), 1L);

        AdminBookingCancelUseCase.AdminCancelResult result = adminBookingCancelUseCase.cancel(
                new AdminCancelCommand(booking.bookingId(), 7L, "공방 사정으로 수업 취소"));

        Refund refund = awaitRefundStatus(RefundStatus.SUCCEEDED);
        assertSoftly(softly -> {
            softly.assertThat(result.booking().getStatus().name()).isEqualTo("CANCELED");
            softly.assertThat(result.balanceSettlementRequired()).isTrue();
            softly.assertThat(result.refund().getAmount()).isEqualTo(5000L);
            softly.assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
            softly.assertThat(bookingStateProbe.getSlot(slot.getId()).getBookedCount()).isZero();
        });
    }

    // -----------------------------------------------------------------------
    // 404 — 잘못된 access_token
    // -----------------------------------------------------------------------

    @DisplayName("잘못된 토큰으로 예약 취소를 요청하면 404를 반환한다")
    @Test
    void cancel_wrongToken_returns404() throws Exception {
        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01033330003", slot.getId());

        mockMvc.perform(delete("/api/v1/bookings/{id}", booking.bookingId())
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

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01044440004", slot.getId());

        // 첫 번째 취소 — 성공
        mockMvc.perform(delete("/api/v1/bookings/{id}", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken()))
                .andExpect(status().isOk());

        // 두 번째 취소 — 400
        mockMvc.perform(delete("/api/v1/bookings/{id}", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    private Refund awaitRefundStatus(RefundStatus status) {
        List<Refund> refunds = await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(25, TimeUnit.MILLISECONDS)
                .until(
                        bookingStateProbe::refunds,
                        found -> found.size() == 1 && found.getFirst().getStatus() == status);
        return refunds.getFirst();
    }

    private List<Refund> awaitRefundCount(int count) {
        return await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(25, TimeUnit.MILLISECONDS)
                .until(
                        bookingStateProbe::refunds,
                        found -> found.size() == count
                                && found.stream().allMatch(
                                        refund -> refund.getStatus() == RefundStatus.SUCCEEDED));
    }

}
