package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.BookingVacancyAlertRetentionPort;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcBookingVacancyAlertRetentionAdapter implements BookingVacancyAlertRetentionPort {

    private final JdbcClient jdbcClient;

    JdbcBookingVacancyAlertRetentionAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public int deleteExpiredBatch(
            LocalDateTime now, LocalDateTime terminalCutoff, int batchSize) {
        return jdbcClient.sql("""
                        DELETE FROM booking_vacancy_alerts
                        WHERE id IN (
                            SELECT id
                            FROM (
                                SELECT alert.id
                                FROM booking_vacancy_alerts alert
                                JOIN slots slot ON slot.id = alert.slot_id
                                WHERE (alert.status = 'WAITING' AND slot.start_at <= :now)
                                   OR (alert.status <> 'WAITING' AND alert.updated_at < :terminalCutoff)
                                ORDER BY alert.id
                                LIMIT :batchSize
                            ) candidates
                        )
                        """)
                .param("now", now)
                .param("terminalCutoff", terminalCutoff)
                .param("batchSize", batchSize)
                .update();
    }
}
