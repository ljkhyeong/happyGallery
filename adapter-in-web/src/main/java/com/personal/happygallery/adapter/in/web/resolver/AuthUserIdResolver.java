package com.personal.happygallery.adapter.in.web.resolver;

import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import java.security.Principal;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Principal requestPrincipal = webRequest.getUserPrincipal();
        Authentication authentication = requestPrincipal instanceof Authentication current
                ? current
                : SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        if (parameter.hasParameterAnnotation(CustomerUserId.class)) {
            return authentication.getPrincipal() instanceof CustomerPrincipal principal
                    ? principal.userId()
                    : null;
        }
        return authentication.getPrincipal() instanceof AdminPrincipal principal
                ? principal.adminUserId()
                : null;
    }
}
