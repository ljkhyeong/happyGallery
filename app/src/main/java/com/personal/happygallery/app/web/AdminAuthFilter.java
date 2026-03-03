package com.personal.happygallery.app.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AdminAuthFilter implements Filter {

    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";
    private static final String ADMIN_PATH_PREFIX = "/admin/";

    @Value("${app.admin.api-key}")
    private String adminApiKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        if (uri.startsWith(ADMIN_PATH_PREFIX)) {
            String key = httpRequest.getHeader(ADMIN_KEY_HEADER);
            if (!adminApiKey.equals(key)) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"관리자 인증이 필요합니다.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
