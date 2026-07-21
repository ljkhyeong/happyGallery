package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.notification.NotificationService;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase;
import com.personal.happygallery.application.customer.port.in.GuestClaimUseCase;
import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.order.OrderService;
import com.personal.happygallery.adapter.in.web.customer.dto.ClaimGuestRecordsRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.GuestRecordRecoveryResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.RecoverGuestRecordsRequest;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.BookingTestHelper;
import com.personal.happygallery.support.CustomerTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.TestFixtures;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.LongStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class CustomerGuestClaimUseCaseIT {

    @Autowired WebApplicationContext context;
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
    @Autowired GuestTokenService guestTokenService;
    @Autowired GuestClaimUseCase guestClaimUseCase;
    @Autowired CustomerAccountLifecycleUseCase accountLifecycleUseCase;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean NotificationService notificationService;

    MockMvc mockMvc;
    CustomerTestHelper customerHelper;

    @BeforeEach
    void setUp() {
        cleanup();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSessionRepositoryFilter)
                .apply(springSecurity())
                .build();
        customerHelper = new CustomerTestHelper(mockMvc, objectMapper, phoneVerificationReaderPort);
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("휴대폰 소유 확인을 마친 회원은 같은 번호의 비회원 주문과 예약을 가져올 수 있다")
    @Test
    void verifyAndClaimGuestRecords() throws Exception {
        String email = "member@example.com";
        Guest guest = guestStorePort.save(TestFixtures.guest("비회원", "01012345678"));

        Product product = productStorePort.save(new Product("테스트 상품", ProductType.READY_STOCK, 29_000L));
        inventoryStorePort.save(new Inventory(product, 5));
        Order order = orderService.createPaidOrder(
                guest.getId(),
                List.of(new OrderService.OrderItemRequest(
                        product.getId(), product.getName(), 1, 29_000L))).order();

        BookingClass bookingClass = classStorePort.save(TestFixtures.defaultBookingClass());
        Slot slot = slotStorePort.save(TestFixtures.slot(
                bookingClass,
                BookingTestHelper.FUTURE,
                BookingTestHelper.FUTURE.plusHours(2)));
        Booking booking = bookingStorePort.save(Booking.forGuestDeposit(
                guest, slot, 10_000L, 40_000L,
                DepositPaymentMethod.CARD,
                "guest-claim-access-token"));

        Cookie sessionCookie = customerHelper.signupAndGetSessionCookie(email, "010-1234-5678");
        User user = userReaderPort.findByEmail(email).orElseThrow();

        mockMvc.perform(get("/api/v1/me/guest-claims/preview")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneVerified").value(true))
                .andExpect(jsonPath("$.orders[0].orderId").value(order.getId()))
                .andExpect(jsonPath("$.bookings[0].bookingId").value(booking.getId()));

        assertThat(userReaderPort.findById(user.getId()).orElseThrow().isPhoneVerified()).isTrue();

        mockMvc.perform(post("/api/v1/me/guest-claims")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ClaimGuestRecordsRequest(
                                List.of(order.getId()), List.of(booking.getId())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimedOrderCount").value(1))
                .andExpect(jsonPath("$.claimedBookingCount").value(1));

        Order claimedOrder = orderReaderPort.findById(order.getId()).orElseThrow();
        Booking claimedBooking = bookingReaderPort.findById(booking.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(claimedOrder.getUserId()).isEqualTo(user.getId());
            softly.assertThat(claimedOrder.getGuestId()).isNull();
            softly.assertThat(claimedOrder.getAccessToken()).isNull();
            softly.assertThat(claimedBooking.getUserId()).isEqualTo(user.getId());
            softly.assertThat(claimedBooking.getGuest()).isNull();
            softly.assertThat(claimedBooking.getAccessToken()).isNull();
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

    @DisplayName("휴대폰 재인증을 마친 비회원은 주문과 예약의 만료된 접근 토큰을 함께 복구한다")
    @Test
    void recoverGuestRecords_afterPhoneVerification() throws Exception {
        String phone = "01012345678";
        Guest guest = guestStorePort.save(TestFixtures.guest("비회원", phone));

        Product product = productStorePort.save(new Product("복구 상품", ProductType.READY_STOCK, 29_000L));
        inventoryStorePort.save(new Inventory(product, 5));
        OrderService.OrderCreationResult createdOrder = orderService.createPaidOrder(
                guest.getId(),
                List.of(new OrderService.OrderItemRequest(
                        product.getId(), product.getName(), 1, 29_000L)));

        BookingClass bookingClass = classStorePort.save(TestFixtures.defaultBookingClass());
        Slot slot = slotStorePort.save(TestFixtures.slot(
                bookingClass,
                BookingTestHelper.FUTURE,
                BookingTestHelper.FUTURE.plusHours(2)));
        GuestTokenService.IssuedToken oldBookingToken = guestTokenService.issue();
        Booking booking = bookingStorePort.save(Booking.forGuestDeposit(
                guest, slot, 10_000L, 40_000L,
                DepositPaymentMethod.CARD,
                oldBookingToken.tokenHash()));

        BookingTestHelper bookingHelper = new BookingTestHelper(
                mockMvc, phoneVerificationReaderPort, objectMapper);
        String verificationCode = bookingHelper.sendVerificationAndGetCode(phone);

        String body = mockMvc.perform(post("/api/v1/guest-records/recovery")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RecoverGuestRecordsRequest(phone, verificationCode))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        GuestRecordRecoveryResponse recovered = objectMapper.readValue(
                body, GuestRecordRecoveryResponse.class);

        assertSoftly(softly -> {
            softly.assertThat(recovered.accessToken()).isNotBlank();
            softly.assertThat(recovered.expiresAt()).isNotNull();
            softly.assertThat(recovered.orders())
                    .extracting(GuestRecordRecoveryResponse.OrderSummary::orderId)
                    .containsExactly(createdOrder.order().getId());
            softly.assertThat(recovered.bookings())
                    .extracting(GuestRecordRecoveryResponse.BookingSummary::bookingId)
                    .containsExactly(booking.getId());
        });

        mockMvc.perform(get("/api/v1/orders/{id}", createdOrder.order().getId())
                        .header("X-Access-Token", recovered.accessToken()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/bookings/{id}", booking.getId())
                        .header("X-Access-Token", recovered.accessToken()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/orders/{id}", createdOrder.order().getId())
                        .header("X-Access-Token", createdOrder.rawAccessToken()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/bookings/{id}", booking.getId())
                        .header("X-Access-Token", oldBookingToken.rawToken()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/guest-records/recovery")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RecoverGuestRecordsRequest(phone, verificationCode))))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("회원에게 같은 슬롯의 활성 예약이 있으면 비회원 예약을 가져올 수 없다")
    @Test
    void claimGuestBooking_conflictsWithMemberBooking() throws Exception {
        String email = "claim-conflict@example.com";
        String phone = "01012345678";
        Guest guest = guestStorePort.save(TestFixtures.guest("비회원", phone));
        Cookie sessionCookie = customerHelper.signupAndGetSessionCookie(email, "010-1234-5678");
        User user = userReaderPort.findByEmail(email).orElseThrow();

        BookingClass bookingClass = classStorePort.save(TestFixtures.defaultBookingClass());
        Slot slot = slotStorePort.save(TestFixtures.slot(
                bookingClass,
                BookingTestHelper.FUTURE,
                BookingTestHelper.FUTURE.plusHours(2)));
        bookingStorePort.save(Booking.forMemberDeposit(
                user.getId(), slot, 10_000L, 40_000L, DepositPaymentMethod.CARD));
        Booking guestBooking = bookingStorePort.save(Booking.forGuestDeposit(
                guest, slot, 10_000L, 40_000L,
                DepositPaymentMethod.CARD, "claim-conflict-token"));

        mockMvc.perform(post("/api/v1/me/guest-claims")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ClaimGuestRecordsRequest(
                                List.of(), List.of(guestBooking.getId())))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_BOOKING"));

        Booking unchanged = bookingReaderPort.findById(guestBooking.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(unchanged.getUserId()).isNull();
            softly.assertThat(unchanged.getGuest().getId()).isEqualTo(guest.getId());
        });
    }

    @DisplayName("탈퇴가 먼저 회원을 잠그면 비회원 이력 가져오기는 탈퇴 계정에 귀속하지 않는다")
    @Test
    void claimGuestRecords_doesNotRaceWithWithdrawal() throws Exception {
        String email = "claim-withdrawal@example.com";
        String phone = "01012345678";
        Guest guest = guestStorePort.save(TestFixtures.guest("비회원", phone));
        customerHelper.signupAndGetSessionCookie(email, phone);
        User user = userReaderPort.findByEmail(email).orElseThrow();

        Product product = productStorePort.save(
                new Product("탈퇴 경합 상품", ProductType.READY_STOCK, 29_000L));
        inventoryStorePort.save(new Inventory(product, 1));
        Order order = orderService.createPaidOrder(
                guest.getId(),
                List.of(new OrderService.OrderItemRequest(
                        product.getId(), product.getName(), 1, product.getPrice())))
                .order();

        CountDownLatch withdrawalApplied = new CountDownLatch(1);
        CountDownLatch allowWithdrawalCommit = new CountDownLatch(1);
        CountDownLatch claimStarted = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var withdrawal = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                accountLifecycleUseCase.withdraw(user.getId());
                withdrawalApplied.countDown();
                await(allowWithdrawalCommit);
            }));
            assertThat(withdrawalApplied.await(5, TimeUnit.SECONDS)).isTrue();

            var claim = executor.submit(() -> {
                claimStarted.countDown();
                return guestClaimUseCase.claim(user.getId(), List.of(order.getId()), List.of());
            });
            assertThat(claimStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> claim.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            allowWithdrawalCommit.countDown();

            withdrawal.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> claim.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(NotFoundException.class);
        } finally {
            allowWithdrawalCommit.countDown();
        }

        Order unchanged = orderReaderPort.findById(order.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(userReaderPort.findById(user.getId())).isEmpty();
            softly.assertThat(unchanged.getUserId()).isNull();
            softly.assertThat(unchanged.getGuestId()).isEqualTo(guest.getId());
        });
    }

    @DisplayName("비회원 이력 가져오기는 주문과 예약 ID를 각각 100건까지만 받는다")
    @Test
    void claimGuestRecords_rejectsTooManyIds() throws Exception {
        Cookie sessionCookie = customerHelper.signupAndGetSessionCookie(
                "claim-limit@example.com", "010-1234-5678");
        List<Long> tooManyIds = LongStream.rangeClosed(1, 101).boxed().toList();

        mockMvc.perform(post("/api/v1/me/guest-claims")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ClaimGuestRecordsRequest(tooManyIds, List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시성 테스트 대기 시간이 초과됐습니다.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시성 테스트 대기가 중단됐습니다.", e);
        }
    }

}
