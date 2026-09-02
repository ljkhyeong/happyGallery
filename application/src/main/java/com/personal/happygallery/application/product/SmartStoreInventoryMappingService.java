package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.DeleteMappingCommand;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.MappingActor;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.MappingHistoryResult;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.MappingResult;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.SaveMappingCommand;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.VariantMapping;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryMappingHistoryPort;
import com.personal.happygallery.application.product.port.out.SmartStoreOrderMappingHistoryPort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockMappingPort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncPort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncQueuePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.SmartStoreInventoryMappingAction;
import com.personal.happygallery.domain.product.SmartStoreInventoryMappingHistory;
import com.personal.happygallery.domain.product.SmartStoreOrderMappingHistory;
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
    private final SmartStoreOrderMappingHistoryPort orderMappingHistoryPort;
    private final SmartStoreInventoryMappingHistoryPort mappingHistoryPort;
    private final SmartStoreStockSyncPort syncPort;
    private final SmartStoreStockSyncQueuePort queuePort;
    private final SmartStoreStockSyncTransactionService stockSyncTransactionService;
    private final Clock clock;

    SmartStoreInventoryMappingService(
            ProductReaderPort productReaderPort,
            ProductOptionConfigurationService optionConfigurationService,
            SmartStoreStockMappingPort mappingPort,
            SmartStoreOrderMappingHistoryPort orderMappingHistoryPort,
            SmartStoreInventoryMappingHistoryPort mappingHistoryPort,
            SmartStoreStockSyncPort syncPort,
            SmartStoreStockSyncQueuePort queuePort,
            SmartStoreStockSyncTransactionService stockSyncTransactionService,
            Clock clock) {
        this.productReaderPort = productReaderPort;
        this.optionConfigurationService = optionConfigurationService;
        this.mappingPort = mappingPort;
        this.orderMappingHistoryPort = orderMappingHistoryPort;
        this.mappingHistoryPort = mappingHistoryPort;
        this.syncPort = syncPort;
        this.queuePort = queuePort;
        this.stockSyncTransactionService = stockSyncTransactionService;
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
    public MappingResult saveMapping(Long productId, SaveMappingCommand command, MappingActor actor) {
        Product product = productReaderPort.findByIdWithLock(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        List<SmartStoreStockMapping> previous = mappings(productId);
        verifyExpectedVersion(previous, command.expectedMappingVersion());
        boolean originChanged = !previous.isEmpty()
                && !previous.getFirst().getOriginProductNo().equals(command.originProductNo());
        requirePreviousOriginConfirmation(originChanged, command.previousOriginConfirmed());
        requireNoUnappliedOrders(productId, originChanged);
        validate(product, command.variants());
        LocalDateTime changedAt = LocalDateTime.now(clock);
        preserveOrderMappings(previous, changedAt);

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
        MappingResult result = result(saved);
        mappingHistoryPort.save(mappingHistory(
                productId, previous, saved, command.previousOriginConfirmed(), actor, changedAt));
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<MappingResult> getMapping(Long productId) {
        if (productReaderPort.findById(productId).isEmpty()) {
            throw new NotFoundException("상품");
        }
        List<SmartStoreStockMapping> mappings = mappings(productId);
        return mappings.isEmpty() ? Optional.empty() : Optional.of(result(mappings));
    }

    @Transactional(readOnly = true)
    public void planDelete(Long productId, DeleteMappingCommand command) {
        if (productReaderPort.findById(productId).isEmpty()) {
            throw new NotFoundException("상품");
        }
        List<SmartStoreStockMapping> current = mappings(productId);
        verifyExpectedVersion(current, command.expectedMappingVersion());
        requirePreviousOriginConfirmation(!current.isEmpty(), command.previousOriginConfirmed());
    }

    @Transactional
    public void deleteMapping(Long productId, DeleteMappingCommand command, MappingActor actor) {
        productReaderPort.findByIdWithLock(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        List<SmartStoreStockMapping> current = mappings(productId);
        verifyExpectedVersion(current, command.expectedMappingVersion());
        requirePreviousOriginConfirmation(!current.isEmpty(), command.previousOriginConfirmed());
        if (current.isEmpty()) {
            syncPort.deleteByProductId(productId);
            return;
        }
        requireNoUnappliedOrders(productId, true);
        LocalDateTime changedAt = LocalDateTime.now(clock);
        preserveOrderMappings(current, changedAt);
        mappingPort.deleteByProductId(productId);
        syncPort.deleteByProductId(productId);
        mappingHistoryPort.save(new SmartStoreInventoryMappingHistory(
                productId,
                SmartStoreInventoryMappingAction.DELETED,
                originProductNo(current),
                null,
                enabled(current),
                null,
                optionMappings(current),
                null,
                mappingVersion(current),
                null,
                command.previousOriginConfirmed(),
                actor.adminUserId(),
                actor.name(),
                changedAt));
    }

    @Transactional(readOnly = true)
    public List<MappingHistoryResult> listMappingHistory(Long productId) {
        if (productReaderPort.findById(productId).isEmpty()) {
            throw new NotFoundException("상품");
        }
        return mappingHistoryPort.findRecentByProductId(productId).stream()
                .map(history -> new MappingHistoryResult(
                        history.getId(),
                        history.getAction(),
                        history.getPreviousOriginProductNo(),
                        history.getNextOriginProductNo(),
                        history.getPreviousEnabled(),
                        history.getNextEnabled(),
                        history.getPreviousOptionMappings(),
                        history.getNextOptionMappings(),
                        history.getPreviousMappingVersion(),
                        history.getNextMappingVersion(),
                        history.isPreviousOriginConfirmed(),
                        history.getChangedByAdminId(),
                        history.getChangedBy(),
                        history.getChangedAt()))
                .toList();
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

    private void requireNoUnappliedOrders(Long productId, boolean previousOriginRemoved) {
        if (previousOriginRemoved && stockSyncTransactionService.hasUnappliedOrders(productId)) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "기존 원상품의 재고 미반영 주문을 먼저 처리해 주세요.");
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

    private void preserveOrderMappings(List<SmartStoreStockMapping> mappings, LocalDateTime closedAt) {
        if (!mappings.isEmpty()) {
            orderMappingHistoryPort.saveAll(mappings.stream()
                    .map(mapping -> new SmartStoreOrderMappingHistory(mapping, closedAt))
                    .toList());
        }
    }

    private static SmartStoreInventoryMappingHistory mappingHistory(
            Long productId,
            List<SmartStoreStockMapping> previous,
            List<SmartStoreStockMapping> next,
            boolean previousOriginConfirmed,
            MappingActor actor,
            LocalDateTime changedAt) {
        SmartStoreInventoryMappingAction action;
        if (previous.isEmpty()) {
            action = SmartStoreInventoryMappingAction.CREATED;
        } else if (!originProductNo(previous).equals(originProductNo(next))) {
            action = SmartStoreInventoryMappingAction.ORIGIN_CHANGED;
        } else if (!enabled(previous) && enabled(next)) {
            action = SmartStoreInventoryMappingAction.ENABLED;
        } else if (enabled(previous) && !enabled(next)) {
            action = SmartStoreInventoryMappingAction.DISABLED;
        } else {
            action = SmartStoreInventoryMappingAction.UPDATED;
        }
        return new SmartStoreInventoryMappingHistory(
                productId,
                action,
                originProductNo(previous),
                originProductNo(next),
                enabled(previous),
                enabled(next),
                optionMappings(previous),
                optionMappings(next),
                previous.isEmpty() ? null : mappingVersion(previous),
                mappingVersion(next),
                previousOriginConfirmed,
                actor.adminUserId(),
                actor.name(),
                changedAt);
    }

    private static Long originProductNo(List<SmartStoreStockMapping> mappings) {
        return mappings.isEmpty() ? null : mappings.getFirst().getOriginProductNo();
    }

    private static Boolean enabled(List<SmartStoreStockMapping> mappings) {
        return mappings.isEmpty() ? null : mappings.getFirst().isEnabled();
    }

    private static String optionMappings(List<SmartStoreStockMapping> mappings) {
        String summary = mappings.stream()
                .filter(mapping -> mapping.getProductVariantId() != null && !mapping.isRetired())
                .sorted((left, right) -> left.getProductVariantId().compareTo(right.getProductVariantId()))
                .map(mapping -> "조합 %d → 옵션 %d".formatted(
                        mapping.getProductVariantId(), mapping.getOptionId()))
                .collect(Collectors.joining(", "));
        return summary.isEmpty() ? null : summary;
    }

    record MappingChangePlan(boolean originChanged) {}
}
