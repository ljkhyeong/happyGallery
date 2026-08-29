package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.ProductVariantReaderPort;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.ChannelOption;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.ProductCommand;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.ProductOption;
import com.personal.happygallery.application.product.port.out.SmartStoreStockMappingPort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncPort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncQueuePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.ProductVariant;
import com.personal.happygallery.domain.product.SmartStoreStockMapping;
import com.personal.happygallery.domain.product.SmartStoreStockSync;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultSmartStoreInventoryService implements SmartStoreInventoryUseCase {

    private final ProductReaderPort productReaderPort;
    private final ProductVariantReaderPort variantReaderPort;
    private final SmartStoreStockMappingPort mappingPort;
    private final SmartStoreStockSyncPort syncPort;
    private final SmartStoreStockSyncQueuePort queuePort;
    private final SmartStoreInventoryProvider inventoryProvider;
    private final SmartStoreStockSyncTransactionService transactionService;
    private final Clock clock;

    public DefaultSmartStoreInventoryService(
            ProductReaderPort productReaderPort,
            ProductVariantReaderPort variantReaderPort,
            SmartStoreStockMappingPort mappingPort,
            SmartStoreStockSyncPort syncPort,
            SmartStoreStockSyncQueuePort queuePort,
            SmartStoreInventoryProvider inventoryProvider,
            SmartStoreStockSyncTransactionService transactionService,
            Clock clock) {
        this.productReaderPort = productReaderPort;
        this.variantReaderPort = variantReaderPort;
        this.mappingPort = mappingPort;
        this.syncPort = syncPort;
        this.queuePort = queuePort;
        this.inventoryProvider = inventoryProvider;
        this.transactionService = transactionService;
        this.clock = clock;
    }

    @Override
    public MappingResult saveMapping(Long productId, SaveMappingCommand command) {
        Product product = productReaderPort.findById(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        validate(product, command.variants());

        mappingPort.deleteByProductId(productId);
        List<SmartStoreStockMapping> mappings = command.variants().isEmpty()
                ? List.of(new SmartStoreStockMapping(
                        productId, null, command.originProductNo(), null, command.enabled()))
                : command.variants().stream()
                        .map(variant -> new SmartStoreStockMapping(
                                productId,
                                variant.productVariantId(),
                                command.originProductNo(),
                                variant.optionId(),
                                command.enabled()))
                        .toList();
        mappingPort.saveAll(mappings);
        if (command.enabled()) {
            queuePort.requestIfMapped(List.of(productId), LocalDateTime.now(clock));
        } else {
            syncPort.deleteByProductId(productId);
        }
        return result(mappings);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MappingResult> getMapping(Long productId) {
        if (productReaderPort.findById(productId).isEmpty()) {
            throw new NotFoundException("상품");
        }
        List<SmartStoreStockMapping> mappings = mappingPort
                .findByProductIdOrderByProductVariantIdAsc(productId);
        return mappings.isEmpty() ? Optional.empty() : Optional.of(result(mappings));
    }

    @Override
    public void deleteMapping(Long productId) {
        if (productReaderPort.findById(productId).isEmpty()) {
            throw new NotFoundException("상품");
        }
        mappingPort.deleteByProductId(productId);
        syncPort.deleteByProductId(productId);
    }

    @Override
    public MappingResult retry(Long productId) {
        List<SmartStoreStockMapping> mappings = mappingPort
                .findByProductIdOrderByProductVariantIdAsc(productId);
        if (mappings.isEmpty()) {
            throw new NotFoundException("스마트스토어 재고 연동 설정");
        }
        queuePort.requestIfMapped(List.of(productId), LocalDateTime.now(clock));
        return result(mappings);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public CatalogPageResult listChannelProducts(int page, int size) {
        requireProviderEnabled();
        var catalog = inventoryProvider.listProducts(page, size);
        return new CatalogPageResult(
                catalog.products().stream()
                        .map(product -> new CatalogProductResult(
                                product.originProductNo(), product.name(), product.status(),
                                product.salePrice(), product.stockQuantity(), product.imageUrl()))
                        .toList(),
                catalog.page(), catalog.size(), catalog.totalElements(), catalog.totalPages());
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public ChannelProductResult getChannelProduct(Long originProductNo) {
        requireProviderEnabled();
        var product = inventoryProvider.getProduct(originProductNo);
        return new ChannelProductResult(
                originProductNo, product.salePrice(), product.status(),
                product.options().stream()
                        .map(option -> new ChannelOptionResult(
                                option.optionId(), option.name(), option.stockQuantity(),
                                option.price(), option.usable()))
                        .toList());
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public ProductPreviewResult previewProduct(Long productId) {
        requireProviderEnabled();
        var local = transactionService.productSnapshot(productId);
        var channel = inventoryProvider.getProduct(local.originProductNo());
        Map<Long, ChannelOption> channelOptions = channel.options().stream()
                .collect(Collectors.toMap(ChannelOption::optionId, Function.identity()));
        List<ProductOptionPreview> options = local.options().stream()
                .map(option -> {
                    ChannelOption remote = channelOptions.get(option.optionId());
                    boolean different = remote == null
                            || option.price() != remote.price()
                            || option.usable() != remote.usable();
                    return new ProductOptionPreview(
                            option.productVariantId(), option.optionId(), option.price(),
                            remote == null ? null : remote.price(), option.usable(),
                            remote == null ? null : remote.usable(), different);
                })
                .toList();
        boolean different = local.salePrice() != channel.salePrice()
                || !local.targetStatus().equals(channel.status())
                || options.stream().anyMatch(ProductOptionPreview::different);
        return new ProductPreviewResult(
                local.productId(), local.productVersion(), local.originProductNo(),
                local.salePrice(), channel.salePrice(), local.targetStatus(), channel.status(),
                different, options);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void applyProduct(Long productId, long productVersion) {
        requireProviderEnabled();
        var local = transactionService.productSnapshot(productId);
        if (local.productVersion() != productVersion) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "상품이 변경되었습니다. 최신 차이를 다시 확인해 주세요.");
        }
        var result = inventoryProvider.applyProduct(new ProductCommand(
                local.originProductNo(), local.salePrice(), local.targetStatus(),
                local.stockQuantity(), local.options().stream()
                        .map(option -> new ProductOption(
                                option.optionId(), option.stockQuantity(), option.price(),
                                option.usable()))
                        .toList()));
        if (!result.success()) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, result.reason());
        }
    }

    private void requireProviderEnabled() {
        if (!inventoryProvider.isEnabled()) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "스마트스토어 연동이 비활성화되어 있습니다.");
        }
    }

    private void validate(Product product, List<VariantMapping> requested) {
        if (product.getType() == ProductType.READY_STOCK) {
            if (!requested.isEmpty()) {
                throw new IllegalArgumentException("기성품은 스마트스토어 옵션 번호를 입력하지 않습니다.");
            }
            return;
        }
        List<ProductVariant> variants = variantReaderPort
                .findWithSelectionsByProductId(product.getId());
        Set<Long> existingIds = variants.stream().map(ProductVariant::getId).collect(HashSet::new, Set::add, Set::addAll);
        Set<Long> requestedIds = requested.stream()
                .map(VariantMapping::productVariantId)
                .collect(HashSet::new, Set::add, Set::addAll);
        if (requested.size() != requestedIds.size() || !requestedIds.equals(existingIds)) {
            throw new IllegalArgumentException("주문제작 상품의 모든 옵션 조합에 스마트스토어 옵션 번호를 지정해 주세요.");
        }
        if (requested.stream().map(VariantMapping::optionId).distinct().count() != requested.size()) {
            throw new IllegalArgumentException("스마트스토어 옵션 번호를 중복해서 지정할 수 없습니다.");
        }
    }

    private MappingResult result(List<SmartStoreStockMapping> mappings) {
        SmartStoreStockMapping first = mappings.getFirst();
        SmartStoreStockSync sync = syncPort.findByProductId(first.getProductId()).orElse(null);
        return new MappingResult(
                first.getProductId(),
                first.getOriginProductNo(),
                first.isEnabled(),
                mappings.stream()
                        .filter(mapping -> mapping.getProductVariantId() != null)
                        .map(mapping -> new VariantMapping(
                                mapping.getProductVariantId(), mapping.getOptionId()))
                        .toList(),
                sync == null ? null : sync.getStatus(),
                sync == null ? 0 : sync.getAttemptCount(),
                sync == null ? null : sync.getLastError(),
                sync == null ? null : sync.getSyncedAt());
    }
}
