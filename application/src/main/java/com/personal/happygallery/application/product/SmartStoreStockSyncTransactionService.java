package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.OptionStock;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.StockCommand;
import com.personal.happygallery.application.product.port.out.SmartStoreStockMappingPort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncPort;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.SmartStoreStockMapping;
import com.personal.happygallery.domain.product.SmartStoreStockSync;
import com.personal.happygallery.domain.product.SmartStoreStockSyncStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final ProductOptionConfigurationService optionConfigurationService;

    SmartStoreStockSyncTransactionService(
            SmartStoreStockSyncPort syncPort,
            SmartStoreStockMappingPort mappingPort,
            ProductReaderPort productReaderPort,
            InventoryReaderPort inventoryReaderPort,
            ProductOptionConfigurationService optionConfigurationService) {
        this.syncPort = syncPort;
        this.mappingPort = mappingPort;
        this.productReaderPort = productReaderPort;
        this.inventoryReaderPort = inventoryReaderPort;
        this.optionConfigurationService = optionConfigurationService;
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

    @Transactional(readOnly = true)
    ProductSyncSnapshot productSnapshot(Long productId) {
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
            return new ProductSyncSnapshot(
                    productId, product.getVersion(), originProductNo, product.getPrice(),
                    targetStatus(product.getStatus(), inventory.getQuantity()),
                    inventory.getQuantity(), List.of());
        }

        List<ProductOptionSnapshot> options = mappedOptions(productId, mappings);
        int totalStock = options.stream().mapToInt(ProductOptionSnapshot::stockQuantity).sum();
        return new ProductSyncSnapshot(
                productId, product.getVersion(), originProductNo, product.getPrice(),
                targetStatus(product.getStatus(), totalStock), null, options);
    }

    private static String targetStatus(ProductStatus status, int stockQuantity) {
        if (status == ProductStatus.INACTIVE) {
            return "SUSPENSION";
        }
        return stockQuantity == 0 ? "OUTOFSTOCK" : "SALE";
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

        List<OptionStock> options = mappedOptions(productId, mappings).stream()
                .map(option -> new OptionStock(option.optionId(), option.stockQuantity()))
                .toList();
        return new StockCommand(originProductNo, null, options);
    }

    private List<ProductOptionSnapshot> mappedOptions(Long productId, List<SmartStoreStockMapping> mappings) {
        Map<Long, ProductOptions.Variant> currentVariants = optionConfigurationService.get(productId, true)
                .variants().stream().collect(Collectors.toMap(ProductOptions.Variant::id, Function.identity()));
        Set<Long> mappedIds = mappings.stream().map(SmartStoreStockMapping::getProductVariantId)
                .collect(Collectors.toSet());
        if (!mappedIds.containsAll(currentVariants.keySet())) {
            throw new IllegalStateException("상품 옵션이 변경되어 스마트스토어 옵션 매핑을 다시 저장해야 합니다.");
        }
        return mappings.stream().map(mapping -> {
            ProductOptions.Variant variant = currentVariants.get(mapping.getProductVariantId());
            boolean usable = variant != null && variant.active();
            return new ProductOptionSnapshot(mapping.getProductVariantId(), mapping.getOptionId(),
                    usable ? variant.quantity() : 0,
                    variant == null ? 0L : variant.priceAdjustment(), usable);
        }).toList();
    }

    record ClaimedStock(long version, StockCommand command, String configurationError) {}

    record ProductSyncSnapshot(
            Long productId,
            long productVersion,
            Long originProductNo,
            long salePrice,
            String targetStatus,
            Integer stockQuantity,
            List<ProductOptionSnapshot> options
    ) {}

    record ProductOptionSnapshot(
            Long productVariantId,
            Long optionId,
            int stockQuantity,
            long price,
            boolean usable
    ) {}
}
