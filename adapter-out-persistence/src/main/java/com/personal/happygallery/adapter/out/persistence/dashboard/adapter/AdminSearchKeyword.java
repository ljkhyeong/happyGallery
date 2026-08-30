package com.personal.happygallery.adapter.out.persistence.dashboard.adapter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

record AdminSearchKeyword(String keyword, Long exactId) {

    private static final long NON_EXISTENT_ID = 0L;

    static AdminSearchKeyword parse(String keyword, Pattern formattedIdPattern) {
        if (keyword == null) {
            return new AdminSearchKeyword(null, null);
        }

        Matcher matcher = formattedIdPattern.matcher(keyword);
        if (!matcher.matches()) {
            return new AdminSearchKeyword(keyword, null);
        }

        try {
            return new AdminSearchKeyword(null, Long.parseLong(matcher.group(1)));
        } catch (NumberFormatException ignored) {
            return new AdminSearchKeyword(null, NON_EXISTENT_ID);
        }
    }
}
