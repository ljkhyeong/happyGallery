package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.booking.port.in.PublicHolidaySyncUseCase;
import com.personal.happygallery.application.booking.port.out.PublicHolidayProvider;
import com.personal.happygallery.application.booking.port.out.PublicHolidaySnapshotPort;
import com.personal.happygallery.application.booking.port.out.PublicHolidaySnapshotPort.PublicHoliday;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DefaultPublicHolidaySyncService implements PublicHolidaySyncUseCase {

    private static final String FETCH_FAILURE = "공식 공휴일 조회 실패";

    private final PublicHolidayProvider provider;
    private final PublicHolidaySnapshotPort snapshotPort;
    private final KoreanPublicHolidayPolicy holidayPolicy;
    private final Clock clock;
    /** 연도별 스냅샷 교체를 독립 커밋한 뒤 캐시를 비우도록 새 트랜잭션을 연다. */
    private final TransactionTemplate requiresNew;

    public DefaultPublicHolidaySyncService(
            PublicHolidayProvider provider,
            PublicHolidaySnapshotPort snapshotPort,
            KoreanPublicHolidayPolicy holidayPolicy,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.provider = provider;
        this.snapshotPort = snapshotPort;
        this.holidayPolicy = holidayPolicy;
        this.clock = clock;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public BatchResult syncAnnualSnapshots() {
        if (!provider.isEnabled()) {
            return BatchResult.successOnly(0);
        }

        int currentYear = LocalDate.now(clock).getYear();
        int successCount = 0;
        Map<String, Integer> failures = new LinkedHashMap<>();
        for (int year = currentYear; year <= currentYear + 1; year++) {
            Optional<List<PublicHoliday>> fetched = provider.fetch(year)
                    .filter(holidays -> !holidays.isEmpty());
            if (fetched.isEmpty()) {
                failures.merge(FETCH_FAILURE, 1, Integer::sum);
                continue;
            }
            int targetYear = year;
            List<PublicHoliday> holidays = fetched.get();
            LocalDateTime syncedAt = LocalDateTime.now(clock);
            requiresNew.executeWithoutResult(
                    status -> snapshotPort.replaceYear(targetYear, holidays, syncedAt));
            holidayPolicy.evict(year);
            successCount++;
        }
        return BatchResult.of(successCount, failures);
    }
}
