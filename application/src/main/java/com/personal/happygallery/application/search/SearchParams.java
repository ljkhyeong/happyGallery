package com.personal.happygallery.application.search;

import com.personal.happygallery.domain.text.DomainText;
import org.springframework.util.StringUtils;

/**
 * 검색 파라미터 정제 유틸.
 */
public final class SearchParams {

    public static final int MAX_KEYWORD_LENGTH = 100;

    private SearchParams() {}

    /** keyword의 앞뒤 공백을 제거하고 빈 값은 null, 긴 값은 최대 100자로 정제한다. */
    public static String clampKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;
        return DomainText.truncate(keyword.strip(), MAX_KEYWORD_LENGTH);
    }
}
