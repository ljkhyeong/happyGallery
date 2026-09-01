package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.MappingResult;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.SaveMappingCommand;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.VariantMapping;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockMappingPort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncPort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncQueuePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.SmartStoreStockMapping;
import com.personal.happygallery.domain.product.SmartStoreStockSync;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SmartStoreInventoryMappingService {

    private final ProductReaderPort productReaderPort;
    private final ProductOptionConfigurationService optionConfigurationService;
    private final SmartStoreStockMappingPort mappingPort;
    private final SmartStoreStockSyncPort syncPort;
    private final SmartStoreStockSyncQueuePort queuePort;
    private final Clock clock;

    SmartStoreInventoryMappingService(
            ProductReaderPort productReaderPort,
            ProductOptionConfigurationService optionConfigurationService,
            SmartStoreStockMappingPort mappingPort,
            SmartStoreStockSyncPort syncPort,
            SmartStoreStockSyncQueuePort queuePort,
            Clock clock) {
        this.productReaderPort = productReaderPort;
        this.optionConfigurationService = optionConfigurationService;
        this.mappingPort = mappingPort;
        this.syncPort = syncPort;
        this.queuePort = queuePort;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MappingChangePlan planChange(Long productId, SaveMappingCommand command) {
        if (productReaderPort.findById(productId).isEmpty()) {
            throw new NotFoundException("상품");
        }
        List<SmartStoreStockMapping> current = mappings(productId);
        verifyExpectedVersion(current, command.expectedMappingVersion());
        boolean originChanged = !current.isEmpty()
                && !current.getFirst().getOriginProductNo().equals(command.originProductNo());
        requirePreviousOriginConfirmation(originChanged, command.previousOriginConfirmed());
        return new MappingChangePlan(originChanged);
    }

    @Transactional
    public MappingResult saveMapping(Long productId, SaveMappingCommand command) {
        Product product = productReaderPort.findByIdWithLock(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        List<SmartStoreStockMapping> previous = mappings(productId);
        verifyExpectedVersion(previous, command.expectedMappingVersion());
        boolean originChanged = !previous.isEmpty()
                && !previous.getFirst().getOriginProductNo().equals(command.originProductNo());
        requirePreviousOriginConfirmation(originChanged, command.previousOriginConfirmed());
        validate(product, command.variants());

        Set<Long> requestedOptionIds = command.variants().stream()
                .map(VariantMapping::optionId).collect(Collectors.toSet());
        mappingPort.deleteByProductId(productId);
        List<SmartStoreStockMapping> next = new ArrayList<>(command.variants().isEmpty()
                ? List.of(new SmartStoreStockMapping(
                        productId, null, command.originProductNo(), null, command.enabled()))
                : command.variants().stream()
                        .map(variant -> new SmartStoreStockMapping(
                                productId,
                                variant.productVariantId(),
                                command.originProductNo(),
                                variant.optionId(),
                                command.enabled()))
                        .toList());
        for (SmartStoreStockMapping mapping : previous) {
            if (product.getType() == ProductType.MADE_TO_ORDER
                    && mapping.getOriginProductNo().equals(command.originProductNo())
                    && mapping.getProductVariantId() != null
                    && !requestedOptionIds.contains(mapping.getOptionId())) {
                next.add(mapping.retiredCopy(command.enabled()));
            }
        }
        List<SmartStoreStockMapping> saved = mappingPort.saveAll(next);
        if (command.enabled()) {
            queuePort.requestIfMapped(List.of(productId), LocalDateTime.now(clock));
        } else {
            syncPort.deleteByProductId(productId);
        }
        return result(saved);
    }

    @Transactional(readOnly = true)
    public Optional<MappingResult> getMapping(Long productId) {
        if (productReaderPort.findById(productId).isEmpty()) {
            throw new NotFoundException("상품");
        }
        List<SmartStoreStockMapping> mappings = mappings(productId);
        return mappings.isEmpty() ? Optional.empty() : Optional.of(result(mappings));
    }

    @Transactional
    public void deleteMapping(Long productId) {
        if (productReaderPort.findById(productId).isEmpty()) {
            throw new NotFoundException("상품");
        }
        mappingPort.deleteByProductId(productId);
        syncPort.deleteByProductId(productId);
    }

    @Transactional
    public MappingResult retry(Long productId) {
        List<SmartStoreStockMapping> mappings = mappings(productId);
        if (mappings.isEmpty()) {
            throw new NotFoundException("스마트스토어 재고 연동 설정");
        }
        queuePort.requestIfMapped(List.of(productId), LocalDateTime.now(clock));
        return result(mappings);
    }

    private List<SmartStoreStockMapping> mappings(Long productId) {
        return mappingPort.findByProductIdOrderByProductVariantIdAsc(productId);
    }

    private void validate(Product product, List<VariantMapping> requested) {
        if (product.getType() == ProductType.READY_STOCK) {
            if (!requested.isEmpty()) {
                throw new IllegalArgumentException("기성품은 스마트스토어 옵션 번호를 입력하지 않습니다.");
            }
            return;
        }
        Set<Long> existingIds = optionConfigurationService.get(product.getId(), true).variants().stream()
                .map(ProductOptions.Variant::id).collect(Collectors.toSet());
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
                mappingVersion(mappings),
                first.getOriginProductNo(),
                first.isEnabled(),
                mappings.stream()
                        .filter(mapping -> mapping.getProductVariantId() != null && !mapping.isRetired())
                        .map(mapping -> new VariantMapping(
                                mapping.getProductVariantId(), mapping.getOptionId()))
                        .toList(),
                sync == null ? null : sync.getStatus(),
                sync == null ? 0 : sync.getAttemptCount(),
                sync == null ? null : sync.getLastError(),
                sync == null ? null : sync.getSyncedAt());
    }

    private static void verifyExpectedVersion(
            List<SmartStoreStockMapping> mappings, Long expectedMappingVersion) {
        Long currentVersion = mappings.isEmpty() ? null : mappingVersion(mappings);
        if (!Objects.equals(currentVersion, expectedMappingVersion)) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "스마트스토어 연동 설정이 변경되었습니다. 최신 설정을 다시 확인해 주세요.");
        }
    }

    private static void requirePreviousOriginConfirmation(boolean originChanged, boolean confirmed) {
        if (originChanged && !confirmed) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "기존 원상품의 판매 중지와 재고 확인을 완료해 주세요.");
        }
    }

    private static long mappingVersion(List<SmartStoreStockMapping> mappings) {
        return mappings.stream()
                .map(SmartStoreStockMapping::getId)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElseThrow(() -> new IllegalStateException("저장된 스마트스토어 매핑 식별자가 없습니다."));
    }

    record MappingChangePlan(boolean originChanged) {}
}
