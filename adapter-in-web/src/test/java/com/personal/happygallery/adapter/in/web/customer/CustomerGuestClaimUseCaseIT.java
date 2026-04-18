package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.notification.NotificationService;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.order.OrderService;
import com.personal.happygallery.adapter.in.web.CustomerAuthFilter;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.BookingTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.TestFixtures;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class CustomerGuestClaimUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired CustomerAuthFilter customerAuthFilter;
    @Autowired @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter;
    @Autowired GuestStorePort guestStorePort;
    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired BookingStorePort bookingStorePort;
    @Autowired UserReaderPort userReaderPort;
    @Autowired BookingReaderPort bookingReaderPort;
    @Autowired OrderReaderPort orderReaderPort;
    @Autowired PhoneVerificationReaderPort phoneVerificationReaderPort;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired OrderService orderService;
    @MockitoBean NotificationService notificationService;

    MockMvc mockMvc;
    BookingTestHelper bookingHelper;

    @BeforeEach
    void setUp() {
        cleanup();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSessionRepositoryFilter, customerAuthFilter)
                .build();
        bookingHelper = new BookingTestHelper(mockMvc, phoneVerificationReaderPort);
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearOrderData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("회원은 휴대폰 재인증 후 같은 번호의 비회원 주문과 예약을 가져올 수 있다")
    @Test
    void verifyAndClaimGuestRecords() throws Exception {
        String email = "member@example.com";
        Guest guest = guestStorePort.save(new Guest("비회원", "01012345678"));

        Product product = productStorePort.save(new Product("테스트 상품", ProductType.READY_STOCK, 29_000L));
        inventoryStorePort.save(new Inventory(product, 5));
        Order order = orderService.createPaidOrder(
                guest.getId(),
                List.of(new OrderService.OrderItemRequest(product.getId(), 1, 29_000L))).order();

        BookingClass bookingClass = classStorePort.save(TestFixtures.defaultBookingClass());
        Slot slot = slotStorePort.save(TestFixtures.slot(
                bookingClass,
                BookingTestHelper.FUTURE,
                BookingTestHelper.FUTURE.plusHours(2)));
        Booking booking = bookingStorePort.save(Booking.forGuestDeposit(
                guest, slot, 10_000L, 40_000L,
                DepositPaymentMethod.CARD,
                "guest-claim-access-token"));

        Cookie sessionCookie = signupAndGetSessionCookie(email, "010-1234-5678");
        User user = userReaderPort.findByEmail(email).orElseThrow();

        String verificationCode = bookingHelper.sendVerificationAndGetCode("01012345678");

        mockMvc.perform(post("/api/v1/me/guest-claims/verify")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verificationCode": "%s"
                                }
                                """.formatted(verificationCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneVerified").value(true))
                .andExpect(jsonPath("$.orders[0].orderId").value(order.getId()))
                .andExpect(jsonPath("$.bookings[0].bookingId").value(booking.getId()));

        assertThat(userReaderPort.findById(user.getId()).orElseThrow().isPhoneVerified()).isTrue();

        mockMvc.perform(post("/api/v1/me/guest-claims")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderIds": [%d],
                                  "bookingIds": [%d]
                                }
                                """.formatted(order.getId(), booking.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimedOrderCount").value(1))
                .andExpect(jsonPath("$.claimedBookingCount").value(1));

        Order claimedOrder = orderReaderPort.findById(order.getId()).orElseThrow();
        Booking claimedBooking = bookingReaderPort.findById(booking.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(claimedOrder.getUserId()).isEqualTo(user.getId());
            softly.assertThat(claimedOrder.getGuestId()).isNull();
            softly.assertThat(claimedBooking.getUserId()).isEqualTo(user.getId());
            softly.assertThat(claimedBooking.getGuest()).isNull();
        });

        mockMvc.perform(get("/api/v1/me/orders")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(order.getId()));

        mockMvc.perform(get("/api/v1/me/bookings")
                        .cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(booking.getId()));
    }

    @DisplayName("휴대폰 재인증 전에는 비회원 이력 미리보기를 조회할 수 없다")
    @Test
    void preview_requiresPhoneVerification() throws Exception {
        Cookie sessionCookie = signupAndGetSessionCookie("preview@example.com", "01012345678");

        mockMvc.perform(get("/api/v1/me/guest-claims/preview")
                        .cookie(sessionCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("휴대폰 인증을 완료한 뒤 다시 시도해주세요."));
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
