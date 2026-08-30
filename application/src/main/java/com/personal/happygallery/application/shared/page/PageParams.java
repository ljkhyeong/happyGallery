package com.personal.happygallery.application.shared.page;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

/** 목록 조회의 페이지 번호와 크기 경계를 관리한다. */
public final class PageParams {

    public static final int MAX_SIZE = 100;

    private PageParams() {}

    public static int clampPage(int page) {
        return Math.max(page, 0);
    }

    public static int clampSize(int size) {
        return Math.clamp(size, 1, MAX_SIZE);
    }

    private static int requirePage(int page) {
        if (page < 0) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "페이지 번호는 0 이상이어야 합니다.");
        }
        return page;
    }

    public static int requireSize(int size) {
        if (size < 1 || size > MAX_SIZE) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "페이지 크기는 1 이상 " + MAX_SIZE + " 이하여야 합니다.");
        }
        return size;
    }

    public static int offset(int page, int size) {
        long offset = (long) requirePage(page) * requireSize(size);
        if (offset > Integer.MAX_VALUE) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "요청한 페이지 범위가 너무 큽니다.");
        }
        return Math.toIntExact(offset);
    }
}
