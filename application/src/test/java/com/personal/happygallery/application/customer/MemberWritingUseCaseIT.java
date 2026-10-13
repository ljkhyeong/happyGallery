package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.support.CustomerTestHelper;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 회원이 남긴 글의 등록 응답은 DB가 정한 작성 시각을 담는다. */
@UseCaseIT
class MemberWritingUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter;
    @Autowired ObjectMapper objectMapper;
    @Autowired PhoneVerificationReaderPort phoneVerificationReader;
    @Autowired ProductStorePort products;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired TestCleanupSupport cleanup;

    MockMvc mockMvc;
    Cookie sessionCookie;

    @BeforeEach
    void setUp() throws Exception {
        // 회원 세션 쿠키를 받으려면 세션 저장소 필터를 함께 건다.
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSessionRepositoryFilter)
                .apply(springSecurity())
                .build();
        sessionCookie = new CustomerTestHelper(mockMvc, objectMapper, phoneVerificationReader)
                .signupAndGetSessionCookie("writing-member@example.com", "01077770001");
    }

    @AfterEach
    void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "product_qna", "inquiry");
        cleanup.clearProductData();
        cleanup.clearUsers();
    }

    @Test
    @DisplayName("회원이 1:1 문의를 등록하면 작성 시각을 담아 201로 응답한다")
    void createInquiry_respondsWithCreatedAt() throws Exception {
        mockMvc.perform(post("/api/v1/me/inquiries")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "예약 문의", "content": "예약 날짜를 바꿀 수 있나요?"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").isString());
    }

    @Test
    @DisplayName("회원이 상품 문의를 등록하면 작성 시각을 담아 201로 응답한다")
    void createProductQna_respondsWithCreatedAt() throws Exception {
        Long productId = products.save(readyStockProduct("문의할 작품", 10_000L)).getId();

        mockMvc.perform(post("/api/v1/me/products/{productId}/qna", productId)
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "배송 문의", "content": "언제 받을 수 있나요?", "secret": false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.createdAt").isString());
    }
}
