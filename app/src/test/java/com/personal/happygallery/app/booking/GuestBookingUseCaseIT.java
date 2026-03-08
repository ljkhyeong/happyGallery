package com.personal.happygallery.app.booking;

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
import com.personal.happygallery.support.BookingTestHelper;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.personal.happygallery.support.BookingTestHelper.extractAccessToken;
import static com.personal.happygallery.support.BookingTestHelper.extractBookingId;
import static com.personal.happygallery.support.TestDataCleaner.clearBookingWithPassData;
import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class GuestBookingUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired ClassRepository classRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired GuestRepository guestRepository;
    @Autowired PhoneVerificationRepository phoneVerificationRepository;
    @Autowired PassLedgerRepository passLedgerRepository;
    @Autowired PassPurchaseRepository passPurchaseRepository;

    Long slotId;
    static final String PHONE = "01012345678";
    BookingTestHelper helper;

    @BeforeEach
    void setUp() {
        helper = new BookingTestHelper(mockMvc);
        clearBookingWithPassData(
                passLedgerRepository,
                bookingHistoryRepository,
                bookingRepository,
                passPurchaseRepository,
                phoneVerificationRepository,
                guestRepository,
                slotRepository,
                classRepository);

        BookingClass cls = classRepository.save(defaultBookingClass());
        Slot slot = slotRepository.save(
                slot(cls, LocalDateTime.of(2026, 3, 1, 10, 0),
                        LocalDateTime.of(2026, 3, 1, 12, 0)));
        slotId = slot.getId();
    }

    // -----------------------------------------------------------------------
    // 1. 인증 코드 발송
    // -----------------------------------------------------------------------

    @DisplayName("전화번호 인증코드 발송이 성공한다")
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

    @DisplayName("유효하지 않은 전화번호로 인증코드를 요청하면 400을 반환한다")
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

    @DisplayName("게스트 예약 생성이 성공한다")
    @Test
    void createGuestBooking_success() throws Exception {
        String code = helper.sendVerificationAndGetCode(PHONE);

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
        Long bookingId = extractBookingId(response);
        assertThat(bookingRepository.findById(bookingId)).isPresent();
    }

    // Proof: 계좌이체로 예약금 결제 시도 → 422 차단
    @DisplayName("게스트 예약에서 계좌이체 결제를 요청하면 422를 반환한다")
    @Test
    void createGuestBooking_bankTransfer_returns422() throws Exception {
        String code = helper.sendVerificationAndGetCode(PHONE);

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

    @DisplayName("게스트가 중복 예약을 시도하면 409를 반환한다")
    @Test
    void createGuestBooking_duplicateBooking_returns409() throws Exception {
        // 첫 번째 예약 성공
        String code1 = helper.sendVerificationAndGetCode(PHONE);
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
        String code2 = helper.sendVerificationAndGetCode(PHONE);
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

    @DisplayName("게스트 예약 시 인증코드가 틀리면 400을 반환한다")
    @Test
    void createGuestBooking_wrongCode_returns400() throws Exception {
        helper.sendVerificationAndGetCode(PHONE); // 코드 발급 (소모 안 함)

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

    @DisplayName("게스트 예약 시 슬롯 정원 초과면 409를 반환한다")
    @Test
    void createGuestBooking_capacityExceeded_returns409() throws Exception {
        // 8명 예약으로 정원 만석 만들기
        for (int i = 0; i < 8; i++) {
            String phone = "0101234567" + i;
            String code = helper.sendVerificationAndGetCode(phone);
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
        String code = helper.sendVerificationAndGetCode(phone);
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

    @DisplayName("토큰으로 예약 조회가 성공한다")
    @Test
    void getBooking_success() throws Exception {
        String code = helper.sendVerificationAndGetCode(PHONE);
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

        Long bookingId = extractBookingId(createResponse);
        String accessToken = extractAccessToken(createResponse);

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

    @DisplayName("잘못된 토큰으로 예약 조회 시 404를 반환한다")
    @Test
    void getBooking_wrongToken_returns404() throws Exception {
        String code = helper.sendVerificationAndGetCode(PHONE);
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

        Long bookingId = extractBookingId(createResponse);

        mockMvc.perform(get("/bookings/{id}", bookingId)
                        .param("token", "invalid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // -----------------------------------------------------------------------
    // helper
    // -----------------------------------------------------------------------

}
