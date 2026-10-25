package com.personal.happygallery.adapter.in.web.config;

import com.personal.happygallery.adapter.in.web.FilterErrorResponseWriter;
import com.personal.happygallery.adapter.in.web.RateLimitFilter;
import com.personal.happygallery.adapter.in.web.config.properties.AdminProperties;
import com.personal.happygallery.adapter.in.web.security.admin.AdminAuthenticationFilter;
import com.personal.happygallery.adapter.in.web.security.admin.AdminAuthenticationProvider;
import com.personal.happygallery.adapter.in.web.security.admin.AdminBearerTokenResolver;
import com.personal.happygallery.adapter.in.web.security.admin.AdminSecurityRoutes;
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
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

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
            CustomerSecurityRoutes.ORDER_PATH,
            "/api/v1/orders/{orderId}/claims",
            "/api/v1/products",
            "/api/v1/products/categories",
            "/api/v1/products/{id}",
            "/api/v1/products/{productId}/qna/page",
            "/api/v1/products/{productId}/qna/{id}",
            "/api/v1/products/{productId}/reviews",
            "/api/v1/media/images/{fileName}",
            "/api/v1/workshop",
            CustomerSecurityRoutes.BOT_PROTECTION_API,
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
                pathPattern(HttpMethod.POST, AdminSecurityRoutes.LOGIN_PATH),
                pathPattern(HttpMethod.POST, AdminSecurityRoutes.MFA_VERIFY_PATH),
                pathPattern(HttpMethod.POST, AdminSecurityRoutes.LOGOUT_PATH),
                pathPattern(HttpMethod.POST, AdminSecurityRoutes.SETUP_PATH),
                pathPattern(HttpMethod.GET, AdminSecurityRoutes.SETUP_STATUS_PATH),
                pathPattern(HttpMethod.HEAD, AdminSecurityRoutes.SETUP_STATUS_PATH));
    }

    @Bean
    RequestMatcher adminMfaEnrollmentEndpoints() {
        return new OrRequestMatcher(
                pathPattern(HttpMethod.GET, AdminSecurityRoutes.MFA_PATH),
                pathPattern(HttpMethod.HEAD, AdminSecurityRoutes.MFA_PATH),
                pathPattern(HttpMethod.POST, AdminSecurityRoutes.MFA_ENROLLMENT_PATH),
                pathPattern(HttpMethod.POST, AdminSecurityRoutes.MFA_ENROLLMENT_CONFIRM_PATH));
    }

    @Bean
    RequestMatcher customerAuthenticationEndpoints() {
        return new OrRequestMatcher(
                pathPattern(CustomerSecurityRoutes.MEMBER_API),
                pathPattern(CustomerSecurityRoutes.MEMBER_API_PATTERN),
                pathPattern(CustomerSecurityRoutes.PAYMENT_API_PATTERN),
                pathPattern(CustomerSecurityRoutes.CLIENT_MONITORING_API));
    }

    @Bean
    RequestMatcher sensitiveCustomerResponses() {
        return new OrRequestMatcher(
                pathPattern(CustomerSecurityRoutes.AUTH_API_PATTERN),
                pathPattern(CustomerSecurityRoutes.MEMBER_API),
                pathPattern(CustomerSecurityRoutes.MEMBER_API_PATTERN),
                pathPattern(CustomerSecurityRoutes.PAYMENT_API_PATTERN),
                pathPattern(CustomerSecurityRoutes.GUEST_RECORD_API_PATTERN),
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

        http.securityMatcher(AdminSecurityRoutes.ADMIN_API, AdminSecurityRoutes.ADMIN_API_PATTERN)
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
                                CustomerSecurityRoutes.GROUP_INQUIRY_PATH,
                                CustomerSecurityRoutes.SIGNUP_PATH,
                                CustomerSecurityRoutes.LOGIN_PATH,
                                "/api/v1/auth/logout",
                                CustomerSecurityRoutes.PASSWORD_RESET_PATH,
                                CustomerSecurityRoutes.SOCIAL_SIGNUP_INTENT_PROVIDER_PATH,
                                CustomerSecurityRoutes.SOCIAL_SIGNUP_COMPLETION_PATH,
                                CustomerSecurityRoutes.PAYMENT_PREPARE_PATH,
                                CustomerSecurityRoutes.PAYMENT_CONFIRM_PATH,
                                "/api/v1/payments/{orderId}/abandon",
                                CustomerSecurityRoutes.CLIENT_MONITORING_API,
                                CustomerSecurityRoutes.DELIVERY_TRACKING_WEBHOOK_PATH,
                                CustomerSecurityRoutes.TOSS_PAYMENTS_WEBHOOK_PATH,
                                CustomerSecurityRoutes.GUEST_RECORD_RECOVERY_PATH,
                                CustomerSecurityRoutes.GUEST_RECORD_PAYMENT_STATUS_RECOVERY_PATH,
                                CustomerSecurityRoutes.PHONE_VERIFICATION_PATH,
                                "/api/v1/slots/{slotId}/vacancy-alerts",
                                "/api/v1/orders/{orderId}/claims",
                                CustomerSecurityRoutes.ORDER_DELAY_RESPONSE_PATH)
                        .permitAll()
                        .requestMatchers(HttpMethod.PUT, CustomerSecurityRoutes.ORDER_SHIPPING_ADDRESS_PATH)
                        .permitAll()
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/bookings/{bookingId}/reschedule",
                                "/api/v1/bookings/{bookingId}/participants")
                        .permitAll()
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/bookings/{bookingId}",
                                "/api/v1/slots/{slotId}/vacancy-alerts",
                                CustomerSecurityRoutes.ORDER_PATH)
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
                        .ignoringRequestMatchers(
                                pathPattern(CustomerSecurityRoutes.DELIVERY_TRACKING_WEBHOOK_PATH),
                                pathPattern(CustomerSecurityRoutes.TOSS_PAYMENTS_WEBHOOK_PATH)))
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
}
