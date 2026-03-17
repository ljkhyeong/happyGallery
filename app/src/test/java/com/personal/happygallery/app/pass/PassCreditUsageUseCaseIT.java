package com.personal.happygallery.app.pass;

import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.ClassRepository;
import com.personal.happygallery.infra.booking.GuestRepository;
import com.personal.happygallery.infra.booking.PhoneVerificationRepository;
import com.personal.happygallery.infra.booking.RefundRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import com.personal.happygallery.support.BookingTestHelper;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.personal.happygallery.support.BookingTestHelper.FUTURE;
import static com.personal.happygallery.support.TestDataCleaner.clearBookingWithPassAndRefundData;
import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static com.personal.happygallery.support.TestFixtures.guest;
import static com.personal.happygallery.support.TestFixtures.passPurchase;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class PassCreditUsageUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired PassPurchaseRepository passPurchaseRepository;
    @Autowired PassLedgerRepository passLedgerRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired GuestRepository guestRepository;
    @Autowired PhoneVerificationRepository phoneVerificationRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired ClassRepository classRepository;

    BookingClass cls;
    Guest guest;
    PassPurchase pass;
    BookingTestHelper helper;

    @BeforeEach
    void setUp() {
        helper = new BookingTestHelper(mockMvc, phoneVerificationRepository);
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

        cls = classRepository.save(defaultBookingClass());
        guest = guestRepository.save(guest("김테스트", "01099990001"));
        pass = passPurchaseRepository.save(passPurchase(guest, FUTURE.plusDays(90), 320_000L));
        // EARN ledger 없이 직접 생성 — 크레딧 잔여 8
    }

    // -----------------------------------------------------------------------
    // Proof 1: 8회권 예약 시 USE ledger(-1), remaining=7
    // -----------------------------------------------------------------------

    @DisplayName("8회권으로 예약하면 크레딧이 차감된다")
    @Test
    void book_with_pass_consumes_credit() throws Exception {
        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        String code = helper.sendVerificationAndGetCode("01099990001");
        mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "01099990001",
                                  "verificationCode": "%s",
                                  "name": "김테스트",
                                  "slotId": %d,
                                  "passId": %d
                                }
                                """.formatted(code, slot.getId(), pass.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"));

        // Proof: USE ledger 1건, amount=1
        var ledgers = passLedgerRepository.findByPassPurchaseId(pass.getId());
        PassPurchase reloaded = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        var bookings = bookingRepository.findAll();
        assertSoftly(softly -> {
            softly.assertThat(ledgers).hasSize(1);
            if (!ledgers.isEmpty()) {
                softly.assertThat(ledgers.get(0).getType()).isEqualTo(PassLedgerType.USE);
                softly.assertThat(ledgers.get(0).getAmount()).isEqualTo(1);
            }
            softly.assertThat(reloaded.getRemainingCredits()).isEqualTo(7);
            softly.assertThat(bookings).hasSize(1);
            if (!bookings.isEmpty()) {
                softly.assertThat(bookings.get(0).isPassBooking()).isTrue();
            }
        });
    }

    // -----------------------------------------------------------------------
    // Proof 2: D-1 이전 취소 → REFUND ledger(+1), remaining=8 복구
    // -----------------------------------------------------------------------

    @DisplayName("8회권 예약을 기한 내 취소하면 크레딧이 환불된다")
    @Test
    void cancel_pass_booking_timely_refunds_credit() throws Exception {
        Slot slot = slotRepository.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedPassBooking("01099990001", slot.getId(), pass.getId());

        // 취소 (FUTURE = 2030년 → D-1 이전이므로 환불 가능)
        mockMvc.perform(delete("/bookings/{id}", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken()))
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
    // Proof 3: D-1 이후 취소 → 크레딧 소멸 유지 (remaining=7)
    // -----------------------------------------------------------------------

    @DisplayName("8회권 예약을 늦게 취소하면 크레딧이 소멸된다")
    @Test
    void cancel_pass_booking_late_loses_credit() throws Exception {
        // 오늘 14:00 시작 슬롯 — D-1 deadline(오늘 00:00) 이미 지남
        LocalDateTime today14 = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .withHour(14).withMinute(0).withSecond(0).withNano(0);
        Slot slot = slotRepository.save(slot(cls, today14, today14.plusHours(2)));

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedPassBooking("01099990001", slot.getId(), pass.getId());

        mockMvc.perform(delete("/bookings/{id}", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken()))
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

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedPassBooking("01099990001", slot.getId(), pass.getId());

        mockMvc.perform(post("/admin/bookings/{id}/no-show", booking.bookingId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(booking.bookingId()))
                .andExpect(jsonPath("$.status").value("NO_SHOW"));

        var ledgers = passLedgerRepository.findByPassPurchaseId(pass.getId());
        PassPurchase reloaded = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(bookingRepository.findById(booking.bookingId()))
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

        // 2회 예약 (remaining: 8 → 6)
        helper.createVerifiedPassBooking("01099990001", slot1.getId(), pass.getId());
        helper.createVerifiedPassBooking("01099990001", slot2.getId(), pass.getId());

        // 전체 환불
        mockMvc.perform(post("/admin/passes/{passId}/refund", pass.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceledBookings").value(2))
                .andExpect(jsonPath("$.refundCredits").value(6))
                .andExpect(jsonPath("$.refundAmount").value(240_000)); // 6 × (320000/8)

        var bookings = bookingRepository.findAll();
        var refundLedgers = passLedgerRepository.findByPassPurchaseId(pass.getId())
                .stream().filter(l -> l.getType() == PassLedgerType.REFUND).toList();
        PassPurchase reloaded = passPurchaseRepository.findById(pass.getId()).orElseThrow();
        Slot reloadedSlot1 = slotRepository.findById(slot1.getId()).orElseThrow();
        Slot reloadedSlot2 = slotRepository.findById(slot2.getId()).orElseThrow();
        long historyCount = bookingHistoryRepository.count();
        assertSoftly(softly -> {
            softly.assertThat(bookings).hasSize(2);
            softly.assertThat(bookings).allMatch(b -> b.getStatus() == BookingStatus.CANCELED);
            softly.assertThat(refundLedgers).hasSize(1);
            softly.assertThat(refundLedgers.get(0).getAmount()).isEqualTo(6);
            softly.assertThat(reloaded.getRemainingCredits()).isEqualTo(0);
            // Q1-T4: slot bookedCount 복구 확인
            softly.assertThat(reloadedSlot1.getBookedCount()).as("slot1 bookedCount").isEqualTo(0);
            softly.assertThat(reloadedSlot2.getBookedCount()).as("slot2 bookedCount").isEqualTo(0);
            // Q1-T4: BookingHistory 적재 확인 (BOOKED×2 + CANCELED×2 = 4)
            softly.assertThat(historyCount).as("booking history count").isEqualTo(4L);
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
        String code = helper.sendVerificationAndGetCode("01099990001");

        mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "01099990001",
                                  "verificationCode": "%s",
                                  "name": "김테스트",
                                  "slotId": %d,
                                  "passId": %d
                                }
                                """.formatted(code, slot.getId(), pass.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PASS_CREDIT_INSUFFICIENT"));
    }

}
