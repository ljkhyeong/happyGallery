package com.personal.happygallery.app.booking;

import com.jayway.jsonpath.JsonPath;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.ClassRepository;
import com.personal.happygallery.infra.booking.GuestRepository;
import com.personal.happygallery.infra.booking.PhoneVerificationRepository;
import com.personal.happygallery.infra.booking.RefundRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import com.personal.happygallery.infra.payment.PaymentProvider;
import com.personal.happygallery.infra.payment.RefundResult;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @MockitoBean PaymentProvider paymentProvider;

    BookingClass cls;

    /** 충분히 먼 미래 슬롯 — isRefundable() 항상 true */
    private static final LocalDateTime FUTURE = LocalDateTime.of(2030, 1, 1, 10, 0);

    @BeforeEach
    void setUp() {
        // 기본: PaymentProvider 성공
        when(paymentProvider.refund(any(), anyLong()))
                .thenReturn(RefundResult.success("FAKE-TEST-REF"));
        // FK 순서: passLedger → refund → bookingHistory → booking → passPurchase
        //         → phoneVerification → guest → slot → class
        passLedgerRepository.deleteAll();
        refundRepository.deleteAll();
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
    // Proof: 취소 후 CANCELED 상태 + CANCELED 이력 + Refund REQUESTED 기록
    // -----------------------------------------------------------------------

    @DisplayName("환불 가능한 예약 취소 시 취소와 환불이 정상 처리된다")
    @Test
    void cancel_refundable_success() throws Exception {
        Slot slot = slotRepository.save(new Slot(cls, FUTURE, FUTURE.plusHours(2)));

        String code = sendVerificationAndGetCode("01011110001");
        String createResp = createBooking("01011110001", code, slot.getId(), 5_000L);

        Long bookingId = ((Number) JsonPath.read(createResp, "$.bookingId")).longValue();
        String token = JsonPath.read(createResp, "$.accessToken");

        // 취소 — D-1 이전 슬롯이므로 환불 가능
        mockMvc.perform(delete("/bookings/{id}", bookingId)
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(true))
                .andExpect(jsonPath("$.refundAmount").value(5000));

        // Proof: booking 상태 CANCELED
        assertThat(bookingRepository.findById(bookingId))
                .hasValueSatisfying(b -> assertThat(b.getStatus().name()).isEqualTo("CANCELED"));

        // Proof: booking_history 2건 (BOOKED + CANCELED)
        assertThat(bookingHistoryRepository.countByBookingId(bookingId)).isEqualTo(2L);

        // Proof: refund 1건 (FakePaymentProvider 성공 → SUCCEEDED)
        assertThat(refundRepository.count()).isEqualTo(1L);
        assertThat(refundRepository.findAll().get(0).getStatus().name()).isEqualTo("SUCCEEDED");

        // Proof: 슬롯 booked_count = 0 (반납 완료)
        assertThat(slotRepository.findById(slot.getId()))
                .hasValueSatisfying(s -> assertThat(s.getBookedCount()).isEqualTo(0));
    }

    // -----------------------------------------------------------------------
    // Proof: PG 환불 실패 → refund FAILED 로 저장됨 (사라지지 않음)
    // -----------------------------------------------------------------------

    @DisplayName("PG 환불 실패 시 환불 이력이 FAILED로 저장된다")
    @Test
    void cancel_refundFailure_refundSavedAsFailed() throws Exception {
        when(paymentProvider.refund(any(), anyLong()))
                .thenReturn(RefundResult.failure("PG 타임아웃"));

        Slot slot = slotRepository.save(new Slot(cls, FUTURE, FUTURE.plusHours(2)));

        String code = sendVerificationAndGetCode("01055550005");
        String createResp = createBooking("01055550005", code, slot.getId(), 5_000L);
        Long bookingId = ((Number) JsonPath.read(createResp, "$.bookingId")).longValue();
        String token = JsonPath.read(createResp, "$.accessToken");

        // 취소 — 환불 가능 구간이지만 PG 실패
        mockMvc.perform(delete("/bookings/{id}", bookingId)
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(true));

        // Proof: 환불 실패여도 refunds 테이블에 FAILED 로 남아 있음
        assertThat(refundRepository.count()).isEqualTo(1L);
        var refund = refundRepository.findAll().get(0);
        assertThat(refund.getStatus().name()).isEqualTo("FAILED");
        assertThat(refund.getFailReason()).isEqualTo("PG 타임아웃");
    }

    @DisplayName("환불 실패 목록 관리자 API는 DTO 응답을 반환한다")
    @Test
    void list_failed_refunds_adminApi_returnsDtoResponse() throws Exception {
        Guest guest = guestRepository.save(new Guest("환불실패", "01077770007"));
        Slot slot = slotRepository.save(new Slot(cls, FUTURE, FUTURE.plusHours(2)));
        Booking booking = bookingRepository.save(new Booking(
                guest, slot, 5_000L, 45_000L, DepositPaymentMethod.CARD, "refund-token"));

        Refund refund = new Refund(booking, 5_000L);
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
        LocalDateTime today14 = LocalDateTime.now(ZoneId.of("Asia/Seoul")).withHour(14).withMinute(0).withSecond(0).withNano(0);
        Slot slot = slotRepository.save(new Slot(cls, today14, today14.plusHours(2)));

        String code = sendVerificationAndGetCode("01022220002");
        String createResp = createBooking("01022220002", code, slot.getId(), 5_000L);

        Long bookingId = ((Number) JsonPath.read(createResp, "$.bookingId")).longValue();
        String token = JsonPath.read(createResp, "$.accessToken");

        mockMvc.perform(delete("/bookings/{id}", bookingId)
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(false))
                .andExpect(jsonPath("$.refundAmount").value(0));

        // Proof: refund 미생성
        assertThat(refundRepository.count()).isEqualTo(0L);

        // Proof: booking_history 2건 (BOOKED + CANCELED)
        assertThat(bookingHistoryRepository.countByBookingId(bookingId)).isEqualTo(2L);
    }

    // -----------------------------------------------------------------------
    // 404 — 잘못된 access_token
    // -----------------------------------------------------------------------

    @DisplayName("잘못된 토큰으로 예약 취소를 요청하면 404를 반환한다")
    @Test
    void cancel_wrongToken_returns404() throws Exception {
        Slot slot = slotRepository.save(new Slot(cls, FUTURE, FUTURE.plusHours(2)));

        String code = sendVerificationAndGetCode("01033330003");
        String createResp = createBooking("01033330003", code, slot.getId(), 5_000L);
        Long bookingId = ((Number) JsonPath.read(createResp, "$.bookingId")).longValue();

        mockMvc.perform(delete("/bookings/{id}", bookingId)
                        .param("token", "invalid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // -----------------------------------------------------------------------
    // 400 — 이미 취소된 예약 재취소 시도
    // -----------------------------------------------------------------------

    @DisplayName("이미 취소된 예약을 다시 취소하면 400을 반환한다")
    @Test
    void cancel_alreadyCanceled_returns400() throws Exception {
        Slot slot = slotRepository.save(new Slot(cls, FUTURE, FUTURE.plusHours(2)));

        String code = sendVerificationAndGetCode("01044440004");
        String createResp = createBooking("01044440004", code, slot.getId(), 5_000L);
        Long bookingId = ((Number) JsonPath.read(createResp, "$.bookingId")).longValue();
        String token = JsonPath.read(createResp, "$.accessToken");

        // 첫 번째 취소 — 성공
        mockMvc.perform(delete("/bookings/{id}", bookingId)
                        .param("token", token))
                .andExpect(status().isOk());

        // 두 번째 취소 — 400
        mockMvc.perform(delete("/bookings/{id}", bookingId)
                        .param("token", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
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

    private String createBooking(String phone, String code, Long slotId, long deposit) throws Exception {
        return mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "verificationCode": "%s",
                                  "name": "홍길동",
                                  "slotId": %d,
                                  "depositAmount": %d,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(phone, code, slotId, deposit)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }
}
