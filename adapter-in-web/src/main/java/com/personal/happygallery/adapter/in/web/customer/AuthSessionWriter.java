package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.CustomerAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 회원 인증 성공 후 세션에 사용자 ID를 묶는 단일 진입점.
 *
 * <p>로그인·회원가입·소셜 로그인 컨트롤러가 같은 setAttribute 라인을 각자
 * 적지 않도록 모은다. 세션 키나 보관 정책이 바뀔 때 수정 지점은 이 컴포넌트 한 곳.
 */
@Component
public class AuthSessionWriter {

    public void bind(HttpServletRequest request, Long userId) {
        request.getSession(true).setAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR, userId);
    }
}
