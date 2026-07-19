package com.personal.happygallery.adapter.in.web.config;

import com.personal.happygallery.adapter.in.web.FilterErrorResponseWriter;
import com.personal.happygallery.adapter.in.web.config.properties.AdminProperties;
import com.personal.happygallery.adapter.in.web.security.admin.AdminAuthenticationFilter;
import com.personal.happygallery.adapter.in.web.security.admin.AdminAuthenticationProvider;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerAuthenticationFilter;
import com.personal.happygallery.adapter.in.web.security.customer.DiscardingOAuth2AuthorizedClientRepository;
import com.personal.happygallery.adapter.in.web.security.customer.SocialLoginAuthenticationHandler;
import com.personal.happygallery.application.admin.port.in.AdminAuthUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.domain.error.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    private static final String ADMIN_LOGIN_REQUIRED = "관리자 인증이 필요합니다.";
    private static final String CUSTOMER_LOGIN_REQUIRED = "로그인이 필요합니다.";
    private static final String SOCIAL_AUTHORIZATION_BASE_URI = "/api/v1/auth/social/authorization";
    private static final String SOCIAL_CALLBACK_BASE_URI = "/api/v1/auth/social/callback/*";
    private static final RequestMatcher SOCIAL_OAUTH_ENDPOINTS = new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher(SOCIAL_AUTHORIZATION_BASE_URI + "/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/social/callback/**"));

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
                adminEndpoint(HttpMethod.POST, "/api/v1/admin/auth/logout"),
                adminEndpoint(HttpMethod.POST, "/api/v1/admin/setup"),
                adminEndpoint(HttpMethod.GET, "/api/v1/admin/setup/status"));
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    @Order(1)
    SecurityFilterChain adminSecurityFilterChain(HttpSecurity http,
                                                 AuthenticationManager adminAuthenticationManager,
                                                 @Qualifier("publicAdminEndpoints")
                                                 RequestMatcher publicAdminEndpoints,
                                                 ObjectMapper objectMapper) throws Exception {
        AuthenticationEntryPoint entryPoint = authenticationEntryPoint(objectMapper, ADMIN_LOGIN_REQUIRED);
        AccessDeniedHandler accessDeniedHandler = accessDeniedHandler(objectMapper);
        AuthenticationFailureHandler failureHandler = (request, response, exception) ->
                entryPoint.commence(request, response, exception);

        http.securityMatcher("/api/v1/admin", "/api/v1/admin/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(publicAdminEndpoints)
                        .permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .addFilterBefore(
                        new AdminAuthenticationFilter(
                                adminAuthenticationManager,
                                failureHandler,
                                publicAdminEndpoints),
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
                .headers(headers -> headers.cacheControl(cache -> cache.disable()));

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain customerSecurityFilterChain(HttpSecurity http,
                                                    CustomerAuthUseCase customerAuthUseCase,
                                                    CookieCsrfTokenRepository csrfTokenRepository,
                                                    SocialLoginAuthenticationHandler socialLoginHandler,
                                                    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
                                                            socialOAuth2AccessTokenResponseClient,
                                                    @Qualifier("naverOAuth2UserService")
                                                    OAuth2UserService<OAuth2UserRequest, OAuth2User>
                                                            naverOAuth2UserService,
                                                    @Qualifier("googleOidcUserService")
                                                    OAuth2UserService<OidcUserRequest, OidcUser> googleOidcUserService,
                                                    ObjectMapper objectMapper) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/me", "/api/v1/me/**").hasRole("CUSTOMER")
                        .requestMatchers(
                                "/error",
                                "/actuator",
                                "/actuator/health", "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/metrics", "/actuator/metrics/**",
                                "/actuator/prometheus",
                                "/api/v1/auth/**",
                                "/api/v1/payments/**",
                                "/api/v1/monitoring/client-events",
                                "/api/v1/bookings/**",
                                "/api/v1/orders/**",
                                "/api/v1/products/**",
                                "/api/v1/classes/**",
                                "/api/v1/slots/**",
                                "/api/v1/notices/**")
                        .permitAll()
                        .anyRequest().denyAll())
                .addFilterBefore(
                        new CustomerAuthenticationFilter(customerAuthUseCase),
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
                        .csrfTokenRepository(csrfTokenRepository))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri(SOCIAL_AUTHORIZATION_BASE_URI))
                        .redirectionEndpoint(endpoint -> endpoint
                                .baseUri(SOCIAL_CALLBACK_BASE_URI))
                        .tokenEndpoint(endpoint -> endpoint
                                .accessTokenResponseClient(socialOAuth2AccessTokenResponseClient))
                        .userInfoEndpoint(endpoint -> endpoint
                                .userService(naverOAuth2UserService)
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
                                SOCIAL_OAUTH_ENDPOINTS,
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
}
