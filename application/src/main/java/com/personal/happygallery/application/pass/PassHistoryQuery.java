package com.personal.happygallery.application.pass;

import com.personal.happygallery.application.search.SearchParams;

/** 회원 전체 이력의 검색 조건. */
public record PassHistoryQuery(String keyword, PassHistoryStatus status, PassHistorySort sort) {
    public PassHistoryQuery {
        keyword = SearchParams.clampKeyword(keyword);
        sort = sort == null ? PassHistorySort.PURCHASE_DESC : sort;
    }

    public boolean isDefault() {
        return keyword == null && status == null && sort == PassHistorySort.PURCHASE_DESC;
    }

    public enum PassHistorySort { PURCHASE_DESC, EXPIRY_ASC, CREDITS_DESC }
    public enum PassHistoryStatus { ACTIVE, USED_UP, EXPIRED }
}
