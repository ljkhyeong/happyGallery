package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class NcpEmailVerificationSenderTest {
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final NcpEmailVerificationSender sender = new NcpEmailVerificationSender(
            builder.build(), credentials(), emailProperties(),
            Clock.fixed(Instant.ofEpochMilli(1789174800000L), ZoneOffset.UTC));

    @Test
    @DisplayName("한국 리전 메일 요청에 공식 HMAC 서명과 단일 인증 수신자를 담는다")
    void sendsSignedKoreanRegionRequest() {
        server.expect(requestTo("https://mail.apigw.ntruss.com/api/v1/mails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-ncp-apigw-timestamp", "1789174800000"))
                .andExpect(header("x-ncp-iam-access-key", "test-access-key"))
                .andExpect(header("x-ncp-apigw-signature-v2", "sUivhBVe0ZI9YKhZPcvrMaOmFgI67grJuG5OI+T9nrE="))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.senderAddress").value("no-reply@mail.happy-gallery.com"))
                .andExpect(jsonPath("$.title").value("이메일 인증번호"))
                .andExpect(jsonPath("$.body").value(containsString("123456")))
                .andExpect(jsonPath("$.recipients.length()").value(1))
                .andExpect(jsonPath("$.recipients[0].address").value("member@example.com"))
                .andExpect(jsonPath("$.recipients[0].type").value("R"))
                .andExpect(jsonPath("$.advertising").value(false))
                .andExpect(jsonPath("$.individual").value(true))
                .andExpect(jsonPath("$.confirmAndSend").value(false))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"requestId\":\"mail-request-1\",\"count\":1}"));

        assertThat(sender.sendResult("member@example.com", "123456")).isEqualTo(NotificationSendResult.SUCCESS);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"requestId\":\"\",\"count\":1}", "{\"requestId\":\"id\",\"count\":0}"})
    @DisplayName("접수 ID나 단일 수신자 접수 건수가 없으면 성공으로 처리하지 않는다")
    void rejectsUnconfirmedAcceptance(String response) {
        server.expect(anything()).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThat(sender.sendResult("member@example.com", "123456"))
                .isEqualTo(NotificationSendResult.DELIVERY_UNKNOWN);
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({"400,PERMANENT_FAILURE", "403,PERMANENT_FAILURE", "429,TRANSIENT_FAILURE",
            "408,DELIVERY_UNKNOWN", "500,DELIVERY_UNKNOWN", "503,DELIVERY_UNKNOWN"})
    @DisplayName("제공자 거절과 접수 여부를 알 수 없는 오류를 구분한다")
    void classifiesResponse(int status, NotificationSendResult expected) {
        server.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(status)));
        assertThat(sender.sendResult("member@example.com", "123456")).isEqualTo(expected);
        server.verify();
    }

    @Test
    @DisplayName("응답 타임아웃은 인증 발송 실패로 반환하고 같은 요청을 재발송하지 않는다")
    void timeoutDoesNotResend() {
        server.expect(anything()).andRespond(withException(new SocketTimeoutException("response timeout")));
        var resilient = new ResilientEmailVerificationSender(sender,
                CircuitBreaker.ofDefaults("email-test"), TimeLimiter.of(Duration.ofSeconds(7)),
                Runnable::run, Duration.ofSeconds(7));
        assertThat(resilient.send("member@example.com", "123456")).isFalse();
        server.verify();
    }

    static NcpMailProperties credentials() {
        return new NcpMailProperties("test-access-key", "test-secret-key", Duration.ofSeconds(2),
                Duration.ofSeconds(1), Duration.ofMillis(500), 2, Duration.ofSeconds(30));
    }

    static EmailVerificationProperties emailProperties() {
        return new EmailVerificationProperties(EmailVerificationProperties.Provider.NCP,
                "no-reply@mail.happy-gallery.com", "이메일 인증번호", Duration.ofSeconds(7));
    }
}
