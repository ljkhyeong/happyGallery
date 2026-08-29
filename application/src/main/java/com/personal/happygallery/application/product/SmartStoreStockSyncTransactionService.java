package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.ProductVariantReaderPort;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.OptionStock;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.StockCommand;
import com.personal.happygallery.application.product.port.out.SmartStoreStockMappingPort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncPort;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.ProductVariant;
import com.personal.happygallery.domain.product.SmartStoreStockMapping;
import com.personal.happygallery.domain.product.SmartStoreStockSync;
import com.personal.happygallery.domain.product.SmartStoreStockSyncStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SmartStoreStockSyncTransactionService {

    private final SmartStoreStockSyncPort syncPort;
    private final SmartStoreStockMappingPort mappingPort;
    private final ProductReaderPort productReaderPort;
    private final InventoryReaderPort inventoryReaderPort;
    private final ProductVariantReaderPort variantReaderPort;

    SmartStoreStockSyncTransactionService(
            SmartStoreStockSyncPort syncPort,
            SmartStoreStockMappingPort mappingPort,
            ProductReaderPort productReaderPort,
            InventoryReaderPort inventoryReaderPort,
            ProductVariantReaderPort variantReaderPort) {
        this.syncPort = syncPort;
        this.mappingPort = mappingPort;
        this.productReaderPort = productReaderPort;
        this.inventoryReaderPort = inventoryReaderPort;
        this.variantReaderPort = variantReaderPort;
    }

    @Transactional
    public Optional<ClaimedStock> claim(Long productId, LocalDateTime now) {
        SmartStoreStockSync sync = syncPort.findByProductIdWithLock(productId).orElse(null);
        if (sync == null
                || (sync.getStatus() == SmartStoreStockSyncStatus.PENDING
                    && sync.getNextAttemptAt().isAfter(now))
                || (sync.getStatus() != SmartStoreStockSyncStatus.PENDING
                    && sync.getStatus() != SmartStoreStockSyncStatus.PROCESSING)) {
            return Optional.empty();
        }
        long claimedVersion;
        try {
            claimedVersion = sync.claim(now, now.minusMinutes(5));
        } catch (IllegalStateException exception) {
            return Optional.empty();
        }
        syncPort.save(sync);

        try {
            return Optional.of(new ClaimedStock(claimedVersion, buildCommand(productId), null));
        } catch (IllegalStateException exception) {
            return Optional.of(new ClaimedStock(claimedVersion, null, exception.getMessage()));
        }
    }

    @Transactional
    public void finish(
            Long productId,
            long claimedVersion,
            boolean success,
            String reason,
            LocalDateTime now) {
        SmartStoreStockSync sync = syncPort.findByProductIdWithLock(productId).orElse(null);
        if (sync == null) {
            return;
        }
        if (success) {
            sync.complete(claimedVersion, now);
        } else {
            sync.fail(claimedVersion, reason, now);
        }
        syncPort.save(sync);
    }

    private StockCommand buildCommand(Long productId) {
        Product product = productReaderPort.findById(productId)
                .orElseThrow(() -> new IllegalStateException("연동할 상품이 없습니다."));
        List<SmartStoreStockMapping> mappings = mappingPort
                .findByProductIdOrderByProductVariantIdAsc(productId).stream()
                .filter(SmartStoreStockMapping::isEnabled)
                .toList();
        if (mappings.isEmpty()) {
            throw new IllegalStateException("스마트스토어 재고 연동 설정이 비활성 상태입니다.");
        }
        Long originProductNo = mappings.getFirst().getOriginProductNo();
        if (product.getType() == ProductType.READY_STOCK) {
            Inventory inventory = inventoryReaderPort.findByProductId(productId)
                    .orElseThrow(() -> new IllegalStateException("연동할 상품 재고가 없습니다."));
            if (mappings.size() != 1 || mappings.getFirst().getProductVariantId() != null) {
                throw new IllegalStateException("기성품 스마트스토어 재고 연동 설정이 올바르지 않습니다.");
            }
            return new StockCommand(originProductNo, inventory.getQuantity(), List.of());
        }

        List<ProductVariant> variants = variantReaderPort.findWithSelectionsByProductId(productId);
        Map<Long, SmartStoreStockMapping> mappingsByVariantId = mappings.stream()
                .filter(mapping -> mapping.getProductVariantId() != null)
                .collect(Collectors.toMap(
                        SmartStoreStockMapping::getProductVariantId,
                        Function.identity()));
        if (mappingsByVariantId.size() != variants.size()) {
            throw new IllegalStateException("상품 옵션이 변경되어 스마트스토어 옵션 매핑을 다시 저장해야 합니다.");
        }
        List<OptionStock> options = variants.stream()
                .map(variant -> optionStock(variant, mappingsByVariantId))
                .toList();
        return new StockCommand(originProductNo, null, options);
    }

    private static OptionStock optionStock(
            ProductVariant variant,
            Map<Long, SmartStoreStockMapping> mappingsByVariantId) {
        SmartStoreStockMapping mapping = mappingsByVariantId.get(variant.getId());
        if (mapping == null) {
            throw new IllegalStateException("스마트스토어 옵션 번호가 없는 상품 옵션이 있습니다.");
        }
        return new OptionStock(
                mapping.getOptionId(),
                variant.isActive() ? variant.getQuantity() : 0);
    }

    record ClaimedStock(long version, StockCommand command, String configurationError) {}
}
