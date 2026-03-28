package com.personal.happygallery.app.web;

import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.ErrorResponse;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @DisplayName("예약 낙관적 락 충돌은 BOOKING_CONFLICT로 매핑된다")
    @Test
    void optimisticLock_booking_mapsToBookingConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLockingFailure(
                new OptimisticLockingFailureException(
                        "Object of class [" + Booking.class.getName() + "] with identifier [1]"));

        assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode().value()).isEqualTo(409);
            softly.assertThat(response.getBody()).isEqualTo(ErrorResponse.of(ErrorCode.BOOKING_CONFLICT));
        });
    }

    @DisplayName("비예약 낙관적 락 충돌은 CONFLICT로 매핑된다")
    @Test
    void optimisticLock_nonBooking_mapsToConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLockingFailure(
                new OptimisticLockingFailureException(
                        "Object of class [" + Order.class.getName() + "] with identifier [1]"));

        assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode().value()).isEqualTo(409);
            softly.assertThat(response.getBody()).isEqualTo(ErrorResponse.of(ErrorCode.CONFLICT));
        });
    }

    @DisplayName("슬롯 유니크 제약 위반은 INVALID_INPUT으로 매핑된다")
    @Test
    void dataIntegrity_slotUnique_mapsToInvalidInput() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("Duplicate entry for key 'uq_slot_class_start'"));

        assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode().value()).isEqualTo(400);
            softly.assertThat(response.getBody()).isEqualTo(ErrorResponse.of(ErrorCode.INVALID_INPUT));
        });
    }
}
