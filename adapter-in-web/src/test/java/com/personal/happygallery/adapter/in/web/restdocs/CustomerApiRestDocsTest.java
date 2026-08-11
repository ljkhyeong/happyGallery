package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.customer.CustomerSessionBinder;
import com.personal.happygallery.adapter.in.web.customer.CustomerAuthController;
import com.personal.happygallery.adapter.in.web.customer.CustomerCredentialController;
import com.personal.happygallery.adapter.in.web.customer.MeBookingController;
import com.personal.happygallery.adapter.in.web.customer.MeAccountController;
import com.personal.happygallery.adapter.in.web.customer.MeCartController;
import com.personal.happygallery.adapter.in.web.customer.MeEmailController;
import com.personal.happygallery.adapter.in.web.customer.MeGuestClaimController;
import com.personal.happygallery.adapter.in.web.customer.MeInquiryController;
import com.personal.happygallery.adapter.in.web.customer.MeNotificationController;
import com.personal.happygallery.adapter.in.web.customer.MeOrderController;
import com.personal.happygallery.adapter.in.web.customer.MePassController;
import com.personal.happygallery.adapter.in.web.customer.MePhoneController;
import com.personal.happygallery.adapter.in.web.customer.MeProductQnaController;
import com.personal.happygallery.adapter.in.web.customer.MeReviewController;
import com.personal.happygallery.adapter.in.web.customer.MeSocialAccountController;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerAuthenticationFilter;
import com.personal.happygallery.adapter.in.web.security.customer.SessionStateCodec;
import com.personal.happygallery.adapter.in.web.security.customer.SocialAccountLinkIntentStore;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerStepUpAuthenticationStore;
import com.personal.happygallery.application.booking.port.in.BookingCancelUseCase;
import com.personal.happygallery.application.booking.port.in.BookingQueryUseCase;
import com.personal.happygallery.application.booking.port.in.BookingRescheduleUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerCredentialUseCase;
import com.personal.happygallery.application.customer.port.in.GuestClaimUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase;
import com.personal.happygallery.application.customer.port.in.MemberPhoneUpdateUseCase;
import com.personal.happygallery.application.customer.port.in.MemberEmailRegistrationUseCase;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.inquiry.port.in.InquiryUseCase;
import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase;
import com.personal.happygallery.application.order.port.in.OrderQueryUseCase;
import com.personal.happygallery.application.pass.port.in.MemberPassRefundUseCase;
import com.personal.happygallery.application.pass.port.in.PassQueryUseCase;
import com.personal.happygallery.application.pass.port.in.PassRefundUseCase.PassRefundResult;
import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.application.review.port.in.ReviewUseCase;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.inquiry.Inquiry;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.qna.ProductQna;
import com.personal.happygallery.domain.review.ReviewCreationStatus;
import com.personal.happygallery.domain.review.ReviewEvidenceProvenance;
import com.personal.happygallery.domain.review.ReviewReportReason;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.domain.user.SocialProvider;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
    private MemberEmailRegistrationUseCase emailRegistrationUseCase;
    private CustomerAccountLifecycleUseCase accountLifecycleUseCase;
    private InquiryUseCase inquiryUseCase;
    private ProductQnaUseCase qnaUseCase;
    private ReviewUseCase reviewUseCase;
    private SubjectRateLimitGuard rateLimitGuard;
    private CustomerStepUpAuthenticationStore stepUpStore;

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
        emailRegistrationUseCase = mock(MemberEmailRegistrationUseCase.class);
        accountLifecycleUseCase = mock(CustomerAccountLifecycleUseCase.class);
        inquiryUseCase = mock(InquiryUseCase.class);
        qnaUseCase = mock(ProductQnaUseCase.class);
        reviewUseCase = mock(ReviewUseCase.class);
        rateLimitGuard = mock(SubjectRateLimitGuard.class);

        User user = RestDocsFixtures.user();
        Order order = RestDocsFixtures.order();
        Booking booking = RestDocsFixtures.booking();
        Refund bookingRefund = RestDocsFixtures.bookingRefund();
        OrderQueryUseCase.OrderDetail orderDetail = RestDocsFixtures.orderDetail();
        PassPurchase pass = RestDocsFixtures.passPurchase();
        Inquiry inquiry = RestDocsFixtures.inquiry();
        ProductQna qna = RestDocsFixtures.productQna();
        ProductQnaUseCase.OwnedQnaListView ownedQna =
                new ProductQnaUseCase.OwnedQnaListView(
                        qna.getId(), qna.getTitle(), qna.isSecret(),
                        qna.hasReply(), qna.getCreatedAt());
        ReviewUseCase.ReviewItem productReview = RestDocsFixtures.productReviewItem();
        ReviewUseCase.ReviewItem classReview = RestDocsFixtures.classReviewItem();

        when(customerAuthUseCase.signup(any())).thenReturn(user);
        when(customerAuthUseCase.login(any())).thenReturn(user);
        when(cartUseCase.getCart(CUSTOMER_USER_ID))
                .thenReturn(new CartUseCase.CartView(
                        List.of(new CartUseCase.CartItemView(
                                1L, "시그니처 캔들", ProductType.READY_STOCK, 39000L, 1, true)),
                        39000L,
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        when(bookingQueryUseCase.listMyBookings(CUSTOMER_USER_ID)).thenReturn(List.of(booking));
        when(bookingQueryUseCase.listMyBookings(eq(CUSTOMER_USER_ID), isNull(), eq(20)))
                .thenReturn(new CursorPage<>(List.of(booking), "cursor-next", true));
        when(bookingQueryUseCase.findMyBooking(100L, CUSTOMER_USER_ID))
                .thenReturn(new BookingQueryUseCase.BookingDetail(booking, null));
        when(bookingRescheduleUseCase.rescheduleMemberBooking(100L, CUSTOMER_USER_ID, 42L))
                .thenReturn(booking);
        when(bookingCancelUseCase.cancelMemberBooking(100L, CUSTOMER_USER_ID))
                .thenReturn(new BookingCancelUseCase.CancelResult(booking, true, bookingRefund, false));
        when(orderQueryUseCase.listMyOrders(CUSTOMER_USER_ID)).thenReturn(List.of(order));
        when(orderQueryUseCase.listMyOrders(eq(CUSTOMER_USER_ID), isNull(), eq(20)))
                .thenReturn(new CursorPage<>(List.of(order), "cursor-next", true));
        when(orderQueryUseCase.findMyOrder(200L, CUSTOMER_USER_ID)).thenReturn(orderDetail);
        PassQueryUseCase.PassView passView = new PassQueryUseCase.PassView(pass, null);
        when(passQueryUseCase.listMyPasses(CUSTOMER_USER_ID)).thenReturn(List.of(passView));
        when(passQueryUseCase.listMyPasses(eq(CUSTOMER_USER_ID), isNull(), eq(20)))
                .thenReturn(new CursorPage<>(List.of(passView), "cursor-next", true));
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
        when(phoneUpdateUseCase.update(any())).thenReturn(user);
        when(customerCredentialUseCase.resetPassword(any()))
                .thenReturn(CUSTOMER_USER_ID);
        when(socialAuthUseCase.listLinkedProviders(CUSTOMER_USER_ID))
                .thenReturn(List.of(SocialProvider.GOOGLE));
        when(inquiryUseCase.create(eq(CUSTOMER_USER_ID), any(), any())).thenReturn(inquiry);
        when(inquiryUseCase.listByUser(CUSTOMER_USER_ID)).thenReturn(List.of(inquiry));
        when(inquiryUseCase.listByUser(eq(CUSTOMER_USER_ID), isNull(), eq(20)))
                .thenReturn(new CursorPage<>(List.of(inquiry), "cursor-next", true));
        when(inquiryUseCase.findByIdAndUser(9L, CUSTOMER_USER_ID)).thenReturn(inquiry);
        when(qnaUseCase.createQuestion(eq(1L), eq(CUSTOMER_USER_ID), any(), any(), eq(false)))
                .thenReturn(qna);
        when(qnaUseCase.listOwnedByProduct(1L, CUSTOMER_USER_ID))
                .thenReturn(List.of(ownedQna));
        when(qnaUseCase.listOwnedByProduct(
                eq(1L), eq(CUSTOMER_USER_ID), isNull(), eq(20)))
                .thenReturn(new CursorPage<>(List.of(ownedQna), "cursor-next", true));
        when(qnaUseCase.getOwnedDetail(1L, 5L, CUSTOMER_USER_ID))
                .thenReturn(new ProductQnaUseCase.QnaWithAuthor(qna, "홍길동"));
        when(reviewUseCase.createProductReview(
                eq(CUSTOMER_USER_ID), eq(201L), eq(5), any()))
                .thenReturn(productReview);
        when(reviewUseCase.createClassReview(
                eq(CUSTOMER_USER_ID), eq(100L), eq(4), any()))
                .thenReturn(classReview);
        when(reviewUseCase.getProductReviewCreationState(CUSTOMER_USER_ID, 202L))
                .thenReturn(new ReviewUseCase.ReviewCreationState(
                        ReviewTargetType.PRODUCT, 202L, ReviewCreationStatus.AVAILABLE));
        when(reviewUseCase.getClassReviewCreationState(CUSTOMER_USER_ID, 100L))
                .thenReturn(new ReviewUseCase.ReviewCreationState(
                        ReviewTargetType.CLASS, 100L, ReviewCreationStatus.RECREATION_BLOCKED));
        when(reviewUseCase.listMyReviews(eq(CUSTOMER_USER_ID), isNull(), eq(20)))
                .thenReturn(new CursorPage<>(
                        List.of(productReview, classReview), "cursor-next", true));
        when(reviewUseCase.listMyOrderReviews(CUSTOMER_USER_ID, 200L))
                .thenReturn(List.of(productReview));
        when(reviewUseCase.listMyBookingReviews(CUSTOMER_USER_ID, 100L))
                .thenReturn(List.of(classReview));
        when(reviewUseCase.updateReview(
                eq(CUSTOMER_USER_ID), eq(31L), eq(1L), eq(4), any()))
                .thenReturn(productReview);
        when(reviewUseCase.listMyReviewOpportunities(CUSTOMER_USER_ID, null, 20))
                .thenReturn(new CursorPage<>(List.of(new ReviewUseCase.ReviewOpportunity(
                        ReviewTargetType.PRODUCT,
                        202L,
                        1L,
                        "시그니처 캔들",
                        200L,
                        null,
                        LocalDateTime.of(2026, 4, 30, 18, 0))), "opportunity-next", true));
        when(reviewUseCase.markHelpful(CUSTOMER_USER_ID, 32L))
                .thenReturn(new ReviewUseCase.HelpfulResult(32L, 2L, true));
        when(reviewUseCase.unmarkHelpful(CUSTOMER_USER_ID, 32L))
                .thenReturn(new ReviewUseCase.HelpfulResult(32L, 1L, false));
        when(reviewUseCase.listMyReviewReactions(
                CUSTOMER_USER_ID, List.of(31L, 32L)))
                .thenReturn(List.of(
                        new ReviewUseCase.ReviewReaction(31L, false, false, true, false),
                        new ReviewUseCase.ReviewReaction(32L, true, false, false, true)));
        when(reviewUseCase.createReport(
                eq(CUSTOMER_USER_ID), eq(32L), eq(ReviewReportReason.SPAM), any()))
                .thenReturn(new ReviewUseCase.ReviewReportItem(
                        71L,
                        32L,
                        CUSTOMER_USER_ID,
                        ReviewReportReason.SPAM,
                        "홍보성 링크가 포함되어 있습니다.",
                        ReviewStatus.PUBLISHED,
                        new ReviewUseCase.ReviewEvidenceItem(
                                82L,
                                classReview.contentRevision(),
                                classReview.rating(),
                                classReview.content(),
                                classReview.editedAt(),
                                ReviewEvidenceProvenance.LIVE,
                                true,
                                List.of(),
                                LocalDateTime.of(2026, 5, 1, 21, 0)),
                        ReviewReportStatus.PENDING,
                        null,
                        null,
                        null,
                        LocalDateTime.of(2026, 5, 1, 21, 0)));
        when(reviewUseCase.addReviewImage(
                eq(CUSTOMER_USER_ID), eq(31L), any(byte[].class), eq("image/png")))
                .thenReturn(new ReviewUseCase.ReviewImageItem(
                        52L,
                        "/api/v1/media/images/review-added.png",
                        1,
                        LocalDateTime.of(2026, 5, 1, 21, 0)));

        SessionStateCodec sessionStateCodec =
                new SessionStateCodec(JsonMapper.builder().build());
        stepUpStore = new CustomerStepUpAuthenticationStore(
                RestDocsFixtures.clock(),
                sessionStateCodec);
        CustomerSessionBinder customerSessionBinder = new CustomerSessionBinder(
                mock(CsrfTokenRepository.class), stepUpStore);
        mockMvc = mockMvc(restDocumentation,
                new CustomerAuthController(customerAuthUseCase, customerSessionBinder, rateLimitGuard),
                new CustomerCredentialController(
                        customerCredentialUseCase,
                        customerSessionBinder,
                        stepUpStore,
                        rateLimitGuard),
                new MeCartController(cartUseCase),
                new MeBookingController(bookingQueryUseCase, bookingRescheduleUseCase,
                        bookingCancelUseCase, RestDocsFixtures.clock()),
                new MeOrderController(orderQueryUseCase),
                new MePassController(passQueryUseCase, memberPassRefundUseCase, rateLimitGuard),
                new MeNotificationController(notificationQueryUseCase),
                new MeGuestClaimController(guestClaimUseCase, rateLimitGuard),
                new MePhoneController(phoneUpdateUseCase, stepUpStore, customerSessionBinder),
                new MeEmailController(
                        emailRegistrationUseCase,
                        stepUpStore,
                        customerSessionBinder),
                new MeAccountController(
                        accountLifecycleUseCase,
                        customerSessionBinder,
                        stepUpStore),
                new MeSocialAccountController(
                        socialAuthUseCase,
                        new SocialAccountLinkIntentStore(
                                RestDocsFixtures.clock(),
                                sessionStateCodec),
                        customerSessionBinder,
                        stepUpStore),
                new MeInquiryController(inquiryUseCase),
                new MeProductQnaController(qnaUseCase),
                new MeReviewController(reviewUseCase, rateLimitGuard));
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
                                    "termsVersion": "2026-08-08-v1",
                                    "termsAccepted": true,
                                    "privacyVersion": "2026-08-11-v2",
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
    @DisplayName("회원 이메일 인증 코드 발송 API를 문서화한다")
    void send_email_verification() throws Exception {
        mockMvc.perform(post("/api/v1/me/email-verifications")
                        .session(recentlyAuthenticatedSession())
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "naver-member@example.com"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("인증한 회원 이메일 등록 API를 문서화한다")
    void register_verified_email() throws Exception {
        mockMvc.perform(patch("/api/v1/me/email")
                        .session(recentlyAuthenticatedSession())
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "naver-member@example.com",
                                  "verificationCode": "123456"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("회원 탈퇴 API를 문서화한다")
    void withdraw_account() throws Exception {
        mockMvc.perform(delete("/api/v1/me")
                        .session(recentlyAuthenticatedSession())
                        .with(customerUser()))
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
                        .session(recentlyAuthenticatedSession())
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
                        .session(recentlyAuthenticatedSession())
                        .with(customerUser()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("현재 비밀번호 본인 확인 API를 문서화한다")
    void reauthenticate_with_password() throws Exception {
        mockMvc.perform(post("/api/v1/me/reauthentication/password")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "password1234"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("연결된 소셜 계정 본인 확인 시작 API를 문서화한다")
    void start_social_reauthentication() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/me/social-accounts/{provider}/reauthentication",
                        "google")
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl")
                        .value(startsWith(
                                "/api/v1/auth/social/authorization/google?linkAttempt=")));
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
    @DisplayName("장바구니 병합 API는 상품 100건을 초과한 요청을 거절한다")
    void merge_guest_cart_rejects_more_than_100_items() throws Exception {
        String items = "[" + String.join(",", Collections.nCopies(
                101, "{\"productId\":1,\"qty\":1}")) + "]";

        mockMvc.perform(post("/api/v1/me/cart/merge")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotencyKey": "e3668dc3-fdd1-45a8-ac19-25f5753157b0",
                                  "items": %s
                                }
                                """.formatted(items)))
                .andExpect(status().isBadRequest());
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
    @DisplayName("내 예약 커서 페이지 API를 문서화한다")
    void list_my_bookings_page() throws Exception {
        mockMvc.perform(get("/api/v1/me/bookings/page")
                        .with(customerUser())
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].bookingId").value(100))
                .andExpect(jsonPath("$.nextCursor").value("cursor-next"))
                .andExpect(jsonPath("$.hasMore").value(true));
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
    @DisplayName("내 주문 커서 페이지 API를 문서화한다")
    void list_my_orders_page() throws Exception {
        mockMvc.perform(get("/api/v1/me/orders/page")
                        .with(customerUser())
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(200))
                .andExpect(jsonPath("$.nextCursor").value("cursor-next"))
                .andExpect(jsonPath("$.hasMore").value(true));
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
    @DisplayName("내 8회권 커서 페이지 API를 문서화한다")
    void list_my_passes_page() throws Exception {
        mockMvc.perform(get("/api/v1/me/passes/page")
                        .with(customerUser())
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].passId").value(300))
                .andExpect(jsonPath("$.nextCursor").value("cursor-next"))
                .andExpect(jsonPath("$.hasMore").value(true));
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
    @DisplayName("내 문의 커서 페이지 API를 문서화한다")
    void list_my_inquiries_page() throws Exception {
        mockMvc.perform(get("/api/v1/me/inquiries/page")
                        .with(customerUser())
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(9))
                .andExpect(jsonPath("$.nextCursor").value("cursor-next"))
                .andExpect(jsonPath("$.hasMore").value(true));
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
                                  "secret": false
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("내 상품 QNA 목록 API를 문서화한다")
    void list_my_product_qna() throws Exception {
        mockMvc.perform(get("/api/v1/me/products/{productId}/qna", 1L)
                        .with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 상품 QNA 커서 페이지 API를 문서화한다")
    void list_my_product_qna_page() throws Exception {
        mockMvc.perform(get("/api/v1/me/products/{productId}/qna/page", 1L)
                        .with(customerUser())
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.nextCursor").value("cursor-next"))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    @DisplayName("내 상품 QNA 상세 API를 문서화한다")
    void get_my_product_qna() throws Exception {
        mockMvc.perform(get("/api/v1/me/products/{productId}/qna/{id}", 1L, 5L)
                        .with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("완료 주문 품목으로 상품 후기를 작성하는 API를 문서화한다")
    void create_product_review() throws Exception {
        mockMvc.perform(post("/api/v1/me/reviews/products")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "orderItemId": 201,
                                  "rating": 5,
                                  "content": "마감이 깔끔하고 선물하기 좋았습니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.targetType").value("PRODUCT"))
                .andExpect(jsonPath("$.sourceType").value("ORDER_ITEM"))
                .andExpect(jsonPath("$.verifiedTransaction").value(true))
                .andExpect(jsonPath("$.officialReply.content").value("소중한 후기 감사합니다."))
                .andExpect(jsonPath("$.helpfulCount").value(3))
                .andExpect(jsonPath("$.images[0].id").value(51));
    }

    @Test
    @DisplayName("완료 예약으로 클래스 후기를 작성하는 API를 문서화한다")
    void create_class_review() throws Exception {
        mockMvc.perform(post("/api/v1/me/reviews/classes")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "bookingId": 100,
                                  "rating": 4,
                                  "content": "설명이 친절해서 즐겁게 참여했습니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(32))
                .andExpect(jsonPath("$.targetType").value("CLASS"))
                .andExpect(jsonPath("$.sourceType").value("BOOKING"))
                .andExpect(jsonPath("$.edited").value(true))
                .andExpect(jsonPath("$.editedAt").exists());
    }

    @Test
    @DisplayName("주문 품목의 후기 작성 가능 상태 API를 문서화한다")
    void get_product_review_creation_state() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/me/reviews/products/{orderItemId}/creation-state", 202L)
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetType").value("PRODUCT"))
                .andExpect(jsonPath("$.sourceType").value("ORDER_ITEM"))
                .andExpect(jsonPath("$.sourceId").value(202))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("숨김 이력 후기 삭제 뒤에도 클래스 후기 재작성 차단 상태를 반환한다")
    void get_class_review_creation_state() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/me/reviews/classes/{bookingId}/creation-state", 100L)
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetType").value("CLASS"))
                .andExpect(jsonPath("$.sourceType").value("BOOKING"))
                .andExpect(jsonPath("$.sourceId").value(100))
                .andExpect(jsonPath("$.status").value("RECREATION_BLOCKED"));
    }

    @Test
    @DisplayName("내 후기 커서 페이지 API를 문서화한다")
    void list_my_reviews() throws Exception {
        mockMvc.perform(get("/api/v1/me/reviews")
                        .with(customerUser())
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(31))
                .andExpect(jsonPath("$.content[1].id").value(32))
                .andExpect(jsonPath("$.nextCursor").value("cursor-next"))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    @DisplayName("작성 가능한 후기 목록 API를 문서화한다")
    void list_my_review_opportunities() throws Exception {
        mockMvc.perform(get("/api/v1/me/reviews/opportunities")
                        .with(customerUser())
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].targetType").value("PRODUCT"))
                .andExpect(jsonPath("$.content[0].sourceType").value("ORDER_ITEM"))
                .andExpect(jsonPath("$.content[0].sourceId").value(202))
                .andExpect(jsonPath("$.content[0].orderId").value(200))
                .andExpect(jsonPath("$.content[0].bookingId").doesNotExist())
                .andExpect(jsonPath("$.nextCursor").value("opportunity-next"))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    @DisplayName("후기별 내 반응을 한 번에 조회하는 API를 문서화한다")
    void list_my_review_reactions() throws Exception {
        mockMvc.perform(get("/api/v1/me/reviews/reactions")
                        .with(customerUser())
                        .param("reviewIds", "31", "32"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewId").value(31))
                .andExpect(jsonPath("$[0].helpfulByMe").value(false))
                .andExpect(jsonPath("$[0].ownedByMe").value(true))
                .andExpect(jsonPath("$[0].canInteract").value(false))
                .andExpect(jsonPath("$[1].reviewId").value(32))
                .andExpect(jsonPath("$[1].helpfulByMe").value(true))
                .andExpect(jsonPath("$[1].ownedByMe").value(false))
                .andExpect(jsonPath("$[1].canInteract").value(true));
    }

    @Test
    @DisplayName("주문 품목별 내 후기 배열 API를 문서화한다")
    void list_my_order_reviews() throws Exception {
        mockMvc.perform(get("/api/v1/me/reviews/orders/{orderId}", 200L)
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(31))
                .andExpect(jsonPath("$[0].sourceType").value("ORDER_ITEM"));
    }

    @Test
    @DisplayName("예약의 내 후기 배열 API를 문서화한다")
    void list_my_booking_reviews() throws Exception {
        mockMvc.perform(get("/api/v1/me/reviews/bookings/{bookingId}", 100L)
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(32))
                .andExpect(jsonPath("$[0].sourceType").value("BOOKING"));
    }

    @Test
    @DisplayName("내 후기 수정 API를 문서화한다")
    void update_my_review() throws Exception {
        mockMvc.perform(patch("/api/v1/me/reviews/{reviewId}", 31L)
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedContentRevision": 1,
                                  "rating": 4,
                                  "content": "사용 후기를 수정했습니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(31));
    }

    @Test
    @DisplayName("공개 후기에 도움돼요를 표시하는 API를 문서화한다")
    void mark_review_helpful() throws Exception {
        mockMvc.perform(put("/api/v1/me/reviews/{reviewId}/helpful", 32L)
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(32))
                .andExpect(jsonPath("$.helpfulCount").value(2))
                .andExpect(jsonPath("$.helpfulByMe").value(true));
    }

    @Test
    @DisplayName("후기 도움돼요 표시를 취소하는 API를 문서화한다")
    void unmark_review_helpful() throws Exception {
        mockMvc.perform(delete("/api/v1/me/reviews/{reviewId}/helpful", 32L)
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(32))
                .andExpect(jsonPath("$.helpfulCount").value(1))
                .andExpect(jsonPath("$.helpfulByMe").value(false));
    }

    @Test
    @DisplayName("공개 후기를 신고하는 API를 문서화한다")
    void report_review() throws Exception {
        mockMvc.perform(post("/api/v1/me/reviews/{reviewId}/reports", 32L)
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "SPAM",
                                  "detail": "홍보성 링크가 포함되어 있습니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(71))
                .andExpect(jsonPath("$.reviewId").value(32))
                .andExpect(jsonPath("$.reason").value("SPAM"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("내 후기에 이미지를 추가하는 multipart API를 문서화한다")
    void add_my_review_image() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "review.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/me/reviews/{reviewId}/images", 31L)
                        .file(file)
                        .with(customerUser()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(52))
                .andExpect(jsonPath("$.imageUrl")
                        .value("/api/v1/media/images/review-added.png"))
                .andExpect(jsonPath("$.sortOrder").value(1));
    }

    @Test
    @DisplayName("내 후기 이미지를 삭제하는 API를 문서화한다")
    void delete_my_review_image() throws Exception {
        mockMvc.perform(delete(
                        "/api/v1/me/reviews/{reviewId}/images/{imageId}", 31L, 52L)
                        .with(customerUser()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("내 후기 삭제 API를 문서화한다")
    void delete_my_review() throws Exception {
        mockMvc.perform(delete("/api/v1/me/reviews/{reviewId}", 31L)
                        .with(customerUser()))
                .andExpect(status().isNoContent());
    }

    private MockHttpSession recentlyAuthenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                CustomerAuthenticationFilter.CUSTOMER_USER_ID_SESSION_ATTRIBUTE,
                CUSTOMER_USER_ID);
        session.setAttribute(
                CustomerAuthenticationFilter.CUSTOMER_CREDENTIAL_VERSION_SESSION_ATTRIBUTE,
                0L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        stepUpStore.markVerified(request, CUSTOMER_USER_ID, 0L);
        return session;
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
