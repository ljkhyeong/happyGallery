package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.booking.port.in.PublicHolidaySyncUseCase;
import com.personal.happygallery.application.booking.port.out.PublicHolidayProvider;
import com.personal.happygallery.application.booking.port.out.PublicHolidaySnapshotPort.PublicHoliday;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DefaultPublicHolidaySyncService implements PublicHolidaySyncUseCase {

    private static final String FETCH_FAILURE = "공식 공휴일 조회 실패";

    private final PublicHolidayProvider provider;
    private final PublicHolidaySnapshotTransactionService transactionService;
    private final KoreanPublicHolidayPolicy holidayPolicy;
    private final Clock clock;

    public DefaultPublicHolidaySyncService(
            PublicHolidayProvider provider,
            PublicHolidaySnapshotTransactionService transactionService,
            KoreanPublicHolidayPolicy holidayPolicy,
            Clock clock) {
        this.provider = provider;
        this.transactionService = transactionService;
        this.holidayPolicy = holidayPolicy;
        this.clock = clock;
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
            transactionService.replace(year, fetched.get(), LocalDateTime.now(clock));
            holidayPolicy.evict(year);
            successCount++;
        }
        return BatchResult.of(successCount, failures);
    }
}
