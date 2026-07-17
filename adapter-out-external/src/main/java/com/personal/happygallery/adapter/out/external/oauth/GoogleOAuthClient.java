package com.personal.happygallery.adapter.out.external.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.personal.happygallery.application.customer.port.out.OAuthTokenExchangePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.SocialProvider;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@Profile("prod")
class GoogleOAuthClient implements OAuthTokenExchangePort {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthClient.class);
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";

    private final RestClient restClient;
    private final GoogleOAuthProperties props;

    GoogleOAuthClient(RestClient googleOAuthRestClient, GoogleOAuthProperties props) {
        this.restClient = googleOAuthRestClient;
        this.props = props;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public AuthorizationUrl buildAuthorizationUrl(String redirectUri, String state) {
        String url = GOOGLE_AUTH_URL
                + "?client_id=" + encode(props.clientId())
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode("openid email profile")
                + "&state=" + encode(state)
                + "&access_type=offline"
                + "&prompt=consent";
        return new AuthorizationUrl(url, state);
    }

    @Override
    public OAuthUserInfo exchangeCodeForUserInfo(String authorizationCode, String redirectUri, String state) {
        String accessToken = exchangeToken(authorizationCode, redirectUri);
        return fetchUserInfo(accessToken);
    }

    private String exchangeToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());
        form.add("redirect_uri", redirectUri);

        TokenResponse response = restClient.post()
                .uri(props.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            log.error("Google OAuth token exchange failed: null response");
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        return response.accessToken();
    }

    private OAuthUserInfo fetchUserInfo(String accessToken) {
        UserInfoResponse response = restClient.get()
                .uri(props.userInfoUrl())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(UserInfoResponse.class);

        if (response == null
                || !StringUtils.hasText(response.sub())
                || !StringUtils.hasText(response.email())
                || !StringUtils.hasText(response.name())
                || !Boolean.TRUE.equals(response.emailVerified())) {
            log.error("Google OAuth userinfo fetch failed: required verified profile field missing");
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        return new OAuthUserInfo(response.sub(), response.email(), response.name());
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {}

    private record UserInfoResponse(
            String sub,
            String email,
            String name,
            @JsonProperty("email_verified") Boolean emailVerified
    ) {}

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
