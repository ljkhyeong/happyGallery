package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.notification.NotificationService;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.adapter.in.web.CustomerAuthFilter;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.support.BookingTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class MeBookingUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired CustomerAuthFilter customerAuthFilter;
    @Autowired @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired TestCleanupSupport cleanupSupport;
    @MockitoBean NotificationService notificationService;

    MockMvc mockMvc;
    Long slotId;
    Long slot2Id;
    Cookie sessionCookie;

    @BeforeEach
    void setUp() throws Exception {
        cleanup();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSessionRepositoryFilter, customerAuthFilter)
                .build();

        BookingClass cls = classStorePort.save(defaultBookingClass());
        Slot s1 = slotStorePort.save(slot(cls, BookingTestHelper.FUTURE, BookingTestHelper.FUTURE.plusHours(2)));
        Slot s2 = slotStorePort.save(slot(cls, BookingTestHelper.FUTURE.plusDays(1), BookingTestHelper.FUTURE.plusDays(1).plusHours(2)));
        slotId = s1.getId();
        slot2Id = s2.getId();

        sessionCookie = signupAndGetSessionCookie("member@test.com", "010-1111-2222");
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("회원 예약금 예약 생성이 성공한다")
    @Test
    void createMemberBooking_success() throws Exception {
        mockMvc.perform(post("/api/v1/me/bookings")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(slotId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").isNumber())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.className").value("향수 클래스"))
                .andExpect(jsonPath("$.depositAmount").value(5000));
    }

    @DisplayName("회원 예약 목록을 조회한다")
    @Test
    void listMyBookings() throws Exception {
        createBooking(slotId);

        mockMvc.perform(get("/api/v1/me/bookings")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").isNumber())
                .andExpect(jsonPath("$[0].status").value("BOOKED"));
    }

    @DisplayName("회원 예약 상세를 조회한다")
    @Test
    void getMyBookingDetail() throws Exception {
        Long bookingId = createBooking(slotId);

        mockMvc.perform(get("/api/v1/me/bookings/{id}", bookingId)
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.className").value("향수 클래스"))
                .andExpect(jsonPath("$.balanceStatus").value("UNPAID"))
                .andExpect(jsonPath("$.passBooking").value(false));
    }

    @DisplayName("회원 예약 슬롯을 변경한다")
    @Test
    void rescheduleMemberBooking() throws Exception {
        Long bookingId = createBooking(slotId);

        mockMvc.perform(patch("/api/v1/me/bookings/{id}/reschedule", bookingId)
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "newSlotId": %d }
                                """.formatted(slot2Id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @DisplayName("회원 예약을 취소한다 — 환불 가능")
    @Test
    void cancelMemberBooking_refundable() throws Exception {
        Long bookingId = createBooking(slotId);

        mockMvc.perform(delete("/api/v1/me/bookings/{id}", bookingId)
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(true));
    }

    @DisplayName("인증 없이 회원 예약 목록을 조회하면 401을 반환한다")
    @Test
    void listMyBookings_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me/bookings"))
                .andExpect(status().isUnauthorized());
    }

    private Long createBooking(Long targetSlotId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/me/bookings")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slotId": %d,
                                  "depositAmount": 5000,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(targetSlotId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.bookingId")).longValue();
    }

    private Cookie signupAndGetSessionCookie(String email, String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123",
                                  "name": "회원",
                                  "phone": "%s"
                                }
                                """.formatted(email, phone)))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getCookie("HG_SESSION");
    }
}
