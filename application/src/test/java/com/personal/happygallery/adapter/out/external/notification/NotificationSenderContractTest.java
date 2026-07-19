package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.domain.notification.NotificationEventType;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NotificationSenderContractTest {

    private static final String IDEMPOTENCY_KEY = "USER:10:BOOKING_CONFIRMED:BOOKING:20";

    @DisplayName("NHN 알림톡은 v2.2 경로와 인증·멱등 헤더로 템플릿 치환 요청을 보낸다")
    @Test
    void alimtalk_send_sendsV22TemplatePayload() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://kakaotalk-bizmessage.api.nhncloudservice.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NhnAlimtalkSender sender = new NhnAlimtalkSender(alimtalkProperties(), builder.build());

        server.expect(requestTo("https://kakaotalk-bizmessage.api.nhncloudservice.com"
                        + "/alimtalk/v2.2/appkeys/app-key/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Secret-Key", "secret-key"))
                .andExpect(header("X-NC-API-IDEMPOTENCY-KEY", IDEMPOTENCY_KEY))
                .andExpect(content().json("""
                        {
                          "senderKey": "sender-key",
                          "templateCode": "HG_BOOKING_CONFIRMED",
                          "recipientList": [
                            {
                              "recipientNo": "01012345678",
                              "templateParameter": {
                                "name": "홍길동"
                              }
                            }
                          ]
                        }
                        """))
                .andRespond(withSuccess(alimtalkSuccessResponse(), MediaType.APPLICATION_JSON));

        boolean sent = sender.send(
                IDEMPOTENCY_KEY, "01012345678", "홍길동", NotificationEventType.BOOKING_CONFIRMED);

        server.verify();
        assertSoftly(softly -> softly.assertThat(sent).isTrue());
    }

    @DisplayName("NHN 알림톡의 수신자별 결과 코드가 실패면 HTTP 200도 발송 실패로 판정한다")
    @Test
    void alimtalk_send_rejectsRecipientFailureResponse() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://kakaotalk-bizmessage.api.nhncloudservice.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NhnAlimtalkSender sender = new NhnAlimtalkSender(alimtalkProperties(), builder.build());

        server.expect(requestTo("https://kakaotalk-bizmessage.api.nhncloudservice.com"
                        + "/alimtalk/v2.2/appkeys/app-key/messages"))
                .andRespond(withSuccess("""
                        {
                          "header": {
                            "isSuccessful": true,
                            "resultCode": 0,
                            "resultMessage": "SUCCESS"
                          },
                          "message": {
                            "requestId": "request-id",
                            "sendResults": [
                              {
                                "recipientSeq": 1,
                                "resultCode": -1000,
                                "resultMessage": "Invalid recipient."
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        boolean sent = sender.send(
                IDEMPOTENCY_KEY, "01012345678", "홍길동", NotificationEventType.BOOKING_CONFIRMED);

        server.verify();
        assertSoftly(softly -> softly.assertThat(sent).isFalse());
    }

    @DisplayName("모든 알림톡 이벤트는 NHN 템플릿 코드 길이 계약을 지킨다")
    @Test
    void alimtalk_templateCodes_fitProviderContract() {
        assertSoftly(softly -> Arrays.stream(NotificationEventType.values()).forEach(eventType ->
                softly.assertThat(KakaoTemplateCatalog.resolveTemplateCode(eventType))
                        .as(eventType.name())
                        .isNotBlank()
                        .hasSizeLessThanOrEqualTo(20)));
    }

    @DisplayName("SMS 발송은 NHN Cloud 경로에 발신번호 수신자 메시지를 JSON 요청으로 보낸다")
    @Test
    void sms_send_sendsMessagePayload() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://sms.api.nhncloudservice.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RealSmsSender sender = new RealSmsSender(smsProperties(), builder.build());

        server.expect(requestTo("https://sms.api.nhncloudservice.com/sms/v3.0/appKeys/api-key/sender/sms"))
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
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        boolean sent = sender.send(
                IDEMPOTENCY_KEY, "01012345678", "홍길동", NotificationEventType.REMINDER_SAME_DAY);

        server.verify();
        assertSoftly(softly -> softly.assertThat(sent).isTrue());
    }

    @DisplayName("휴대폰 인증 SMS는 인증 코드와 유효 시간을 NHN Cloud 요청으로 보낸다")
    @Test
    void phoneVerification_send_sendsCodePayload() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://sms.api.nhncloudservice.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RealPhoneVerificationSender sender = new RealPhoneVerificationSender(smsProperties(), builder.build());

        server.expect(requestTo("https://sms.api.nhncloudservice.com/sms/v3.0/appKeys/api-key/sender/auth/sms"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(headerDoesNotExist("X-NC-API-IDEMPOTENCY-KEY"))
                .andExpect(content().json("""
                        {
                          "body": "[해피갤러리] 인증번호는 123456입니다. 5분 안에 입력해주세요.",
                          "sendNo": "0212345678",
                          "recipientList": [
                            {
                              "recipientNo": "01012345678"
                            }
                          ]
                        }
                        """))
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        boolean sent = sender.send("01012345678", "123456");

        server.verify();
        assertSoftly(softly -> softly.assertThat(sent).isTrue());
    }

    @DisplayName("NHN Cloud가 HTTP 200 본문으로 실패를 반환하면 SMS 발송 실패로 판정한다")
    @Test
    void sms_send_rejectsLogicalFailureResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://sms.api.nhncloudservice.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RealSmsSender sender = new RealSmsSender(smsProperties(), builder.build());

        server.expect(requestTo("https://sms.api.nhncloudservice.com/sms/v3.0/appKeys/api-key/sender/sms"))
                .andRespond(withSuccess("""
                        {
                          "header": {
                            "isSuccessful": false,
                            "resultCode": -1000,
                            "resultMessage": "Invalid appKey."
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        boolean sent = sender.send(
                IDEMPOTENCY_KEY, "01012345678", "홍길동", NotificationEventType.REMINDER_SAME_DAY);

        server.verify();
        assertSoftly(softly -> softly.assertThat(sent).isFalse());
    }

    private static String successResponse() {
        return """
                {
                  "header": {
                    "isSuccessful": true,
                    "resultCode": 0,
                    "resultMessage": "SUCCESS"
                  }
                }
                """;
    }

    private static String alimtalkSuccessResponse() {
        return """
                {
                  "header": {
                    "isSuccessful": true,
                    "resultCode": 0,
                    "resultMessage": "SUCCESS"
                  },
                  "message": {
                    "requestId": "request-id",
                    "sendResults": [
                      {
                        "recipientSeq": 1,
                        "resultCode": 0,
                        "resultMessage": "SUCCESS"
                      }
                    ]
                  }
                }
                """;
    }

    private static AlimtalkNotificationProperties alimtalkProperties() {
        return new AlimtalkNotificationProperties(
                "app-key",
                "secret-key",
                "sender-key",
                "https://kakaotalk-bizmessage.api.nhncloudservice.com",
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
                "https://sms.api.nhncloudservice.com",
                5_000,
                2_000,
                1_000,
                20,
                30_000);
    }
}
