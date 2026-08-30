package com.personal.happygallery.adapter.in.web;

import com.personal.happygallery.adapter.in.web.error.ErrorResponse;
import com.personal.happygallery.adapter.in.web.ratelimit.RateLimitExceededException;
import com.personal.happygallery.adapter.in.web.ratelimit.RateLimitUnavailableException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import io.sentry.Sentry;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;

import static java.util.stream.Collectors.joining;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Map<String, ErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
            "uq_bookings_active_user_slot", ErrorCode.DUPLICATE_BOOKING,
            "uq_bookings_active_guest_slot", ErrorCode.DUPLICATE_BOOKING,
            "uq_bookings_active_phone_slot", ErrorCode.DUPLICATE_BOOKING,
            "uq_users_phone_hmac", ErrorCode.PHONE_ALREADY_IN_USE,
            "uq_users_email_hmac", ErrorCode.EMAIL_ALREADY_EXISTS,
            "uq_user_social_accounts_provider_identity", ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED,
            "uq_user_social_accounts_user_provider", ErrorCode.SOCIAL_PROVIDER_ALREADY_LINKED,
            "uq_issued_coupons_user_definition", ErrorCode.CONFLICT,
            "uq_slot_class_start", ErrorCode.INVALID_INPUT
    );

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

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(joining(", "));
        return mvcErrorResponse(ErrorCode.INVALID_INPUT, message, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return mvcErrorResponse(
                ErrorCode.INVALID_INPUT,
                "요청 JSON 형식이 올바르지 않습니다.",
                headers,
                status);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return mvcErrorResponse(
                ErrorCode.INVALID_INPUT,
                "이미지는 5MB 이하여야 합니다.",
                headers,
                HttpStatusCode.valueOf(ErrorCode.INVALID_INPUT.httpStatus));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return invalidRequestParameter(headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(
            ServletRequestBindingException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return invalidRequestParameter(headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return invalidRequestParameter(headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ErrorCode errorCode = resolveMvcErrorCode(status);
        if (status.is5xxServerError()) {
            log.error("Spring MVC 요청 처리 오류 [status={}]", status.value(), e);
            Sentry.captureException(e);
        }
        return mvcErrorResponse(errorCode, errorCode.message, headers, status);
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

    /** DB 제약 위반. 이름과 의미를 아는 제약만 클라이언트 오류로 번역한다. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        Optional<String> constraintName = findConstraintName(e)
                .map(GlobalExceptionHandler::normalizeConstraintName)
                .filter(StringUtils::hasText);
        ErrorCode errorCode = constraintName
                .map(CONSTRAINT_ERROR_CODES::get)
                .orElse(null);
        if (errorCode == null) {
            log.error("알 수 없는 DB 제약 위반 [constraint={}]",
                    constraintName.orElse("unknown"), e);
            Sentry.captureException(e);
            errorCode = ErrorCode.INTERNAL_ERROR;
        } else {
            log.warn("DB 제약 위반 [constraint={}, code={}]",
                    constraintName.orElseThrow(), errorCode);
        }
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

    private ResponseEntity<Object> invalidRequestParameter(
            HttpHeaders headers,
            HttpStatusCode status
    ) {
        return mvcErrorResponse(
                ErrorCode.INVALID_INPUT,
                "요청 파라미터가 올바르지 않습니다.",
                headers,
                status);
    }

    private static ErrorCode resolveMvcErrorCode(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> ErrorCode.NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 406 -> ErrorCode.NOT_ACCEPTABLE;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case 503 -> ErrorCode.SERVICE_UNAVAILABLE;
            default -> status.is4xxClientError()
                    ? ErrorCode.INVALID_INPUT
                    : ErrorCode.INTERNAL_ERROR;
        };
    }

    private static ResponseEntity<Object> mvcErrorResponse(
            ErrorCode errorCode,
            String message,
            HttpHeaders headers,
            HttpStatusCode status
    ) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(
                ErrorResponse.of(errorCode, message, requestId()),
                responseHeaders,
                status);
    }
}
