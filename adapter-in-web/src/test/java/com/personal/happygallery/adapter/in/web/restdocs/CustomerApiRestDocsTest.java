package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.customer.CustomerSessionBinder;
import com.personal.happygallery.adapter.in.web.customer.CustomerAuthController;
import com.personal.happygallery.adapter.in.web.customer.CustomerCredentialController;
import com.personal.happygallery.adapter.in.web.customer.MeBookingController;
import com.personal.happygallery.adapter.in.web.customer.MeAccountController;
import com.personal.happygallery.adapter.in.web.customer.MeCartController;
import com.personal.happygallery.adapter.in.web.customer.MeGuestClaimController;
import com.personal.happygallery.adapter.in.web.customer.MeInquiryController;
import com.personal.happygallery.adapter.in.web.customer.MeNotificationController;
import com.personal.happygallery.adapter.in.web.customer.MeOrderController;
import com.personal.happygallery.adapter.in.web.customer.MePassController;
import com.personal.happygallery.adapter.in.web.customer.MePhoneController;
import com.personal.happygallery.adapter.in.web.customer.MeProductQnaController;
import com.personal.happygallery.adapter.in.web.customer.MeSocialAccountController;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.adapter.in.web.security.customer.SocialAccountLinkIntentStore;
import com.personal.happygallery.application.booking.port.in.BookingCancelUseCase;
import com.personal.happygallery.application.booking.port.in.BookingQueryUseCase;
import com.personal.happygallery.application.booking.port.in.BookingRescheduleUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerCredentialUseCase;
import com.personal.happygallery.application.customer.port.in.GuestClaimUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase;
import com.personal.happygallery.application.customer.port.in.MemberPhoneUpdateUseCase;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.inquiry.port.in.InquiryUseCase;
import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase;
import com.personal.happygallery.application.order.port.in.OrderQueryUseCase;
import com.personal.happygallery.application.pass.port.in.MemberPassRefundUseCase;
import com.personal.happygallery.application.pass.port.in.PassQueryUseCase;
import com.personal.happygallery.application.pass.port.in.PassRefundUseCase.PassRefundResult;
import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.inquiry.Inquiry;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.qna.ProductQna;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.domain.user.SocialProvider;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerApiRestDocsTest extends RestDocsTestSupport {

    private MockMvc mockMvc;

    private CustomerAuthUseCase customerAuthUseCase;
    private CustomerCredentialUseCase customerCredentialUseCase;
    private SocialAuthUseCase socialAuthUseCase;
    private CartUseCase cartUseCase;
    private BookingQueryUseCase bookingQueryUseCase;
    private BookingRescheduleUseCase bookingRescheduleUseCase;
    private BookingCancelUseCase bookingCancelUseCase;
    private OrderQueryUseCase orderQueryUseCase;
    private PassQueryUseCase passQueryUseCase;
    private MemberPassRefundUseCase memberPassRefundUseCase;
    private NotificationQueryUseCase notificationQueryUseCase;
    private GuestClaimUseCase guestClaimUseCase;
    private MemberPhoneUpdateUseCase phoneUpdateUseCase;
    private CustomerAccountLifecycleUseCase accountLifecycleUseCase;
    private InquiryUseCase inquiryUseCase;
    private ProductQnaUseCase qnaUseCase;
    private SubjectRateLimitGuard rateLimitGuard;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        customerAuthUseCase = mock(CustomerAuthUseCase.class);
        customerCredentialUseCase = mock(CustomerCredentialUseCase.class);
        socialAuthUseCase = mock(SocialAuthUseCase.class);
        cartUseCase = mock(CartUseCase.class);
        bookingQueryUseCase = mock(BookingQueryUseCase.class);
        bookingRescheduleUseCase = mock(BookingRescheduleUseCase.class);
        bookingCancelUseCase = mock(BookingCancelUseCase.class);
        orderQueryUseCase = mock(OrderQueryUseCase.class);
        passQueryUseCase = mock(PassQueryUseCase.class);
        memberPassRefundUseCase = mock(MemberPassRefundUseCase.class);
        notificationQueryUseCase = mock(NotificationQueryUseCase.class);
        guestClaimUseCase = mock(GuestClaimUseCase.class);
        phoneUpdateUseCase = mock(MemberPhoneUpdateUseCase.class);
        accountLifecycleUseCase = mock(CustomerAccountLifecycleUseCase.class);
        inquiryUseCase = mock(InquiryUseCase.class);
        qnaUseCase = mock(ProductQnaUseCase.class);
        rateLimitGuard = mock(SubjectRateLimitGuard.class);

        User user = RestDocsFixtures.user();
        Order order = RestDocsFixtures.order();
        Booking booking = RestDocsFixtures.booking();
        Refund bookingRefund = RestDocsFixtures.bookingRefund();
        OrderQueryUseCase.OrderDetail orderDetail = RestDocsFixtures.orderDetail();
        PassPurchase pass = RestDocsFixtures.passPurchase();
        Inquiry inquiry = RestDocsFixtures.inquiry();
        ProductQna qna = RestDocsFixtures.productQna();

        when(customerAuthUseCase.signup(any())).thenReturn(user);
        when(customerAuthUseCase.login(any())).thenReturn(user);
        when(cartUseCase.getCart(CUSTOMER_USER_ID))
                .thenReturn(new CartUseCase.CartView(
                        List.of(new CartUseCase.CartItemView(
                                1L, "시그니처 캔들", ProductType.READY_STOCK, 39000L, 1, true)),
                        39000L));
        when(bookingQueryUseCase.listMyBookings(CUSTOMER_USER_ID)).thenReturn(List.of(booking));
        when(bookingQueryUseCase.findMyBooking(100L, CUSTOMER_USER_ID))
                .thenReturn(new BookingQueryUseCase.BookingDetail(booking, null));
        when(bookingRescheduleUseCase.rescheduleMemberBooking(100L, CUSTOMER_USER_ID, 42L))
                .thenReturn(booking);
        when(bookingCancelUseCase.cancelMemberBooking(100L, CUSTOMER_USER_ID))
                .thenReturn(new BookingCancelUseCase.CancelResult(booking, true, bookingRefund, false));
        when(orderQueryUseCase.listMyOrders(CUSTOMER_USER_ID)).thenReturn(List.of(order));
        when(orderQueryUseCase.findMyOrder(200L, CUSTOMER_USER_ID)).thenReturn(orderDetail);
        PassQueryUseCase.PassView passView = new PassQueryUseCase.PassView(pass, null);
        when(passQueryUseCase.listMyPasses(CUSTOMER_USER_ID)).thenReturn(List.of(passView));
        when(passQueryUseCase.findMyPass(300L, CUSTOMER_USER_ID)).thenReturn(passView);
        when(memberPassRefundUseCase.refundMyPass(300L, CUSTOMER_USER_ID))
                .thenReturn(new PassRefundResult(1, 8, 240000L, 901L, RefundStatus.REQUESTED));
        when(notificationQueryUseCase.listNotifications(eq(CUSTOMER_USER_ID), any(), eq(0), eq(20)))
                .thenReturn(List.of(new NotificationQueryUseCase.NotificationView(
                        1L,
                        NotificationEventType.ORDER_PAID,
                        "ORDER",
                        200L,
                        LocalDateTime.of(2026, 3, 28, 9, 15),
                        null)));
        when(notificationQueryUseCase.countUnread(CUSTOMER_USER_ID, null)).thenReturn(3L);
        when(guestClaimUseCase.preview(CUSTOMER_USER_ID)).thenReturn(claimPreview(false));
        when(guestClaimUseCase.verifyPhoneAndPreview(CUSTOMER_USER_ID, "123456")).thenReturn(claimPreview(true));
        when(guestClaimUseCase.claim(eq(CUSTOMER_USER_ID), any(), any()))
                .thenReturn(new GuestClaimUseCase.ClaimResult(1, 1));
        when(phoneUpdateUseCase.update(CUSTOMER_USER_ID, "01012345678", "123456"))
                .thenReturn(user);
        when(customerCredentialUseCase.resetPassword(any()))
                .thenReturn(CUSTOMER_USER_ID);
        when(socialAuthUseCase.listLinkedProviders(CUSTOMER_USER_ID))
                .thenReturn(List.of(SocialProvider.GOOGLE));
        when(inquiryUseCase.create(eq(CUSTOMER_USER_ID), any(), any())).thenReturn(inquiry);
        when(inquiryUseCase.listByUser(CUSTOMER_USER_ID)).thenReturn(List.of(inquiry));
        when(inquiryUseCase.findByIdAndUser(9L, CUSTOMER_USER_ID)).thenReturn(inquiry);
        when(qnaUseCase.createQuestion(eq(1L), eq(CUSTOMER_USER_ID), any(), any(), eq(false), any()))
                .thenReturn(qna);

        CustomerSessionBinder customerSessionBinder = new CustomerSessionBinder(mock(CsrfTokenRepository.class));
        mockMvc = mockMvc(restDocumentation,
                new CustomerAuthController(customerAuthUseCase, customerSessionBinder, rateLimitGuard),
                new CustomerCredentialController(
                        customerCredentialUseCase, customerSessionBinder, rateLimitGuard),
                new MeCartController(cartUseCase),
                new MeBookingController(bookingQueryUseCase, bookingRescheduleUseCase,
                        bookingCancelUseCase, RestDocsFixtures.clock()),
                new MeOrderController(orderQueryUseCase),
                new MePassController(passQueryUseCase, memberPassRefundUseCase, rateLimitGuard),
                new MeNotificationController(notificationQueryUseCase),
                new MeGuestClaimController(guestClaimUseCase, rateLimitGuard),
                new MePhoneController(phoneUpdateUseCase, rateLimitGuard),
                new MeAccountController(accountLifecycleUseCase, customerSessionBinder),
                new MeSocialAccountController(
                        socialAuthUseCase,
                        new SocialAccountLinkIntentStore(RestDocsFixtures.clock()),
                        customerSessionBinder),
                new MeInquiryController(inquiryUseCase),
                new MeProductQnaController(qnaUseCase));
    }

    @Test
    @DisplayName("회원 가입 API를 문서화한다")
    void signup() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "member@example.com",
                                  "password": "password1234",
                                  "name": "회원",
                                  "phone": "01012345678",
                                  "verificationCode": "123456",
                                  "policyAcceptance": {
                                    "termsVersion": "2026-07-21-v1",
                                    "termsAccepted": true,
                                    "privacyVersion": "2026-07-21-v1",
                                    "privacyAccepted": true
                                  }
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("회원 로그인 API를 문서화한다")
    void login() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "member@example.com",
                                  "password": "password1234"
                                }
                                """))
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
        mockMvc.perform(get("/api/v1/me").with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원 휴대폰 변경 API를 문서화한다")
    void update_phone() throws Exception {
        mockMvc.perform(patch("/api/v1/me/phone")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "01012345678",
                                  "verificationCode": "123456"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원 탈퇴 API를 문서화한다")
    void withdraw_account() throws Exception {
        mockMvc.perform(delete("/api/v1/me").with(customerUser()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("연결된 소셜 계정 조회 API를 문서화한다")
    void get_social_accounts() throws Exception {
        mockMvc.perform(get("/api/v1/me/social-accounts").with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkedProviders[0]").value("GOOGLE"));
    }

    @Test
    @DisplayName("소셜 계정 연결 시작 API를 문서화한다")
    void start_social_account_link() throws Exception {
        mockMvc.perform(post("/api/v1/me/social-accounts/{provider}/authorization", "naver")
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl")
                        .value(startsWith(
                                "/api/v1/auth/social/authorization/naver?linkAttempt=")));
    }

    @Test
    @DisplayName("소셜 계정 연결 해제 API를 문서화한다")
    void unlink_social_account() throws Exception {
        mockMvc.perform(delete("/api/v1/me/social-accounts/{provider}", "google")
                        .with(customerUser()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("회원 비밀번호 변경 API를 문서화한다")
    void change_password() throws Exception {
        mockMvc.perform(patch("/api/v1/me/password")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "password1234",
                                  "newPassword": "newPassword1234"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("검증된 휴대폰으로 비밀번호 재설정 API를 문서화한다")
    void reset_password() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "member@example.com",
                                  "phone": "01012345678",
                                  "verificationCode": "123456",
                                  "newPassword": "newPassword1234"
                                }
                                """))
                .andExpect(status().isNoContent());
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
                        .contentType(APPLICATION_JSON)
                        .content("{\"productId\":1,\"qty\":1}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("비회원 장바구니 병합 API를 문서화한다")
    void merge_guest_cart() throws Exception {
        mockMvc.perform(post("/api/v1/me/cart/merge")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "e3668dc3-fdd1-45a8-ac19-25f5753157b0",
                                  "items": [{"productId": 1, "qty": 2}]
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("장바구니 수량 변경 API를 문서화한다")
    void update_cart_item() throws Exception {
        mockMvc.perform(put("/api/v1/me/cart/items/{productId}", 1L)
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("{\"qty\":2}"))
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
    @DisplayName("내 예약 목록 API를 문서화한다")
    void list_my_bookings() throws Exception {
        mockMvc.perform(get("/api/v1/me/bookings").with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].participantCount").value(3));
    }

    @Test
    @DisplayName("내 예약 상세 API를 문서화한다")
    void get_my_booking() throws Exception {
        mockMvc.perform(get("/api/v1/me/bookings/{id}", 100L).with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(3));
    }

    @Test
    @DisplayName("내 예약 변경 API를 문서화한다")
    void reschedule_my_booking() throws Exception {
        mockMvc.perform(patch("/api/v1/me/bookings/{id}/reschedule", 100L)
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("{\"newSlotId\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(3));
    }

    @Test
    @DisplayName("내 예약 취소 API를 문서화한다")
    void cancel_my_booking() throws Exception {
        mockMvc.perform(delete("/api/v1/me/bookings/{id}", 100L).with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(3))
                .andExpect(jsonPath("$.refund.amount").value(15000))
                .andExpect(jsonPath("$.refund.status").value("REQUESTED"));
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-00000200"));
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
    @DisplayName("내 8회권 정산 환불 API를 문서화한다")
    void refund_my_pass() throws Exception {
        mockMvc.perform(post("/api/v1/me/passes/{id}/refund", 300L).with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceledBookings").value(1))
                .andExpect(jsonPath("$.refundCredits").value(8))
                .andExpect(jsonPath("$.refundAmount").value(240000))
                .andExpect(jsonPath("$.refundStatus").value("REQUESTED"))
                .andExpect(jsonPath("$.refundId").doesNotExist());
    }

    @Test
    @DisplayName("내 알림 목록 API를 문서화한다")
    void list_my_notifications() throws Exception {
        mockMvc.perform(get("/api/v1/me/notifications")
                        .with(customerUser())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].eventType").value("ORDER_PAID"))
                .andExpect(jsonPath("$[0].aggregateType").value("ORDER"))
                .andExpect(jsonPath("$[0].aggregateId").value(200))
                .andExpect(jsonPath("$[0].deliveredAt").value("2026-03-28T09:15:00"))
                .andExpect(jsonPath("$[0].readAt").doesNotExist())
                .andExpect(jsonPath("$[0].read").value(false));
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
                        .contentType(APPLICATION_JSON)
                        .content("{\"verificationCode\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비회원 기록 인수 실행 API를 문서화한다")
    void claim_guest_records() throws Exception {
        mockMvc.perform(post("/api/v1/me/guest-claims")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("{\"orderIds\":[200],\"bookingIds\":[100]}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 문의 생성 API를 문서화한다")
    void create_inquiry() throws Exception {
        mockMvc.perform(post("/api/v1/me/inquiries")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "예약 문의",
                                  "content": "예약 변경이 가능한가요?"
                                }
                                """))
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
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "배송 문의",
                                  "content": "언제 받을 수 있나요?",
                                  "secret": false,
                                  "password": null
                                }
                                """))
                .andExpect(status().isCreated());
    }

    private static GuestClaimUseCase.ClaimPreview claimPreview(boolean verified) {
        return new GuestClaimUseCase.ClaimPreview(
                verified,
                List.of(new GuestClaimUseCase.ClaimOrderSummary(
                        200L, "PAID_APPROVAL_PENDING", 39000L,
                        LocalDateTime.of(2026, 5, 1, 20, 50).atOffset(ZoneOffset.UTC))),
                List.of(new GuestClaimUseCase.ClaimBookingSummary(
                        100L, "BOOKED", "향수 원데이",
                        LocalDateTime.of(2026, 5, 7, 19, 0),
                        LocalDateTime.of(2026, 5, 7, 21, 0))));
    }
}
