package com.personal.happygallery.domain.user;

import com.personal.happygallery.domain.text.DomainText;
import java.util.Locale;

public final class EmailAddress {

    public static final int MAX_LENGTH = 254;

    private EmailAddress() {}

    /** 소문자로 정규화한 값의 길이를 검사한다. */
    public static String required(String value) {
        String lowered = value == null ? null : value.toLowerCase(Locale.ROOT);
        return DomainText.required(lowered, "이메일은", MAX_LENGTH);
    }

    public static String optional(String value) {
        return value == null ? null : required(value);
    }
}
