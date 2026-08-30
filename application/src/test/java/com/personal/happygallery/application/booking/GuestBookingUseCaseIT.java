package com.personal.happygallery.application.booking;

import com.personal.happygallery.adapter.in.web.booking.dto.SendVerificationRequest;
import com.personal.happygallery.adapter.in.web.payment.dto.ConfirmPaymentRequest;
import com.personal.happygallery.adapter.out.persistence.booking.GuestRepository;
import com.personal.happygallery.adapter.out.persistence.booking.PhoneVerificationRepository;
import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.GuestPersonalDataProtector;
import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationSender;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.support.BookingStateProbe;
import com.personal.happygallery.support.BookingTestHelper;
import com.personal.happygallery.support.PaymentTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import static com.personal.happygallery.support.TestFixtures.acceptedPolicies;
import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static com.personal.happygallery.support.TestFixtures.slot;
import static com.personal.happygallery.support.PaymentRequestFixtures.prepareRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class GuestBookingUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired GuestBookingUseCase guestBookingUseCase;
    @Autowired PhoneVerificationReaderPort phoneVerificationReaderPort;
    @Autowired PhoneVerificationRepository phoneVerificationRepository;
    @Autowired GuestStorePort guestStorePort;
    @Autowired GuestRepository guestRepository;
    @Autowired GuestPersonalDataProtector guestPersonalDataProtector;
    @Autowired BookingReaderPort bookingReaderPort;
    @Autowired PaymentAttemptReaderPort paymentAttemptReaderPort;
    @Autowired BookingStateProbe bookingStateProbe;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired ObjectMapper objectMapper;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean PhoneVerificationSender phoneVerificationSender;

    Long classId;
    Long slotId;
    static final String PHONE = "01012345678";
    BookingTestHelper helper;
    PaymentTestHelper paymentHelper;

    @BeforeEach
    void setUp() {
        helper = new BookingTestHelper(mockMvc, phoneVerificationReaderPort, objectMapper);
        paymentHelper = new PaymentTestHelper(mockMvc, objectMapper);
        when(phoneVerificationSender.send(anyString(), anyString())).thenReturn(true);

        BookingClass cls = classStorePort.save(defaultBookingClass());
        classId = cls.getId();
        Slot slot = slotStorePort.save(
                slot(cls, LocalDateTime.of(2026, 3, 2, 10, 0),
                        LocalDateTime.of(2026, 3, 2, 12, 0)));
        slotId = slot.getId();
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearBookingWithPassAndRefundData();
    }

    // -----------------------------------------------------------------------
    // 1. 인증 코드 발송
    // -----------------------------------------------------------------------

    @DisplayName("전화번호 인증코드 발송이 성공한다")
    @Test
    void sendVerification_success() throws Exception {
        mockMvc.perform(post("/api/v1/bookings/phone-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendVerificationRequest(
                                PHONE, PhoneVerificationPurpose.GUEST_BOOKING))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationId").isNumber())
                .andExpect(jsonPath("$.phone").value(PHONE))
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @DisplayName("유효하지 않은 전화번호로 인증코드를 요청하면 400을 반환한다")
    @Test
    void sendVerification_invalidPhone_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/bookings/phone-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "phone": "12345" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @DisplayName("SMS 발송 실패 시 저장 이력은 남지만 인증 코드는 활성화하지 않는다")
    @Test
    void sendVerification_deliveryFailed_keepsInactiveRecord() throws Exception {
        AtomicBoolean transactionActiveDuringSend = new AtomicBoolean(true);
        when(phoneVerificationSender.send(anyString(), anyString())).thenAnswer(invocation -> {
            transactionActiveDuringSend.set(TransactionSynchronizationManager.isActualTransactionActive());
            return false;
        });

        mockMvc.perform(post("/api/v1/bookings/phone-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendVerificationRequest(
                                PHONE, PhoneVerificationPurpose.GUEST_BOOKING))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));

        assertSoftly(softly -> {
            softly.assertThat(transactionActiveDuringSend).isFalse();
            softly.assertThat(phoneVerificationRepository.count()).isEqualTo(1L);
            softly.assertThat(phoneVerificationReaderPort.findLatestUnverifiedCode(
                    PHONE, PhoneVerificationPurpose.GUEST_BOOKING)).isEmpty();
        });
    }

    @DisplayName("새 인증 코드 발송이 성공하면 이전 미소모 코드를 폐기한다")
    @Test
    void sendVerification_reissueInvalidatesPreviousCode() throws Exception {
        helper.sendVerificationAndGetCode(PHONE);
        helper.sendVerificationAndGetCode(PHONE);

        var verifications = phoneVerificationRepository.findAll();
        assertSoftly(softly -> {
            softly.assertThat(verifications).hasSize(2);
            softly.assertThat(verifications)
                    .filteredOn(it -> it.isDelivered() && !it.isVerified())
                    .hasSize(1);
            softly.assertThat(verifications)
                    .filteredOn(it -> it.isVerified())
                    .hasSize(1);
        });
    }

    @DisplayName("같은 전화번호라도 인증 목적별 최신 코드를 독립적으로 조회한다")
    @Test
    void sendVerification_differentPurposes_keepIndependentCodes() throws Exception {
        String bookingCode = helper.sendVerificationAndGetCode(
                PHONE, PhoneVerificationPurpose.GUEST_BOOKING);
        String orderCode = helper.sendVerificationAndGetCode(
                PHONE, PhoneVerificationPurpose.GUEST_ORDER);

        assertSoftly(softly -> {
            softly.assertThat(phoneVerificationReaderPort.findLatestUnverifiedCode(
                            PHONE, PhoneVerificationPurpose.GUEST_BOOKING))
                    .map(PhoneVerification::getCode)
                    .contains(bookingCode);
            softly.assertThat(phoneVerificationReaderPort.findLatestUnverifiedCode(
                            PHONE, PhoneVerificationPurpose.GUEST_ORDER))
                    .map(PhoneVerification::getCode)
                    .contains(orderCode);
        });
    }

    @DisplayName("먼저 발급한 인증 코드의 발송 완료가 늦어도 최신 발급 코드만 활성화한다")
    @Test
    void sendVerification_outOfOrderDeliveryCompletion_keepsLatestIssuedCode() throws Exception {
        CountDownLatch firstSendStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstSend = new CountDownLatch(1);
        AtomicBoolean firstInvocation = new AtomicBoolean(true);
        when(phoneVerificationSender.send(anyString(), anyString())).thenAnswer(invocation -> {
            if (firstInvocation.compareAndSet(true, false)) {
                firstSendStarted.countDown();
                if (!releaseFirstSend.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("첫 번째 SMS 발송 대기가 시간 안에 해제되지 않았습니다.");
                }
            }
            return true;
        });

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<PhoneVerification> first = executor.submit(
                    () -> guestBookingUseCase.sendVerificationCode(
                            PHONE, PhoneVerificationPurpose.GUEST_BOOKING));
            assertThat(firstSendStarted.await(10, TimeUnit.SECONDS)).isTrue();

            Future<PhoneVerification> second = executor.submit(
                    () -> guestBookingUseCase.sendVerificationCode(
                            PHONE, PhoneVerificationPurpose.GUEST_BOOKING));
            PhoneVerification latestIssued = second.get(10, TimeUnit.SECONDS);
            releaseFirstSend.countDown();
            PhoneVerification earlierIssued = first.get(10, TimeUnit.SECONDS);

            List<PhoneVerification> stored = phoneVerificationRepository.findAll().stream()
                    .sorted(Comparator.comparing(PhoneVerification::getId))
                    .toList();
            assertSoftly(softly -> {
                softly.assertThat(earlierIssued.getId()).isLessThan(latestIssued.getId());
                softly.assertThat(stored).hasSize(2);
                softly.assertThat(stored.getFirst().isVerified()).isTrue();
                softly.assertThat(stored.getFirst().isDelivered()).isFalse();
                softly.assertThat(stored.getLast().isVerified()).isFalse();
                softly.assertThat(stored.getLast().isDelivered()).isTrue();
                softly.assertThat(phoneVerificationReaderPort.findLatestUnverifiedCode(
                                PHONE, PhoneVerificationPurpose.GUEST_BOOKING))
                        .map(PhoneVerification::getId)
                        .contains(latestIssued.getId());
            });
        } finally {
            releaseFirstSend.countDown();
        }
    }

    // -----------------------------------------------------------------------
    // 2. 게스트 예약 생성
    // -----------------------------------------------------------------------

    @DisplayName("게스트 예약 생성이 성공한다")
    @Test
    void createGuestBooking_success() throws Exception {
        String verificationCode = helper.sendVerificationAndGetCode(PHONE);
        PaymentTestHelper.PreparedPayment prepared = paymentHelper.preparePayment(
                PaymentContext.BOOKING,
                bookingPayload(PHONE, verificationCode, "홍길동", slotId, DepositPaymentMethod.CARD));
        String storedPayload = paymentAttemptReaderPort.findByOrderIdExternal(prepared.orderId())
                .orElseThrow()
                .getPayloadEnc();

        assertThat(storedPayload)
                .doesNotContain(PHONE)
                .doesNotContain(verificationCode);

        PaymentTestHelper.ConfirmedPayment confirmed = paymentHelper.confirmPayment(
                prepared, "test-payment-key");
        BookingTestHelper.CreatedBooking created = new BookingTestHelper.CreatedBooking(
                confirmed.domainId(), confirmed.accessToken());

        mockMvc.perform(get("/api/v1/bookings/{id}", created.bookingId())
                        .header("X-Access-Token", created.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").isNumber())
                .andExpect(jsonPath("$.bookingNumber").value(startsWith("BK-")))
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.depositAmount").value(5000))
                .andExpect(jsonPath("$.balanceAmount").value(45000))
                .andExpect(jsonPath("$.className").value("향수 클래스"));

        // DB 저장 확인
        assertSoftly(softly -> {
            softly.assertThat(bookingReaderPort.findById(created.bookingId())).isPresent();
            softly.assertThat(phoneVerificationReaderPort.findLatestUnverifiedCode(
                    PHONE, PhoneVerificationPurpose.GUEST_BOOKING)).isEmpty();
        });
    }

    @DisplayName("동일 전화번호의 게스트를 동시에 생성해도 하나만 저장한다")
    @Test
    void createGuest_concurrently_reusesSameGuest() throws Exception {
        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    return getOrCreateGuestInTx("홍길동").getId();
                }));
            }

            startLatch.countDown();
            List<Long> guestIds = new ArrayList<>();
            for (Future<Long> future : futures) {
                guestIds.add(future.get(10, TimeUnit.SECONDS));
            }

            Guest stored = guestRepository.findByPhoneHmac(guestPersonalDataProtector.indexPhone(PHONE)).orElseThrow();
            String storedPhoneEnc = stored.getPhoneEnc();
            Guest reused = getOrCreateGuestInTx("변경된 이름");

            assertSoftly(softly -> {
                softly.assertThat(guestIds).containsOnly(guestIds.getFirst());
                softly.assertThat(guestRepository.count()).isEqualTo(1L);
                softly.assertThat(reused.getId()).isEqualTo(stored.getId());
                softly.assertThat(guestPersonalDataProtector.decryptName(reused)).isEqualTo("홍길동");
                softly.assertThat(reused.getPhoneEnc()).isEqualTo(storedPhoneEnc);
                softly.assertThat(guestPersonalDataProtector.decryptPhone(reused)).isEqualTo(PHONE);
            });
        } finally {
            executor.shutdownNow();
        }
    }

    // Proof: 계좌이체로 예약금 결제 시도 → 422 차단
    @DisplayName("게스트 예약에서 계좌이체 결제를 요청하면 422를 반환한다")
    @Test
    void createGuestBooking_bankTransfer_returns422() throws Exception {
        String code = helper.sendVerificationAndGetCode(PHONE);

        mockMvc.perform(post("/api/v1/payments/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prepareRequest(
                                PaymentContext.BOOKING,
                                bookingPayload(PHONE, code, "홍길동", slotId,
                                        DepositPaymentMethod.BANK_TRANSFER)))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PAYMENT_METHOD_NOT_ALLOWED"));

        // Proof: 예약 레코드 미생성
        assertThat(bookingStateProbe.bookingCount()).isEqualTo(0L);
    }

    @DisplayName("게스트가 중복 예약을 시도하면 409를 반환한다")
    @Test
    void createGuestBooking_duplicateBooking_returns409() throws Exception {
        // 첫 번째 예약 성공
        helper.createVerifiedCardBooking(PHONE, slotId);

        // 동일 전화번호 + 동일 슬롯 재예약 → 409
        String code2 = helper.sendVerificationAndGetCode(PHONE);
        PaymentTestHelper.PreparedPayment prepared = paymentHelper.preparePayment(
                PaymentContext.BOOKING,
                bookingPayload(PHONE, code2, "홍길동", slotId, DepositPaymentMethod.CARD));
        mockMvc.perform(post("/api/v1/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(PaymentTestHelper.PAYMENT_STATUS_TOKEN_HEADER, prepared.statusToken())
                        .content(confirmRequest(prepared, "test-payment-key")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_BOOKING"));
    }

    @DisplayName("게스트 예약 시 인증코드가 틀리면 400을 반환한다")
    @Test
    void createGuestBooking_wrongCode_returns400() throws Exception {
        helper.sendVerificationAndGetCode(PHONE); // 코드 발급 (소모 안 함)

        mockMvc.perform(post("/api/v1/payments/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prepareRequest(
                                PaymentContext.BOOKING,
                                bookingPayload(
                                        PHONE, "000000", "홍길동", slotId,
                                        DepositPaymentMethod.CARD)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PHONE_VERIFICATION_FAILED"));
    }

    @DisplayName("게스트 예약 시 슬롯 정원 초과면 409를 반환한다")
    @Test
    void createGuestBooking_capacityExceeded_returns409() throws Exception {
        // 8명 예약으로 정원 만석 만들기
        for (int i = 0; i < 8; i++) {
            String phone = "0101234567" + i;
            String code = helper.sendVerificationAndGetCode(phone);
            paymentHelper.createGuestBooking(phone, code, "예약자%d".formatted(i), slotId, 1);
        }

        // 9번째 예약 → 정원 초과
        String phone = "01099999999";
        String code = helper.sendVerificationAndGetCode(phone);
        mockMvc.perform(post("/api/v1/payments/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prepareRequest(
                                PaymentContext.BOOKING,
                                bookingPayload(
                                        phone, code, "초과예약자", slotId,
                                        DepositPaymentMethod.CARD)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITY_EXCEEDED"));
    }

    // -----------------------------------------------------------------------
    // 3. 예약 조회
    // -----------------------------------------------------------------------

    @DisplayName("토큰으로 예약 조회가 성공한다")
    @Test
    void getBooking_success() throws Exception {
        BookingTestHelper.CreatedBooking created = helper.createVerifiedCardBooking(PHONE, slotId);

        mockMvc.perform(get("/api/v1/bookings/{id}", created.bookingId())
                        .header("X-Access-Token", created.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(created.bookingId()))
                .andExpect(jsonPath("$.bookingNumber").value("BK-%08d".formatted(created.bookingId())))
                .andExpect(jsonPath("$.classId").value(classId))
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.guestName").value("홍길동"))
                .andExpect(jsonPath("$.guestPhone").value("010****5678"))
                .andExpect(jsonPath("$.className").value("향수 클래스"));
    }

    @DisplayName("잘못된 토큰으로 예약 조회 시 404를 반환한다")
    @Test
    void getBooking_wrongToken_returns404() throws Exception {
        BookingTestHelper.CreatedBooking created = helper.createVerifiedCardBooking(PHONE, slotId);

        mockMvc.perform(get("/api/v1/bookings/{id}", created.bookingId())
                        .header("X-Access-Token", "invalid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private BookingPayload bookingPayload(String phone,
                                          String verificationCode,
                                          String name,
                                          Long requestedSlotId,
                                          DepositPaymentMethod paymentMethod) {
        return new BookingPayload(
                null,
                phone,
                verificationCode,
                name,
                requestedSlotId,
                null,
                paymentMethod,
                acceptedPolicies());
    }

    private String confirmRequest(PaymentTestHelper.PreparedPayment prepared, String paymentKey) throws Exception {
        return objectMapper.writeValueAsString(
                new ConfirmPaymentRequest(paymentKey, prepared.orderId(), prepared.amount()));
    }

    private Guest getOrCreateGuestInTx(String name) {
        return new TransactionTemplate(transactionManager).execute(status ->
                guestStorePort.getOrCreateByPhoneHmac(guestPersonalDataProtector.newGuest(name, PHONE)));
    }
}
