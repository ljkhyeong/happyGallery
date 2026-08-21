package com.personal.happygallery.application.pass;

import com.personal.happygallery.adapter.in.web.payment.dto.ConfirmPaymentRequest;
import com.personal.happygallery.adapter.out.persistence.booking.BookingHistoryRepository;
import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.booking.ClassRepository;
import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import com.personal.happygallery.adapter.out.persistence.booking.SlotRepository;
import com.personal.happygallery.adapter.out.persistence.pass.PassLedgerRepository;
import com.personal.happygallery.adapter.out.persistence.pass.PassPurchaseRepository;
import com.personal.happygallery.application.booking.port.in.MemberBookingUseCase;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import com.personal.happygallery.domain.booking.BalanceStatus;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPlan;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.support.CustomerTestHelper;
import com.personal.happygallery.support.NotificationLogProbe;
import com.personal.happygallery.support.PaymentTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static com.personal.happygallery.support.BookingTestHelper.FUTURE;
import static com.personal.happygallery.support.TestFixtures.bookingClass;
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
import static org.mockito.Mockito.never;
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
    @Autowired UserReaderPort userReaderPort;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;
    @Autowired ObjectMapper objectMapper;
    @Autowired PhoneVerificationReaderPort phoneVerificationReader;
    @Autowired NotificationLogProbe notificationLogProbe;
    @Autowired MemberBookingUseCase memberBookingUseCase;
    @Autowired JdbcTemplate jdbcTemplate;
    @MockitoBean PaymentPort paymentProvider;

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
        customerHelper = new CustomerTestHelper(mockMvc, objectMapper, phoneVerificationReader);

        cls = classRepository.save(bookingClass("우드 정규 클래스", "WOOD", 120, 50_000L, 30));
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenReturn(RefundResult.success("FAKE-TEST-PASS-REF"));
        sessionCookie = customerHelper.signupAndGetSessionCookie("pass-member@example.com", "01099990001");
        Long userId = userReaderPort.findByEmail("pass-member@example.com").orElseThrow().getId();
        pass = passPurchase(userId, FUTURE.plusDays(90), 320_000L);
        pass.recordPaymentKey("test-pass-payment-key");
        pass = passPurchaseRepository.save(pass);
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearNotificationLogs();
        cleanupSupport.clearUsers();
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
            softly.assertThat(ledgers).singleElement().satisfies(ledger -> {
                softly.assertThat(ledger.getType()).isEqualTo(PassLedgerType.USE);
                softly.assertThat(ledger.getAmount()).isEqualTo(1);
                softly.assertThat(ledger.getRelatedBookingId()).isEqualTo(confirmed.domainId());
            });
            softly.assertThat(reloaded.getRemainingCredits()).isEqualTo(7);
            softly.assertThat(bookings).singleElement().satisfies(booking -> {
                softly.assertThat(booking.getId()).isEqualTo(confirmed.domainId());
                softly.assertThat(booking.isPassBooking()).isTrue();
                softly.assertThat(booking.getBalanceStatus()).isEqualTo(BalanceStatus.PAID);
            });
        });
    }

    @DisplayName("prepare 후 클래스가 8회권 사용 불가로 바뀌면 confirm에서 다시 거절한다")
    @Test
    void book_with_regularCraftPass_forIneligibleCraftClass_returns422WithoutMutation() throws Exception {
        BookingClass ineligibleClass = classRepository.save(new BookingClass(
                "우드 원데이 클래스",
                "WOOD",
                120,
                50_000L,
                30,
                true,
                null,
                null,
                null,
                null));
        Slot ineligibleSlot = slotRepository.save(slot(ineligibleClass, FUTURE, FUTURE.plusHours(2)));
        PaymentTestHelper.PreparedPayment prepared = paymentHelper.preparePayment(
                PaymentContext.BOOKING,
                passBookingPayload(pass, ineligibleSlot),
                sessionCookie);
        ineligibleClass.updateDetails(
                ineligibleClass.getName(),
                ineligibleClass.getCategory(),
                ineligibleClass.getPrice(),
                false,
                ineligibleClass.getDescription(),
                ineligibleClass.getImageUrl(),
                ineligibleClass.getPreparationInfo(),
                ineligibleClass.getTargetAudience());
        classRepository.save(ineligibleClass);

        mockMvc.perform(post("/api/v1/payments/confirm")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest(prepared)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PASS_NOT_APPLICABLE"));

        PassPurchase reloadedPass = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        Slot reloadedSlot = slotRepository.findById(ineligibleSlot.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(reloadedPass.getRemainingCredits()).isEqualTo(8);
            softly.assertThat(reloadedSlot.getBookedCount()).isZero();
            softly.assertThat(bookingRepository.findAll()).isEmpty();
            softly.assertThat(passLedgerRepository.findByPassPurchaseId(pass.getId())).isEmpty();
        });
    }

    @DisplayName("정책 도입 전에 구매한 8회권은 기존처럼 향수 클래스에도 사용할 수 있다")
    @Test
    void book_with_legacyPass_forPerfumeClass_preservesPreviousContract() throws Exception {
        jdbcTemplate.update(
                "UPDATE pass_purchases SET plan_code = 'LEGACY_ALL_CLASSES' WHERE id = ?",
                pass.getId());
        BookingClass perfumeClass = classRepository.save(defaultBookingClass());
        Slot perfumeSlot = slotRepository.save(slot(perfumeClass, FUTURE, FUTURE.plusHours(2)));

        PaymentTestHelper.ConfirmedPayment confirmed = paymentHelper.createMemberPassBooking(
                sessionCookie, pass.getUserId(), perfumeSlot.getId(), pass.getId());

        PassPurchase reloadedPass = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(confirmed.domainId()).isPositive();
            softly.assertThat(reloadedPass.getPlan()).isEqualTo(PassPlan.LEGACY_ALL_CLASSES);
            softly.assertThat(reloadedPass.getRemainingCredits()).isEqualTo(7);
            softly.assertThat(slotRepository.findById(perfumeSlot.getId()).orElseThrow().getBookedCount())
                    .isEqualTo(1);
        });
    }

    @DisplayName("같은 8회권으로 서로 다른 클래스에 동시에 예약해도 크레딧과 원장이 모두 반영된다")
    @Test
    void concurrentBookings_withSamePass_areSerializedByPassLock() throws Exception {
        int bookingCount = 4;
        List<Long> slotIds = new ArrayList<>();
        for (int i = 0; i < bookingCount; i++) {
            BookingClass eligibleClass = classRepository.save(bookingClass(
                    "정규 공예 클래스 " + i,
                    i % 2 == 0 ? "WOOD" : "KNIT",
                    120,
                    50_000L,
                    30));
            LocalDateTime startAt = FUTURE.plusDays(i);
            slotIds.add(slotRepository.save(slot(eligibleClass, startAt, startAt.plusHours(2))).getId());
        }

        ExecutorService executor = Executors.newFixedThreadPool(bookingCount);
        CountDownLatch ready = new CountDownLatch(bookingCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> bookings = new ArrayList<>();
        try {
            for (Long slotId : slotIds) {
                bookings.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return memberBookingUseCase.createMemberPassBooking(
                            pass.getUserId(), slotId, pass.getId());
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> booking : bookings) {
                booking.get(15, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        PassPurchase reloaded = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        var useLedgers = passLedgerRepository.findByPassPurchaseId(pass.getId()).stream()
                .filter(ledger -> ledger.getType() == PassLedgerType.USE)
                .toList();
        assertSoftly(softly -> {
            softly.assertThat(reloaded.getRemainingCredits()).isEqualTo(8 - bookingCount);
            softly.assertThat(useLedgers).hasSize(bookingCount);
            softly.assertThat(useLedgers).extracting(ledger -> ledger.getRelatedBookingId())
                    .doesNotHaveDuplicates();
            softly.assertThat(bookingRepository.findAll()).hasSize(bookingCount);
            softly.assertThat(slotRepository.findAll())
                    .allMatch(savedSlot -> savedSlot.getBookedCount() == 1);
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
            softly.assertThat(ledgers)
                    .filteredOn(ledger -> ledger.getType() == PassLedgerType.REFUND)
                    .singleElement();
            softly.assertThat(reloaded.getRemainingCredits()).isEqualTo(8);
            softly.assertThat(refundRepository.count()).isEqualTo(0);
        });
    }

    @DisplayName("만료된 8회권 예약을 취소하면 크레딧을 복구하지 않고 잔액을 소멸시킨다")
    @Test
    void cancel_expiredPassBooking_doesNotRestoreCredit() throws Exception {
        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        Long bookingId = createPassBooking(slot.getId());
        jdbcTemplate.update(
                "UPDATE pass_purchases SET expires_at = ? WHERE id = ?",
                LocalDateTime.now(clock), pass.getId());

        mockMvc.perform(delete("/api/v1/me/bookings/{id}", bookingId)
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(false));

        PassPurchase reloaded = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        var ledgers = passLedgerRepository.findByPassPurchaseId(pass.getId());
        assertSoftly(softly -> {
            softly.assertThat(reloaded.getRemainingCredits()).isZero();
            softly.assertThat(ledgers).extracting(ledger -> ledger.getType())
                    .containsExactlyInAnyOrder(PassLedgerType.USE, PassLedgerType.EXPIRE);
            softly.assertThat(ledgers).filteredOn(ledger -> ledger.getType() == PassLedgerType.EXPIRE)
                    .singleElement()
                    .satisfies(ledger -> softly.assertThat(ledger.getAmount()).isEqualTo(7));
            softly.assertThat(ledgers).noneMatch(ledger -> ledger.getType() == PassLedgerType.REFUND);
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
            softly.assertThat(ledgers).singleElement()
                    .extracting(PassLedger::getType)
                    .isEqualTo(PassLedgerType.USE);
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
        LocalDateTime now = LocalDateTime.now(clock);
        jdbcTemplate.update(
                "UPDATE slots SET start_at = ?, end_at = ? WHERE id = ?",
                now.minusHours(2), now, slot.getId());

        mockMvc.perform(post("/api/v1/admin/bookings/{id}/no-show", bookingId)
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.status").value("NO_SHOW"));

        var ledgers = passLedgerRepository.findByPassPurchaseId(pass.getId());
        PassPurchase reloaded = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(bookingRepository.findById(bookingId))
                    .hasValueSatisfying(b -> assertThat(b.getStatus()).isEqualTo(BookingStatus.NO_SHOW));
            softly.assertThat(ledgers).singleElement()
                    .extracting(PassLedger::getType)
                    .isEqualTo(PassLedgerType.USE);
            softly.assertThat(reloaded.getRemainingCredits()).isEqualTo(7);
        });
    }

    @DisplayName("수업 종료 전에는 노쇼 처리할 수 없다")
    @Test
    void mark_no_show_before_class_end_returns_400() throws Exception {
        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        Long bookingId = createPassBooking(slot.getId());

        mockMvc.perform(post("/api/v1/admin/bookings/{id}/no-show", bookingId)
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        assertSoftly(softly -> {
            softly.assertThat(bookingRepository.findById(bookingId))
                    .hasValueSatisfying(booking -> softly.assertThat(booking.getStatus())
                            .isEqualTo(BookingStatus.BOOKED));
            softly.assertThat(passLedgerRepository.findByPassPurchaseId(pass.getId()))
                    .singleElement()
                    .satisfies(ledger -> softly.assertThat(ledger.getType()).isEqualTo(PassLedgerType.USE));
            softly.assertThat(passPurchaseRepository.findById(pass.getId()).orElseThrow().getRemainingCredits())
                    .isEqualTo(7);
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
        mockMvc.perform(post("/api/v1/admin/passes/{passId}/refund", pass.getId())
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceledBookings").value(2))
                .andExpect(jsonPath("$.refundCredits").value(8))
                .andExpect(jsonPath("$.refundAmount").value(320_000)); // 8 × (320000/8)

        var bookings = bookingRepository.findAll();
        var ledgers = passLedgerRepository.findByPassPurchaseId(pass.getId());
        Refund refund = awaitRefundStatus(RefundStatus.SUCCEEDED);
        await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(25, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(notificationLogProbe.all())
                        .anySatisfy(log -> {
                            assertThat(log.getUserId()).isEqualTo(pass.getUserId());
                            assertThat(log.getEventType()).isEqualTo(NotificationEventType.PASS_REFUNDED);
                            assertThat(log.getStatus()).isEqualTo("SUCCESS");
                        }));
        mockMvc.perform(get("/api/v1/me/passes")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].refund.amount").value(320_000))
                .andExpect(jsonPath("$[0].refund.status").value("SUCCEEDED"));
        Cookie otherSession = customerHelper.signupAndGetSessionCookie(
                "pass-other@example.com", "01099990002");
        mockMvc.perform(get("/api/v1/me/passes/{passId}", pass.getId())
                        .cookie(otherSession))
                .andExpect(status().isNotFound());
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
            softly.assertThat(ledgers)
                    .filteredOn(ledger -> ledger.getType() == PassLedgerType.REFUND)
                    .singleElement()
                    .extracting(PassLedger::getAmount)
                    .isEqualTo(8);
            softly.assertThat(reloaded.getRemainingCredits()).isEqualTo(0);
            // Q1-T4: slot bookedCount 복구 확인
            softly.assertThat(reloadedSlot1.getBookedCount()).as("slot1 bookedCount").isEqualTo(0);
            softly.assertThat(reloadedSlot2.getBookedCount()).as("slot2 bookedCount").isEqualTo(0);
            // Q1-T4: BookingHistory 적재 확인 (BOOKED×2 + CANCELED×2 = 4)
            softly.assertThat(historyCount).as("booking history count").isEqualTo(4L);
        });
    }

    @DisplayName("만료된 8회권은 전체 환불을 거절하고 EXPIRE 원장을 한 번만 기록한다")
    @Test
    void refund_expiredPass_isRejectedAndExpiredOnce() throws Exception {
        jdbcTemplate.update(
                "UPDATE pass_purchases SET expires_at = ? WHERE id = ?",
                LocalDateTime.now(clock), pass.getId());

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/v1/admin/passes/{passId}/refund", pass.getId())
                            .header("X-Admin-Key", ADMIN_KEY))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("PASS_EXPIRED"));
        }

        PassPurchase reloaded = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        var ledgers = passLedgerRepository.findByPassPurchaseId(pass.getId());
        assertSoftly(softly -> {
            softly.assertThat(reloaded.getRemainingCredits()).isZero();
            softly.assertThat(ledgers).singleElement().satisfies(ledger -> {
                softly.assertThat(ledger.getType()).isEqualTo(PassLedgerType.EXPIRE);
                softly.assertThat(ledger.getAmount()).isEqualTo(8);
            });
            softly.assertThat(refundRepository.count()).isZero();
        });
        verify(paymentProvider, never()).refund(any(), anyLong(), any());
    }

    @DisplayName("8회권 전체 환불 PG 실패 시 FAILED 환불 이력을 남긴다")
    @Test
    void refund_pass_pgFailure_recordsFailedRefund() throws Exception {
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenReturn(RefundResult.failure("PG가 환불을 거절했습니다."));

        mockMvc.perform(post("/api/v1/admin/passes/{passId}/refund", pass.getId())
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceledBookings").value(0))
                .andExpect(jsonPath("$.refundCredits").value(8))
                .andExpect(jsonPath("$.refundAmount").value(320_000))
                .andExpect(jsonPath("$.refundStatus").value("REQUESTED"));

        Refund refund = awaitRefundStatus(RefundStatus.FAILED);
        PassPurchase reloaded = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        verify(paymentProvider).refund(eq("test-pass-payment-key"), eq(320_000L), any());

        mockMvc.perform(get("/api/v1/admin/refunds/failed")
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].passPurchaseId").value(pass.getId()))
                .andExpect(jsonPath("$.content[0].bookingId").value(nullValue()))
                .andExpect(jsonPath("$.content[0].orderId").value(nullValue()));

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

    @DisplayName("prepare 후 잔여 크레딧이 소진되면 confirm에서 다시 422를 반환한다")
    @Test
    void book_with_pass_no_credits_returns_422() throws Exception {
        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        PaymentTestHelper.PreparedPayment prepared = paymentHelper.preparePayment(
                PaymentContext.BOOKING,
                passBookingPayload(pass, slot),
                sessionCookie);
        pass.expire();
        passPurchaseRepository.save(pass);

        mockMvc.perform(post("/api/v1/payments/confirm")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest(prepared)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PASS_CREDIT_INSUFFICIENT"));
    }

    @DisplayName("prepare 후 만료 시각에 도달한 8회권은 confirm에서 다시 422를 반환한다")
    @Test
    void book_with_pass_at_expiry_returns_422() throws Exception {
        PassPurchase expiredPass = passPurchase(
                pass.getUserId(), LocalDateTime.now(clock).plusDays(1), 320_000L);
        expiredPass.recordPaymentKey("expired-pass-payment-key");
        expiredPass = passPurchaseRepository.save(expiredPass);
        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        PaymentTestHelper.PreparedPayment prepared = paymentHelper.preparePayment(
                PaymentContext.BOOKING,
                passBookingPayload(expiredPass, slot),
                sessionCookie);
        jdbcTemplate.update(
                "UPDATE pass_purchases SET expires_at = ? WHERE id = ?",
                LocalDateTime.now(clock),
                expiredPass.getId());

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

    private BookingPayload passBookingPayload(PassPurchase passPurchase, Slot slot) {
        return new BookingPayload(
                passPurchase.getUserId(), null, null, null, slot.getId(), passPurchase.getId(), null);
    }

    private String confirmRequest(PaymentTestHelper.PreparedPayment prepared) throws Exception {
        return objectMapper.writeValueAsString(
                new ConfirmPaymentRequest(null, prepared.orderId(), prepared.amount()));
    }

    private Refund awaitRefundStatus(RefundStatus status) {
        List<Refund> refunds = await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(25, TimeUnit.MILLISECONDS)
                .until(
                        refundRepository::findAll,
                        found -> found.size() == 1 && found.getFirst().getStatus() == status);
        return refunds.getFirst();
    }

}
