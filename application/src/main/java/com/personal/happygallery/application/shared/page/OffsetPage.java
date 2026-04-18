package com.personal.happygallery.application.shared.page;

import java.util.List;

/**
 * OFFSET 기반 페이지 응답.
 *
 * @param content    현재 페이지의 항목 목록
 * @param page       현재 페이지 번호 (0-based)
 * @param size       페이지 크기
 * @param totalCount 전체 항목 수
 * @param totalPages 전체 페이지 수
 */
public record OffsetPage<T>(
        List<T> content,
        int page,
        int size,
        long totalCount,
        int totalPages
) {

    public static <T> OffsetPage<T> of(List<T> content, int page, int size, long totalCount) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be >= 1");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        int totalPages = (int) Math.ceil((double) totalCount / size);
        return new OffsetPage<>(content, page, size, totalCount, totalPages);
    }
}
