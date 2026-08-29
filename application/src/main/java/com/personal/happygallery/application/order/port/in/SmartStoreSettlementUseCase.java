package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.domain.order.SmartStoreSettlementEntry;
import java.util.List;
import java.time.LocalDate;

public interface SmartStoreSettlementUseCase {

    BatchResult synchronizeRecent();

    BatchResult synchronize(LocalDate from, LocalDate to);

    List<SmartStoreSettlementEntry> findIssues(int limit);
}
