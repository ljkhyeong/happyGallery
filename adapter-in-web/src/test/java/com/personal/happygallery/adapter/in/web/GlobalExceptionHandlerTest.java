package com.personal.happygallery.adapter.in.web;

import com.personal.happygallery.adapter.in.web.booking.SlotController;
import com.personal.happygallery.adapter.in.web.error.ErrorResponse;
import com.personal.happygallery.adapter.in.web.product.ProductReviewController;
import com.personal.happygallery.application.booking.port.in.SlotQueryUseCase;
import com.personal.happygallery.application.review.port.in.ReviewUseCase;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.product.Product;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Stream;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @DisplayName("낙관적 락 충돌은 대상에 맞는 오류로 매핑된다")
    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @MethodSource("optimisticLockCases")
    void optimisticLock_mapsToTargetError(Class<?> entityType, ErrorCode expected) {
        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLockingFailure(
                new ObjectOptimisticLockingFailureException(entityType.getName(), 1L));

        assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode().value()).isEqualTo(expected.httpStatus);
            softly.assertThat(response.getBody()).isEqualTo(ErrorResponse.of(expected));
        });
    }

    private static Stream<Arguments> optimisticLockCases() {
        return Stream.of(
                Arguments.of(Booking.class, ErrorCode.BOOKING_CONFLICT),
                Arguments.of(Product.class, ErrorCode.CONFLICT));
    }

    @DisplayName("DB 제약 위반은 제약 이름에 맞는 오류로 매핑된다")
    @ParameterizedTest
    @CsvSource({
            "uq_slot_class_start, INVALID_INPUT",
            "bookings.uq_bookings_active_phone_slot, DUPLICATE_BOOKING",
            "uq_users_phone_hmac, PHONE_ALREADY_IN_USE",
            "uq_user_social_accounts_provider_identity, SOCIAL_ACCOUNT_ALREADY_LINKED",
            "uq_user_social_accounts_user_provider, SOCIAL_PROVIDER_ALREADY_LINKED",
            "uq_issued_coupons_user_definition, CONFLICT",
            "uq_unmapped_constraint, INTERNAL_ERROR"
    })
    void dataIntegrity_mapsToConstraintError(
            String constraint,
            ErrorCode expected
    ) {
        ResponseEntity<ErrorResponse> response =
                handler.handleDataIntegrityViolation(constraintViolation(constraint));

        assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode().value()).isEqualTo(expected.httpStatus);
            softly.assertThat(response.getBody()).isEqualTo(ErrorResponse.of(expected));
        });
    }

    @DisplayName("요청 JSON 파싱 실패는 INVALID_INPUT으로 매핑된다")
    @Test
    void httpMessageNotReadable_mapsToInvalidInput() throws Exception {
        var mockMvc = mediaMockMvc();

        mockMvc.perform(post("/test/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("요청 JSON 형식이 올바르지 않습니다."));
    }

    @DisplayName("허용되지 않은 HTTP 메서드는 Allow 헤더와 405 상태를 보존한다")
    @Test
    void methodNotAllowed_preservesStatusAndAllowHeader() throws Exception {
        var mockMvc = mediaMockMvc();

        mockMvc.perform(put("/test/resources"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @DisplayName("지원하지 않는 요청 미디어 타입은 415 상태를 보존한다")
    @Test
    void unsupportedMediaType_preservesStatus() throws Exception {
        var mockMvc = mediaMockMvc();

        mockMvc.perform(post("/test/resources")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @DisplayName("제공할 수 없는 응답 미디어 타입은 406 상태를 보존한다")
    @Test
    void notAcceptable_preservesStatus() throws Exception {
        var mockMvc = mediaMockMvc();

        mockMvc.perform(get("/test/resources").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"));
    }

    @DisplayName("존재하지 않는 경로는 404 상태와 NOT_FOUND 응답을 반환한다")
    @Test
    void notFound_preservesStatus() throws Exception {
        var mockMvc = mediaMockMvc();

        mockMvc.perform(get("/test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @DisplayName("잘못된 슬롯 조회 파라미터는 INVALID_INPUT으로 매핑된다")
    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/slots?classId=1&date=not-a-date",
            "/api/v1/slots?date=2026-01-01"
    })
    void invalidQueryParameter_mapsToInvalidInput(String path) throws Exception {
        var mockMvc = standaloneSetup(new SlotController(mock(SlotQueryUseCase.class)))
                .setControllerAdvice(handler)
                .build();

        mockMvc.perform(get(path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @DisplayName("후기 목록의 범위 밖 경로와 쿼리 파라미터는 INVALID_INPUT으로 매핑된다")
    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/products/0/reviews",
            "/api/v1/products/1/reviews?rating=0"
    })
    void invalidReviewParameter_mapsToInvalidInput(String path) throws Exception {
        var mockMvc = standaloneSetup(
                        new ProductReviewController(mock(ReviewUseCase.class)))
                .setControllerAdvice(handler)
                .build();

        mockMvc.perform(get(path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @DisplayName("내부 Jackson 처리 오류는 INTERNAL_ERROR로 매핑된다")
    @Test
    void jacksonException_mapsToInternalError() {
        ResponseEntity<ErrorResponse> response = handler.handleJacksonException(
                mock(JacksonException.class));

        assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode().value()).isEqualTo(500);
            softly.assertThat(response.getBody()).isEqualTo(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
        });
    }

    private static DataIntegrityViolationException constraintViolation(String constraintName) {
        return new DataIntegrityViolationException(
                "DB constraint violation",
                new ConstraintViolationException("Duplicate entry", new SQLException(), constraintName));
    }

    private MockMvc mediaMockMvc() {
        return standaloneSetup(new MediaController())
                .setControllerAdvice(handler)
                .build();
    }

    @RestController
    @RequestMapping("/test/resources")
    static class MediaController {

        @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, String> read() {
            return Map.of("status", "ok");
        }

        @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
        void write(@RequestBody Map<String, String> body) {
        }
    }
}
