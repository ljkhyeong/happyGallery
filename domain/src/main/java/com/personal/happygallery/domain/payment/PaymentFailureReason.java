package com.personal.happygallery.domain.payment;

import com.personal.happygallery.domain.text.DomainText;

/**
 * 결제 시도·환불의 {@code fail_reason} 컬럼(500자)에 저장하는 실패 사유 정규화.
 *
 * <p>PG 응답 문구는 거절할 수 없는 값이므로 비어 있으면 호출자의 기본 문구를 쓰고 컬럼 길이에 맞게 앞에서 자른다.
 * 서비스는 같은 문구를 HTTP 응답 메시지에도 쓰므로 저장 전뿐 아니라 예외를 만들기 전에 {@link #normalize}로 정규화하고,
 * 엔티티 전이 메서드는 {@link #truncate}로 길이를 다시 보장한다.
 */
public final class PaymentFailureReason {

    public static final int MAX_LENGTH = 500;

    private PaymentFailureReason() {}

    /** 비어 있으면 {@code fallback}을 쓰고 저장 컬럼 길이에 맞게 자른다. */
    public static String normalize(String reason, String fallback) {
        return truncate(reason == null || reason.isBlank() ? fallback : reason);
    }

    /** 저장 컬럼 길이에 맞게 앞에서 자른다. null은 그대로 돌려준다. */
    public static String truncate(String reason) {
        return DomainText.truncate(reason, MAX_LENGTH);
    }
}
