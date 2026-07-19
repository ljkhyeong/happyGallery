package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.MemberRescheduleRequest;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.notification.NotificationService;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.support.BookingTestHelper;
import com.personal.happygallery.support.CustomerTestHelper;
import com.personal.happygallery.support.PaymentTestHelper;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class MeBookingUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired UserReaderPort userReaderPort;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired ObjectMapper objectMapper;
    @Autowired PhoneVerificationReaderPort phoneVerificationReader;
    @MockitoBean NotificationService notificationService;

    MockMvc mockMvc;
    Long slotId;
    Long slot2Id;
    Cookie sessionCookie;
    Long userId;
    PaymentTestHelper paymentHelper;
    CustomerTestHelper customerHelper;

    @BeforeEach
    void setUp() throws Exception {
        cleanup();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSessionRepositoryFilter)
                .apply(springSecurity())
                .build();
        paymentHelper = new PaymentTestHelper(mockMvc, objectMapper);
        customerHelper = new CustomerTestHelper(mockMvc, objectMapper, phoneVerificationReader);

        BookingClass cls = classStorePort.save(defaultBookingClass());
        Slot s1 = slotStorePort.save(slot(cls, BookingTestHelper.FUTURE, BookingTestHelper.FUTURE.plusHours(2)));
        Slot s2 = slotStorePort.save(slot(cls, BookingTestHelper.FUTURE.plusDays(1), BookingTestHelper.FUTURE.plusDays(1).plusHours(2)));
        slotId = s1.getId();
        slot2Id = s2.getId();

        sessionCookie = customerHelper.signupAndGetSessionCookie("member@test.com", "010-1111-2222");
        userId = userReaderPort.findByEmail("member@test.com").orElseThrow().getId();
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
        Long bookingId = paymentHelper.createMemberDepositBooking(sessionCookie, userId, slotId)
                .domainId();

        mockMvc.perform(get("/api/v1/me/bookings/{id}", bookingId)
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
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
                .andExpect(jsonPath("$.passBooking").value(false))
                .andExpect(jsonPath("$.cancelPolicy.refundable").value(true))
                .andExpect(jsonPath("$.cancelPolicy.deadlineAt").value("2030-01-01T00:00:00"))
                .andExpect(jsonPath("$.cancelPolicy.passCreditRestorable").value(false));
    }

    @DisplayName("회원 예약 슬롯을 변경한다")
    @Test
    void rescheduleMemberBooking() throws Exception {
        Long bookingId = createBooking(slotId);

        mockMvc.perform(patch("/api/v1/me/bookings/{id}/reschedule", bookingId)
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MemberRescheduleRequest(slot2Id))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @DisplayName("회원 예약을 취소한다 — 환불 가능")
    @Test
    void cancelMemberBooking_refundable() throws Exception {
        Long bookingId = createBooking(slotId);

        mockMvc.perform(delete("/api/v1/me/bookings/{id}", bookingId)
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.refundable").value(true))
                .andExpect(jsonPath("$.refund.amount").value(5000))
                .andExpect(jsonPath("$.refund.status").value("REQUESTED"));
    }

    @DisplayName("인증 없이 회원 예약 목록을 조회하면 401을 반환한다")
    @Test
    void listMyBookings_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me/bookings"))
                .andExpect(status().isUnauthorized());
    }

    private Long createBooking(Long targetSlotId) throws Exception {
        return paymentHelper.createMemberDepositBooking(sessionCookie, userId, targetSlotId)
                .domainId();
    }

}
