package com.personal.happygallery.support;

import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.infra.notification.NotificationLogRepository;
import java.util.List;

public final class NotificationLogTestHelper {

    private static final long TIMEOUT_MILLIS = 2_000L;
    private static final long POLL_INTERVAL_MILLIS = 25L;

    private NotificationLogTestHelper() {
    }

    public static List<NotificationLog> awaitLogCount(NotificationLogRepository repository, int expectedCount) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        List<NotificationLog> logs = repository.findAll();
        while (System.currentTimeMillis() < deadline) {
            logs = repository.findAll();
            if (logs.size() == expectedCount) {
                return logs;
            }
            sleepQuietly();
        }
        return repository.findAll();
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(POLL_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("알림 로그 대기 중 인터럽트가 발생했습니다.", e);
        }
    }
}
