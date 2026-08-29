package com.personal.happygallery.application.product.port.in;

import com.personal.happygallery.domain.product.SmartStoreStockSyncStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SmartStoreInventoryUseCase {

    MappingResult saveMapping(Long productId, SaveMappingCommand command);

    Optional<MappingResult> getMapping(Long productId);

    void deleteMapping(Long productId);

    MappingResult retry(Long productId);

    record SaveMappingCommand(
            Long originProductNo,
            boolean enabled,
            List<VariantMapping> variants
    ) {
        public SaveMappingCommand {
            variants = variants == null ? List.of() : List.copyOf(variants);
        }
    }

    record VariantMapping(Long productVariantId, Long optionId) {}

    record MappingResult(
            Long productId,
            Long originProductNo,
            boolean enabled,
            List<VariantMapping> variants,
            SmartStoreStockSyncStatus syncStatus,
            int attemptCount,
            String lastError,
            LocalDateTime syncedAt
    ) {}
}
