package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.PublicHolidaySnapshotPort;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcPublicHolidaySnapshotAdapter implements PublicHolidaySnapshotPort {

    private final JdbcTemplate jdbcTemplate;

    JdbcPublicHolidaySnapshotAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Set<LocalDate> findDatesByYear(int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        return new LinkedHashSet<>(jdbcTemplate.queryForList(
                """
                SELECT holiday_date
                FROM public_holiday_snapshot
                WHERE holiday_date >= ? AND holiday_date < ?
                ORDER BY holiday_date
                """,
                LocalDate.class,
                from,
                from.plusYears(1)));
    }

    @Override
    public void replaceYear(int year, List<PublicHoliday> holidays, LocalDateTime syncedAt) {
        LocalDate from = LocalDate.of(year, 1, 1);
        jdbcTemplate.update(
                "DELETE FROM public_holiday_snapshot WHERE holiday_date >= ? AND holiday_date < ?",
                from,
                from.plusYears(1));
        jdbcTemplate.batchUpdate(
                "INSERT INTO public_holiday_snapshot (holiday_date, name, synced_at) VALUES (?, ?, ?)",
                holidays,
                holidays.size(),
                (statement, holiday) -> {
                    statement.setDate(1, Date.valueOf(holiday.date()));
                    statement.setString(2, holiday.name());
                    statement.setTimestamp(3, Timestamp.valueOf(syncedAt));
                });
    }
}
