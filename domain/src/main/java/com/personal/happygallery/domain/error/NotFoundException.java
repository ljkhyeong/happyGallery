package com.personal.happygallery.domain.error;

import java.util.function.Supplier;

public class NotFoundException extends HappyGalleryException {

    public NotFoundException(String resource) {
        super(ErrorCode.NOT_FOUND, resource + "을(를) 찾을 수 없습니다.");
    }

    public static Supplier<NotFoundException> supplier(String resource) {
        return () -> new NotFoundException(resource);
    }
}
