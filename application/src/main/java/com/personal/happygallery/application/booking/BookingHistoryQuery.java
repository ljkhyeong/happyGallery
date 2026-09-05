package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.search.SearchParams;
import com.personal.happygallery.domain.booking.BookingStatus;

/** 회원 전체 이력의 검색 조건. */
public record BookingHistoryQuery(String keyword, BookingStatus status, BookingHistorySort sort) {
    public BookingHistoryQuery {
        keyword = SearchParams.clampKeyword(keyword);
        sort = sort == null ? BookingHistorySort.CREATED_DESC : sort;
    }

    public boolean isDefault() {
        return keyword == null && status == null && sort == BookingHistorySort.CREATED_DESC;
    }

    public enum BookingHistorySort { CREATED_DESC, SOONEST, LATEST, DEPOSIT_DESC }
}
