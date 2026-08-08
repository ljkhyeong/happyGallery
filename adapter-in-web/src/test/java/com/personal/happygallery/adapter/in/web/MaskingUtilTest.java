package com.personal.happygallery.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class MaskingUtilTest {

    @DisplayName("이름 마스킹은 보조 평면 문자를 자르지 않고 첫 문자소를 보존한다")
    @Test
    void maskName_preservesFirstSupplementaryCodePoint() {
        assertSoftly(softly -> {
            softly.assertThat(MaskingUtil.maskName("𠮷田")).isEqualTo("𠮷*");
            softly.assertThat(MaskingUtil.maskName("😀회원")).isEqualTo("😀**");
        });
    }

    @DisplayName("결합 문자와 이모지 시퀀스는 하나의 문자소로 마스킹한다")
    @Test
    void maskName_preservesFirstExtendedGraphemeCluster() {
        assertSoftly(softly -> {
            softly.assertThat(MaskingUtil.maskName("e\u0301린")).isEqualTo("e\u0301*");
            softly.assertThat(MaskingUtil.maskName("👩🏽‍🎨작가")).isEqualTo("👩🏽‍🎨**");
            softly.assertThat(MaskingUtil.maskName("👨‍👩‍👧‍👦")).isEqualTo("*");
        });
    }

    @DisplayName("한 코드 포인트 이하의 이름은 전체를 마스킹한다")
    @Test
    void maskName_masksShortValuesCompletely() {
        assertSoftly(softly -> {
            softly.assertThat(MaskingUtil.maskName("😀")).isEqualTo("*");
            softly.assertThat(MaskingUtil.maskName("")).isEqualTo("*");
            softly.assertThat(MaskingUtil.maskName(null)).isEqualTo("*");
        });
    }

    @DisplayName("BMP 문자 이름은 기존과 같이 첫 글자만 남긴다")
    @Test
    void maskName_keepsExistingBmpBehavior() {
        assertThat(MaskingUtil.maskName("홍길동")).isEqualTo("홍**");
    }
}
