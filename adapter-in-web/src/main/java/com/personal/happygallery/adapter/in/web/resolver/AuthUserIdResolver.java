package com.personal.happygallery.adapter.in.web.resolver;

import com.personal.happygallery.adapter.in.web.AdminAuthFilter;
import com.personal.happygallery.adapter.in.web.CustomerAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthUserIdResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CustomerUserId.class)
                || parameter.hasParameterAnnotation(AdminUserId.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) return null;

        if (parameter.hasParameterAnnotation(CustomerUserId.class)) {
            return request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
        }
        return request.getAttribute(AdminAuthFilter.ADMIN_USER_ID_ATTR);
    }
}
