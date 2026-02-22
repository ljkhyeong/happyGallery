package com.personal.happygallery.app.web;

import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.ErrorResponse;
import com.personal.happygallery.common.error.HappyGalleryException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HappyGalleryException.class)
    public ResponseEntity<ErrorResponse> handleHappyGalleryException(HappyGalleryException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.httpStatus)
                .body(ErrorResponse.of(errorCode, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity
                .status(400)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, message));
    }

    /**
     * DB 유니크 제약 위반 — TOCTOU 경쟁 조건에서 발생할 수 있는 최후 방어선.
     * 예: (slot_id, guest_id) 동시 삽입 충돌 (ADR-0004 참고)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        return ResponseEntity
                .status(409)
                .body(ErrorResponse.of(ErrorCode.DUPLICATE_BOOKING));
    }

    /**
     * 낙관적 락 충돌 — 동시 변경 시 @Version 불일치.
     * 예: 두 기기에서 동시에 같은 예약을 변경할 때 (ADR-0006 참고)
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(OptimisticLockingFailureException e) {
        return ResponseEntity
                .status(409)
                .body(ErrorResponse.of(ErrorCode.BOOKING_CONFLICT));
    }
}
