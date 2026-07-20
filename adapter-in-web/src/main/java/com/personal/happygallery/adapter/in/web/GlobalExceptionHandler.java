package com.personal.happygallery.adapter.in.web;

import com.personal.happygallery.adapter.in.web.error.ErrorResponse;
import com.personal.happygallery.adapter.in.web.ratelimit.RateLimitExceededException;
import com.personal.happygallery.adapter.in.web.ratelimit.RateLimitUnavailableException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import io.sentry.Sentry;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;

import static java.util.stream.Collectors.joining;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RateLimitUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitUnavailable() {
        return ResponseEntity
                .status(ErrorCode.SERVICE_UNAVAILABLE.httpStatus)
                .header(HttpHeaders.RETRY_AFTER, "1")
                .body(ErrorResponse.of(ErrorCode.SERVICE_UNAVAILABLE,
                        ErrorCode.SERVICE_UNAVAILABLE.message, requestId()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException e) {
        return ResponseEntity
                .status(ErrorCode.TOO_MANY_REQUESTS.httpStatus)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.retryAfterSeconds()))
                .header("X-RateLimit-Limit", String.valueOf(e.limit()))
                .header("X-RateLimit-Remaining", String.valueOf(e.remaining()))
                .body(ErrorResponse.of(ErrorCode.TOO_MANY_REQUESTS,
                        ErrorCode.TOO_MANY_REQUESTS.message, requestId()));
    }

    @ExceptionHandler(HappyGalleryException.class)
    public ResponseEntity<ErrorResponse> handleHappyGalleryException(HappyGalleryException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.httpStatus)
                .body(ErrorResponse.of(errorCode, e.getMessage(), requestId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(joining(", "));
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.httpStatus)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, message, requestId()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.httpStatus)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, "요청 JSON 형식이 올바르지 않습니다.", requestId()));
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            ServletRequestBindingException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<ErrorResponse> handleRequestBindingException(Exception e) {
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.httpStatus)
                .body(ErrorResponse.of(
                        ErrorCode.INVALID_INPUT, "요청 파라미터가 올바르지 않습니다.", requestId()));
    }

    /**
     * 서버 내부 직렬화/역직렬화 실패.
     * 요청 JSON 파싱 오류는 Spring의 HttpMessageNotReadableException 경로에서 400으로 처리한다.
     */
    @ExceptionHandler(JacksonException.class)
    public ResponseEntity<ErrorResponse> handleJacksonException(JacksonException e) {
        log.error("JSON 처리 중 내부 오류", e);
        Sentry.captureException(e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.httpStatus)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.message, requestId()));
    }

    /** DB 제약 위반. 활성 예약 UNIQUE 충돌만 중복 예약으로 응답한다. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("DB 제약 위반");
        ErrorCode errorCode = resolveDataIntegrityErrorCode(e);
        return ResponseEntity
                .status(errorCode.httpStatus)
                .body(ErrorResponse.of(errorCode, errorCode.message, requestId()));
    }

    /**
     * 낙관적 락 충돌 — 동시 변경 시 @Version 불일치.
     * 예: 두 기기에서 동시에 같은 예약을 변경할 때 (ADR-0006 참고)
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(OptimisticLockingFailureException e) {
        log.warn("낙관적 락 충돌");
        ErrorCode errorCode = resolveOptimisticLockErrorCode(e);
        return ResponseEntity
                .status(errorCode.httpStatus)
                .body(ErrorResponse.of(errorCode, errorCode.message, requestId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
        log.error("처리되지 않은 예외", e);
        Sentry.captureException(e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.httpStatus)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.message, requestId()));
    }

    private static String requestId() {
        return MDC.get("requestId");
    }

    private static final Set<String> DUPLICATE_BOOKING_CONSTRAINTS = Set.of(
            "uq_bookings_active_user_slot",
            "uq_bookings_active_guest_slot"
    );

    private ErrorCode resolveDataIntegrityErrorCode(DataIntegrityViolationException e) {
        return findConstraintName(e)
                .map(GlobalExceptionHandler::normalizeConstraintName)
                .filter(DUPLICATE_BOOKING_CONSTRAINTS::contains)
                .map(name -> ErrorCode.DUPLICATE_BOOKING)
                .orElse(ErrorCode.INVALID_INPUT);
    }

    private static String normalizeConstraintName(String constraintName) {
        String normalized = constraintName.toLowerCase(Locale.ROOT)
                .replace("`", "")
                .replace("\"", "")
                .replace("'", "");
        return StringUtils.unqualify(normalized);
    }

    private static Optional<String> findConstraintName(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException violation) {
                return Optional.ofNullable(violation.getConstraintName());
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    private ErrorCode resolveOptimisticLockErrorCode(OptimisticLockingFailureException e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof ObjectOptimisticLockingFailureException objectFailure
                    && (Booking.class.equals(objectFailure.getPersistentClass())
                    || Booking.class.getName().equals(objectFailure.getPersistentClassName()))) {
                return ErrorCode.BOOKING_CONFLICT;
            }
            current = current.getCause();
        }
        return ErrorCode.CONFLICT;
    }
}
