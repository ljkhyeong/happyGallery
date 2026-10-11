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
        // 클레임·환불은 원래 주문·예약으로 잇고, 모든 원본은 수신자 본인 소유일 때만 붙인다.
        return jdbc.sql("""
                SELECT n.id AS notification_id,
                       CASE WHEN o.id IS NOT NULL THEN oi.product_name
                            WHEN b.id IS NOT NULL OR va.id IS NOT NULL THEN c.name
                            WHEN r.id IS NOT NULL THEN CONCAT(p.name,
                                CASE WHEN r.option_label = '' THEN '' ELSE CONCAT(' · ', r.option_label) END)
                            WHEN q.id IS NOT NULL THEN p.name
                       END AS name,
                       CASE WHEN o.id IS NOT NULL
                            THEN (SELECT COUNT(*) FROM order_items i WHERE i.order_id = o.id)
                            ELSE 1 END AS item_count,
                       s.start_at AS scheduled_at,
                       o.id AS order_id,
                       b.id AS booking_id,
                       p.id AS product_id,
                       r.product_variant_id AS product_variant_id,
                       CASE WHEN va.id IS NOT NULL THEN s.class_id END AS class_id,
                       va.slot_id AS slot_id
                FROM notification_outbox n
                LEFT JOIN order_claims oc ON n.aggregate_type = 'ORDER_CLAIM' AND oc.id = n.aggregate_id
                LEFT JOIN refunds rf ON n.aggregate_type = 'REFUND' AND rf.id = n.aggregate_id
                LEFT JOIN orders o ON o.user_id = :userId AND o.id = CASE n.aggregate_type
                        WHEN 'ORDER' THEN n.aggregate_id
                        WHEN 'ORDER_CLAIM' THEN oc.order_id
                        WHEN 'REFUND' THEN rf.order_id
                    END
                LEFT JOIN order_items oi ON oi.id = (SELECT MIN(i.id) FROM order_items i WHERE i.order_id = o.id)
                LEFT JOIN bookings b ON b.user_id = :userId AND b.id = CASE n.aggregate_type
                        WHEN 'BOOKING' THEN n.aggregate_id
                        WHEN 'REFUND' THEN rf.booking_id
                    END
                LEFT JOIN booking_vacancy_alerts va ON n.aggregate_type = 'VACANCY_ALERT'
                    AND va.id = n.aggregate_id AND va.user_id = :userId
                LEFT JOIN slots s ON s.id = COALESCE(b.slot_id, va.slot_id)
                LEFT JOIN classes c ON c.id = COALESCE(b.class_id, s.class_id)
                LEFT JOIN product_restock_alerts r ON n.aggregate_type = 'RESTOCK_ALERT'
                    AND r.id = n.aggregate_id AND r.user_id = :userId
                LEFT JOIN product_qna q ON n.aggregate_type = 'PRODUCT_QNA'
                    AND q.id = n.aggregate_id AND q.user_id = :userId
                LEFT JOIN products p ON p.id = COALESCE(r.product_id, q.product_id)
                WHERE n.id IN (:ids) AND n.status = 'SENT'
                  AND n.user_id = :userId
                """)
                .param("ids", notificationIds)
                .param("userId", userId)
                .query(Context.class)
                .list();
    }
}
