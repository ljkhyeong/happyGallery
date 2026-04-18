package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.notification.NotificationService;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.adapter.in.web.CustomerAuthFilter;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
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

import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static com.personal.happygallery.support.TestFixtures.inventory;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class MeOrderUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired CustomerAuthFilter customerAuthFilter;
    @Autowired @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter;
    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired TestCleanupSupport cleanupSupport;
    @MockitoBean NotificationService notificationService;

    MockMvc mockMvc;
    Long productId;
    Cookie sessionCookie;

    @BeforeEach
    void setUp() throws Exception {
        cleanup();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSessionRepositoryFilter, customerAuthFilter)
                .build();

        Product product = productStorePort.save(readyStockProduct("테스트 상품", 29_000L));
        inventoryStorePort.save(inventory(product, 10));
        productId = product.getId();

        sessionCookie = signupAndGetSessionCookie("order@test.com", "010-3333-4444");
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("회원 주문 생성이 성공한다")
    @Test
    void createMemberOrder_success() throws Exception {
        mockMvc.perform(post("/api/v1/me/orders")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    { "productId": %d, "qty": 1 }
                                  ]
                                }
                                """.formatted(productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNumber())
                .andExpect(jsonPath("$.status").value("PAID_APPROVAL_PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(29000));
    }

    @DisplayName("회원 주문 목록을 조회한다")
    @Test
    void listMyOrders() throws Exception {
        createOrder();

        mockMvc.perform(get("/api/v1/me/orders")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").isNumber())
                .andExpect(jsonPath("$[0].status").value("PAID_APPROVAL_PENDING"));
    }

    @DisplayName("회원 주문 상세를 조회한다")
    @Test
    void getMyOrderDetail() throws Exception {
        Long orderId = createOrder();

        mockMvc.perform(get("/api/v1/me/orders/{id}", orderId)
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("PAID_APPROVAL_PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(29000));
    }

    @DisplayName("인증 없이 회원 주문 목록을 조회하면 401을 반환한다")
    @Test
    void listMyOrders_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me/orders"))
                .andExpect(status().isUnauthorized());
    }

    private Long createOrder() throws Exception {
        String response = mockMvc.perform(post("/api/v1/me/orders")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    { "productId": %d, "qty": 1 }
                                  ]
                                }
                                """.formatted(productId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.orderId")).longValue();
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
