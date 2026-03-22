package com.personal.happygallery.config;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 민감 데이터를 마스킹하는 Logback PatternLayout.
 *
 * <p>logback-spring.xml에서 {@code <layout class="...MaskingPatternLayout">}로 등록한다.
 * 전화번호, Bearer 토큰, 세션 토큰을 정규식으로 치환한다.
 */
public class MaskingPatternLayout extends PatternLayout {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(01[016789])[-]?(\\d{3,4})[-]?(\\d{4})");

    private static final Pattern BEARER_PATTERN =
            Pattern.compile("(Bearer\\s)[A-Za-z0-9._\\-]+");

    private static final Pattern SESSION_PATTERN =
            Pattern.compile("(HG_SESSION=)[^\\s;]+");

    private static final Pattern ACCESS_TOKEN_PATTERN =
            Pattern.compile("(X-Access-Token[=:]\\s?)[A-Fa-f0-9]{32,64}");

    @Override
    public String doLayout(ILoggingEvent event) {
        return maskSensitive(super.doLayout(event));
    }

    static String maskSensitive(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        String masked = PHONE_PATTERN.matcher(message).replaceAll("$1-****-****");
        masked = BEARER_PATTERN.matcher(masked).replaceAll("$1***");
        masked = SESSION_PATTERN.matcher(masked).replaceAll("$1***");
        masked = ACCESS_TOKEN_PATTERN.matcher(masked).replaceAll("$1***");
        return masked;
    }
}
