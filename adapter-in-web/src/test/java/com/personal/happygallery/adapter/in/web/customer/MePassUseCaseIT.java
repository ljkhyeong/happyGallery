package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.booking.ClassRepository;
import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import com.personal.happygallery.adapter.out.persistence.booking.SlotRepository;
import com.personal.happygallery.adapter.out.persistence.pass.PassPurchaseRepository;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.notification.NotificationService;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PassPayload;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.support.CustomerTestHelper;
import com.personal.happygallery.support.PaymentTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static com.personal.happygallery.support.BookingTestHelper.FUTURE;
import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.passPurchase;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class MePassUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter;
    @Autowired UserReaderPort userReaderPort;
    @Autowired PhoneVerificationReaderPort phoneVerificationReader;
    @Autowired PassPurchaseRepository passPurchaseRepository;
    @Autowired ClassRepository classRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;
    @MockitoBean NotificationService notificationService;

    MockMvc mockMvc;
    Cookie sessionCookie;
    Long userId;
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
        sessionCookie = customerHelper.signupAndGetSessionCookie("pass@test.com", "010-5555-6666");
        userId = userReaderPort.findByEmail("pass@test.com").orElseThrow().getId();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
    }

    @Test
    @DisplayName("8회권은 만료 여부와 잔여 횟수를 서버에서 구분하고 잔여 횟수순 페이지를 조회한다")
    void searchMyPassHistory() throws Exception {
        Long lowCreditsId = purchasePass();
        Long highCreditsId = purchasePass();
        Long expiredId = purchasePass();
        jdbcTemplate.update("UPDATE pass_purchases SET remaining_credits = 2 WHERE id = ?", lowCreditsId);
        jdbcTemplate.update("UPDATE pass_purchases SET expires_at = '2020-01-01 00:00:00' WHERE id = ?", expiredId);
        var first = mockMvc.perform(get("/api/v1/me/passes/page").cookie(sessionCookie)
                        .param("status", "ACTIVE").param("sort", "CREDITS_DESC").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].passId").value(highCreditsId))
                .andExpect(jsonPath("$.hasMore").value(true)).andReturn();
        String cursor = objectMapper.readTree(first.getResponse().getContentAsString()).get("nextCursor").asText();
        mockMvc.perform(get("/api/v1/me/passes/page").cookie(sessionCookie)
                        .param("status", "ACTIVE").param("sort", "CREDITS_DESC")
                        .param("size", "1").param("cursor", cursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].passId").value(lowCreditsId))
                .andExpect(jsonPath("$.hasMore").value(false));
        mockMvc.perform(get("/api/v1/me/passes/page").cookie(sessionCookie)
                        .param("status", "EXPIRED").param("sort", "EXPIRY_ASC").param("keyword", expiredId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].passId").value(expiredId));
        mockMvc.perform(get("/api/v1/me/passes/page").cookie(sessionCookie).param("status", "USED_UP"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
    }

    @DisplayName("회원 8회권 목록과 페이지는 결제 영수증을 함께 조회한다")
    @Test
    void listMyPasses() throws Exception {
        Long passId = purchasePass();
        String receiptUrl = "https://dashboard.tosspayments.com/receipt/member-pass";
        jdbcTemplate.update("""
                UPDATE payment_attempt SET confirmed_receipt_url = ?
                WHERE context = 'PASS' AND fulfilled_domain_id = ?
                """, receiptUrl, passId);

        mockMvc.perform(get("/api/v1/me/passes")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passId").isNumber())
                .andExpect(jsonPath("$[0].receiptUrl").value(receiptUrl))
                .andExpect(jsonPath("$[0].planCode").value("REGULAR_CRAFT_8"))
                .andExpect(jsonPath("$[0].planName").value("정규 공예 8회권"))
                .andExpect(jsonPath("$[0].totalCredits").value(8));

        mockMvc.perform(get("/api/v1/me/passes/page")
                        .cookie(sessionCookie)
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].passId").isNumber())
                .andExpect(jsonPath("$.content[0].receiptUrl").value(receiptUrl))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @DisplayName("회원 8회권 상세를 조회한다")
    @Test
    void getMyPassDetail() throws Exception {
        Long passId = purchasePass();

        mockMvc.perform(get("/api/v1/me/passes/{id}", passId)
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passId").value(passId))
                .andExpect(jsonPath("$.totalCredits").value(8))
                .andExpect(jsonPath("$.remainingCredits").value(8))
                .andExpect(jsonPath("$.receiptUrl").value(nullValue()))
                .andExpect(jsonPath("$.totalPrice").value(240000));
    }

    @DisplayName("인증 없이 회원 8회권 목록을 조회하면 401을 반환한다")
    @Test
    void listMyPasses_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me/passes"))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("회원이 본인 8회권을 환불하면 미래 예약과 잔여 횟수를 함께 정산한다")
    @Test
    void refundMyPass_cancelsFutureBookingAndStartsRefund() throws Exception {
        Long passId = savePass(userId);
        var savedClass = classRepository.save(bookingClass(
                "우드 정규 클래스", "WOOD", 120, 50_000L, 30));
        Slot savedSlot = slotRepository.save(slot(savedClass, FUTURE, FUTURE.plusHours(2)));
        Long bookingId = paymentHelper.createMemberPassBooking(
                sessionCookie, userId, savedSlot.getId(), passId).domainId();

        mockMvc.perform(post("/api/v1/me/passes/{id}/refund", passId)
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceledBookings").value(1))
                .andExpect(jsonPath("$.refundCredits").value(8))
                .andExpect(jsonPath("$.refundAmount").value(240000))
                .andExpect(jsonPath("$.refundStatus").value("REQUESTED"));

        assertThat(passPurchaseRepository.findById(passId).orElseThrow().getRemainingCredits()).isZero();
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELED);
        assertThat(slotRepository.findById(savedSlot.getId()).orElseThrow().getBookedCount()).isZero();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(refundRepository.findByPassPurchaseId(passId))
                        .hasValueSatisfying(refund ->
                                assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCEEDED)));
    }

    @DisplayName("회원이 타인의 8회권 환불을 요청하면 존재 여부를 숨긴다")
    @Test
    void refundOtherUsersPass_returns404WithoutMutation() throws Exception {
        Long passId = savePass(userId);
        Cookie otherSession = customerHelper.signupAndGetSessionCookie(
                "other-pass@test.com", "010-5555-7777");

        mockMvc.perform(post("/api/v1/me/passes/{id}/refund", passId)
                        .with(csrf())
                        .cookie(otherSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        assertThat(passPurchaseRepository.findById(passId).orElseThrow().getRemainingCredits())
                .isEqualTo(8);
        assertThat(refundRepository.findByPassPurchaseId(passId)).isEmpty();
    }

    @DisplayName("인증 없이 8회권 환불을 요청하면 401을 반환한다")
    @Test
    void refundMyPass_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/me/passes/{id}/refund", 1L).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    private Long purchasePass() throws Exception {
        PaymentTestHelper.PreparedPayment prepared = paymentHelper.preparePayment(
                PaymentContext.PASS,
                new PassPayload(userId),
                sessionCookie);
        return paymentHelper.confirmPayment(prepared, "test-payment-key", sessionCookie).domainId();
    }

    private Long savePass(Long ownerId) {
        PassPurchase pass = passPurchase(ownerId, FUTURE.plusDays(90), 240_000L);
        pass.recordPaymentKey("test-member-refund-payment-key");
        return passPurchaseRepository.save(pass).getId();
    }

}
