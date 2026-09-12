package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.booking.BookingController;
import com.personal.happygallery.adapter.in.web.customer.MeGroupInquiryController;
import com.personal.happygallery.adapter.in.web.inquiry.GroupInquiryController;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.adapter.in.web.security.bot.BotProtectionController;
import com.personal.happygallery.application.booking.port.in.BookingCancelUseCase;
import com.personal.happygallery.application.booking.port.in.BookingQueryUseCase;
import com.personal.happygallery.application.booking.port.in.BookingRescheduleUseCase;
import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase;
import com.personal.happygallery.application.customer.GuestPersonalDataProtector;
import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import com.personal.happygallery.application.security.DefaultBotProtectionService;
import com.personal.happygallery.application.security.port.out.BotChallengeVerifier;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BotProtectionApiRestDocsTest extends RestDocsTestSupport {
    private final BotChallengeVerifier verifier = mock(BotChallengeVerifier.class);
    private final GroupInquiryUseCase inquiries = mock(GroupInquiryUseCase.class);
    private final GuestBookingUseCase bookings = mock(GuestBookingUseCase.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider documentation) {
        when(verifier.siteKey()).thenReturn("public-site-key");
        var protection = new DefaultBotProtectionService(verifier);
        mvc = mockMvc(documentation,
                new BotProtectionController(protection),
                new GroupInquiryController(inquiries, protection),
                new MeGroupInquiryController(inquiries, protection),
                new BookingController(bookings, mock(BookingQueryUseCase.class),
                        mock(BookingRescheduleUseCase.class), mock(BookingCancelUseCase.class),
                        mock(GuestPersonalDataProtector.class), mock(SubjectRateLimitGuard.class),
                        protection, RestDocsFixtures.clock()));
    }

    @Test
    @DisplayName("자동 입력 방지 화면 설정에는 공개 키만 반환한다")
    void publicConfiguration() throws Exception {
        mvc.perform(get("/api/v1/bot-protection"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.siteKey").value("public-site-key"))
                .andExpect(jsonPath("$.secretKey").doesNotExist());
    }

    @Test
    @DisplayName("자동 입력 방지가 꺼져 있으면 공개 키를 null로 반환한다")
    void disabledConfiguration() throws Exception {
        when(verifier.siteKey()).thenReturn(null);
        mvc.perform(get("/api/v1/bot-protection"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.siteKey").value(nullValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/group-inquiries", "/api/v1/me/group-inquiries", "/api/v1/bookings/phone-verifications"})
    @DisplayName("필수 토큰이 없으면 문의 저장과 인증문자 발송을 실행하지 않는다")
    void missingTokenStopsWork(String path) throws Exception {
        mvc.perform(post(path).with(customerUser()).contentType(APPLICATION_JSON).content(body(path)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        verifyNoInteractions(inquiries, bookings);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/group-inquiries", "/api/v1/me/group-inquiries", "/api/v1/bookings/phone-verifications"})
    @DisplayName("만료되거나 재사용된 토큰으로 실제 작업을 실행하지 않는다")
    void rejectedTokenStopsWork(String path) throws Exception {
        mvc.perform(post(path).with(customerUser()).header("X-Bot-Token", "spent-token")
                        .contentType(APPLICATION_JSON).content(body(path)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(inquiries, bookings);
    }

    @Test
    @DisplayName("검증 API 장애는 503으로 알리고 인증문자를 발송하지 않는다")
    void outageStopsSms() throws Exception {
        when(verifier.verify(any(), any())).thenThrow(new HappyGalleryException(ErrorCode.SERVICE_UNAVAILABLE));
        String path = "/api/v1/bookings/phone-verifications";
        mvc.perform(post(path).header("X-Bot-Token", "token").contentType(APPLICATION_JSON).content(body(path)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
        verifyNoInteractions(bookings);
    }

    private String body(String path) {
        return path.endsWith("phone-verifications")
                ? "{\"phone\":\"01012345678\",\"purpose\":\"GUEST_BOOKING\"}"
                : GroupInquiryRestDocsFixtures.REQUEST;
    }
}
