package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.notification.NotificationService;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.support.CustomerTestHelper;
import com.personal.happygallery.support.PaymentTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static com.personal.happygallery.support.TestFixtures.inventory;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class MeOrderUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter;
    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired UserReaderPort userReaderPort;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired ObjectMapper objectMapper;
    @Autowired PhoneVerificationReaderPort phoneVerificationReader;
    @MockitoBean NotificationService notificationService;

    MockMvc mockMvc;
    Long productId;
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

        Product product = productStorePort.save(readyStockProduct("테스트 상품", 29_000L));
        inventoryStorePort.save(inventory(product, 10));
        productId = product.getId();

        sessionCookie = customerHelper.signupAndGetSessionCookie("order@test.com", "010-3333-4444");
        userId = userReaderPort.findByEmail("order@test.com").orElseThrow().getId();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearUsers();
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

        mockMvc.perform(get("/api/v1/me/orders/page")
                        .cookie(sessionCookie)
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").isNumber())
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @DisplayName("회원 주문 상세를 조회한다")
    @Test
    void getMyOrderDetail() throws Exception {
        Long orderId = createShippingOrder(new ShippingAddress(
                "주문회원",
                "010-3333-4444",
                "27352",
                "충북 충주시 계명대로 161",
                "1층"));

        mockMvc.perform(get("/api/v1/me/orders/{id}", orderId)
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.orderNumber").value("ORD-%08d".formatted(orderId)))
                .andExpect(jsonPath("$.status").value("PAID_APPROVAL_PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(29000))
                .andExpect(jsonPath("$.fulfillment.shippingAddress.recipientName").value("주문회원"))
                .andExpect(jsonPath("$.fulfillment.shippingAddress.phone").value("01033334444"))
                .andExpect(jsonPath("$.fulfillment.shippingAddress.addressLine1")
                        .value("충북 충주시 계명대로 161"));
    }

    @DisplayName("인증 없이 회원 주문 목록을 조회하면 401을 반환한다")
    @Test
    void listMyOrders_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me/orders"))
                .andExpect(status().isUnauthorized());
    }

    private Long createOrder() throws Exception {
        var prepared = paymentHelper.preparePayment(
                PaymentContext.ORDER,
                new OrderPayload(
                        userId,
                        null,
                        null,
                        null,
                        List.of(new OrderItemRef(productId, 1))),
                sessionCookie);
        return paymentHelper.confirmPayment(prepared, "test-payment-key", sessionCookie)
                .domainId();
    }

    private Long createShippingOrder(ShippingAddress shippingAddress) throws Exception {
        var prepared = paymentHelper.preparePayment(
                PaymentContext.ORDER,
                new OrderPayload(
                        userId,
                        null,
                        null,
                        null,
                        List.of(new OrderItemRef(productId, 1)),
                        false,
                        FulfillmentType.SHIPPING,
                        shippingAddress),
                sessionCookie);
        return paymentHelper.confirmPayment(prepared, "test-payment-key", sessionCookie)
                .domainId();
    }

}
