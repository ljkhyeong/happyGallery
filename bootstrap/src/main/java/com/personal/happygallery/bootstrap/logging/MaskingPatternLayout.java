package com.personal.happygallery.bootstrap.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * 민감 데이터를 마스킹하는 Logback PatternLayout.
 *
 * <p>logback-spring.xml에서 {@code <layout class="...MaskingPatternLayout">}로 등록한다.
 * 전화번호, Bearer 토큰, 세션 토큰, 비회원 접근 토큰을 정규식으로 치환한다.
 */
public class MaskingPatternLayout extends PatternLayout {

    @Override
    public String doLayout(ILoggingEvent event) {
        return SensitiveLogMasker.mask(super.doLayout(event));
    }
}
