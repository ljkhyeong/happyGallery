package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.SmartStoreSettlementEntry;
import java.util.List;
import java.util.Optional;

public interface SmartStoreSettlementPort {

    Optional<SmartStoreSettlementEntry> findByEntryKey(String entryKey);

    List<SmartStoreSettlementEntry> findIssues(int limit);

    <S extends SmartStoreSettlementEntry> S save(S entry);
}
