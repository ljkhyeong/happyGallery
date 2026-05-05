package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.domain.notification.NotificationEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NotificationSenderContractTest {

    @DisplayName("카카오 알림톡은 이벤트별 템플릿 코드와 수신자 정보를 JSON 요청으로 보낸다")
    @Test
    void kakao_send_sendsTemplatePayload() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://bizapi.kakao.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoAlimtalkSender sender = new KakaoAlimtalkSender(kakaoProperties(), builder.build(), new KakaoTemplateCatalog());

        server.expect(requestTo("https://bizapi.kakao.com/v1/api/talk/friends/message/default/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "senderKey": "sender-key",
                          "templateCode": "HG_BOOKING_CONFIRMED",
                          "recipientNo": "01012345678",
                          "templateArgs": {
                            "name": "홍길동"
                          }
                        }
                        """))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        boolean sent = sender.send("01012345678", "홍길동", NotificationEventType.BOOKING_CONFIRMED);

        server.verify();
        assertSoftly(softly -> softly.assertThat(sent).isTrue());
    }

    @DisplayName("SMS 발송은 NHN Cloud 경로에 발신번호 수신자 메시지를 JSON 요청으로 보낸다")
    @Test
    void sms_send_sendsMessagePayload() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api-sms.cloud.toast.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RealSmsSender sender = new RealSmsSender(smsProperties(), builder.build(), new SmsMessageCatalog());

        server.expect(requestTo("https://api-sms.cloud.toast.com/sms/v3.0/appKeys/api-key/sender/sms"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "body": "[해피갤러리] 홍길동님, 오늘 체험이 예정되어 있습니다.",
                          "sendNo": "0212345678",
                          "recipientList": [
                            {
                              "recipientNo": "01012345678"
                            }
                          ]
                        }
                        """))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        boolean sent = sender.send("01012345678", "홍길동", NotificationEventType.REMINDER_SAME_DAY);

        server.verify();
        assertSoftly(softly -> softly.assertThat(sent).isTrue());
    }

    private static KakaoNotificationProperties kakaoProperties() {
        return new KakaoNotificationProperties(
                "api-key",
                "sender-key",
                "https://bizapi.kakao.com",
                5_000,
                2_000,
                1_000,
                20,
                30_000);
    }

    private static SmsNotificationProperties smsProperties() {
        return new SmsNotificationProperties(
                "api-key",
                "api-secret",
                "0212345678",
                "https://api-sms.cloud.toast.com",
                5_000,
                2_000,
                1_000,
                20,
                30_000);
    }
}
