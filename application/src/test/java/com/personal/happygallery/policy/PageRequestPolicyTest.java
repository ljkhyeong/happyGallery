package com.personal.happygallery.policy;

import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("policy")
class PageRequestPolicyTest {

    @Test
    @DisplayName("페이지 요청은 음수 번호와 허용 범위 밖 크기 및 OFFSET 오버플로를 거절한다")
    void rejectInvalidPageRequest() {
        assertInvalid(() -> PageParams.offset(-1, 20));
        assertInvalid(() -> PageParams.requireSize(0));
        assertInvalid(() -> PageParams.requireSize(101));
        assertInvalid(() -> PageParams.offset(Integer.MAX_VALUE, 100));
    }

    private static void assertInvalid(Runnable request) {
        assertThatThrownBy(request::run)
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
