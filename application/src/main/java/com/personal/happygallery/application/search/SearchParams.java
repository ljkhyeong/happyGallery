package com.personal.happygallery.application.search;

/**
 * 검색 파라미터 정제 유틸.
 */
public final class SearchParams {

    public static final int MAX_KEYWORD_LENGTH = 100;

    private SearchParams() {}

    /** keyword를 trim → empty→null → max 100자로 정제한다. */
    public static String clampKeyword(String keyword) {
        if (keyword == null) return null;
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > MAX_KEYWORD_LENGTH
                ? trimmed.substring(0, MAX_KEYWORD_LENGTH)
                : trimmed;
    }

    public static int clampPage(int page) {
        return Math.max(page, 0);
    }

    public static int clampSize(int size, int maxSize) {
        return Math.min(Math.max(size, 1), maxSize);
    }
}
