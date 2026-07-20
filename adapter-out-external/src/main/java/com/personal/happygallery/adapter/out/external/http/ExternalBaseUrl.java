package com.personal.happygallery.adapter.out.external.http;

import java.net.URI;

/** 외부 서비스 base URL이 경로 없는 HTTPS origin인지 검증한다. */
public final class ExternalBaseUrl {

    private ExternalBaseUrl() {}

    public static void requireHttpsOrigin(String value, String serviceName) {
        if (value == null || value.isBlank()) {
            return;
        }

        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(serviceName + " base URL 형식이 올바르지 않습니다.", exception);
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))) {
            throw new IllegalArgumentException(serviceName + " base URL은 경로가 없는 HTTPS 주소여야 합니다.");
        }
    }
}
