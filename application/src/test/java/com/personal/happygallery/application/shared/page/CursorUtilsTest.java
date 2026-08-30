package com.personal.happygallery.application.shared.page;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorUtilsTest {

    @DisplayName("날짜 형식이 잘못된 커서는 잘못된 입력으로 거절한다")
    @Test
    void decode_invalidTimestamp_rejectsAsInvalidInput() {
        String cursor = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("not-a-date|1".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> CursorUtils.decode(cursor))
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
