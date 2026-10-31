package com.personal.happygallery.application.search;

import com.personal.happygallery.application.search.port.out.AdminSearchPort;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.application.shared.page.PageParams;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/** 관리자 오프셋 검색의 공통 골격: keyword·page·size 정제, 건수 조회, 범위를 벗어난 쪽의 빈 결과 단락. */
final class AdminSearchHelper {

    private AdminSearchHelper() {}

    /** 정제한 keyword·offset·size로 한 쪽을 읽는다. */
    @FunctionalInterface
    interface PageFetcher<D> {
        List<D> fetch(String keyword, int offset, int size);
    }

    static <S, D, R> OffsetPage<R> search(AdminSearchPort<S, D> port,
                                           S status, LocalDate dateFrom, LocalDate dateTo,
                                           String keyword, int page, int size,
                                           Function<D, R> rowMapper) {
        return search(keyword, page, size,
                safeKeyword -> port.count(status, dateFrom, dateTo, safeKeyword),
                (safeKeyword, offset, limit) -> port.search(status, dateFrom, dateTo, safeKeyword, offset, limit),
                rowMapper);
    }

    /** 건수가 0이거나 offset이 건수를 넘으면 조회 없이 빈 쪽을 돌려준다. */
    static <D, R> OffsetPage<R> search(String keyword, int page, int size,
                                        ToLongFunction<String> counter, PageFetcher<D> fetcher,
                                        Function<D, R> rowMapper) {
        String safeKeyword = SearchParams.clampKeyword(keyword);
        int safePage = PageParams.clampPage(page);
        int clampedSize = PageParams.clampSize(size);
        int offset = PageParams.offset(safePage, clampedSize);
        long totalCount = counter.applyAsLong(safeKeyword);
        if (totalCount == 0 || offset >= totalCount) {
            return OffsetPage.of(List.of(), safePage, clampedSize, totalCount);
        }
        List<R> rows = fetcher.fetch(safeKeyword, offset, clampedSize).stream()
                .map(rowMapper)
                .toList();
        return OffsetPage.of(rows, safePage, clampedSize, totalCount);
    }
}
