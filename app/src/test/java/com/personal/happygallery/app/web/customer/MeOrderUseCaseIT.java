package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.infra.order.FulfillmentRepository;
import com.personal.happygallery.infra.order.OrderApprovalHistoryRepository;
import com.personal.happygallery.infra.order.OrderItemRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import com.personal.happygallery.infra.product.InventoryRepository;
import com.personal.happygallery.infra.product.ProductRepository;
import com.personal.happygallery.infra.booking.RefundRepository;
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

import static com.personal.happygallery.support.TestDataCleaner.clearOrderData;
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
    @Autowired UserRepository userRepository;
    @Autowired UserSessionRepository userSessionRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired OrderApprovalHistoryRepository orderApprovalHistoryRepository;
    @Autowired FulfillmentRepository fulfillmentRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired ProductRepository productRepository;
    @Autowired InventoryRepository inventoryRepository;
    @MockitoBean NotificationService notificationService;

    MockMvc mockMvc;
    Long productId;
    Cookie sessionCookie;

    @BeforeEach
    void setUp() throws Exception {
        cleanup();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(customerAuthFilter)
                .build();

        Product product = productRepository.save(readyStockProduct("테스트 상품", 29_000L));
        inventoryRepository.save(inventory(product, 10));
        productId = product.getId();

        sessionCookie = signupAndGetSessionCookie("order@test.com", "010-3333-4444");
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        clearOrderData(refundRepository, fulfillmentRepository,
                orderApprovalHistoryRepository, orderItemRepository,
                orderRepository, inventoryRepository, productRepository);
        userSessionRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
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
