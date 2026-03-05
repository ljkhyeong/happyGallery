package com.personal.happygallery.app.booking;

import com.jayway.jsonpath.JsonPath;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.ClassRepository;
import com.personal.happygallery.infra.booking.GuestRepository;
import com.personal.happygallery.infra.booking.PhoneVerificationRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class BookingRescheduleUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired ClassRepository classRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired GuestRepository guestRepository;
    @Autowired PhoneVerificationRepository phoneVerificationRepository;
    @Autowired SlotManagementService slotManagementService;
    @Autowired PassLedgerRepository passLedgerRepository;
    @Autowired PassPurchaseRepository passPurchaseRepository;

    BookingClass cls;

    /** 충분히 먼 미래 슬롯 시작 시각 — isChangeable() 항상 통과 */
    private static final LocalDateTime FUTURE = LocalDateTime.of(2030, 1, 1, 10, 0);

    @BeforeEach
    void setUp() {
        // FK 순서에 맞게 삭제
        passLedgerRepository.deleteAll();
        bookingHistoryRepository.deleteAll();
        bookingRepository.deleteAll();
        passPurchaseRepository.deleteAll();
        phoneVerificationRepository.deleteAll();
        guestRepository.deleteAll();
        slotRepository.deleteAll();
        classRepository.deleteAll();

        cls = classRepository.save(new BookingClass("향수 클래스", "PERFUME", 120, 50_000L, 30));
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
            slots[i] = slotRepository.save(new Slot(cls,
                    FUTURE.plusHours(i * 3L),
                    FUTURE.plusHours(i * 3L + 2)));
        }

        // 초기 예약 생성 (slots[0])
        String code = sendVerificationAndGetCode("01011110000");
        String createResp = mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "01011110000",
                                  "verificationCode": "%s",
                                  "name": "홍길동",
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(code, slots[0].getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookingId = ((Number) JsonPath.read(createResp, "$.bookingId")).longValue();
        String token = JsonPath.read(createResp, "$.accessToken");

        // 5번 연속 변경 (slots[1] → slots[2] → ... → slots[5])
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(patch("/bookings/{id}/reschedule", bookingId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "newSlotId": %d,
                                      "token": "%s"
                                    }
                                    """.formatted(slots[i].getId(), token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingId").value(bookingId))
                    .andExpect(jsonPath("$.slotId").value(slots[i].getId()))
                    .andExpect(jsonPath("$.status").value("BOOKED"));
        }

        // Proof: bookings 1건 유지 + 예약금 그대로 (재결제 없음)
        assertThat(bookingRepository.findById(bookingId))
                .isPresent()
                .hasValueSatisfying(b -> {
                    assertThat(b.getSlot().getId()).isEqualTo(slots[5].getId());
                    assertThat(b.getStatus().name()).isEqualTo("BOOKED");
                    assertThat(b.getDepositAmount()).isEqualTo(5000L);
                });
        assertThat(bookingRepository.count()).isEqualTo(1L);

        // Proof: booking_history 6건 (BOOKED×1 + RESCHEDULED×5)
        assertThat(bookingHistoryRepository.countByBookingId(bookingId)).isEqualTo(6L);

        // 슬롯 정원 상태 확인: 최종 슬롯만 1, 나머지는 0
        assertThat(slotRepository.findById(slots[5].getId()))
                .hasValueSatisfying(s -> assertThat(s.getBookedCount()).isEqualTo(1));
        for (int i = 0; i < 5; i++) {
            int idx = i;
            assertThat(slotRepository.findById(slots[idx].getId()))
                    .hasValueSatisfying(s -> assertThat(s.getBookedCount()).isEqualTo(0));
        }
    }

    // -----------------------------------------------------------------------
    // 422 — 시간 경계 정책 위반
    // -----------------------------------------------------------------------

    @DisplayName("변경 가능 시간이 지난 예약을 변경하면 422를 반환한다")
    @Test
    void reschedule_changeNotAllowed_returns422() throws Exception {
        // 현재 시각 기준 30분 후 시작하는 슬롯 (1시간 이내 → 변경 불가)
        LocalDateTime soonStart = LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusMinutes(30);
        Slot nearSlot = slotRepository.save(new Slot(cls, soonStart, soonStart.plusHours(2)));
        Slot targetSlot = slotRepository.save(new Slot(cls, FUTURE, FUTURE.plusHours(2)));

        String code = sendVerificationAndGetCode("01022220001");
        String createResp = mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "01022220001",
                                  "verificationCode": "%s",
                                  "name": "홍길동",
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(code, nearSlot.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookingId = ((Number) JsonPath.read(createResp, "$.bookingId")).longValue();
        String token = JsonPath.read(createResp, "$.accessToken");

        mockMvc.perform(patch("/bookings/{id}/reschedule", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newSlotId": %d,
                                  "token": "%s"
                                }
                                """.formatted(targetSlot.getId(), token)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CHANGE_NOT_ALLOWED"));
    }

    // -----------------------------------------------------------------------
    // 400 — 동일 슬롯으로 변경 시도
    // -----------------------------------------------------------------------

    @DisplayName("동일 슬롯으로 예약 변경을 요청하면 400을 반환한다")
    @Test
    void reschedule_sameSlot_returns400() throws Exception {
        Slot slot = slotRepository.save(new Slot(cls, FUTURE, FUTURE.plusHours(2)));

        String code = sendVerificationAndGetCode("01033330001");
        String createResp = mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "01033330001",
                                  "verificationCode": "%s",
                                  "name": "홍길동",
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(code, slot.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookingId = ((Number) JsonPath.read(createResp, "$.bookingId")).longValue();
        String token = JsonPath.read(createResp, "$.accessToken");

        mockMvc.perform(patch("/bookings/{id}/reschedule", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newSlotId": %d,
                                  "token": "%s"
                                }
                                """.formatted(slot.getId(), token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    // -----------------------------------------------------------------------
    // 409 — 비활성 슬롯으로 변경 시도
    // -----------------------------------------------------------------------

    @DisplayName("비활성 슬롯으로 예약 변경을 요청하면 409를 반환한다")
    @Test
    void reschedule_slotNotAvailable_returns409() throws Exception {
        Slot fromSlot = slotRepository.save(new Slot(cls, FUTURE, FUTURE.plusHours(2)));
        Slot inactiveSlot = slotRepository.save(new Slot(cls, FUTURE.plusHours(4), FUTURE.plusHours(6)));
        slotManagementService.deactivateSlot(inactiveSlot.getId());

        String code = sendVerificationAndGetCode("01044440001");
        String createResp = mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "01044440001",
                                  "verificationCode": "%s",
                                  "name": "홍길동",
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(code, fromSlot.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookingId = ((Number) JsonPath.read(createResp, "$.bookingId")).longValue();
        String token = JsonPath.read(createResp, "$.accessToken");

        mockMvc.perform(patch("/bookings/{id}/reschedule", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newSlotId": %d,
                                  "token": "%s"
                                }
                                """.formatted(inactiveSlot.getId(), token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_NOT_AVAILABLE"));
    }

    // -----------------------------------------------------------------------
    // 409 — 정원 초과 슬롯으로 변경 시도
    // -----------------------------------------------------------------------

    @DisplayName("예약 변경 시 정원 초과 슬롯을 선택하면 409를 반환한다")
    @Test
    void reschedule_capacityExceeded_returns409() throws Exception {
        Slot fromSlot = slotRepository.save(new Slot(cls, FUTURE, FUTURE.plusHours(2)));
        Slot fullSlot = slotRepository.save(new Slot(cls, FUTURE.plusHours(4), FUTURE.plusHours(6)));

        // fullSlot을 8명으로 채운다 (서비스 직접 호출)
        for (int i = 0; i < 8; i++) {
            slotManagementService.confirmBooking(fullSlot.getId());
        }

        String code = sendVerificationAndGetCode("01055550001");
        String createResp = mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "01055550001",
                                  "verificationCode": "%s",
                                  "name": "홍길동",
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(code, fromSlot.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookingId = ((Number) JsonPath.read(createResp, "$.bookingId")).longValue();
        String token = JsonPath.read(createResp, "$.accessToken");

        mockMvc.perform(patch("/bookings/{id}/reschedule", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newSlotId": %d,
                                  "token": "%s"
                                }
                                """.formatted(fullSlot.getId(), token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITY_EXCEEDED"));
    }

    // -----------------------------------------------------------------------
    // 404 — 잘못된 access_token
    // -----------------------------------------------------------------------

    @DisplayName("잘못된 토큰으로 예약 변경을 요청하면 404를 반환한다")
    @Test
    void reschedule_wrongToken_returns404() throws Exception {
        Slot fromSlot = slotRepository.save(new Slot(cls, FUTURE, FUTURE.plusHours(2)));
        Slot toSlot   = slotRepository.save(new Slot(cls, FUTURE.plusHours(4), FUTURE.plusHours(6)));

        String code = sendVerificationAndGetCode("01066660001");
        String createResp = mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "01066660001",
                                  "verificationCode": "%s",
                                  "name": "홍길동",
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(code, fromSlot.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookingId = ((Number) JsonPath.read(createResp, "$.bookingId")).longValue();

        mockMvc.perform(patch("/bookings/{id}/reschedule", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newSlotId": %d,
                                  "token": "invalid-token"
                                }
                                """.formatted(toSlot.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // -----------------------------------------------------------------------
    // helper
    // -----------------------------------------------------------------------

    private String sendVerificationAndGetCode(String phone) throws Exception {
        String resp = mockMvc.perform(post("/bookings/phone-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "phone": "%s" }
                                """.formatted(phone)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resp, "$.code");
    }
}
