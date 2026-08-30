package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.core5.http.ConnectionRequestTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

final class NhnNotificationFailureClassifier {

    private NhnNotificationFailureClassifier() {}

    static NotificationSendResult classify(RestClientResponseException exception) {
        HttpStatusCode status = exception.getStatusCode();
        if (status.is5xxServerError()
                || status.isSameCodeAs(HttpStatus.REQUEST_TIMEOUT)
                || status.isSameCodeAs(HttpStatus.TOO_EARLY)
                || status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
            return NotificationSendResult.TRANSIENT_FAILURE;
        }
        return NotificationSendResult.PERMANENT_FAILURE;
    }

    static NotificationSendResult classify(ResourceAccessException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof UnknownHostException
                    || cause instanceof ConnectException
                    || cause instanceof NoRouteToHostException
                    || cause instanceof ConnectTimeoutException
                    || cause instanceof ConnectionRequestTimeoutException
                    || cause instanceof SSLHandshakeException
                    || cause instanceof SSLPeerUnverifiedException) {
                return NotificationSendResult.TRANSIENT_FAILURE;
            }
        }
        return NotificationSendResult.DELIVERY_UNKNOWN;
    }
}
