package com.personal.happygallery.application.product;

import com.personal.happygallery.application.order.port.in.SmartStoreOrderSyncBatchUseCase;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.ChannelOption;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.ProductCommand;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.ProductOption;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncQueuePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultSmartStoreInventoryService implements SmartStoreInventoryUseCase {

    private final SmartStoreInventoryMappingService mappingService;
    private final SmartStoreStockSyncQueuePort queuePort;
    private final SmartStoreInventoryProvider inventoryProvider;
    private final SmartStoreStockSyncTransactionService transactionService;
    private final SmartStoreOrderSyncBatchUseCase orderSyncUseCase;
    private final Clock clock;

    public DefaultSmartStoreInventoryService(
            SmartStoreInventoryMappingService mappingService,
            SmartStoreStockSyncQueuePort queuePort,
            SmartStoreInventoryProvider inventoryProvider,
            SmartStoreStockSyncTransactionService transactionService,
            SmartStoreOrderSyncBatchUseCase orderSyncUseCase,
            Clock clock) {
        this.mappingService = mappingService;
        this.queuePort = queuePort;
        this.inventoryProvider = inventoryProvider;
        this.transactionService = transactionService;
        this.orderSyncUseCase = orderSyncUseCase;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public MappingResult saveMapping(Long productId, SaveMappingCommand command) {
        var plan = mappingService.planChange(productId, command);
        if (plan.originChanged()) {
            synchronizePreviousOriginOrders();
        }
        return mappingService.saveMapping(productId, command);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MappingResult> getMapping(Long productId) {
        return mappingService.getMapping(productId);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void deleteMapping(Long productId, DeleteMappingCommand command) {
        mappingService.planDelete(productId, command);
        synchronizePreviousOriginOrders();
        mappingService.deleteMapping(productId, command);
    }

    @Override
    public MappingResult retry(Long productId) {
        return mappingService.retry(productId);
    }

    private void synchronizePreviousOriginOrders() {
        if (!orderSyncUseCase.synchronizeBeforeStock()) {
            throw new HappyGalleryException(ErrorCode.CONFLICT,
                    "기존 원상품 주문 수집이 완료되지 않아 연동 변경을 보류했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public CatalogPageResult listChannelProducts(int page, int size) {
        requireProviderEnabled();
        var catalog = inventoryProvider.listProducts(page, size);
        return new CatalogPageResult(
                catalog.products().stream()
                        .map(product -> new CatalogProductResult(
                                product.originProductNo(), product.channelProductNo(),
                                product.name(), product.status(),
                                product.salePrice(), product.stockQuantity(), product.imageUrl()))
                        .toList(),
                catalog.page(), catalog.size(), catalog.totalElements(), catalog.totalPages());
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public InspectionPageResult listInspectionProducts(int page, int size) {
        requireProviderEnabled();
        var inspections = inventoryProvider.listInspectionProducts(page, size);
        return new InspectionPageResult(
                inspections.products().stream()
                        .map(product -> new InspectionProductResult(
                                product.channelProductNo(), product.reason(), product.action(),
                                product.restorationRequestAvailable()))
                        .toList(),
                inspections.page(), inspections.size(),
                inspections.totalElements(), inspections.totalPages());
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void restoreInspectionProduct(Long channelProductNo) {
        requireProviderEnabled();
        inventoryProvider.restoreInspectionProduct(channelProductNo);
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
                local.productId(), local.previewVersion(), local.originProductNo(),
                local.salePrice(), channel.salePrice(), local.targetStatus(), channel.status(),
                different, options);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void applyProduct(Long productId, String previewVersion) {
        requireProviderEnabled();
        if (!orderSyncUseCase.synchronizeBeforeStock()) {
            throw new HappyGalleryException(ErrorCode.CONFLICT,
                    "스마트스토어 주문 수집이 완료되지 않아 상품 반영을 보류했습니다. 잠시 후 다시 시도해 주세요.");
        }
        if (transactionService.hasUnappliedOrders(productId)) {
            throw new HappyGalleryException(ErrorCode.CONFLICT,
                    "스마트스토어 재고 미반영 주문을 먼저 확인해 주세요.");
        }
        var local = transactionService.productSnapshot(productId);
        if (!local.previewVersion().equals(previewVersion)) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "상품 또는 스마트스토어 연결이 변경되었습니다. 최신 차이를 다시 확인해 주세요.");
        }
        try {
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
        } finally {
            queuePort.requestIfMapped(List.of(productId), LocalDateTime.now(clock));
        }
    }

    private void requireProviderEnabled() {
        if (!inventoryProvider.isEnabled()) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "스마트스토어 연동이 비활성화되어 있습니다.");
        }
    }

}
