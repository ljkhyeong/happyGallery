package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import com.personal.happygallery.infra.user.UserRepository;
import com.personal.happygallery.infra.user.UserSessionRepository;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class MePassUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired CustomerAuthFilter customerAuthFilter;
    @Autowired UserRepository userRepository;
    @Autowired UserSessionRepository userSessionRepository;
    @Autowired PassPurchaseRepository passPurchaseRepository;
    @Autowired PassLedgerRepository passLedgerRepository;
    @MockitoBean NotificationService notificationService;

    MockMvc mockMvc;
    Cookie sessionCookie;

    @BeforeEach
    void setUp() throws Exception {
        cleanup();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(customerAuthFilter)
                .build();
        sessionCookie = signupAndGetSessionCookie("pass@test.com", "010-5555-6666");
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        passLedgerRepository.deleteAllInBatch();
        passPurchaseRepository.deleteAllInBatch();
        userSessionRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("회원 8회권 구매가 성공한다")
    @Test
    void purchaseMemberPass_success() throws Exception {
        mockMvc.perform(post("/api/v1/me/passes")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "totalPrice": 120000 }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passId").isNumber())
                .andExpect(jsonPath("$.totalCredits").value(8))
                .andExpect(jsonPath("$.remainingCredits").value(8))
                .andExpect(jsonPath("$.totalPrice").value(120000));
    }

    @DisplayName("회원 8회권 목록을 조회한다")
    @Test
    void listMyPasses() throws Exception {
        purchasePass();

        mockMvc.perform(get("/api/v1/me/passes")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passId").isNumber())
                .andExpect(jsonPath("$[0].totalCredits").value(8));
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
                .andExpect(jsonPath("$.totalPrice").value(120000));
    }

    @DisplayName("인증 없이 회원 8회권 목록을 조회하면 401을 반환한다")
    @Test
    void listMyPasses_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me/passes"))
                .andExpect(status().isUnauthorized());
    }

    private Long purchasePass() throws Exception {
        String response = mockMvc.perform(post("/api/v1/me/passes")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "totalPrice": 120000 }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.passId")).longValue();
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
