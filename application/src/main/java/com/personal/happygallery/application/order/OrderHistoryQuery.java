package com.personal.happygallery.application.order;

import com.personal.happygallery.application.search.SearchParams;
import com.personal.happygallery.domain.order.OrderStatus;

/** 회원 전체 이력의 검색 조건. */
public record OrderHistoryQuery(String keyword, OrderStatus status, OrderHistorySort sort) {
    public OrderHistoryQuery {
        keyword = SearchParams.clampKeyword(keyword);
        sort = sort == null ? OrderHistorySort.LATEST : sort;
    }

    public boolean isDefault() {
        return keyword == null && status == null && sort == OrderHistorySort.LATEST;
    }

    public enum OrderHistorySort { LATEST, OLDEST, AMOUNT_DESC, AMOUNT_ASC }
}
