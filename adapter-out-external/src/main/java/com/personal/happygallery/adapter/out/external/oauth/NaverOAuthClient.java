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
class NaverOAuthClient implements OAuthTokenExchangePort {

    private static final Logger log = LoggerFactory.getLogger(NaverOAuthClient.class);
    private static final String NAVER_AUTH_URL = "https://nid.naver.com/oauth2.0/authorize";

    private final RestClient restClient;
    private final NaverOAuthProperties props;

    NaverOAuthClient(RestClient naverOAuthRestClient, NaverOAuthProperties props) {
        this.restClient = naverOAuthRestClient;
        this.props = props;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.NAVER;
    }

    @Override
    public AuthorizationUrl buildAuthorizationUrl(String redirectUri, String state) {
        String url = NAVER_AUTH_URL
                + "?response_type=code"
                + "&client_id=" + encode(props.clientId())
                + "&redirect_uri=" + encode(redirectUri)
                + "&state=" + encode(state);
        return new AuthorizationUrl(url, state);
    }

    @Override
    public OAuthUserInfo exchangeCodeForUserInfo(String authorizationCode, String redirectUri, String state) {
        String accessToken = exchangeToken(authorizationCode, state);
        return fetchUserInfo(accessToken);
    }

    private String exchangeToken(String code, String state) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());
        form.add("code", code);
        form.add("state", state);

        TokenResponse response = restClient.post()
                .uri(props.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || !StringUtils.hasText(response.accessToken())) {
            log.error("Naver OAuth token exchange failed: access token missing");
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        return response.accessToken();
    }

    private OAuthUserInfo fetchUserInfo(String accessToken) {
        UserInfoEnvelope envelope = restClient.get()
                .uri(props.userInfoUrl())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(UserInfoEnvelope.class);

        UserInfoResponse response = envelope == null ? null : envelope.response();
        if (response == null
                || !StringUtils.hasText(response.id())
                || !StringUtils.hasText(response.email())
                || !StringUtils.hasText(response.name())) {
            log.error("Naver OAuth userinfo fetch failed: required profile field missing");
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        return new OAuthUserInfo(response.id(), response.email(), response.name());
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {}

    private record UserInfoEnvelope(UserInfoResponse response) {}

    private record UserInfoResponse(String id, String email, String name) {}

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
