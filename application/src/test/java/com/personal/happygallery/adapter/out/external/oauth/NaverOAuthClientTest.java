package com.personal.happygallery.adapter.out.external.oauth;

import com.personal.happygallery.application.customer.port.out.OAuthTokenExchangePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NaverOAuthClientTest {

    @DisplayName("네이버 인증 URL은 redirectUri와 state를 인코딩해 생성한다")
    @Test
    void buildAuthorizationUrl_encodesRedirectUriAndState() {
        NaverOAuthClient client = new NaverOAuthClient(RestClient.builder().build(), properties());

        OAuthTokenExchangePort.AuthorizationUrl result = client.buildAuthorizationUrl(
                "https://happy.test/oauth/callback?next=/me",
                "state value");

        assertSoftly(softly -> {
            softly.assertThat(result.state()).isEqualTo("state value");
            softly.assertThat(result.url()).contains("response_type=code");
            softly.assertThat(result.url()).contains("client_id=client-id");
            softly.assertThat(result.url())
                    .contains("redirect_uri=https%3A%2F%2Fhappy.test%2Foauth%2Fcallback%3Fnext%3D%2Fme");
            softly.assertThat(result.url()).contains("state=state+value");
        });
    }

    @DisplayName("네이버 코드 교환은 state를 포함한 form 요청 후 Bearer 토큰으로 프로필을 조회한다")
    @Test
    void exchangeCodeForUserInfo_sendsStateAndMapsNestedProfile() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://oauth.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverOAuthClient client = new NaverOAuthClient(builder.build(), properties());

        expectTokenExchange(server);
        server.expect(requestTo("https://oauth.test/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andRespond(withSuccess("""
                        {
                          "resultcode": "00",
                          "message": "success",
                          "response": {
                            "id": "naver-id",
                            "email": "naver@example.com",
                            "name": "네이버회원"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        OAuthTokenExchangePort.OAuthUserInfo userInfo = client.exchangeCodeForUserInfo(
                "auth-code",
                "https://happy.test/oauth/callback",
                "state-value");

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(userInfo.providerId()).isEqualTo("naver-id");
            softly.assertThat(userInfo.email()).isEqualTo("naver@example.com");
            softly.assertThat(userInfo.name()).isEqualTo("네이버회원");
        });
    }

    @DisplayName("네이버 프로필에 이메일이나 이름이 없으면 소셜 로그인에 실패한다")
    @ParameterizedTest
    @ValueSource(strings = {
            "{\"id\":\"naver-id\",\"name\":\"네이버회원\"}",
            "{\"id\":\"naver-id\",\"email\":\"naver@example.com\"}"
    })
    void exchangeCodeForUserInfo_rejectsMissingRequiredProfile(String profileJson) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://oauth.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverOAuthClient client = new NaverOAuthClient(builder.build(), properties());

        expectTokenExchange(server);
        server.expect(requestTo("https://oauth.test/userinfo"))
                .andRespond(withSuccess("{\"response\":" + profileJson + "}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.exchangeCodeForUserInfo(
                "auth-code",
                "https://happy.test/oauth/callback",
                "state-value"))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SOCIAL_LOGIN_FAILED);
        server.verify();
    }

    private static void expectTokenExchange(MockRestServiceServer server) {
        server.expect(requestTo("https://oauth.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(Matchers.containsString("grant_type=authorization_code")))
                .andExpect(content().string(Matchers.containsString("client_id=client-id")))
                .andExpect(content().string(Matchers.containsString("client_secret=client-secret")))
                .andExpect(content().string(Matchers.containsString("code=auth-code")))
                .andExpect(content().string(Matchers.containsString("state=state-value")))
                .andRespond(withSuccess("{\"access_token\":\"access-token\"}", MediaType.APPLICATION_JSON));
    }

    private static NaverOAuthProperties properties() {
        return new NaverOAuthProperties(
                "client-id",
                "client-secret",
                "https://oauth.test/token",
                "https://oauth.test/userinfo",
                5_000,
                2_000,
                1_000,
                10,
                30_000);
    }
}
