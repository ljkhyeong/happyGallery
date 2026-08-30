package com.personal.happygallery.adapter.in.web.security.admin;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public final class AdminBearerTokenResolver {

    private static final String BEARER_SCHEME = "Bearer";

    public Resolution resolve(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return Resolution.notBearer();
        }

        String value = authorization.strip();
        if (!value.regionMatches(true, 0, BEARER_SCHEME, 0, BEARER_SCHEME.length())) {
            return Resolution.notBearer();
        }
        if (value.length() == BEARER_SCHEME.length()) {
            return Resolution.malformedBearer();
        }
        if (!Character.isWhitespace(value.charAt(BEARER_SCHEME.length()))) {
            return Resolution.notBearer();
        }
        if (value.charAt(BEARER_SCHEME.length()) != ' ') {
            return Resolution.malformedBearer();
        }

        String token = value.substring(BEARER_SCHEME.length() + 1).strip();
        if (!StringUtils.hasText(token) || token.chars().anyMatch(Character::isWhitespace)) {
            return Resolution.malformedBearer();
        }
        return Resolution.bearer(token);
    }

    public record Resolution(boolean bearer, String token) {

        private static Resolution notBearer() {
            return new Resolution(false, null);
        }

        private static Resolution malformedBearer() {
            return new Resolution(true, null);
        }

        private static Resolution bearer(String token) {
            return new Resolution(true, token);
        }

        public boolean hasToken() {
            return token != null;
        }
    }
}
