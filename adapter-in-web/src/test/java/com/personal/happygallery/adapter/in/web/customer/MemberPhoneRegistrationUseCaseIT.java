package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.UpdateMemberPhoneRequest;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerAuthenticationFilter;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerStepUpAuthenticationStore;
import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase;
import com.personal.happygallery.application.booking.port.in.MemberBookingUseCase;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialLoginCommand;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.user.SocialProvider;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static com.personal.happygallery.support.TestFixtures.acceptedPolicies;
import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class MemberPhoneRegistrationUseCaseIT {

    private static final String PHONE = "01012345678";
    private static final String CHANGED_PHONE = "01087654321";

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @Autowired SocialAuthUseCase socialAuth;
    @Autowired GuestBookingUseCase guestBookingUseCase;
    @Autowired MemberBookingUseCase memberBookingUseCase;
    @Autowired BookingReaderPort bookingReader;
    @Autowired ClassStorePort classStore;
    @Autowired SlotStorePort slotStore;
    @Autowired UserReaderPort userReader;
    @Autowired Clock clock;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired CustomerStepUpAuthenticationStore stepUpAuthenticationStore;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("회원은 인증한 휴대폰 번호를 등록하고 다시 변경할 수 있다")
    @Test
    void socialMemberRegistersVerifiedPhoneBeforePayment() throws Exception {
        User socialUser = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "phone-onboarding-naver-id",
                "phone-onboarding@example.com",
                "소셜 회원",
                acceptedPolicies())).user();
        MockHttpSession session = customerSession(socialUser);
        String prepareBody = """
                {
                  "context": "PASS",
                  "payload": {
                    "type": "PASS",
                    "userId": %d
                  }
                }
                """.formatted(socialUser.getId());

        mockMvc.perform(post("/api/v1/payments/prepare")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prepareBody))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PHONE_VERIFICATION_REQUIRED"));

        String code = guestBookingUseCase.sendVerificationCode(
                PHONE, PhoneVerificationPurpose.MEMBER_PHONE_REGISTRATION).getCode();
        mockMvc.perform(patch("/api/v1/me/phone")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateMemberPhoneRequest(PHONE, code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value(PHONE))
                .andExpect(jsonPath("$.phoneVerified").value(true));

        User registered = userReader.findById(socialUser.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(registered.getPhone()).isEqualTo(PHONE);
            softly.assertThat(registered.isPhoneVerified()).isTrue();
            softly.assertThat(registered.getPhoneEnc()).isNotEqualTo(PHONE);
            softly.assertThat(registered.getPhoneHmac()).isNotBlank();
        });
        String originalPhoneHmac = registered.getPhoneHmac();
        BookingClass bookingClass = classStore.save(
                bookingClass("전화번호 변경 클래스", "CRAFT", 120, 50_000L, 30));
        LocalDateTime startAt = LocalDateTime.now(clock).plusDays(1);
        Slot slot = slotStore.save(
                slot(bookingClass, startAt, startAt.plusHours(2)));
        Booking booking = memberBookingUseCase.createMemberDepositBooking(
                registered.getId(), slot.getId(), DepositPaymentMethod.CARD, 5_000L, 45_000L);
        long originalBookingVersion = booking.getVersion();

        markStepUp(session, registered);
        String changeCode = guestBookingUseCase.sendVerificationCode(
                CHANGED_PHONE, PhoneVerificationPurpose.MEMBER_PHONE_CHANGE).getCode();
        mockMvc.perform(patch("/api/v1/me/phone")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateMemberPhoneRequest(CHANGED_PHONE, changeCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value(CHANGED_PHONE));

        User changed = userReader.findById(socialUser.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(changed.getPhone()).isEqualTo(CHANGED_PHONE);
            softly.assertThat(changed.isPhoneVerified()).isTrue();
            softly.assertThat(changed.getPhoneEnc()).isNotEqualTo(CHANGED_PHONE);
            softly.assertThat(changed.getPhoneHmac()).isNotEqualTo(originalPhoneHmac);
            softly.assertThat(bookingReader.findById(booking.getId()))
                    .hasValueSatisfying(savedBooking -> {
                        softly.assertThat(savedBooking.getOwnerPhoneHmac())
                                .isEqualTo(changed.getPhoneHmac());
                        softly.assertThat(savedBooking.getVersion())
                                .isEqualTo(originalBookingVersion + 1);
                    });
        });

        mockMvc.perform(get("/api/v1/me")
                        .session(session))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("최근 본인 확인이 없는 세션은 기존 휴대폰 번호를 변경할 수 없다")
    @Test
    void rejectsPhoneChangeWithoutRecentAuthentication() throws Exception {
        User member = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "phone-step-up-naver-id",
                "phone-step-up@example.com",
                "본인 확인 회원",
                acceptedPolicies())).user();
        String registrationCode = guestBookingUseCase.sendVerificationCode(
                PHONE, PhoneVerificationPurpose.MEMBER_PHONE_REGISTRATION).getCode();
        mockMvc.perform(patch("/api/v1/me/phone")
                        .with(csrf())
                        .session(customerSession(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateMemberPhoneRequest(PHONE, registrationCode))))
                .andExpect(status().isOk());

        User registered = userReader.findById(member.getId()).orElseThrow();
        String changeCode = guestBookingUseCase.sendVerificationCode(
                CHANGED_PHONE, PhoneVerificationPurpose.MEMBER_PHONE_CHANGE).getCode();

        mockMvc.perform(patch("/api/v1/me/phone")
                        .with(csrf())
                        .session(customerSessionWithoutStepUp(registered))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateMemberPhoneRequest(CHANGED_PHONE, changeCode))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REAUTHENTICATION_REQUIRED"));
    }

    @DisplayName("다른 회원이 사용하는 휴대폰 번호로 변경할 수 없다")
    @Test
    void rejectPhoneAlreadyUsedByAnotherMember() throws Exception {
        User owner = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "phone-owner-naver-id",
                "phone-owner@example.com",
                "번호 소유자",
                acceptedPolicies())).user();
        String ownerCode = guestBookingUseCase.sendVerificationCode(
                PHONE, PhoneVerificationPurpose.MEMBER_PHONE_REGISTRATION).getCode();
        mockMvc.perform(patch("/api/v1/me/phone")
                        .with(csrf())
                        .session(customerSession(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateMemberPhoneRequest(PHONE, ownerCode))))
                .andExpect(status().isOk());

        User another = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "phone-another-naver-id",
                "phone-another@example.com",
                "다른 회원",
                acceptedPolicies())).user();
        String anotherCode = guestBookingUseCase.sendVerificationCode(
                PHONE, PhoneVerificationPurpose.MEMBER_PHONE_REGISTRATION).getCode();

        mockMvc.perform(patch("/api/v1/me/phone")
                        .with(csrf())
                        .session(customerSession(another))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateMemberPhoneRequest(PHONE, anotherCode))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHONE_ALREADY_IN_USE"));
    }

    @DisplayName("새 전화번호의 비회원 예약과 겹치면 회원 예약 식별자와 전화번호 변경을 함께 롤백한다")
    @Test
    void rejectPhoneChangeConflictingWithGuestBooking() throws Exception {
        User member = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "phone-booking-owner-naver-id",
                "phone-booking-owner@example.com",
                "예약 회원",
                acceptedPolicies())).user();
        MockHttpSession session = customerSession(member);
        String registrationCode = guestBookingUseCase.sendVerificationCode(
                PHONE, PhoneVerificationPurpose.MEMBER_PHONE_REGISTRATION).getCode();
        mockMvc.perform(patch("/api/v1/me/phone")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateMemberPhoneRequest(PHONE, registrationCode))))
                .andExpect(status().isOk());

        BookingClass bookingClass = classStore.save(
                bookingClass("번호 충돌 클래스", "CRAFT", 120, 50_000L, 30));
        LocalDateTime startAt = LocalDateTime.now(clock).plusDays(1);
        Slot slot = slotStore.save(
                slot(bookingClass, startAt, startAt.plusHours(2)));
        Booking memberBooking = memberBookingUseCase.createMemberDepositBooking(
                member.getId(), slot.getId(), DepositPaymentMethod.CARD, 5_000L, 45_000L);
        String guestCode = guestBookingUseCase.sendVerificationCode(
                CHANGED_PHONE, PhoneVerificationPurpose.GUEST_BOOKING).getCode();
        guestBookingUseCase.createGuestBooking(
                new GuestBookingUseCase.CreateGuestBookingCommand(
                        CHANGED_PHONE, guestCode, "비회원 예약자", slot.getId(),
                        DepositPaymentMethod.CARD, 5_000L, 45_000L));

        markStepUp(session, userReader.findById(member.getId()).orElseThrow());
        String changeCode = guestBookingUseCase.sendVerificationCode(
                CHANGED_PHONE, PhoneVerificationPurpose.MEMBER_PHONE_CHANGE).getCode();
        mockMvc.perform(patch("/api/v1/me/phone")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateMemberPhoneRequest(CHANGED_PHONE, changeCode))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_BOOKING"));

        User unchanged = userReader.findById(member.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(unchanged.getPhone()).isEqualTo(PHONE);
            softly.assertThat(bookingReader.findById(memberBooking.getId()))
                    .hasValueSatisfying(savedBooking ->
                            softly.assertThat(savedBooking.getOwnerPhoneHmac())
                                    .isEqualTo(unchanged.getPhoneHmac()));
        });
    }

    private MockHttpSession customerSession(User user) {
        MockHttpSession session = customerSessionWithoutStepUp(user);
        markStepUp(session, user);
        return session;
    }

    private MockHttpSession customerSessionWithoutStepUp(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                CustomerAuthenticationFilter.CUSTOMER_USER_ID_SESSION_ATTRIBUTE,
                user.getId());
        session.setAttribute(
                CustomerAuthenticationFilter.CUSTOMER_CREDENTIAL_VERSION_SESSION_ATTRIBUTE,
                user.getCredentialVersion());
        return session;
    }

    private void markStepUp(MockHttpSession session, User user) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        stepUpAuthenticationStore.markVerified(
                request, user.getId(), user.getCredentialVersion());
    }
}
