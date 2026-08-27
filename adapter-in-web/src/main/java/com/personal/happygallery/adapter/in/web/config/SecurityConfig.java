package com.personal.happygallery.adapter.in.web.config;

import com.personal.happygallery.adapter.in.web.FilterErrorResponseWriter;
import com.personal.happygallery.adapter.in.web.RateLimitFilter;
import com.personal.happygallery.adapter.in.web.config.properties.AdminProperties;
import com.personal.happygallery.adapter.in.web.security.admin.AdminAuthenticationFilter;
import com.personal.happygallery.adapter.in.web.security.admin.AdminAuthenticationProvider;
import com.personal.happygallery.adapter.in.web.security.admin.AdminBearerTokenResolver;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerAuthenticationFilter;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerSecurityRoutes;
import com.personal.happygallery.adapter.in.web.security.customer.DiscardingOAuth2AuthorizedClientRepository;
import com.personal.happygallery.adapter.in.web.security.customer.SocialLoginAuthenticationHandler;
import com.personal.happygallery.adapter.in.web.security.customer.SocialOAuth2AuthorizationRequestResolver;
import com.personal.happygallery.application.admin.port.in.AdminAuthUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.domain.error.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    private static final String ADMIN_LOGIN_REQUIRED = "관리자 인증이 필요합니다.";
    private static final String CUSTOMER_LOGIN_REQUIRED = "로그인이 필요합니다.";
    private static final String GUEST_ACCESS_TOKEN_HEADER = "X-Access-Token";
    private static final String[] PUBLIC_READ_PATHS = {
            "/actuator",
            "/actuator/health", "/actuator/health/**",
            "/actuator/info",
            "/actuator/metrics", "/actuator/metrics/**",
            "/actuator/prometheus",
            "/api/v1/auth/csrf",
            CustomerSecurityRoutes.SOCIAL_AUTHORIZATION_PROVIDER_PATH,
            CustomerSecurityRoutes.SOCIAL_CALLBACK_PROVIDER_PATH,
            "/api/v1/policies/current",
            "/api/v1/payments/{orderId}",
            "/api/v1/payments/pass-policy",
            "/api/v1/bookings/{bookingId}",
            "/api/v1/guest-records/recovery/orders",
            "/api/v1/guest-records/recovery/bookings",
            "/api/v1/orders/policy",
            "/api/v1/orders/{id}",
            "/api/v1/orders/{orderId}/claims",
            "/api/v1/products",
            "/api/v1/products/categories",
            "/api/v1/products/{id}",
            "/api/v1/products/{productId}/qna",
            "/api/v1/products/{productId}/qna/page",
            "/api/v1/products/{productId}/qna/{id}",
            "/api/v1/products/{productId}/reviews",
            "/api/v1/media/images/{fileName}",
            "/api/v1/workshop",
            "/api/v1/classes",
            "/api/v1/classes/{id}",
            "/api/v1/classes/{classId}/reviews",
            "/api/v1/slots",
            "/api/v1/slots/upcoming",
            "/api/v1/notices",
            "/api/v1/notices/{id}",
            "/api/v1/events",
            "/api/v1/events/{id}"
    };

    @Bean
    AuthenticationProvider adminAuthenticationProvider(AdminAuthUseCase adminAuthUseCase,
                                                       AdminProperties adminProperties) {
        return new AdminAuthenticationProvider(adminAuthUseCase, adminProperties);
    }

    @Bean
    AuthenticationManager adminAuthenticationManager(AuthenticationProvider adminAuthenticationProvider) {
        return new ProviderManager(adminAuthenticationProvider);
    }

    @Bean
    RequestMatcher publicAdminEndpoints() {
        return new OrRequestMatcher(
                adminEndpoint(HttpMethod.POST, "/api/v1/admin/auth/login"),
                adminEndpoint(HttpMethod.POST, "/api/v1/admin/auth/mfa/verify"),
                adminEndpoint(HttpMethod.POST, "/api/v1/admin/auth/logout"),
                adminEndpoint(HttpMethod.POST, "/api/v1/admin/setup"),
                adminEndpoint(HttpMethod.GET, "/api/v1/admin/setup/status"),
                adminEndpoint(HttpMethod.HEAD, "/api/v1/admin/setup/status"));
    }

    @Bean
    RequestMatcher adminMfaEnrollmentEndpoints() {
        return new OrRequestMatcher(
                adminEndpoint(HttpMethod.GET, "/api/v1/admin/auth/mfa"),
                adminEndpoint(HttpMethod.HEAD, "/api/v1/admin/auth/mfa"),
                adminEndpoint(HttpMethod.POST, "/api/v1/admin/auth/mfa/enrollment"),
                adminEndpoint(HttpMethod.POST, "/api/v1/admin/auth/mfa/enrollment/confirm"));
    }

    @Bean
    RequestMatcher customerAuthenticationEndpoints() {
        return new OrRequestMatcher(
                endpoint(CustomerSecurityRoutes.MEMBER_API),
                endpoint(CustomerSecurityRoutes.MEMBER_API_PATTERN),
                endpoint(CustomerSecurityRoutes.PAYMENT_API_PATTERN),
                endpoint(CustomerSecurityRoutes.CLIENT_MONITORING_API));
    }

    @Bean
    RequestMatcher sensitiveCustomerResponses() {
        return new OrRequestMatcher(
                endpoint(CustomerSecurityRoutes.AUTH_API_PATTERN),
                endpoint(CustomerSecurityRoutes.MEMBER_API),
                endpoint(CustomerSecurityRoutes.MEMBER_API_PATTERN),
                endpoint(CustomerSecurityRoutes.PAYMENT_API_PATTERN),
                endpoint(CustomerSecurityRoutes.GUEST_RECORD_API_PATTERN),
                new RequestHeaderRequestMatcher(GUEST_ACCESS_TOKEN_HEADER));
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter rateLimitFilter) {
        var registration = new FilterRegistrationBean<>(rateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @Order(1)
    SecurityFilterChain adminSecurityFilterChain(HttpSecurity http,
                                                 AuthenticationManager adminAuthenticationManager,
                                                 @Qualifier("publicAdminEndpoints")
                                                 RequestMatcher publicAdminEndpoints,
                                                 @Qualifier("adminMfaEnrollmentEndpoints")
                                                 RequestMatcher adminMfaEnrollmentEndpoints,
                                                 AdminBearerTokenResolver adminBearerTokenResolver,
                                                 RateLimitFilter rateLimitFilter,
                                                 ObjectMapper objectMapper) throws Exception {
        AuthenticationEntryPoint entryPoint = authenticationEntryPoint(objectMapper, ADMIN_LOGIN_REQUIRED);
        AccessDeniedHandler accessDeniedHandler = accessDeniedHandler(objectMapper);
        AuthenticationFailureHandler failureHandler = (request, response, exception) ->
                entryPoint.commence(request, response, exception);

        http.securityMatcher("/api/v1/admin", "/api/v1/admin/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(publicAdminEndpoints)
                        .permitAll()
                        .requestMatchers(adminMfaEnrollmentEndpoints)
                        .authenticated()
                        .anyRequest().hasRole("ADMIN"))
                .addFilterAfter(rateLimitFilter, HeaderWriterFilter.class)
                .addFilterBefore(
                        new AdminAuthenticationFilter(
                                adminAuthenticationManager,
                                failureHandler,
                                publicAdminEndpoints,
                                adminBearerTokenResolver),
                        AnonymousAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .sessionFixation(fixation -> fixation.none()))
                .csrf(csrf -> csrf.disable())
                .requestCache(cache -> cache.disable())
                .logout(logout -> logout.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .headers(headers -> headers
                        .cacheControl(cache -> cache.disable())
                        .addHeaderWriter(
                                new StaticHeadersWriter(HttpHeaders.CACHE_CONTROL, "no-store")));

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain customerSecurityFilterChain(HttpSecurity http,
                                                    CustomerAuthUseCase customerAuthUseCase,
                                                    CookieCsrfTokenRepository csrfTokenRepository,
                                                    SocialLoginAuthenticationHandler socialLoginHandler,
                                                    SocialOAuth2AuthorizationRequestResolver
                                                            socialAuthorizationRequestResolver,
                                                    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
                                                            socialOAuth2AccessTokenResponseClient,
                                                    @Qualifier("socialOAuth2UserService")
                                                    OAuth2UserService<OAuth2UserRequest, OAuth2User>
                                                            socialOAuth2UserService,
                                                    @Qualifier("googleOidcUserService")
                                                    OAuth2UserService<OidcUserRequest, OidcUser> googleOidcUserService,
                                                    @Qualifier("customerAuthenticationEndpoints")
                                                    RequestMatcher customerAuthenticationEndpoints,
                                                    @Qualifier("sensitiveCustomerResponses")
                                                    RequestMatcher sensitiveCustomerResponses,
                                                    RateLimitFilter rateLimitFilter,
                                                    ObjectMapper objectMapper) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                CustomerSecurityRoutes.MEMBER_API,
                                CustomerSecurityRoutes.MEMBER_API_PATTERN)
                        .hasRole("CUSTOMER")
                        .requestMatchers("/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_READ_PATHS)
                        .permitAll()
                        .requestMatchers(HttpMethod.HEAD, PUBLIC_READ_PATHS)
                        .permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/signup",
                                "/api/v1/auth/login",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/password/reset",
                                CustomerSecurityRoutes.SOCIAL_SIGNUP_INTENT_PROVIDER_PATH,
                                "/api/v1/payments/prepare",
                                "/api/v1/payments/confirm",
                                CustomerSecurityRoutes.CLIENT_MONITORING_API,
                                "/api/v1/webhooks/delivery-tracking",
                                "/api/v1/guest-records/recovery",
                                "/api/v1/guest-records/payment-status-recovery",
                                "/api/v1/bookings/phone-verifications",
                                "/api/v1/orders/{orderId}/claims",
                                "/api/v1/orders/{id}/delay-response")
                        .permitAll()
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/bookings/{bookingId}/reschedule")
                        .permitAll()
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/bookings/{bookingId}",
                                "/api/v1/orders/{id}")
                        .permitAll()
                        .anyRequest().denyAll())
                .addFilterAfter(rateLimitFilter, HeaderWriterFilter.class)
                .addFilterBefore(
                        new CustomerAuthenticationFilter(
                                customerAuthUseCase,
                                customerAuthenticationEndpoints),
                        AnonymousAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint(objectMapper, CUSTOMER_LOGIN_REQUIRED))
                        .accessDeniedHandler(accessDeniedHandler(objectMapper)))
                .securityContext(context -> context
                        .securityContextRepository(new NullSecurityContextRepository()))
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.none()))
                .csrf(csrf -> csrf
                        .spa()
                        .csrfTokenRepository(csrfTokenRepository)
                        .ignoringRequestMatchers(endpoint("/api/v1/webhooks/delivery-tracking")))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri(CustomerSecurityRoutes.SOCIAL_AUTHORIZATION_BASE_URI)
                                .authorizationRequestResolver(socialAuthorizationRequestResolver))
                        .redirectionEndpoint(endpoint -> endpoint
                                .baseUri(CustomerSecurityRoutes.SOCIAL_CALLBACK_BASE_URI))
                        .tokenEndpoint(endpoint -> endpoint
                                .accessTokenResponseClient(socialOAuth2AccessTokenResponseClient))
                        .userInfoEndpoint(endpoint -> endpoint
                                .userService(socialOAuth2UserService)
                                .oidcUserService(googleOidcUserService))
                        .authorizedClientRepository(new DiscardingOAuth2AuthorizedClientRepository())
                        .successHandler(socialLoginHandler)
                        .failureHandler(socialLoginHandler))
                .requestCache(cache -> cache.disable())
                .logout(logout -> logout.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .headers(headers -> headers
                        .cacheControl(cache -> cache.disable())
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                sensitiveCustomerResponses,
                                new StaticHeadersWriter(HttpHeaders.CACHE_CONTROL, "no-store"))));

        return http.build();
    }

    private AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper, String message) {
        return (request, response, exception) ->
                FilterErrorResponseWriter.write(response, objectMapper, ErrorCode.UNAUTHORIZED, message);
    }

    private AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, exception) ->
                FilterErrorResponseWriter.write(response, objectMapper, ErrorCode.FORBIDDEN);
    }

    private static RequestMatcher adminEndpoint(HttpMethod method, String path) {
        return PathPatternRequestMatcher.withDefaults().matcher(method, path);
    }

    private static RequestMatcher endpoint(String path) {
        return PathPatternRequestMatcher.withDefaults().matcher(path);
    }
}
