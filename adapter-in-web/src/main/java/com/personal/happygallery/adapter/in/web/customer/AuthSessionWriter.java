package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.security.customer.CustomerAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

/**
 * 회원 인증 성공 후 세션에 사용자 ID를 묶는 단일 진입점.
 *
 * <p>로그인·회원가입·소셜 로그인 컨트롤러가 같은 세션 처리를 각자 적지 않도록 모은다.
 * 기존 세션은 속성을 보존한 채 ID를 교체해 세션 고정 공격을 방어한다.
 */
@Component
public class AuthSessionWriter {

    private final CsrfTokenRepository csrfTokenRepository;

    public AuthSessionWriter(CsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;
    }

    public void bind(HttpServletRequest request, HttpServletResponse response, Long userId) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession(true);
        } else {
            request.changeSessionId();
        }
        session.setAttribute(CustomerAuthenticationFilter.CUSTOMER_USER_ID_SESSION_ATTRIBUTE, userId);
        csrfTokenRepository.saveToken(null, request, response);
    }

    public void unbind(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        csrfTokenRepository.saveToken(null, request, response);
    }
}
