package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.domain.product.SmartStoreOrderMappingHistory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SmartStoreOrderMappingHistoryPort {

    Optional<SmartStoreOrderMappingHistory> findResolvable(
            Long originProductNo, Long optionId, LocalDateTime orderedAt);

    <S extends SmartStoreOrderMappingHistory> List<S> saveAll(Iterable<S> mappings);
}
