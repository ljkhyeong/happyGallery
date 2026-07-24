package com.personal.happygallery.adapter.in.web;

import com.personal.happygallery.adapter.in.web.booking.SlotController;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.adapter.in.web.error.ErrorResponse;
import com.personal.happygallery.application.booking.port.in.SlotQueryUseCase;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.product.Product;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import tools.jackson.core.JacksonException;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @DisplayName("예약 낙관적 락 충돌은 BOOKING_CONFLICT로 매핑된다")
    @Test
    void optimisticLock_booking_mapsToBookingConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLockingFailure(
                new ObjectOptimisticLockingFailureException(Booking.class.getName(), 1L));

        assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode().value()).isEqualTo(409);
            softly.assertThat(response.getBody()).isEqualTo(ErrorResponse.of(ErrorCode.BOOKING_CONFLICT));
        });
    }

    @DisplayName("비예약 낙관적 락 충돌은 CONFLICT로 매핑된다")
    @Test
    void optimisticLock_nonBooking_mapsToConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLockingFailure(
                new ObjectOptimisticLockingFailureException(Product.class.getName(), 1L));

        assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode().value()).isEqualTo(409);
            softly.assertThat(response.getBody()).isEqualTo(ErrorResponse.of(ErrorCode.CONFLICT));
        });
    }

    @DisplayName("슬롯 유니크 제약 위반은 INVALID_INPUT으로 매핑된다")
    @Test
    void dataIntegrity_slotUnique_mapsToInvalidInput() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                constraintViolation("uq_slot_class_start"));

        assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode().value()).isEqualTo(400);
            softly.assertThat(response.getBody()).isEqualTo(ErrorResponse.of(ErrorCode.INVALID_INPUT));
        });
    }

    @DisplayName("활성 예약 유니크 제약 위반은 DUPLICATE_BOOKING으로 매핑된다")
    @Test
    void dataIntegrity_activeBookingUnique_mapsToDuplicateBooking() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                constraintViolation("bookings.uq_bookings_active_guest_slot"));

        assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode().value()).isEqualTo(409);
            softly.assertThat(response.getBody()).isEqualTo(ErrorResponse.of(ErrorCode.DUPLICATE_BOOKING));
        });
    }

    @DisplayName("요청 JSON 파싱 실패는 INVALID_INPUT으로 매핑된다")
    @Test
    void httpMessageNotReadable_mapsToInvalidInput() {
        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("JSON parse error", new MockHttpInputMessage(new byte[0])));

        assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode().value()).isEqualTo(400);
            softly.assertThat(response.getBody()).isEqualTo(
                    ErrorResponse.of(ErrorCode.INVALID_INPUT, "요청 JSON 형식이 올바르지 않습니다."));
        });
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

    @DisplayName("내부 Jackson 처리 오류는 INTERNAL_ERROR로 매핑된다")
    @Test
    void jacksonException_mapsToInternalError() {
        ResponseEntity<ErrorResponse> response = handler.handleJacksonException(
                new TestJacksonException("결제 payload 역직렬화 실패"));

        assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode().value()).isEqualTo(500);
            softly.assertThat(response.getBody()).isEqualTo(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
        });
    }

    private static class TestJacksonException extends JacksonException {
        TestJacksonException(String message) {
            super(message);
        }
    }

    private static DataIntegrityViolationException constraintViolation(String constraintName) {
        return new DataIntegrityViolationException(
                "DB constraint violation",
                new ConstraintViolationException("Duplicate entry", new SQLException(), constraintName));
    }
}
