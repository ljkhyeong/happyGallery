package com.personal.happygallery.bootstrap.logging;

import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class SensitiveLogMasker {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(01[016789])[-]?(\\d{3,4})[-]?(\\d{4})");

    private static final Pattern BEARER_PATTERN =
            Pattern.compile("(Bearer\\s)[A-Za-z0-9._\\-]+");

    private static final Pattern SESSION_PATTERN =
            Pattern.compile("(HG_SESSION=)[^\\s;]+");

    private static final Pattern ACCESS_TOKEN_PATTERN =
            Pattern.compile(
                    "((?i:X-Access-Token)[\"']?\\s*[=:]\\s*[\"']?)"
                            + "(?:[A-Za-z0-9_-]{16,}\\.[A-Za-z0-9_-]{32,}|[A-Fa-f0-9]{32,64})"
                            + "(?![A-Za-z0-9._-])");

    private SensitiveLogMasker() {}

    static String mask(String message) {
        if (!StringUtils.hasLength(message)) {
            return message;
        }
        String masked = PHONE_PATTERN.matcher(message).replaceAll("$1-****-****");
        masked = BEARER_PATTERN.matcher(masked).replaceAll("$1***");
        masked = SESSION_PATTERN.matcher(masked).replaceAll("$1***");
        masked = ACCESS_TOKEN_PATTERN.matcher(masked).replaceAll("$1***");
        return masked;
    }
}
