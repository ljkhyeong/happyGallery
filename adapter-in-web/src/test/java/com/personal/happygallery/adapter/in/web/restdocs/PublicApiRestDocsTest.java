package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.booking.BookingController;
import com.personal.happygallery.adapter.in.web.booking.ClassController;
import com.personal.happygallery.adapter.in.web.booking.SlotController;
import com.personal.happygallery.adapter.in.web.customer.GuestRecordRecoveryController;
import com.personal.happygallery.adapter.in.web.monitoring.ClientMonitoringController;
import com.personal.happygallery.adapter.in.web.notice.NoticeController;
import com.personal.happygallery.adapter.in.web.order.OrderController;
import com.personal.happygallery.adapter.in.web.payment.PaymentController;
import com.personal.happygallery.adapter.in.web.payment.PaymentQueryController;
import com.personal.happygallery.adapter.in.web.product.ProductController;
import com.personal.happygallery.adapter.in.web.product.ProductQnaController;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.adapter.in.web.workshop.WorkshopProfileController;
import com.personal.happygallery.application.booking.port.in.BookingCancelUseCase;
import com.personal.happygallery.application.booking.port.in.BookingQueryUseCase;
import com.personal.happygallery.application.booking.port.in.BookingRescheduleUseCase;
import com.personal.happygallery.application.booking.port.in.ClassQueryUseCase;
import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase;
import com.personal.happygallery.application.booking.port.in.SlotQueryUseCase;
import com.personal.happygallery.application.customer.GuestPersonalDataProtector;
import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase;
import com.personal.happygallery.application.monitoring.port.in.ClientMonitoringUseCase;
import com.personal.happygallery.application.notice.port.in.NoticeQueryUseCase;
import com.personal.happygallery.application.order.port.in.OrderQueryUseCase;
import com.personal.happygallery.application.order.OrderPriceProperties;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentStatusRecoveryUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentStatusQueryUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentStatusQueryUseCase.CustomerPaymentStatus;
import com.personal.happygallery.application.pass.PassPriceProperties;
import com.personal.happygallery.application.product.ProductFilter;
import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.application.store.port.in.WorkshopProfileUseCase;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.notice.Notice;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.store.WorkshopProfile;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicApiRestDocsTest extends RestDocsTestSupport {

    private MockMvc mockMvc;

    private ProductQueryUseCase productQueryUseCase;
    private ProductQnaUseCase qnaUseCase;
    private ClassQueryUseCase classQueryUseCase;
    private SlotQueryUseCase slotQueryUseCase;
    private GuestBookingUseCase guestBookingUseCase;
    private BookingQueryUseCase bookingQueryUseCase;
    private BookingRescheduleUseCase bookingRescheduleUseCase;
    private BookingCancelUseCase bookingCancelUseCase;
    private GuestPersonalDataProtector guestPersonalDataProtector;
    private OrderQueryUseCase orderQueryUseCase;
    private PaymentPrepareUseCase paymentPrepareUseCase;
    private PaymentConfirmUseCase paymentConfirmUseCase;
    private PaymentStatusQueryUseCase paymentStatusQueryUseCase;
    private PaymentStatusRecoveryUseCase paymentStatusRecoveryUseCase;
    private NoticeQueryUseCase noticeQueryUseCase;
    private ClientMonitoringUseCase clientMonitoringUseCase;
    private GuestRecordRecoveryUseCase guestRecordRecoveryUseCase;
    private SubjectRateLimitGuard rateLimitGuard;
    private WorkshopProfileUseCase workshopProfileUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        productQueryUseCase = mock(ProductQueryUseCase.class);
        qnaUseCase = mock(ProductQnaUseCase.class);
        classQueryUseCase = mock(ClassQueryUseCase.class);
        slotQueryUseCase = mock(SlotQueryUseCase.class);
        guestBookingUseCase = mock(GuestBookingUseCase.class);
        bookingQueryUseCase = mock(BookingQueryUseCase.class);
        bookingRescheduleUseCase = mock(BookingRescheduleUseCase.class);
        bookingCancelUseCase = mock(BookingCancelUseCase.class);
        guestPersonalDataProtector = mock(GuestPersonalDataProtector.class);
        orderQueryUseCase = mock(OrderQueryUseCase.class);
        paymentPrepareUseCase = mock(PaymentPrepareUseCase.class);
        paymentConfirmUseCase = mock(PaymentConfirmUseCase.class);
        paymentStatusQueryUseCase = mock(PaymentStatusQueryUseCase.class);
        paymentStatusRecoveryUseCase = mock(PaymentStatusRecoveryUseCase.class);
        noticeQueryUseCase = mock(NoticeQueryUseCase.class);
        clientMonitoringUseCase = mock(ClientMonitoringUseCase.class);
        guestRecordRecoveryUseCase = mock(GuestRecordRecoveryUseCase.class);
        rateLimitGuard = mock(SubjectRateLimitGuard.class);
        workshopProfileUseCase = mock(WorkshopProfileUseCase.class);

        ProductQueryUseCase.ProductWithInventory product = RestDocsFixtures.productWithInventory();
        ProductQnaUseCase.QnaWithAuthor qna = qna();
        BookingClass bookingClass = RestDocsFixtures.bookingClass();
        Slot slot = RestDocsFixtures.slot();
        PhoneVerification phoneVerification = RestDocsFixtures.phoneVerification();
        Booking booking = RestDocsFixtures.booking();
        Refund bookingRefund = RestDocsFixtures.bookingRefund();
        OrderQueryUseCase.OrderDetail orderDetail = RestDocsFixtures.orderDetail();
        Notice notice = RestDocsFixtures.notice();

        when(productQueryUseCase.listActiveProducts(any(ProductFilter.class)))
                .thenReturn(List.of(product));
        when(productQueryUseCase.listActiveCategories()).thenReturn(List.of("CANDLE", "PERFUME"));
        when(productQueryUseCase.getProduct(1L)).thenReturn(product);
        when(qnaUseCase.listByProduct(1L)).thenReturn(List.of(qna));
        when(qnaUseCase.getPublicDetail(1L, 5L)).thenReturn(qna);
        when(qnaUseCase.verifyAndGet(eq(1L), eq(5L), any())).thenReturn(qna);
        when(classQueryUseCase.listActive()).thenReturn(List.of(bookingClass));
        when(slotQueryUseCase.listAvailable(any(), any())).thenReturn(List.of(slot));
        when(slotQueryUseCase.listUpcoming(any(), anyInt())).thenReturn(List.of(slot));
        when(guestBookingUseCase.sendVerificationCode(any())).thenReturn(phoneVerification);
        when(bookingQueryUseCase.getBookingByToken(eq(100L), any()))
                .thenReturn(new BookingQueryUseCase.BookingDetail(booking, null));
        when(guestPersonalDataProtector.decryptPhone(any(Guest.class))).thenReturn("01012345678");
        when(guestPersonalDataProtector.decryptName(any(Guest.class))).thenReturn("홍길동");
        when(bookingRescheduleUseCase.rescheduleBooking(eq(100L), any(), eq(42L)))
                .thenReturn(booking);
        when(bookingCancelUseCase.cancelBooking(eq(100L), any()))
                .thenReturn(new BookingCancelUseCase.CancelResult(booking, true, bookingRefund, false));
        when(orderQueryUseCase.getOrderByToken(eq(200L), any())).thenReturn(orderDetail);
        when(paymentPrepareUseCase.prepare(any()))
                .thenReturn(new PaymentPrepareUseCase.PrepareResult(
                        "pay_20260501_0001", 39000L, PaymentContext.ORDER, "payment-status-token"));
        when(paymentConfirmUseCase.confirm(any()))
                .thenReturn(new PaymentConfirmUseCase.ConfirmResult(
                        PaymentContext.ORDER, 200L, "guest-access-token", false));
        when(paymentStatusQueryUseCase.getStatus(any(), any(), any()))
                .thenReturn(new PaymentStatusQueryUseCase.PaymentStatusResult(
                        PaymentContext.ORDER, 39_000L, CustomerPaymentStatus.REFUNDING, null, null, false));
        when(paymentStatusRecoveryUseCase.recover(eq("01012345678"), eq("123456")))
                .thenReturn(new PaymentStatusRecoveryUseCase.RecoveryResult(
                        "recovered-payment-status-token",
                        Instant.parse("2026-05-31T00:00:00Z"),
                        List.of(new PaymentStatusRecoveryUseCase.RecoveredPayment(
                                "pay_20260501_0001",
                                PaymentContext.ORDER,
                                39_000L,
                                CustomerPaymentStatus.REFUNDING))));
        when(noticeQueryUseCase.listAll()).thenReturn(List.of(notice));
        when(noticeQueryUseCase.getDetail(1L)).thenReturn(notice);
        when(guestRecordRecoveryUseCase.recover(eq("01012345678"), eq("123456")))
                .thenReturn(new GuestRecordRecoveryUseCase.RecoveryResult(
                        "guest-recovery-token",
                        Instant.parse("2026-05-02T00:00:00Z"),
                        List.of(new GuestRecordRecoveryUseCase.RecoveredOrder(
                                200L, "PAID_APPROVAL_PENDING", 39_000L,
                                OffsetDateTime.parse("2026-05-01T00:00:00Z"))),
                        List.of(new GuestRecordRecoveryUseCase.RecoveredBooking(
                                100L, "BOOKED", "향수 클래스",
                                LocalDateTime.parse("2026-05-07T10:00:00"),
                                LocalDateTime.parse("2026-05-07T12:00:00")))));
        WorkshopProfile workshop = new WorkshopProfile("해피갤러리");
        workshop.update(
                "해피갤러리", "010-9635-5608", null,
                "충북 충주시 계명대로 161", "1층", null,
                "https://m.place.naver.com/place/21668321", null, "303-11-87052",
                "홍지현", "ssi1972@naver.com", "2011-충북 충주-127",
                "해피갤러리는 빈티지 가죽공예, 레진아트, 플루이드아트, 톨페인팅, 냅킨아트, "
                        + "양말목공예, 하바리움, 위빙, POP 원데이클래스부터 자격증반, 창업반을 운영합니다.",
                "ssim1972",
                "https://talk.naver.com/w4xufy",
                "https://blog.naver.com/ssim1972",
                "https://www.instagram.com/happygallery_by/",
                "https://smartstore.naver.com/happygallery",
                LocalDateTime.of(2026, 5, 1, 21, 0));
        when(workshopProfileUseCase.get()).thenReturn(workshop);

        mockMvc = mockMvc(restDocumentation,
                new ProductController(productQueryUseCase),
                new ProductQnaController(qnaUseCase),
                new ClassController(classQueryUseCase),
                new SlotController(slotQueryUseCase),
                new BookingController(guestBookingUseCase, bookingQueryUseCase,
                        bookingRescheduleUseCase, bookingCancelUseCase, guestPersonalDataProtector,
                        rateLimitGuard, RestDocsFixtures.clock()),
                new OrderController(orderQueryUseCase, new OrderPriceProperties(3_000L)),
                new PaymentController(paymentPrepareUseCase, paymentConfirmUseCase, rateLimitGuard),
                new PaymentQueryController(paymentStatusQueryUseCase, new PassPriceProperties(240_000L)),
                new NoticeController(noticeQueryUseCase),
                new WorkshopProfileController(workshopProfileUseCase),
                new GuestRecordRecoveryController(
                        guestRecordRecoveryUseCase, paymentStatusRecoveryUseCase, rateLimitGuard),
                new ClientMonitoringController(clientMonitoringUseCase));
    }

    @Test
    @DisplayName("공개 상품 목록 API를 문서화한다")
    void list_products() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("type", "READY_STOCK")
                        .param("category", "CANDLE")
                        .param("keyword", "캔들")
                        .param("sort", "newest"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공개 상품 카테고리 API를 문서화한다")
    void list_product_categories() throws Exception {
        mockMvc.perform(get("/api/v1/products/categories"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공개 상품 상세 API를 문서화한다")
    void get_product() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공개 상품 QNA 목록 API를 문서화한다")
    void list_product_qna() throws Exception {
        mockMvc.perform(get("/api/v1/products/{productId}/qna", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공개 상품 QNA 일반글 상세 API를 문서화한다")
    void get_public_product_qna() throws Exception {
        mockMvc.perform(get("/api/v1/products/{productId}/qna/{id}", 1L, 5L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공개 상품 QNA 비밀번호 확인 API를 문서화한다")
    void verify_product_qna_password() throws Exception {
        mockMvc.perform(post("/api/v1/products/{productId}/qna/{id}/verify", 1L, 5L)
                        .contentType(APPLICATION_JSON)
                        .content("{\"password\":\"qna-secret\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공개 클래스 목록 API를 문서화한다")
    void list_classes() throws Exception {
        mockMvc.perform(get("/api/v1/classes"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공개 공방과 사업자 정보 API를 문서화한다")
    void get_workshop_profile() throws Exception {
        mockMvc.perform(get("/api/v1/workshop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("해피갤러리"))
                .andExpect(jsonPath("$.phone").value("010-9635-5608"))
                .andExpect(jsonPath("$.addressLine1").value("충북 충주시 계명대로 161"))
                .andExpect(jsonPath("$.addressLine2").value("1층"))
                .andExpect(jsonPath("$.businessRegistrationNumber").value("303-11-87052"))
                .andExpect(jsonPath("$.representativeName").value("홍지현"))
                .andExpect(jsonPath("$.email").value("ssi1972@naver.com"))
                .andExpect(jsonPath("$.mailOrderRegistrationNumber").value("2011-충북 충주-127"))
                .andExpect(jsonPath("$.kakaoTalkId").value("ssim1972"))
                .andExpect(jsonPath("$.naverTalkUrl").value("https://talk.naver.com/w4xufy"))
                .andExpect(jsonPath("$.naverBlogUrl").value("https://blog.naver.com/ssim1972"))
                .andExpect(jsonPath("$.instagramUrl")
                        .value("https://www.instagram.com/happygallery_by/"))
                .andExpect(jsonPath("$.smartStoreUrl")
                        .value("https://smartstore.naver.com/happygallery"));
    }

    @Test
    @DisplayName("공개 슬롯 목록 API를 문서화한다")
    void list_slots() throws Exception {
        mockMvc.perform(get("/api/v1/slots")
                        .param("classId", "1")
                        .param("date", "2026-05-07"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("향후 공개 슬롯 목록 API를 문서화한다")
    void list_upcoming_slots() throws Exception {
        mockMvc.perform(get("/api/v1/slots/upcoming")
                        .param("classId", "1")
                        .param("days", "14"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비회원 휴대폰 인증 발송 API를 문서화한다")
    void send_booking_phone_verification() throws Exception {
        mockMvc.perform(post("/api/v1/bookings/phone-verifications")
                        .contentType(APPLICATION_JSON)
                        .content("{\"phone\":\"01012345678\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비회원 예약 조회 API를 문서화한다")
    void get_guest_booking() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/{bookingId}", 100L)
                        .header("X-Access-Token", "guest-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(3));
    }

    @Test
    @DisplayName("비회원 예약 변경 API를 문서화한다")
    void reschedule_guest_booking() throws Exception {
        mockMvc.perform(patch("/api/v1/bookings/{bookingId}/reschedule", 100L)
                        .header("X-Access-Token", "guest-access-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"newSlotId\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(3));
    }

    @Test
    @DisplayName("다인 예약 결제 prepare API를 문서화한다")
    void prepare_booking_payment() throws Exception {
        mockMvc.perform(post("/api/v1/payments/prepare")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "context": "BOOKING",
                                  "payload": {
                                    "type": "BOOKING",
                                    "userId": 11,
                                    "slotId": 42,
                                    "paymentMethod": "CARD",
                                    "participantCount": 3
                                  }
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비회원 예약 취소 API를 문서화한다")
    void cancel_guest_booking() throws Exception {
        mockMvc.perform(delete("/api/v1/bookings/{bookingId}", 100L)
                        .header("X-Access-Token", "guest-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(3))
                .andExpect(jsonPath("$.refund.amount").value(15000))
                .andExpect(jsonPath("$.refund.status").value("REQUESTED"));
    }

    @Test
    @DisplayName("비회원 주문 조회 API를 문서화한다")
    void get_guest_order() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", 200L)
                        .header("X-Access-Token", "guest-access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-00000200"));
    }

    @Test
    @DisplayName("주문 배송비 정책 API를 문서화한다")
    void get_order_price_policy() throws Exception {
        mockMvc.perform(get("/api/v1/orders/policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shippingFee").value(3000))
                .andExpect(jsonPath("$.madeToOrderConsentVersion").value("2026-07-21-v1"))
                .andExpect(jsonPath("$.madeToOrderConsentText").isNotEmpty());
    }

    @Test
    @DisplayName("비회원 주문·예약 접근 권한 복구 API를 문서화한다")
    void recover_guest_records() throws Exception {
        mockMvc.perform(post("/api/v1/guest-records/recovery")
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
    @DisplayName("비회원 결제 상태 조회 권한 복구 API를 문서화한다")
    void recover_guest_payment_statuses() throws Exception {
        mockMvc.perform(post("/api/v1/guest-records/payment-status-recovery")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "01012345678",
                                  "verificationCode": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusToken").value("recovered-payment-status-token"))
                .andExpect(jsonPath("$.payments[0].orderId").value("pay_20260501_0001"))
                .andExpect(jsonPath("$.payments[0].status").value("REFUNDING"));
    }

    @Test
    @DisplayName("결제 prepare API를 문서화한다")
    void prepare_payment() throws Exception {
        mockMvc.perform(post("/api/v1/payments/prepare")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "context": "ORDER",
                                  "payload": {
                                    "type": "ORDER",
                                    "userId": 11,
                                    "items": [],
                                    "cartCheckout": true,
                                    "fulfillmentType": "PICKUP",
                                    "shippingAddress": null,
                                    "madeToOrderConsent": false
                                  }
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("결제 confirm API를 문서화한다")
    void confirm_payment() throws Exception {
        mockMvc.perform(post("/api/v1/payments/confirm")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentKey": "toss-payment-key",
                                  "orderId": "pay_20260501_0001",
                                  "amount": 39000
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("소유권을 확인하는 결제 상태 조회 API를 문서화한다")
    void get_payment_status() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{orderId}", "pay_20260501_0001")
                .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDING"));
    }

    @Test
    @DisplayName("8회권 결제 정책 조회 API를 문서화한다")
    void get_pass_payment_policy() throws Exception {
        mockMvc.perform(get("/api/v1/payments/pass-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(240000))
                .andExpect(jsonPath("$.totalCredits").value(8))
                .andExpect(jsonPath("$.validityDays").value(90));
    }

    @Test
    @DisplayName("공지 목록 API를 문서화한다")
    void list_notices() throws Exception {
        mockMvc.perform(get("/api/v1/notices"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공지 상세 API를 문서화한다")
    void get_notice() throws Exception {
        mockMvc.perform(get("/api/v1/notices/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("클라이언트 모니터링 이벤트 수집 API를 문서화한다")
    void capture_client_event() throws Exception {
        mockMvc.perform(post("/api/v1/monitoring/client-events")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "event": "GUEST_LOOKUP_HUB_VIEWED",
                                  "path": "/guest",
                                  "source": "GuestLookupPage",
                                  "target": "primary-cta"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    private static ProductQnaUseCase.QnaWithAuthor qna() {
        return new ProductQnaUseCase.QnaWithAuthor(RestDocsFixtures.productQna(), "홍길동");
    }
}
