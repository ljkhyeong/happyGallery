package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.security.customer.CustomerAuthenticationFilter;
import com.personal.happygallery.adapter.in.web.security.customer.SocialAccountLinkIntentStore;
import com.personal.happygallery.adapter.in.web.security.customer.SocialSignupIntentStore;
import com.personal.happygallery.domain.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

import static org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME;

/**
 * 회원 인증 성공 후 세션에 사용자 ID를 연결하고 해제하는 단일 진입점.
 *
 * <p>로그인·회원가입·소셜 로그인 컨트롤러가 같은 세션 처리를 각자 적지 않도록 모은다.
 * 기존 세션은 속성을 보존한 채 ID를 교체해 세션 고정 공격을 방어한다.
 */
@Component
public class CustomerSessionBinder {

    private final CsrfTokenRepository csrfTokenRepository;

    public CustomerSessionBinder(CsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;
    }

    public void bind(HttpServletRequest request, HttpServletResponse response, User user) {
        HttpSession session = request.getSession();
        SocialAccountLinkIntentStore.clear(session);
        SocialSignupIntentStore.clear(session);
        if (!session.isNew()) {
            request.changeSessionId();
        }
        session.setAttribute(CustomerAuthenticationFilter.CUSTOMER_USER_ID_SESSION_ATTRIBUTE, user.getId());
        session.setAttribute(CustomerAuthenticationFilter.CUSTOMER_CREDENTIAL_VERSION_SESSION_ATTRIBUTE,
                user.getCredentialVersion());
        session.setAttribute(PRINCIPAL_NAME_INDEX_NAME,
                user.getId() + ":" + user.getCredentialVersion());
        csrfTokenRepository.saveToken(null, request, response);
    }

    public void unbind(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        csrfTokenRepository.saveToken(null, request, response);
    }

    public void unbindIfBoundTo(HttpServletRequest request,
                                HttpServletResponse response,
                                Long userId) {
        HttpSession session = request.getSession(false);
        if (session == null
                || !userId.equals(session.getAttribute(
                        CustomerAuthenticationFilter.CUSTOMER_USER_ID_SESSION_ATTRIBUTE))) {
            return;
        }
        unbind(request, response);
    }
}
