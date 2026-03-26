package com.personal.happygallery.app.search;

/**
 * 관리자 검색 공통 파라미터 정제 유틸.
 */
final class SearchParams {

    static final int MAX_KEYWORD_LENGTH = 100;

    private SearchParams() {}

    static String clampKeyword(String keyword) {
        if (keyword == null || keyword.length() <= MAX_KEYWORD_LENGTH) {
            return keyword;
        }
        return keyword.substring(0, MAX_KEYWORD_LENGTH);
    }

    static int clampPage(int page) {
        return Math.max(page, 0);
    }
}
