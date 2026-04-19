package com.personal.happygallery.application.search;

import com.personal.happygallery.application.search.port.out.AdminSearchPort;
import com.personal.happygallery.application.shared.page.OffsetPage;
import java.time.LocalDate;
import java.util.List;

final class AdminSearchHelper {

    private static final int MAX_SIZE = 100;

    private AdminSearchHelper() {}

    static <S, R> OffsetPage<R> search(AdminSearchPort<S, R> port,
                                        S status, LocalDate dateFrom, LocalDate dateTo,
                                        String keyword, int page, int size) {
        String safeKeyword = SearchParams.clampKeyword(keyword);
        int safePage = SearchParams.clampPage(page);
        int clampedSize = SearchParams.clampSize(size, MAX_SIZE);
        long totalCount = port.count(status, dateFrom, dateTo, safeKeyword);
        if (totalCount == 0 || (long) safePage * clampedSize >= totalCount) {
            return OffsetPage.of(List.of(), safePage, clampedSize, totalCount);
        }
        List<R> rows = port.search(
                status, dateFrom, dateTo, safeKeyword, safePage * clampedSize, clampedSize);
        return OffsetPage.of(rows, safePage, clampedSize, totalCount);
    }
}
