package com.personal.happygallery.app.web;

import java.util.List;

/**
 * 커서 기반 페이지 응답.
 *
 * @param content    현재 페이지의 항목 목록
 * @param nextCursor 다음 페이지 커서 (마지막 페이지면 {@code null})
 * @param hasMore    다음 페이지 존재 여부
 */
public record CursorPage<T>(
        List<T> content,
        String nextCursor,
        boolean hasMore
) {

    /**
     * size + 1개를 조회한 결과로부터 CursorPage를 생성한다.
     *
     * @param fetchedItems size + 1개를 조회한 결과
     * @param size         요청된 페이지 크기
     * @param cursorExtractor 마지막 항목에서 커서 문자열을 추출하는 함수
     */
    public static <T> CursorPage<T> of(List<T> fetchedItems, int size,
                                        java.util.function.Function<T, String> cursorExtractor) {
        if (fetchedItems.size() > size) {
            List<T> content = fetchedItems.subList(0, size);
            String nextCursor = cursorExtractor.apply(content.get(content.size() - 1));
            return new CursorPage<>(List.copyOf(content), nextCursor, true);
        }
        return new CursorPage<>(List.copyOf(fetchedItems), null, false);
    }
}
