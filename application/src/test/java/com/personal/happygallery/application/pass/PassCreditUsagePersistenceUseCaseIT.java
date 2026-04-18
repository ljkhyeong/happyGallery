package com.personal.happygallery.application.pass;

import com.personal.happygallery.application.booking.port.out.BookingHistoryPort;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.pass.port.out.PassLedgerReaderPort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.adapter.in.web.CustomerAuthFilter;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.support.BookingTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.servlet.Filter;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class PassCreditUsagePersistenceUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired CustomerAuthFilter customerAuthFilter;
    @Autowired @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired UserReaderPort userReaderPort;
    @Autowired PassPurchaseStorePort passPurchaseStorePort;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;
    @Autowired BookingReaderPort bookingReaderPort;
    @Autowired BookingHistoryPort bookingHistoryPort;
    @Autowired SlotReaderPort slotReaderPort;
    @Autowired PassLedgerReaderPort passLedgerReaderPort;
    @Autowired PassPurchaseReaderPort passPurchaseReaderPort;

    private PassCreditUsageFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new PassCreditUsageFixture(
                context,
                customerAuthFilter,
                springSessionRepositoryFilter,
                classStorePort,
                slotStorePort,
                userReaderPort,
                passPurchaseStorePort,
                cleanupSupport,
                clock);
        fixture.setUp();
    }

    @DisplayName("8회권 예약 시 USE 원장과 잔여 크레딧 차감이 저장된다")
    @Test
    void bookingWithPass_persistsUseLedgerAndRemainingCredits() throws Exception {
        Long bookingId = fixture.createPassBooking(fixture.createFutureSlot().getId());

        var ledgers = passLedgerReaderPort.findByPassPurchaseId(fixture.pass().getId());
        var reloadedPass = passPurchaseReaderPort.findById(fixture.pass().getId()).orElseThrow();
        var bookings = bookingReaderPort.findByUserIdWithDetails(fixture.pass().getUserId());

        assertSoftly(softly -> {
            softly.assertThat(ledgers).hasSize(1);
            softly.assertThat(ledgers.get(0).getType()).isEqualTo(PassLedgerType.USE);
            softly.assertThat(ledgers.get(0).getAmount()).isEqualTo(1);
            softly.assertThat(reloadedPass.getRemainingCredits()).isEqualTo(7);
            softly.assertThat(bookings).hasSize(1);
            softly.assertThat(bookings.get(0).isPassBooking()).isTrue();
            softly.assertThat(bookings.get(0).getId()).isEqualTo(bookingId);
        });
    }

    @DisplayName("8회권 예약을 기한 내 취소하면 REFUND 원장과 크레딧 복구가 저장된다")
    @Test
    void timelyCancel_persistsRefundLedgerAndCreditRestoration() throws Exception {
        Long bookingId = fixture.createPassBooking(fixture.createFutureSlot().getId());

        fixture.mockMvc().perform(delete("/api/v1/me/bookings/{id}", bookingId)
                        .cookie(fixture.sessionCookie()))
                .andExpect(status().isOk());

        var ledgers = passLedgerReaderPort.findByPassPurchaseId(fixture.pass().getId());
        var reloadedPass = passPurchaseReaderPort.findById(fixture.pass().getId()).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(ledgers).hasSize(2);
            softly.assertThat(ledgers.stream().filter(ledger -> ledger.getType() == PassLedgerType.REFUND).count())
                    .isEqualTo(1);
            softly.assertThat(reloadedPass.getRemainingCredits()).isEqualTo(8);
        });
    }

    @DisplayName("8회권 예약을 늦게 취소하면 USE 원장만 남고 크레딧이 유지 차감된다")
    @Test
    void lateCancel_keepsOnlyUseLedgerAndConsumedCredits() throws Exception {
        LocalDateTime today14 = LocalDateTime.now(fixture.clock()).toLocalDate().atTime(14, 0);
        Long bookingId = fixture.createPassBooking(fixture.createFutureSlot(today14).getId());

        fixture.mockMvc().perform(delete("/api/v1/me/bookings/{id}", bookingId)
                        .cookie(fixture.sessionCookie()))
                .andExpect(status().isOk());

        var ledgers = passLedgerReaderPort.findByPassPurchaseId(fixture.pass().getId());
        var reloadedPass = passPurchaseReaderPort.findById(fixture.pass().getId()).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(ledgers).hasSize(1);
            softly.assertThat(ledgers.get(0).getType()).isEqualTo(PassLedgerType.USE);
            softly.assertThat(reloadedPass.getRemainingCredits()).isEqualTo(7);
        });
    }

    @DisplayName("노쇼 처리 시 예약 상태만 NO_SHOW로 바뀌고 크레딧은 유지 차감된다")
    @Test
    void noShow_changesStatusOnlyAndKeepsConsumedCredits() throws Exception {
        Long bookingId = fixture.createPassBooking(fixture.createFutureSlot().getId());

        fixture.mockMvc().perform(post("/admin/bookings/{id}/no-show", bookingId))
                .andExpect(status().isOk());

        var ledgers = passLedgerReaderPort.findByPassPurchaseId(fixture.pass().getId());
        var reloadedPass = passPurchaseReaderPort.findById(fixture.pass().getId()).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(bookingReaderPort.findById(bookingId).orElseThrow().getStatus())
                    .isEqualTo(BookingStatus.NO_SHOW);
            softly.assertThat(ledgers).hasSize(1);
            softly.assertThat(ledgers.get(0).getType()).isEqualTo(PassLedgerType.USE);
            softly.assertThat(reloadedPass.getRemainingCredits()).isEqualTo(7);
        });
    }

    @DisplayName("8회권 전체 환불은 미래 예약 취소와 슬롯 정원 복구까지 저장한다")
    @Test
    void fullRefund_persistsBookingCancellationAndSlotRecovery() throws Exception {
        var firstSlot = fixture.createFutureSlot();
        var secondSlot = fixture.createFutureSlot(BookingTestHelper.FUTURE.plusDays(1));
        Long firstBookingId = fixture.createPassBooking(firstSlot.getId());
        Long secondBookingId = fixture.createPassBooking(secondSlot.getId());

        fixture.mockMvc().perform(post("/admin/passes/{passId}/refund", fixture.pass().getId()))
                .andExpect(status().isOk());

        var bookings = bookingReaderPort.findByUserIdWithDetails(fixture.pass().getUserId());
        var refundLedgers = passLedgerReaderPort.findByPassPurchaseId(fixture.pass().getId()).stream()
                .filter(ledger -> ledger.getType() == PassLedgerType.REFUND)
                .toList();
        var reloadedPass = passPurchaseReaderPort.findById(fixture.pass().getId()).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(bookings).hasSize(2);
            softly.assertThat(bookings).allMatch(booking -> booking.getStatus() == BookingStatus.CANCELED);
            softly.assertThat(refundLedgers).hasSize(1);
            softly.assertThat(refundLedgers.get(0).getAmount()).isEqualTo(6);
            softly.assertThat(reloadedPass.getRemainingCredits()).isEqualTo(0);
            softly.assertThat(slotReaderPort.findById(firstSlot.getId()).orElseThrow().getBookedCount()).isEqualTo(0);
            softly.assertThat(slotReaderPort.findById(secondSlot.getId()).orElseThrow().getBookedCount()).isEqualTo(0);
            softly.assertThat(bookingHistoryPort.countByBookingId(firstBookingId)
                    + bookingHistoryPort.countByBookingId(secondBookingId)).isEqualTo(4L);
        });
    }
}
