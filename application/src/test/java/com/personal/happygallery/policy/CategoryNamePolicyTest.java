package com.personal.happygallery.policy;

import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.category.CategoryName;
import com.personal.happygallery.domain.error.HappyGalleryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("policy")
class CategoryNamePolicyTest {

    @DisplayName("선택 카테고리는 앞뒤 공백을 제거하고 대문자 토큰으로 정규화한다")
    @Test
    void optional_trimsAndUppercases() {
        assertThat(CategoryName.optional(" perfume ")).isEqualTo("PERFUME");
    }

    @DisplayName("선택 카테고리가 null 또는 공백이면 null로 정규화한다")
    @Test
    void optional_whenBlank_returnsNull() {
        assertSoftly(softly -> {
            softly.assertThat(CategoryName.optional(null)).isNull();
            softly.assertThat(CategoryName.optional("   ")).isNull();
        });
    }

    @DisplayName("필수 카테고리가 공백이면 예외가 발생한다")
    @Test
    void required_whenBlank_throwsInvalidInput() {
        assertThatThrownBy(() -> CategoryName.required("   "))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("카테고리");
    }

    @DisplayName("클래스 기본 생성자는 카테고리 오류를 시간과 버퍼 오류보다 먼저 반환한다")
    @Test
    void bookingClassConstructor_whenSeveralInputsInvalid_reportsCategoryFirst() {
        assertThatThrownBy(() -> new BookingClass(
                "복합 오류 클래스", "   ", 0, BookingClass.MIN_PRICE, -1))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessage("카테고리는 필수입니다.");
    }
}
