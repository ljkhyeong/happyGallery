package com.personal.happygallery.adapter.out.persistence.notification;

import com.personal.happygallery.application.notification.port.out.NotificationContextPort;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcNotificationContextAdapter implements NotificationContextPort {
    private final JdbcClient jdbc;

    JdbcNotificationContextAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Context> findContexts(Collection<Long> notificationIds, Long userId) {
        if (notificationIds.isEmpty()) return List.of();
        return jdbc.sql("""
                SELECT n.id AS notification_id,
                       CASE WHEN o.id IS NOT NULL THEN oi.product_name
                            WHEN b.id IS NOT NULL THEN c.name
                            WHEN r.id IS NOT NULL THEN CONCAT(p.name,
                                CASE WHEN r.option_label = '' THEN '' ELSE CONCAT(' · ', r.option_label) END)
                       END AS name,
                       CASE WHEN o.id IS NOT NULL
                            THEN (SELECT COUNT(*) FROM order_items i WHERE i.order_id = o.id)
                            ELSE 1 END AS item_count,
                       s.start_at AS scheduled_at
                FROM notification_outbox n
                LEFT JOIN orders o ON n.aggregate_type = 'ORDER' AND o.id = n.aggregate_id
                    AND o.user_id = :userId
                LEFT JOIN order_items oi ON oi.id = (SELECT MIN(i.id) FROM order_items i WHERE i.order_id = o.id)
                LEFT JOIN bookings b ON n.aggregate_type = 'BOOKING' AND b.id = n.aggregate_id
                    AND b.user_id = :userId
                LEFT JOIN classes c ON c.id = b.class_id
                LEFT JOIN slots s ON s.id = b.slot_id
                LEFT JOIN product_restock_alerts r ON n.aggregate_type = 'RESTOCK_ALERT'
                    AND r.id = n.aggregate_id AND r.user_id = :userId
                LEFT JOIN products p ON p.id = r.product_id
                WHERE n.id IN (:ids) AND n.status = 'SENT'
                  AND n.user_id = :userId
                """)
                .param("ids", notificationIds)
                .param("userId", userId)
                .query(Context.class)
                .list();
    }
}
