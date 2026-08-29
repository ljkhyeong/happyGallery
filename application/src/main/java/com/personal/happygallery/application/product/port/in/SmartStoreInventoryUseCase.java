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

    CatalogPageResult listChannelProducts(int page, int size);

    ChannelProductResult getChannelProduct(Long originProductNo);

    ProductPreviewResult previewProduct(Long productId);

    void applyProduct(Long productId, long productVersion);

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

    record CatalogPageResult(
            List<CatalogProductResult> products,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    record CatalogProductResult(
            Long originProductNo,
            String name,
            String status,
            long salePrice,
            Integer stockQuantity,
            String imageUrl
    ) {}

    record ChannelProductResult(
            Long originProductNo,
            long salePrice,
            String status,
            List<ChannelOptionResult> options
    ) {}

    record ChannelOptionResult(
            Long optionId,
            String name,
            int stockQuantity,
            long price,
            boolean usable
    ) {}

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

    record ProductPreviewResult(
            Long productId,
            long productVersion,
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
