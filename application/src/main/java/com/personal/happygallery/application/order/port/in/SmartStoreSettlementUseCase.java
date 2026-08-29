package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.domain.order.SmartStoreSettlementEntry;
import java.util.List;

public interface SmartStoreSettlementUseCase {

    BatchResult synchronizeRecent();

    List<SmartStoreSettlementEntry> findIssues(int limit);
}
