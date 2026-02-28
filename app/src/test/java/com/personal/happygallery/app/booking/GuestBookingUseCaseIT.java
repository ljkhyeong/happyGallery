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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class GuestBookingUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired ClassRepository classRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired GuestRepository guestRepository;
    @Autowired PhoneVerificationRepository phoneVerificationRepository;
    @Autowired PassLedgerRepository passLedgerRepository;
    @Autowired PassPurchaseRepository passPurchaseRepository;

    MockMvc mockMvc;
    Long slotId;
    static final String PHONE = "01012345678";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        passLedgerRepository.deleteAll();
        bookingHistoryRepository.deleteAll();
        bookingRepository.deleteAll();
        passPurchaseRepository.deleteAll();
        phoneVerificationRepository.deleteAll();
        guestRepository.deleteAll();
        slotRepository.deleteAll();
        classRepository.deleteAll();

        BookingClass cls = classRepository.save(
                new BookingClass("향수 클래스", "PERFUME", 120, 50_000L, 30));
        Slot slot = slotRepository.save(
                new Slot(cls, LocalDateTime.of(2026, 3, 1, 10, 0),
                        LocalDateTime.of(2026, 3, 1, 12, 0)));
        slotId = slot.getId();
    }

    // -----------------------------------------------------------------------
    // 1. 인증 코드 발송
    // -----------------------------------------------------------------------

    @Test
    void sendVerification_success() throws Exception {
        mockMvc.perform(post("/bookings/phone-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "phone": "%s" }
                                """.formatted(PHONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationId").isNumber())
                .andExpect(jsonPath("$.phone").value(PHONE))
                .andExpect(jsonPath("$.code").isString());
    }

    @Test
    void sendVerification_invalidPhone_returns400() throws Exception {
        mockMvc.perform(post("/bookings/phone-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "phone": "12345" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    // -----------------------------------------------------------------------
    // 2. 게스트 예약 생성
    // -----------------------------------------------------------------------

    @Test
    void createGuestBooking_success() throws Exception {
        String code = sendVerificationAndGetCode(PHONE);

        String response = mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "verificationCode": "%s",
                                  "name": "홍길동",
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(PHONE, code, slotId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").isNumber())
                .andExpect(jsonPath("$.bookingNumber").value(org.hamcrest.Matchers.startsWith("BK-")))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.depositAmount").value(5000))
                .andExpect(jsonPath("$.balanceAmount").value(45000))
                .andExpect(jsonPath("$.className").value("향수 클래스"))
                .andReturn().getResponse().getContentAsString();

        // DB 저장 확인
        Long bookingId = ((Number) JsonPath.read(response, "$.bookingId")).longValue();
        assertThat(bookingRepository.findById(bookingId)).isPresent();
    }

    // Proof: 계좌이체로 예약금 결제 시도 → 422 차단
    @Test
    void createGuestBooking_bankTransfer_returns422() throws Exception {
        String code = sendVerificationAndGetCode(PHONE);

        mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "verificationCode": "%s",
                                  "name": "홍길동",
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "BANK_TRANSFER"
                                }
                                """.formatted(PHONE, code, slotId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAYMENT_METHOD_NOT_ALLOWED"));

        // Proof: 예약 레코드 미생성
        assertThat(bookingRepository.count()).isEqualTo(0L);
    }

    @Test
    void createGuestBooking_duplicateBooking_returns409() throws Exception {
        // 첫 번째 예약 성공
        String code1 = sendVerificationAndGetCode(PHONE);
        mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "verificationCode": "%s",
                                  "name": "홍길동",
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(PHONE, code1, slotId)))
                .andExpect(status().isCreated());

        // 동일 전화번호 + 동일 슬롯 재예약 → 409
        String code2 = sendVerificationAndGetCode(PHONE);
        mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "verificationCode": "%s",
                                  "name": "홍길동",
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(PHONE, code2, slotId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_BOOKING"));
    }

    @Test
    void createGuestBooking_wrongCode_returns400() throws Exception {
        sendVerificationAndGetCode(PHONE); // 코드 발급 (소모 안 함)

        mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "verificationCode": "000000",
                                  "name": "홍길동",
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(PHONE, slotId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PHONE_VERIFICATION_FAILED"));
    }

    @Test
    void createGuestBooking_capacityExceeded_returns409() throws Exception {
        // 8명 예약으로 정원 만석 만들기
        for (int i = 0; i < 8; i++) {
            String phone = "0101234567" + i;
            String code = sendVerificationAndGetCode(phone);
            mockMvc.perform(post("/bookings/guest")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "phone": "%s",
                                      "verificationCode": "%s",
                                      "name": "예약자%d",
                                      "slotId": %d,
                                      "depositAmount": 5000,
                                      "paymentMethod": "CARD"
                                    }
                                    """.formatted(phone, code, i, slotId)))
                    .andExpect(status().isCreated());
        }

        // 9번째 예약 → 정원 초과
        String phone = "01099999999";
        String code = sendVerificationAndGetCode(phone);
        mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "verificationCode": "%s",
                                  "name": "초과예약자",
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(phone, code, slotId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITY_EXCEEDED"));
    }

    // -----------------------------------------------------------------------
    // 3. 예약 조회
    // -----------------------------------------------------------------------

    @Test
    void getBooking_success() throws Exception {
        String code = sendVerificationAndGetCode(PHONE);
        String createResponse = mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "verificationCode": "%s",
                                  "name": "홍길동",
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(PHONE, code, slotId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookingId = ((Number) JsonPath.read(createResponse, "$.bookingId")).longValue();
        String accessToken = JsonPath.read(createResponse, "$.accessToken");

        mockMvc.perform(get("/bookings/{id}", bookingId)
                        .param("token", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.bookingNumber").value("BK-%08d".formatted(bookingId)))
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.guestName").value("홍길동"))
                .andExpect(jsonPath("$.guestPhone").value("010****5678"))
                .andExpect(jsonPath("$.className").value("향수 클래스"));
    }

    @Test
    void getBooking_wrongToken_returns404() throws Exception {
        String code = sendVerificationAndGetCode(PHONE);
        String createResponse = mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "verificationCode": "%s",
                                  "name": "홍길동",
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(PHONE, code, slotId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookingId = ((Number) JsonPath.read(createResponse, "$.bookingId")).longValue();

        mockMvc.perform(get("/bookings/{id}", bookingId)
                        .param("token", "invalid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // -----------------------------------------------------------------------
    // helper
    // -----------------------------------------------------------------------

    private String sendVerificationAndGetCode(String phone) throws Exception {
        String response = mockMvc.perform(post("/bookings/phone-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "phone": "%s" }
                                """.formatted(phone)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.code");
    }
}
