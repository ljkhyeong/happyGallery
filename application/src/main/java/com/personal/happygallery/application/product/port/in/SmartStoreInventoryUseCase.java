package com.personal.happygallery.application.product.port.in;

import com.personal.happygallery.application.product.dto.SmartStoreCatalogPage;
import com.personal.happygallery.application.product.dto.SmartStoreChannelOption;
import com.personal.happygallery.application.product.dto.SmartStoreInspectionPage;
import com.personal.happygallery.application.shared.AdminActor;
import com.personal.happygallery.domain.product.SmartStoreStockSyncStatus;
import com.personal.happygallery.domain.product.SmartStoreInventoryMappingAction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SmartStoreInventoryUseCase {

    MappingResult saveMapping(Long productId, SaveMappingCommand command, AdminActor actor);

    default MappingResult saveMapping(Long productId, SaveMappingCommand command) {
        return saveMapping(productId, command, AdminActor.system());
    }

    Optional<MappingResult> getMapping(Long productId);

    void deleteMapping(Long productId, DeleteMappingCommand command, AdminActor actor);

    default void deleteMapping(Long productId, DeleteMappingCommand command) {
        deleteMapping(productId, command, AdminActor.system());
    }

    List<MappingHistoryResult> listMappingHistory(Long productId);

    MappingResult retry(Long productId);

    SmartStoreCatalogPage listChannelProducts(int page, int size);

    SmartStoreInspectionPage listInspectionProducts(int page, int size);

    void restoreInspectionProduct(Long channelProductNo);

    ChannelProductResult getChannelProduct(Long originProductNo);

    ProductPreviewResult previewProduct(Long productId);

    void applyProduct(Long productId, String previewVersion);

    record SaveMappingCommand(
            Long originProductNo,
            boolean enabled,
            List<VariantMapping> variants,
            Long expectedMappingVersion,
            boolean previousOriginConfirmed
    ) {
        public SaveMappingCommand {
            variants = variants == null ? List.of() : List.copyOf(variants);
        }

        public SaveMappingCommand(
                Long originProductNo,
                boolean enabled,
                List<VariantMapping> variants) {
            this(originProductNo, enabled, variants, null, false);
        }
    }

    record VariantMapping(Long productVariantId, Long optionId) {}

    record DeleteMappingCommand(
            Long expectedMappingVersion,
            boolean previousOriginConfirmed
    ) {}

    record ChannelProductResult(
            Long originProductNo,
            long salePrice,
            String status,
            List<SmartStoreChannelOption> options
    ) {}

    record MappingResult(
            Long productId,
            long mappingVersion,
            Long originProductNo,
            boolean enabled,
            List<VariantMapping> variants,
            SmartStoreStockSyncStatus syncStatus,
            int attemptCount,
            String lastError,
            LocalDateTime syncedAt
    ) {}

    record MappingHistoryResult(
            long id,
            SmartStoreInventoryMappingAction action,
            Long previousOriginProductNo,
            Long nextOriginProductNo,
            Boolean previousEnabled,
            Boolean nextEnabled,
            String previousOptionMappings,
            String nextOptionMappings,
            Long previousMappingVersion,
            Long nextMappingVersion,
            boolean previousOriginConfirmed,
            Long changedByAdminId,
            String changedBy,
            LocalDateTime changedAt
    ) {}

    record ProductPreviewResult(
            Long productId,
            String previewVersion,
            Long originProductNo,
            long localSalePrice,
            long channelSalePrice,
            String localStatus,
            String channelStatus,
            boolean different,
            List<ProductOptionPreview> options
    ) {}

    record ProductOptionPreview(
            Long productVariantId,
            Long optionId,
            long localPrice,
            Long channelPrice,
            boolean localUsable,
            Boolean channelUsable,
            boolean different
    ) {}
}
