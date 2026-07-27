package com.personal.happygallery.adapter.in.web.security;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class CsrfController {

    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {
        // 지연된 토큰을 평가해야 저장소가 응답 쿠키를 기록한다.
        csrfToken.getToken();
        return new CsrfTokenResponse(CSRF_COOKIE_NAME, CSRF_HEADER_NAME);
    }

    public record CsrfTokenResponse(String cookieName, String headerName) {}
}
