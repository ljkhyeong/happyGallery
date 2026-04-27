package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.customer.CustomerAuthController;
import com.personal.happygallery.adapter.in.web.customer.MeBookingController;
import com.personal.happygallery.adapter.in.web.customer.MeCartController;
import com.personal.happygallery.adapter.in.web.customer.MeGuestClaimController;
import com.personal.happygallery.adapter.in.web.customer.MeInquiryController;
import com.personal.happygallery.adapter.in.web.customer.MeNotificationController;
import com.personal.happygallery.adapter.in.web.customer.MeOrderController;
import com.personal.happygallery.adapter.in.web.customer.MePassController;
import com.personal.happygallery.adapter.in.web.customer.MeProductQnaController;
import com.personal.happygallery.adapter.in.web.customer.SocialLoginController;
import com.personal.happygallery.application.booking.port.in.BookingCancelUseCase;
import com.personal.happygallery.application.booking.port.in.BookingQueryUseCase;
import com.personal.happygallery.application.booking.port.in.BookingRescheduleUseCase;
import com.personal.happygallery.application.cart.port.in.CartCheckoutUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.application.customer.port.in.GuestClaimUseCase;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.inquiry.port.in.InquiryUseCase;
import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase;
import com.personal.happygallery.application.order.port.in.OrderQueryUseCase;
import com.personal.happygallery.application.pass.port.in.PassQueryUseCase;
import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.inquiry.Inquiry;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.qna.ProductQna;
import com.personal.happygallery.domain.user.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerApiRestDocsTest extends RestDocsTestSupport {

    private MockMvc mockMvc;

    private CustomerAuthUseCase customerAuthUseCase;
    private SocialAuthUseCase socialAuthUseCase;
    private CartUseCase cartUseCase;
    private CartCheckoutUseCase cartCheckoutUseCase;
    private BookingQueryUseCase bookingQueryUseCase;
    private BookingRescheduleUseCase bookingRescheduleUseCase;
    private BookingCancelUseCase bookingCancelUseCase;
    private OrderQueryUseCase orderQueryUseCase;
    private PassQueryUseCase passQueryUseCase;
    private NotificationQueryUseCase notificationQueryUseCase;
    private GuestClaimUseCase guestClaimUseCase;
    private InquiryUseCase inquiryUseCase;
    private ProductQnaUseCase qnaUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        customerAuthUseCase = mock(CustomerAuthUseCase.class);
        socialAuthUseCase = mock(SocialAuthUseCase.class);
        cartUseCase = mock(CartUseCase.class);
        cartCheckoutUseCase = mock(CartCheckoutUseCase.class);
        bookingQueryUseCase = mock(BookingQueryUseCase.class);
        bookingRescheduleUseCase = mock(BookingRescheduleUseCase.class);
        bookingCancelUseCase = mock(BookingCancelUseCase.class);
        orderQueryUseCase = mock(OrderQueryUseCase.class);
        passQueryUseCase = mock(PassQueryUseCase.class);
        notificationQueryUseCase = mock(NotificationQueryUseCase.class);
        guestClaimUseCase = mock(GuestClaimUseCase.class);
        inquiryUseCase = mock(InquiryUseCase.class);
        qnaUseCase = mock(ProductQnaUseCase.class);

        User user = RestDocsFixtures.user();
        Order order = RestDocsFixtures.order();
        Booking booking = RestDocsFixtures.booking();
        OrderQueryUseCase.OrderDetail orderDetail = RestDocsFixtures.orderDetail();
        PassPurchase pass = RestDocsFixtures.passPurchase();
        Inquiry inquiry = RestDocsFixtures.inquiry();
        ProductQna qna = RestDocsFixtures.productQna();

        when(customerAuthUseCase.signup(any())).thenReturn(user);
        when(customerAuthUseCase.login(any())).thenReturn(user);
        when(socialAuthUseCase.buildAuthorizationUrl(any()))
                .thenReturn(new SocialAuthUseCase.AuthorizationUrlResult(
                        "https://accounts.google.com/o/oauth2/v2/auth?state=state-123",
                        "state-123"));
        when(socialAuthUseCase.socialLogin(any()))
                .thenReturn(new SocialAuthUseCase.SocialLoginResult(user, false));
        when(cartUseCase.getCart(CUSTOMER_USER_ID))
                .thenReturn(new CartUseCase.CartView(
                        List.of(new CartUseCase.CartItemView(1L, "시그니처 캔들", 39000L, 1, true)),
                        39000L));
        when(cartCheckoutUseCase.checkout(CUSTOMER_USER_ID)).thenReturn(order);
        when(bookingQueryUseCase.listMyBookings(CUSTOMER_USER_ID)).thenReturn(List.of(booking));
        when(bookingQueryUseCase.findMyBooking(100L, CUSTOMER_USER_ID)).thenReturn(booking);
        when(bookingRescheduleUseCase.rescheduleMemberBooking(100L, CUSTOMER_USER_ID, 42L))
                .thenReturn(booking);
        when(bookingCancelUseCase.cancelMemberBooking(100L, CUSTOMER_USER_ID))
                .thenReturn(new BookingCancelUseCase.CancelResult(booking, true));
        when(orderQueryUseCase.listMyOrders(CUSTOMER_USER_ID)).thenReturn(List.of(order));
        when(orderQueryUseCase.findMyOrder(200L, CUSTOMER_USER_ID)).thenReturn(orderDetail);
        when(passQueryUseCase.listMyPasses(CUSTOMER_USER_ID)).thenReturn(List.of(pass));
        when(passQueryUseCase.findMyPass(300L, CUSTOMER_USER_ID)).thenReturn(pass);
        when(notificationQueryUseCase.listNotifications(eq(CUSTOMER_USER_ID), any(), eq(0), eq(20)))
                .thenReturn(List.of());
        when(notificationQueryUseCase.countUnread(CUSTOMER_USER_ID, null)).thenReturn(3L);
        when(guestClaimUseCase.preview(CUSTOMER_USER_ID)).thenReturn(claimPreview(false));
        when(guestClaimUseCase.verifyPhoneAndPreview(CUSTOMER_USER_ID, "123456")).thenReturn(claimPreview(true));
        when(guestClaimUseCase.claim(eq(CUSTOMER_USER_ID), any(), any()))
                .thenReturn(new GuestClaimUseCase.ClaimResult(1, 1));
        when(inquiryUseCase.create(eq(CUSTOMER_USER_ID), any(), any())).thenReturn(inquiry);
        when(inquiryUseCase.listByUser(CUSTOMER_USER_ID)).thenReturn(List.of(inquiry));
        when(inquiryUseCase.findByIdAndUser(9L, CUSTOMER_USER_ID)).thenReturn(inquiry);
        when(qnaUseCase.createQuestion(eq(1L), eq(CUSTOMER_USER_ID), any(), any(), eq(false), any()))
                .thenReturn(qna);

        mockMvc = mockMvc(restDocumentation,
                new CustomerAuthController(customerAuthUseCase),
                new SocialLoginController(socialAuthUseCase),
                new MeCartController(cartUseCase, cartCheckoutUseCase),
                new MeBookingController(bookingQueryUseCase, bookingRescheduleUseCase, bookingCancelUseCase),
                new MeOrderController(orderQueryUseCase),
                new MePassController(passQueryUseCase),
                new MeNotificationController(notificationQueryUseCase),
                new MeGuestClaimController(guestClaimUseCase),
                new MeInquiryController(inquiryUseCase),
                new MeProductQnaController(qnaUseCase));
    }

    @Test
    @DisplayName("회원 가입 API를 문서화한다")
    void signup() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(jsonContent())
                        .content(json("""
                                {
                                  "email": "member@example.com",
                                  "password": "password1234",
                                  "name": "회원",
                                  "phone": "01012345678"
                                }
                                """)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("회원 로그인 API를 문서화한다")
    void login() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(jsonContent())
                        .content(json("""
                                {
                                  "email": "member@example.com",
                                  "password": "password1234"
                                }
                                """)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원 로그아웃 API를 문서화한다")
    void logout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("내 정보 조회 API를 문서화한다")
    void me() throws Exception {
        mockMvc.perform(get("/api/v1/me").with(customerUser(RestDocsFixtures.user())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("구글 로그인 URL API를 문서화한다")
    void google_auth_url() throws Exception {
        mockMvc.perform(get("/api/v1/auth/social/google/url")
                        .param("redirectUri", "https://happygallery.example/auth/callback"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("구글 로그인 API를 문서화한다")
    void google_login() throws Exception {
        mockMvc.perform(post("/api/v1/auth/social/google")
                        .contentType(jsonContent())
                        .content(json("""
                                {
                                  "code": "oauth-code",
                                  "redirectUri": "https://happygallery.example/auth/callback"
                                }
                                """)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("장바구니 조회 API를 문서화한다")
    void get_cart() throws Exception {
        mockMvc.perform(get("/api/v1/me/cart").with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("장바구니 상품 추가 API를 문서화한다")
    void add_cart_item() throws Exception {
        mockMvc.perform(post("/api/v1/me/cart/items")
                        .with(customerUser())
                        .contentType(jsonContent())
                        .content(json("{\"productId\":1,\"qty\":1}")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("장바구니 수량 변경 API를 문서화한다")
    void update_cart_item() throws Exception {
        mockMvc.perform(put("/api/v1/me/cart/items/{productId}", 1L)
                        .with(customerUser())
                        .contentType(jsonContent())
                        .content(json("{\"qty\":2}")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("장바구니 상품 삭제 API를 문서화한다")
    void remove_cart_item() throws Exception {
        mockMvc.perform(delete("/api/v1/me/cart/items/{productId}", 1L)
                        .with(customerUser()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("장바구니 결제 생성 API를 문서화한다")
    void checkout_cart() throws Exception {
        mockMvc.perform(post("/api/v1/me/cart/checkout").with(customerUser()))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("내 예약 목록 API를 문서화한다")
    void list_my_bookings() throws Exception {
        mockMvc.perform(get("/api/v1/me/bookings").with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 예약 상세 API를 문서화한다")
    void get_my_booking() throws Exception {
        mockMvc.perform(get("/api/v1/me/bookings/{id}", 100L).with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 예약 변경 API를 문서화한다")
    void reschedule_my_booking() throws Exception {
        mockMvc.perform(patch("/api/v1/me/bookings/{id}/reschedule", 100L)
                        .with(customerUser())
                        .contentType(jsonContent())
                        .content(json("{\"newSlotId\":42}")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 예약 취소 API를 문서화한다")
    void cancel_my_booking() throws Exception {
        mockMvc.perform(delete("/api/v1/me/bookings/{id}", 100L).with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 주문 목록 API를 문서화한다")
    void list_my_orders() throws Exception {
        mockMvc.perform(get("/api/v1/me/orders").with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 주문 상세 API를 문서화한다")
    void get_my_order() throws Exception {
        mockMvc.perform(get("/api/v1/me/orders/{id}", 200L).with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 8회권 목록 API를 문서화한다")
    void list_my_passes() throws Exception {
        mockMvc.perform(get("/api/v1/me/passes").with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 8회권 상세 API를 문서화한다")
    void get_my_pass() throws Exception {
        mockMvc.perform(get("/api/v1/me/passes/{id}", 300L).with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 알림 목록 API를 문서화한다")
    void list_my_notifications() throws Exception {
        mockMvc.perform(get("/api/v1/me/notifications")
                        .with(customerUser())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 읽지 않은 알림 수 API를 문서화한다")
    void get_unread_notification_count() throws Exception {
        mockMvc.perform(get("/api/v1/me/notifications/unread-count").with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 알림 읽음 처리 API를 문서화한다")
    void mark_notification_as_read() throws Exception {
        mockMvc.perform(patch("/api/v1/me/notifications/{id}/read", 1L).with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 알림 전체 읽음 처리 API를 문서화한다")
    void mark_all_notifications_as_read() throws Exception {
        mockMvc.perform(patch("/api/v1/me/notifications/read-all").with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비회원 기록 인수 미리보기 API를 문서화한다")
    void preview_guest_claims() throws Exception {
        mockMvc.perform(get("/api/v1/me/guest-claims/preview").with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비회원 기록 인수 휴대폰 확인 API를 문서화한다")
    void verify_guest_claim_phone() throws Exception {
        mockMvc.perform(post("/api/v1/me/guest-claims/verify")
                        .with(customerUser())
                        .contentType(jsonContent())
                        .content(json("{\"verificationCode\":\"123456\"}")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비회원 기록 인수 실행 API를 문서화한다")
    void claim_guest_records() throws Exception {
        mockMvc.perform(post("/api/v1/me/guest-claims")
                        .with(customerUser())
                        .contentType(jsonContent())
                        .content(json("{\"orderIds\":[200],\"bookingIds\":[100]}")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 문의 생성 API를 문서화한다")
    void create_inquiry() throws Exception {
        mockMvc.perform(post("/api/v1/me/inquiries")
                        .with(customerUser())
                        .contentType(jsonContent())
                        .content(json("""
                                {
                                  "title": "예약 문의",
                                  "content": "예약 변경이 가능한가요?"
                                }
                                """)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("내 문의 목록 API를 문서화한다")
    void list_my_inquiries() throws Exception {
        mockMvc.perform(get("/api/v1/me/inquiries").with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 문의 상세 API를 문서화한다")
    void get_my_inquiry() throws Exception {
        mockMvc.perform(get("/api/v1/me/inquiries/{id}", 9L).with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 상품 QNA 생성 API를 문서화한다")
    void create_my_product_qna() throws Exception {
        mockMvc.perform(post("/api/v1/me/products/{productId}/qna", 1L)
                        .with(customerUser())
                        .contentType(jsonContent())
                        .content(json("""
                                {
                                  "title": "배송 문의",
                                  "content": "언제 받을 수 있나요?",
                                  "secret": false,
                                  "password": null
                                }
                                """)))
                .andExpect(status().isCreated());
    }

    private static GuestClaimUseCase.ClaimPreview claimPreview(boolean verified) {
        return new GuestClaimUseCase.ClaimPreview(
                verified,
                List.of(new GuestClaimUseCase.ClaimOrderSummary(
                        200L, "PAID_APPROVAL_PENDING", 39000L, LocalDateTime.of(2026, 5, 1, 20, 50))),
                List.of(new GuestClaimUseCase.ClaimBookingSummary(
                        100L, "BOOKED", "향수 원데이",
                        LocalDateTime.of(2026, 5, 7, 19, 0),
                        LocalDateTime.of(2026, 5, 7, 21, 0))));
    }
}
