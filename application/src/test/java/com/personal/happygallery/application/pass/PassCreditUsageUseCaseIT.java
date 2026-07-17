package com.personal.happygallery.application.pass;

import com.personal.happygallery.adapter.in.web.payment.dto.ConfirmPaymentRequest;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.adapter.out.persistence.booking.BookingHistoryRepository;
import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.booking.ClassRepository;
import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import com.personal.happygallery.adapter.out.persistence.booking.SlotRepository;
import com.personal.happygallery.adapter.out.persistence.pass.PassLedgerRepository;
import com.personal.happygallery.adapter.out.persistence.pass.PassPurchaseRepository;
import com.personal.happygallery.adapter.out.persistence.user.UserRepository;
import com.personal.happygallery.adapter.out.external.payment.PaymentProvider;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.support.CustomerTestHelper;
import com.personal.happygallery.support.PaymentTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static com.personal.happygallery.support.BookingTestHelper.FUTURE;
import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static com.personal.happygallery.support.TestFixtures.passPurchase;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class PassCreditUsageUseCaseIT {

    private static final String ADMIN_KEY = "dev-admin-key";

    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter;
    MockMvc mockMvc;
    @Autowired PassPurchaseRepository passPurchaseRepository;
    @Autowired PassLedgerRepository passLedgerRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired ClassRepository classRepository;
    @Autowired UserRepository userRepository;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean PaymentProvider paymentProvider;

    BookingClass cls;
    PassPurchase pass;
    Cookie sessionCookie;
    PaymentTestHelper paymentHelper;
    CustomerTestHelper customerHelper;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSessionRepositoryFilter)
                .apply(springSecurity())
                .build();
        paymentHelper = new PaymentTestHelper(mockMvc, objectMapper);
        customerHelper = new CustomerTestHelper(mockMvc, objectMapper);
        cleanupSupport.clearBookingWithPassAndRefundData();

        cls = classRepository.save(defaultBookingClass());
        cleanupSupport.clearUsers();
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenReturn(RefundResult.success("FAKE-TEST-PASS-REF"));
        sessionCookie = signupAndGetSessionCookie("pass-member@example.com", "01099990001");
        Long userId = userRepository.findByEmail("pass-member@example.com").orElseThrow().getId();
        pass = passPurchase(userId, FUTURE.plusDays(90), 320_000L);
        pass.recordPaymentKey("test-pass-payment-key");
        pass = passPurchaseRepository.save(pass);
    }

    // -----------------------------------------------------------------------
    // Proof 1: 8회권 예약 시 USE ledger(-1), remaining=7
    // -----------------------------------------------------------------------

    @DisplayName("8회권으로 예약하면 크레딧이 차감된다")
    @Test
    void book_with_pass_consumes_credit() throws Exception {
        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        PaymentTestHelper.ConfirmedPayment confirmed = paymentHelper.createMemberPassBooking(
                sessionCookie, pass.getUserId(), slot.getId(), pass.getId());

        // Proof: USE ledger 1건, amount=1
        var ledgers = passLedgerRepository.findByPassPurchaseId(pass.getId());
        PassPurchase reloaded = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        var bookings = bookingRepository.findAll();
        assertSoftly(softly -> {
            softly.assertThat(ledgers).hasSize(1);
            if (!ledgers.isEmpty()) {
                softly.assertThat(ledgers.get(0).getType()).isEqualTo(PassLedgerType.USE);
                softly.assertThat(ledgers.get(0).getAmount()).isEqualTo(1);
                softly.assertThat(ledgers.get(0).getRelatedBookingId()).isEqualTo(confirmed.domainId());
            }
            softly.assertThat(reloaded.getRemainingCredits()).isEqualTo(7);
            softly.assertThat(bookings).hasSize(1);
            if (!bookings.isEmpty()) {
                softly.assertThat(bookings.get(0).getId()).isEqualTo(confirmed.domainId());
                softly.assertThat(bookings.get(0).isPassBooking()).isTrue();
            }
        });
    }

    // -----------------------------------------------------------------------
    // Proof 2: 취소 보상 마감 이전 취소 → REFUND ledger(+1), remaining=8 복구
    // -----------------------------------------------------------------------

    @DisplayName("8회권 예약을 기한 내 취소하면 크레딧이 환불된다")
    @Test
    void cancel_pass_booking_timely_refunds_credit() throws Exception {
        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        Long bookingId = createPassBooking(slot.getId());

        // 취소 (FUTURE = 2030년 → 취소 보상 마감 이전이므로 환불 가능)
        mockMvc.perform(delete("/api/v1/me/bookings/{id}", bookingId)
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(true));

        // Proof: REFUND ledger 추가
        var ledgers = passLedgerRepository.findByPassPurchaseId(pass.getId());
        PassPurchase reloaded = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(ledgers).hasSize(2); // USE + REFUND
            softly.assertThat(ledgers.stream().filter(l -> l.getType() == PassLedgerType.REFUND).count()).isEqualTo(1);
            softly.assertThat(reloaded.getRemainingCredits()).isEqualTo(8);
            softly.assertThat(refundRepository.count()).isEqualTo(0);
        });
    }

    // -----------------------------------------------------------------------
    // Proof 3: 취소 보상 마감 이후 취소 → 크레딧 소멸 유지 (remaining=7)
    // -----------------------------------------------------------------------

    @DisplayName("8회권 예약을 늦게 취소하면 크레딧이 소멸된다")
    @Test
    void cancel_pass_booking_late_loses_credit() throws Exception {
        // 오늘 14:00 시작 슬롯 — 취소 보상 마감(오늘 00:00) 이미 지남
        LocalDateTime today14 = LocalDateTime.now(clock).toLocalDate().atTime(14, 0);
        Slot slot = slotRepository.save(slot(cls, today14, today14.plusHours(2)));

        Long bookingId = createPassBooking(slot.getId());

        mockMvc.perform(delete("/api/v1/me/bookings/{id}", bookingId)
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(false));

        var ledgers = passLedgerRepository.findByPassPurchaseId(pass.getId());
        PassPurchase reloaded = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(ledgers).hasSize(1);
            softly.assertThat(ledgers.get(0).getType()).isEqualTo(PassLedgerType.USE);
            softly.assertThat(reloaded.getRemainingCredits()).isEqualTo(7);
        });
    }

    // -----------------------------------------------------------------------
    // Proof 4: 결석 처리 → status=NO_SHOW, 크레딧 변동 없음
    // -----------------------------------------------------------------------

    @DisplayName("노쇼 처리 시 상태만 변경되고 크레딧은 변하지 않는다")
    @Test
    void mark_no_show_status_only_no_credit_change() throws Exception {
        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        Long bookingId = createPassBooking(slot.getId());

        mockMvc.perform(post("/admin/bookings/{id}/no-show", bookingId)
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.status").value("NO_SHOW"));

        var ledgers = passLedgerRepository.findByPassPurchaseId(pass.getId());
        PassPurchase reloaded = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(bookingRepository.findById(bookingId))
                    .hasValueSatisfying(b -> assertThat(b.getStatus()).isEqualTo(BookingStatus.NO_SHOW));
            softly.assertThat(ledgers).hasSize(1);
            softly.assertThat(ledgers.get(0).getType()).isEqualTo(PassLedgerType.USE);
            softly.assertThat(reloaded.getRemainingCredits()).isEqualTo(7);
        });
    }

    // -----------------------------------------------------------------------
    // Proof 5: 전체 환불 → 미래 예약 자동 취소 + REFUND ledger + remaining=0
    // -----------------------------------------------------------------------

    @DisplayName("8회권 전체 환불 시 미래 예약이 취소되고 잔여 크레딧이 소멸된다")
    @Test
    void refund_pass_cancels_future_bookings_and_empties_credits() throws Exception {
        Slot slot1 = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        Slot slot2 = slotRepository.save(slot(cls, FUTURE.plusDays(1), FUTURE.plusDays(1).plusHours(2)));

        // 2회 예약 (remaining: 8 → 6). 미래 예약은 아직 쓰지 않은 크레딧이므로 전체 환불에 포함한다.
        createPassBooking(slot1.getId());
        createPassBooking(slot2.getId());

        // 전체 환불
        mockMvc.perform(post("/admin/passes/{passId}/refund", pass.getId())
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceledBookings").value(2))
                .andExpect(jsonPath("$.refundCredits").value(8))
                .andExpect(jsonPath("$.refundAmount").value(320_000)); // 8 × (320000/8)

        var bookings = bookingRepository.findAll();
        var refundLedgers = passLedgerRepository.findByPassPurchaseId(pass.getId())
                .stream().filter(l -> l.getType() == PassLedgerType.REFUND).toList();
        Refund refund = awaitRefundStatus(RefundStatus.SUCCEEDED);
        PassPurchase reloaded = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        Slot reloadedSlot1 = slotRepository.findById(slot1.getId()).orElseThrow();
        Slot reloadedSlot2 = slotRepository.findById(slot2.getId()).orElseThrow();
        long historyCount = bookingHistoryRepository.count();
        verify(paymentProvider).refund(eq("test-pass-payment-key"), eq(320_000L), any());
        assertSoftly(softly -> {
            softly.assertThat(bookings).hasSize(2);
            softly.assertThat(bookings).allMatch(b -> b.getStatus() == BookingStatus.CANCELED);
            softly.assertThat(refund.getBookingId()).isNull();
            softly.assertThat(refund.getOrderId()).isNull();
            softly.assertThat(refund.getPassPurchaseId()).isEqualTo(pass.getId());
            softly.assertThat(refund.getAmount()).isEqualTo(320_000L);
            softly.assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
            softly.assertThat(refund.getPaymentKey()).isEqualTo("test-pass-payment-key");
            softly.assertThat(refund.getRefundTransactionKey()).isEqualTo("FAKE-TEST-PASS-REF");
            softly.assertThat(refundLedgers).hasSize(1);
            softly.assertThat(refundLedgers.get(0).getAmount()).isEqualTo(8);
            softly.assertThat(reloaded.getRemainingCredits()).isEqualTo(0);
            // Q1-T4: slot bookedCount 복구 확인
            softly.assertThat(reloadedSlot1.getBookedCount()).as("slot1 bookedCount").isEqualTo(0);
            softly.assertThat(reloadedSlot2.getBookedCount()).as("slot2 bookedCount").isEqualTo(0);
            // Q1-T4: BookingHistory 적재 확인 (BOOKED×2 + CANCELED×2 = 4)
            softly.assertThat(historyCount).as("booking history count").isEqualTo(4L);
        });
    }

    @DisplayName("8회권 전체 환불 PG 실패 시 FAILED 환불 이력을 남긴다")
    @Test
    void refund_pass_pgFailure_recordsFailedRefund() throws Exception {
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenReturn(RefundResult.failure("PG가 환불을 거절했습니다."));

        mockMvc.perform(post("/admin/passes/{passId}/refund", pass.getId())
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceledBookings").value(0))
                .andExpect(jsonPath("$.refundCredits").value(8))
                .andExpect(jsonPath("$.refundAmount").value(320_000))
                .andExpect(jsonPath("$.refundStatus").value("REQUESTED"));

        Refund refund = awaitRefundStatus(RefundStatus.FAILED);
        PassPurchase reloaded = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        verify(paymentProvider).refund(eq("test-pass-payment-key"), eq(320_000L), any());

        mockMvc.perform(get("/admin/refunds/failed")
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passPurchaseId").value(pass.getId()))
                .andExpect(jsonPath("$[0].bookingId").value(nullValue()))
                .andExpect(jsonPath("$[0].orderId").value(nullValue()));

        assertSoftly(softly -> {
            softly.assertThat(refund.getPassPurchaseId()).isEqualTo(pass.getId());
            softly.assertThat(refund.getAmount()).isEqualTo(320_000L);
            softly.assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
            softly.assertThat(refund.getPaymentKey()).isEqualTo("test-pass-payment-key");
            softly.assertThat(refund.getRefundTransactionKey()).isNull();
            softly.assertThat(refund.getFailReason()).isEqualTo("PG가 환불을 거절했습니다.");
            softly.assertThat(reloaded.getRemainingCredits()).isEqualTo(0);
            softly.assertThat(passLedgerRepository.findByPassPurchaseId(pass.getId()))
                    .anySatisfy(ledger -> {
                        softly.assertThat(ledger.getType()).isEqualTo(PassLedgerType.REFUND);
                        softly.assertThat(ledger.getAmount()).isEqualTo(8);
                    });
        });
    }

    // -----------------------------------------------------------------------
    // Proof 6: 잔여 크레딧 0 → 예약 시도 422
    // -----------------------------------------------------------------------

    @DisplayName("잔여 크레딧이 없으면 8회권 예약 시 422를 반환한다")
    @Test
    void book_with_pass_no_credits_returns_422() throws Exception {
        // remaining을 0으로 강제 소멸
        pass.expire();
        passPurchaseRepository.save(pass);

        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        PaymentTestHelper.PreparedPayment prepared = paymentHelper.preparePayment(
                PaymentContext.BOOKING,
                passBookingPayload(pass, slot),
                sessionCookie);

        mockMvc.perform(post("/api/v1/payments/confirm")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest(prepared)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PASS_CREDIT_INSUFFICIENT"));
    }

    @DisplayName("만료 시각에 도달한 8회권으로 예약하면 422를 반환한다")
    @Test
    void book_with_pass_at_expiry_returns_422() throws Exception {
        PassPurchase expiredPass = passPurchase(pass.getUserId(), LocalDateTime.now(clock), 320_000L);
        expiredPass.recordPaymentKey("expired-pass-payment-key");
        expiredPass = passPurchaseRepository.save(expiredPass);
        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        PaymentTestHelper.PreparedPayment prepared = paymentHelper.preparePayment(
                PaymentContext.BOOKING,
                passBookingPayload(expiredPass, slot),
                sessionCookie);

        mockMvc.perform(post("/api/v1/payments/confirm")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest(prepared)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PASS_EXPIRED"));
    }

    private Long createPassBooking(Long slotId) throws Exception {
        return paymentHelper.createMemberPassBooking(sessionCookie, pass.getUserId(), slotId, pass.getId())
                .domainId();
    }

    private Cookie signupAndGetSessionCookie(String email, String phone) throws Exception {
        return customerHelper.signupAndGetSessionCookie(email, phone);
    }

    private BookingPayload passBookingPayload(PassPurchase passPurchase, Slot slot) {
        return new BookingPayload(
                passPurchase.getUserId(), null, null, null, slot.getId(), passPurchase.getId(), null);
    }

    private String confirmRequest(PaymentTestHelper.PreparedPayment prepared) throws Exception {
        return objectMapper.writeValueAsString(
                new ConfirmPaymentRequest(null, prepared.orderId(), prepared.amount()));
    }

    private Refund awaitRefundStatus(RefundStatus status) {
        await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(25, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var refunds = refundRepository.findAll();
                    assertThat(refunds).hasSize(1);
                    assertThat(refunds.get(0).getStatus()).isEqualTo(status);
                });
        return refundRepository.findAll().get(0);
    }

}
