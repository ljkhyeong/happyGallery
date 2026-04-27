package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.booking.BookingController;
import com.personal.happygallery.adapter.in.web.booking.ClassController;
import com.personal.happygallery.adapter.in.web.booking.SlotController;
import com.personal.happygallery.adapter.in.web.monitoring.ClientMonitoringController;
import com.personal.happygallery.adapter.in.web.notice.NoticeController;
import com.personal.happygallery.adapter.in.web.order.OrderController;
import com.personal.happygallery.adapter.in.web.payment.PaymentController;
import com.personal.happygallery.adapter.in.web.product.ProductController;
import com.personal.happygallery.adapter.in.web.product.ProductQnaController;
import com.personal.happygallery.application.booking.port.in.BookingCancelUseCase;
import com.personal.happygallery.application.booking.port.in.BookingQueryUseCase;
import com.personal.happygallery.application.booking.port.in.BookingRescheduleUseCase;
import com.personal.happygallery.application.booking.port.in.ClassQueryUseCase;
import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase;
import com.personal.happygallery.application.booking.port.in.SlotQueryUseCase;
import com.personal.happygallery.application.monitoring.port.in.ClientMonitoringUseCase;
import com.personal.happygallery.application.notice.port.in.NoticeQueryUseCase;
import com.personal.happygallery.application.order.port.in.OrderQueryUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.product.ProductFilter;
import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notice.Notice;
import com.personal.happygallery.domain.payment.PaymentContext;
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
    private OrderQueryUseCase orderQueryUseCase;
    private PaymentPrepareUseCase paymentPrepareUseCase;
    private PaymentConfirmUseCase paymentConfirmUseCase;
    private NoticeQueryUseCase noticeQueryUseCase;
    private ClientMonitoringUseCase clientMonitoringUseCase;

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
        orderQueryUseCase = mock(OrderQueryUseCase.class);
        paymentPrepareUseCase = mock(PaymentPrepareUseCase.class);
        paymentConfirmUseCase = mock(PaymentConfirmUseCase.class);
        noticeQueryUseCase = mock(NoticeQueryUseCase.class);
        clientMonitoringUseCase = mock(ClientMonitoringUseCase.class);

        ProductQueryUseCase.ProductWithInventory product = RestDocsFixtures.productWithInventory();
        ProductQnaUseCase.QnaWithAuthor qna = qna();
        BookingClass bookingClass = RestDocsFixtures.bookingClass();
        Slot slot = RestDocsFixtures.slot();
        PhoneVerification phoneVerification = RestDocsFixtures.phoneVerification();
        Booking booking = RestDocsFixtures.booking();
        OrderQueryUseCase.OrderDetail orderDetail = RestDocsFixtures.orderDetail();
        Notice notice = RestDocsFixtures.notice();

        when(productQueryUseCase.listActiveProducts(any(ProductFilter.class)))
                .thenReturn(List.of(product));
        when(productQueryUseCase.listActiveCategories()).thenReturn(List.of("CANDLE", "PERFUME"));
        when(productQueryUseCase.getProduct(1L)).thenReturn(product);
        when(qnaUseCase.listByProduct(1L)).thenReturn(List.of(qna));
        when(qnaUseCase.verifyAndGet(eq(5L), any())).thenReturn(qna);
        when(classQueryUseCase.listAll()).thenReturn(List.of(bookingClass));
        when(slotQueryUseCase.listAvailable(any(), any())).thenReturn(List.of(slot));
        when(guestBookingUseCase.sendVerificationCode(any())).thenReturn(phoneVerification);
        when(bookingQueryUseCase.getBookingByToken(eq(100L), any())).thenReturn(booking);
        when(bookingRescheduleUseCase.rescheduleBooking(eq(100L), any(), eq(42L)))
                .thenReturn(booking);
        when(bookingCancelUseCase.cancelBooking(eq(100L), any()))
                .thenReturn(new BookingCancelUseCase.CancelResult(booking, true));
        when(orderQueryUseCase.getOrderByToken(eq(200L), any())).thenReturn(orderDetail);
        when(paymentPrepareUseCase.prepare(any()))
                .thenReturn(new PaymentPrepareUseCase.PrepareResult("pay_20260501_0001", 39000L, PaymentContext.ORDER));
        when(paymentConfirmUseCase.confirm(any()))
                .thenReturn(new PaymentConfirmUseCase.ConfirmResult(PaymentContext.ORDER, 200L, "guest-access-token"));
        when(noticeQueryUseCase.listAll()).thenReturn(List.of(notice));
        when(noticeQueryUseCase.getDetail(1L)).thenReturn(notice);

        mockMvc = mockMvc(restDocumentation,
                new ProductController(productQueryUseCase),
                new ProductQnaController(qnaUseCase),
                new ClassController(classQueryUseCase),
                new SlotController(slotQueryUseCase),
                new BookingController(guestBookingUseCase, bookingQueryUseCase,
                        bookingRescheduleUseCase, bookingCancelUseCase),
                new OrderController(orderQueryUseCase),
                new PaymentController(paymentPrepareUseCase, paymentConfirmUseCase),
                new NoticeController(noticeQueryUseCase),
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
    @DisplayName("공개 상품 QNA 비밀번호 확인 API를 문서화한다")
    void verify_product_qna_password() throws Exception {
        mockMvc.perform(post("/api/v1/products/{productId}/qna/{id}/verify", 1L, 5L)
                        .contentType(jsonContent())
                        .content(json("{\"password\":\"qna-secret\"}")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공개 클래스 목록 API를 문서화한다")
    void list_classes() throws Exception {
        mockMvc.perform(get("/api/v1/classes"))
                .andExpect(status().isOk());
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
    @DisplayName("비회원 휴대폰 인증 발송 API를 문서화한다")
    void send_booking_phone_verification() throws Exception {
        mockMvc.perform(post("/api/v1/bookings/phone-verifications")
                        .contentType(jsonContent())
                        .content(json("{\"phone\":\"01012345678\"}")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비회원 예약 조회 API를 문서화한다")
    void get_guest_booking() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/{bookingId}", 100L)
                        .header("X-Access-Token", "guest-access-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비회원 예약 변경 API를 문서화한다")
    void reschedule_guest_booking() throws Exception {
        mockMvc.perform(patch("/api/v1/bookings/{bookingId}/reschedule", 100L)
                        .header("X-Access-Token", "guest-access-token")
                        .contentType(jsonContent())
                        .content(json("{\"newSlotId\":42}")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비회원 예약 취소 API를 문서화한다")
    void cancel_guest_booking() throws Exception {
        mockMvc.perform(delete("/api/v1/bookings/{bookingId}", 100L)
                        .header("X-Access-Token", "guest-access-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비회원 주문 조회 API를 문서화한다")
    void get_guest_order() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", 200L)
                        .header("X-Access-Token", "guest-access-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("결제 prepare API를 문서화한다")
    void prepare_payment() throws Exception {
        mockMvc.perform(post("/api/v1/payments/prepare")
                        .with(customerUser())
                        .contentType(jsonContent())
                        .content(json("""
                                {
                                  "context": "ORDER",
                                  "payload": {
                                    "type": "ORDER",
                                    "userId": 11,
                                    "items": [{ "productId": 1, "qty": 1 }]
                                  }
                                }
                                """)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("결제 confirm API를 문서화한다")
    void confirm_payment() throws Exception {
        mockMvc.perform(post("/api/v1/payments/confirm")
                        .with(customerUser())
                        .contentType(jsonContent())
                        .content(json("""
                                {
                                  "paymentKey": "toss-payment-key",
                                  "orderId": "pay_20260501_0001",
                                  "amount": 39000
                                }
                                """)))
                .andExpect(status().isOk());
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
                        .contentType(jsonContent())
                        .content(json("""
                                {
                                  "event": "GUEST_LOOKUP_HUB_VIEWED",
                                  "path": "/guest",
                                  "source": "GuestLookupPage",
                                  "target": "primary-cta"
                                }
                                """)))
                .andExpect(status().isNoContent());
    }

    private static ProductQnaUseCase.QnaWithAuthor qna() {
        return new ProductQnaUseCase.QnaWithAuthor(RestDocsFixtures.productQna(), "홍길동");
    }
}
