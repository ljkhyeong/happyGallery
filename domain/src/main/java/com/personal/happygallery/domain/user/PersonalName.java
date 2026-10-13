package com.personal.happygallery.domain.user;

import com.personal.happygallery.domain.text.DomainText;

public final class PersonalName {

    private static final int MAX_LENGTH = 100;

    private PersonalName() {}

    public static String required(String value) {
        return DomainText.required(value, "이름은", MAX_LENGTH);
    }
}
