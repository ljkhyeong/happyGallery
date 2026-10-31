package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import java.net.SocketTimeoutException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class NhnNotificationFailureClassifierTest {

    @ParameterizedTest
    @CsvSource({
            "408,DELIVERY_UNKNOWN", "500,DELIVERY_UNKNOWN", "502,DELIVERY_UNKNOWN",
            "503,DELIVERY_UNKNOWN", "504,DELIVERY_UNKNOWN",
            "425,TRANSIENT_FAILURE", "429,TRANSIENT_FAILURE", "400,PERMANENT_FAILURE"
    })
    @DisplayName("발송 여부가 불확실한 HTTP 오류는 재시도 가능한 거절과 구분한다")
    void classifyHttpResponse_distinguishesUnknownDelivery(int status, NotificationSendResult expected) {
        var exception = new RestClientResponseException("test", status, "test", null, null, null);

        assertThat(NhnNotificationFailureClassifier.classify(exception)).isEqualTo(expected);
    }

    @DisplayName("전송 전 연결 실패만 재시도하고 응답 대기 시간 초과는 결과 불명으로 분류한다")
    @Test
    void classifyResourceAccessException_distinguishesPreSendFailureFromUnknownDelivery() {
        NotificationSendResult dnsFailure = NhnNotificationFailureClassifier.classify(
                new ResourceAccessException("dns", new UnknownHostException("nhn")));
        NotificationSendResult routingFailure = NhnNotificationFailureClassifier.classify(
                new ResourceAccessException("route", new NoRouteToHostException("nhn")));
        NotificationSendResult tlsHandshakeFailure = NhnNotificationFailureClassifier.classify(
                new ResourceAccessException("tls", new SSLHandshakeException("certificate")));
        NotificationSendResult responseTimeout = NhnNotificationFailureClassifier.classify(
                new ResourceAccessException("read", new SocketTimeoutException("read timed out")));

        assertSoftly(softly -> {
            softly.assertThat(dnsFailure).isEqualTo(NotificationSendResult.TRANSIENT_FAILURE);
            softly.assertThat(routingFailure).isEqualTo(NotificationSendResult.TRANSIENT_FAILURE);
            softly.assertThat(tlsHandshakeFailure).isEqualTo(NotificationSendResult.TRANSIENT_FAILURE);
            softly.assertThat(responseTimeout).isEqualTo(NotificationSendResult.DELIVERY_UNKNOWN);
        });
    }
}
