package com.personal.happygallery.support;

import com.personal.happygallery.domain.notification.NotificationLog;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public final class NotificationLogTestHelper {

    private static final long TIMEOUT_MILLIS = 2_000L;
    private static final long POLL_INTERVAL_MILLIS = 25L;

    private NotificationLogTestHelper() {
    }

    public static List<NotificationLog> awaitLogCount(NotificationLogProbe notificationLogProbe, int expectedCount) {
        await()
                .atMost(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(notificationLogProbe.all()).hasSize(expectedCount));
        return notificationLogProbe.all();
    }
}
