package com.personal.happygallery.application.search;

import com.personal.happygallery.application.search.port.out.AdminSearchPort;
import com.personal.happygallery.application.shared.page.OffsetPage;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

final class AdminSearchHelper {

    private static final int MAX_SIZE = 100;

    private AdminSearchHelper() {}

    static <S, D, R> OffsetPage<R> search(AdminSearchPort<S, D> port,
                                           S status, LocalDate dateFrom, LocalDate dateTo,
                                           String keyword, int page, int size,
                                           Function<D, R> rowMapper) {
        String safeKeyword = SearchParams.clampKeyword(keyword);
        int safePage = SearchParams.clampPage(page);
        int clampedSize = SearchParams.clampSize(size, MAX_SIZE);
        long totalCount = port.count(status, dateFrom, dateTo, safeKeyword);
        if (totalCount == 0 || (long) safePage * clampedSize >= totalCount) {
            return OffsetPage.of(List.of(), safePage, clampedSize, totalCount);
        }
        List<R> rows = port.search(
                        status, dateFrom, dateTo, safeKeyword, safePage * clampedSize, clampedSize)
                .stream()
                .map(rowMapper)
                .toList();
        return OffsetPage.of(rows, safePage, clampedSize, totalCount);
    }
}
