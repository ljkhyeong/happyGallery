package com.personal.happygallery.adapter.in.web.security.admin;

import java.security.Principal;

public record AdminPrincipal(
        Long adminUserId,
        String username,
        AuthenticationSource authenticationSource
) implements Principal {

    public enum AuthenticationSource {
        BEARER_SESSION,
        API_KEY
    }

    public static AdminPrincipal bearerSession(Long adminUserId, String username) {
        return new AdminPrincipal(adminUserId, username, AuthenticationSource.BEARER_SESSION);
    }

    public static AdminPrincipal apiKey() {
        return new AdminPrincipal(null, null, AuthenticationSource.API_KEY);
    }

    @Override
    public String getName() {
        return username != null ? username : "local-api-key";
    }
}
