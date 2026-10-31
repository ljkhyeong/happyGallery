package com.personal.happygallery.policy;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 정책 테스트가 공유하는 {@link HappyGalleryException} 오류 코드 단언. */
final class PolicyAssertions {

    private PolicyAssertions() {}

    static void assertInvalidInput(ThrowingCallable command) {
        assertErrorCode(command, ErrorCode.INVALID_INPUT);
    }

    static void assertErrorCode(ThrowingCallable command, ErrorCode expected) {
        assertThatThrownBy(command)
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
