package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.support.BookingTestHelper;
import com.personal.happygallery.support.BookingStateProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static com.personal.happygallery.support.BookingTestHelper.FUTURE;
import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class BookingRescheduleUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired PhoneVerificationReaderPort phoneVerificationReaderPort;
    @Autowired BookingStateProbe bookingStateProbe;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired DefaultSlotManagementService slotManagementService;
    @Autowired SlotBookingSupport slotBookingSupport;
    @Autowired Clock clock;
    @Autowired PlatformTransactionManager transactionManager;

    BookingClass cls;
    BookingTestHelper helper;

    @BeforeEach
    void setUp() {
        helper = new BookingTestHelper(mockMvc, phoneVerificationReaderPort);
        cleanupSupport.clearBookingWithPassAndRefundData();

        cls = classStorePort.save(defaultBookingClass());
    }

    // -----------------------------------------------------------------------
    // Proof 테스트: 5번 변경 후 bookings 1건 + booking_history 6건(BOOKED×1 + RESCHEDULED×5)
    // -----------------------------------------------------------------------

    @DisplayName("예약 변경 성공 시 상태와 이력이 5회 반복 검증에서도 일관된다")
    @Test
    void reschedule_success_and_5times_proofTest() throws Exception {
        // 슬롯 6개 생성 (간격을 충분히 벌려 버퍼 간섭 방지)
        Slot[] slots = new Slot[6];
        for (int i = 0; i < 6; i++) {
            slots[i] = slotStorePort.save(slot(cls,
                    FUTURE.plusHours(i * 3L),
                    FUTURE.plusHours(i * 3L + 2)));
        }

        // 초기 예약 생성 (slots[0])
        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01011110000", slots[0].getId(), 5000L);

        // 5번 연속 변경 (slots[1] → slots[2] → ... → slots[5])
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(patch("/bookings/{id}/reschedule", booking.bookingId())
                            .header("X-Access-Token", booking.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "newSlotId": %d
                                    }
                                    """.formatted(slots[i].getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingId").value(booking.bookingId()))
                    .andExpect(jsonPath("$.slotId").value(slots[i].getId()))
                    .andExpect(jsonPath("$.status").value("BOOKED"));
        }

        // Proof: bookings 1건 유지 + 예약금 그대로 (재결제 없음)
        var savedBooking = bookingStateProbe.getBooking(booking.bookingId());

        // Proof: booking_history 6건 (BOOKED×1 + RESCHEDULED×5)
        long historyCount = bookingStateProbe.bookingHistoryCountByBookingId(booking.bookingId());
        int finalSlotBookedCount = bookingStateProbe.getSlot(slots[5].getId()).getBookedCount();
        assertSoftly(softly -> {
            softly.assertThat(savedBooking.getSlot().getId()).isEqualTo(slots[5].getId());
            softly.assertThat(savedBooking.getStatus().name()).isEqualTo("BOOKED");
            softly.assertThat(savedBooking.getDepositAmount()).isEqualTo(5000L);
            softly.assertThat(bookingStateProbe.bookingCount()).isEqualTo(1L);
            softly.assertThat(historyCount).isEqualTo(6L);
            softly.assertThat(finalSlotBookedCount).isEqualTo(1);
        });

        // 슬롯 정원 상태 확인: 나머지는 0
        for (int i = 0; i < 5; i++) {
            int idx = i;
            assertThat(bookingStateProbe.getSlot(slots[idx].getId()).getBookedCount()).isEqualTo(0);
        }
    }

    // -----------------------------------------------------------------------
    // 422 — 시간 경계 정책 위반
    // -----------------------------------------------------------------------

    @DisplayName("변경 가능 시간이 지난 예약을 변경하면 422를 반환한다")
    @Test
    void reschedule_changeNotAllowed_returns422() throws Exception {
        // 현재 시각 기준 30분 후 시작하는 슬롯 (1시간 이내 → 변경 불가)
        LocalDateTime soonStart = LocalDateTime.now(clock).plusMinutes(30);
        Slot nearSlot = slotStorePort.save(slot(cls, soonStart, soonStart.plusHours(2)));
        Slot targetSlot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01022220001", nearSlot.getId(), 5000L);

        mockMvc.perform(patch("/bookings/{id}/reschedule", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newSlotId": %d
                                }
                                """.formatted(targetSlot.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CHANGE_NOT_ALLOWED"));
    }

    // -----------------------------------------------------------------------
    // 400 — 동일 슬롯으로 변경 시도
    // -----------------------------------------------------------------------

    @DisplayName("동일 슬롯으로 예약 변경을 요청하면 400을 반환한다")
    @Test
    void reschedule_sameSlot_returns400() throws Exception {
        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01033330001", slot.getId(), 5000L);

        mockMvc.perform(patch("/bookings/{id}/reschedule", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newSlotId": %d
                                }
                                """.formatted(slot.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    // -----------------------------------------------------------------------
    // 409 — 비활성 슬롯으로 변경 시도
    // -----------------------------------------------------------------------

    @DisplayName("비활성 슬롯으로 예약 변경을 요청하면 409를 반환한다")
    @Test
    void reschedule_slotNotAvailable_returns409() throws Exception {
        Slot fromSlot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        Slot inactiveSlot = slotStorePort.save(slot(cls, FUTURE.plusHours(4), FUTURE.plusHours(6)));
        slotManagementService.deactivateSlot(inactiveSlot.getId());

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01044440001", fromSlot.getId(), 5000L);

        mockMvc.perform(patch("/bookings/{id}/reschedule", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newSlotId": %d
                                }
                                """.formatted(inactiveSlot.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_NOT_AVAILABLE"));
    }

    // -----------------------------------------------------------------------
    // 409 — 정원 초과 슬롯으로 변경 시도
    // -----------------------------------------------------------------------

    @DisplayName("예약 변경 시 정원 초과 슬롯을 선택하면 409를 반환한다")
    @Test
    void reschedule_capacityExceeded_returns409() throws Exception {
        Slot fromSlot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        Slot fullSlot = slotStorePort.save(slot(cls, FUTURE.plusHours(4), FUTURE.plusHours(6)));

        // fullSlot을 8명으로 채운다 (서비스 직접 호출)
        for (int i = 0; i < 8; i++) {
            confirmBookingInTx(fullSlot.getId());
        }

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01055550001", fromSlot.getId(), 5000L);

        mockMvc.perform(patch("/bookings/{id}/reschedule", booking.bookingId())
                        .header("X-Access-Token", booking.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newSlotId": %d
                                }
                                """.formatted(fullSlot.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITY_EXCEEDED"));
    }

    // -----------------------------------------------------------------------
    // 404 — 잘못된 access_token
    // -----------------------------------------------------------------------

    @DisplayName("잘못된 토큰으로 예약 변경을 요청하면 404를 반환한다")
    @Test
    void reschedule_wrongToken_returns404() throws Exception {
        Slot fromSlot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        Slot toSlot = slotStorePort.save(slot(cls, FUTURE.plusHours(4), FUTURE.plusHours(6)));

        BookingTestHelper.CreatedBooking booking = helper.createVerifiedCardBooking("01066660001", fromSlot.getId(), 5000L);

        mockMvc.perform(patch("/bookings/{id}/reschedule", booking.bookingId())
                        .header("X-Access-Token", "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newSlotId": %d
                                }
                                """.formatted(toSlot.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private void confirmBookingInTx(Long slotId) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> slotBookingSupport.confirmBooking(slotId));
    }

}
