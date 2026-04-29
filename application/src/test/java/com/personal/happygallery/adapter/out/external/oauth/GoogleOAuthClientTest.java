package com.personal.happygallery.adapter.out.external.oauth;

import com.personal.happygallery.application.customer.port.out.OAuthTokenExchangePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.hamcrest.Matchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleOAuthClientTest {

    @DisplayName("구글 인증 URL은 redirectUri와 state를 인코딩해 생성한다")
    @Test
    void buildAuthorizationUrl_encodesRedirectUriAndState() {
        GoogleOAuthClient client = new GoogleOAuthClient(RestClient.builder().build(), properties());

        OAuthTokenExchangePort.AuthorizationUrl result = client.buildAuthorizationUrl(
                "https://happy.test/oauth/callback?next=/me",
                "state value");

        assertSoftly(softly -> {
            softly.assertThat(result.state()).isEqualTo("state value");
            softly.assertThat(result.url()).contains("client_id=client-id");
            softly.assertThat(result.url()).contains("redirect_uri=https%3A%2F%2Fhappy.test%2Foauth%2Fcallback%3Fnext%3D%2Fme");
            softly.assertThat(result.url()).contains("scope=openid+email+profile");
            softly.assertThat(result.url()).contains("state=state+value");
        });
    }

    @DisplayName("구글 코드 교환은 form 요청 후 Bearer 토큰으로 사용자 정보를 조회한다")
    @Test
    void exchangeCodeForUserInfo_sendsTokenFormAndUserInfoRequest() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://oauth.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GoogleOAuthClient client = new GoogleOAuthClient(builder.build(), properties());

        server.expect(requestTo("https://oauth.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(Matchers.containsString("grant_type=authorization_code")))
                .andExpect(content().string(Matchers.containsString("code=auth-code")))
                .andExpect(content().string(Matchers.containsString("client_id=client-id")))
                .andExpect(content().string(Matchers.containsString("client_secret=client-secret")))
                .andExpect(content().string(Matchers.containsString("redirect_uri=https%3A%2F%2Fhappy.test%2Foauth%2Fcallback")))
                .andRespond(withSuccess("""
                        {
                          "access_token": "access-token"
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://oauth.test/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andRespond(withSuccess("""
                        {
                          "sub": "google-sub",
                          "email": "google@example.com",
                          "name": "구글회원"
                        }
                        """, MediaType.APPLICATION_JSON));

        OAuthTokenExchangePort.OAuthUserInfo userInfo = client.exchangeCodeForUserInfo(
                "auth-code",
                "https://happy.test/oauth/callback");

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(userInfo.providerId()).isEqualTo("google-sub");
            softly.assertThat(userInfo.email()).isEqualTo("google@example.com");
            softly.assertThat(userInfo.name()).isEqualTo("구글회원");
        });
    }

    private static GoogleOAuthProperties properties() {
        return new GoogleOAuthProperties(
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
