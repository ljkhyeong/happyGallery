package com.personal.happygallery.application.search;

import com.personal.happygallery.application.search.port.out.AdminSearchPort;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.application.shared.page.PageParams;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

final class AdminSearchHelper {

    private AdminSearchHelper() {}

    static <S, D, R> OffsetPage<R> search(AdminSearchPort<S, D> port,
                                           S status, LocalDate dateFrom, LocalDate dateTo,
                                           String keyword, int page, int size,
                                           Function<D, R> rowMapper) {
        String safeKeyword = SearchParams.clampKeyword(keyword);
        int safePage = PageParams.clampPage(page);
        int clampedSize = PageParams.clampSize(size);
        int offset = PageParams.offset(safePage, clampedSize);
        long totalCount = port.count(status, dateFrom, dateTo, safeKeyword);
        if (totalCount == 0 || offset >= totalCount) {
            return OffsetPage.of(List.of(), safePage, clampedSize, totalCount);
        }
        List<R> rows = port.search(
                        status, dateFrom, dateTo, safeKeyword, offset, clampedSize)
                .stream()
                .map(rowMapper)
                .toList();
        return OffsetPage.of(rows, safePage, clampedSize, totalCount);
    }
}
