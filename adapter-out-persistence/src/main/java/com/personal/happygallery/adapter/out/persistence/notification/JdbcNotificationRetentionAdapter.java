package com.personal.happygallery.adapter.out.persistence.notification;

import com.personal.happygallery.application.notification.port.out.NotificationRetentionPort;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class JdbcNotificationRetentionAdapter implements NotificationRetentionPort {

    private final JdbcClient jdbc;

    public JdbcNotificationRetentionAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int deleteChannelLogsBefore(LocalDateTime cutoff, int limit) {
        return jdbc.sql("""
                        DELETE FROM notification_log
                        WHERE sent_at < :cutoff
                        ORDER BY sent_at, id
                        LIMIT :limit
                        """)
                .param("cutoff", cutoff)
                .param("limit", limit)
                .update();
    }

    @Override
    public int deleteTerminalOutboxesBefore(LocalDateTime cutoff, int limit) {
        return jdbc.sql("""
                        DELETE FROM notification_outbox
                        WHERE status IN ('SENT', 'OBSOLETE', 'FAILED')
                          AND processed_at < :cutoff
                        ORDER BY processed_at, id
                        LIMIT :limit
                        """)
                .param("cutoff", cutoff)
                .param("limit", limit)
                .update();
    }
}
