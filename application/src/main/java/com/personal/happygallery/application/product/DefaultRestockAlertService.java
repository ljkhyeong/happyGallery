package com.personal.happygallery.application.product;

import com.personal.happygallery.application.customer.MemberAccountGuard;
import com.personal.happygallery.application.product.port.in.RestockAlertUseCase;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.ProductVariantStorePort;
import com.personal.happygallery.application.product.port.out.RestockAlertPort;
import com.personal.happygallery.application.product.port.out.RestockAlertDeliveryPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.PhoneVerificationRequiredException;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.RestockAlert;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultRestockAlertService implements RestockAlertUseCase {
    private final MemberAccountGuard members;
    private final ProductReaderPort products;
    private final InventoryStorePort inventories;
    private final ProductVariantStorePort variants;
    private final ProductOptionConfigurationService options;
    private final RestockAlertPort alerts;
    private final RestockAlertDeliveryPort delivery;
    private final Clock clock;

    public DefaultRestockAlertService(MemberAccountGuard members, ProductReaderPort products,
            InventoryStorePort inventories, ProductVariantStorePort variants,
            ProductOptionConfigurationService options, RestockAlertPort alerts,
            RestockAlertDeliveryPort delivery, Clock clock) {
        this.members = members;
        this.products = products;
        this.inventories = inventories;
        this.variants = variants;
        this.options = options;
        this.alerts = alerts;
        this.delivery = delivery;
        this.clock = clock;
    }

    @Override
    public RestockAlert register(Long userId, Long productId, Long productVariantId) {
        var user = members.requireActiveForUpdate(userId);
        if (!user.isActive()) throw new NotFoundException("회원");
        if (!user.isPhoneVerified()) throw new PhoneVerificationRequiredException();
        var product = products.findByIdWithLock(productId)
                .filter(value -> value.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(NotFoundException.supplier("상품"));
        String label = requireSoldOut(product, productVariantId);
        var existing = alerts.findByActiveKey(RestockAlert.activeKey(userId, productId, productVariantId));
        if (existing.isPresent()) {
            var sentAt = delivery.findSentAt(existing.get().getId());
            if (sentAt.isEmpty()) return existing.get();
            existing.get().markNotified(sentAt.get());
            alerts.saveAndFlush(existing.get());
        }
        return alerts.saveAndFlush(new RestockAlert(userId, productId, productVariantId, label, LocalDateTime.now(clock)));
    }

    private String requireSoldOut(Product product, Long variantId) {
        if (product.getType() == ProductType.READY_STOCK) {
            if (variantId != null) throw invalidTarget();
            var inventory = inventories.findByProductIdInWithLock(List.of(product.getId())).stream().findFirst()
                    .orElseThrow(NotFoundException.supplier("재고"));
            if (inventory.getQuantity() != 0) throw invalidTarget();
            return "기본 상품";
        }
        if (variantId == null) throw invalidTarget();
        var variant = variants.findByIdInWithLock(List.of(variantId)).stream()
                .filter(value -> value.getProductId().equals(product.getId()) && value.isActive()).findFirst()
                .orElseThrow(NotFoundException.supplier("옵션 조합"));
        if (variant.getQuantity() != 0) throw invalidTarget();
        var catalog = options.get(product.getId(), false);
        var selected = catalog.variants().stream().filter(value -> value.id().equals(variantId)).findFirst().orElseThrow();
        if (selected.selections().isEmpty()) return "기본 조합";
        return selected.selections().stream().map(selection -> {
            var group = catalog.groups().stream().filter(value -> value.key().equals(selection.groupKey())).findFirst().orElseThrow();
            var value = group.values().stream().filter(item -> item.key().equals(selection.valueKey())).findFirst().orElseThrow();
            return group.name() + ": " + value.name();
        }).collect(Collectors.joining(" / "));
    }

    private static HappyGalleryException invalidTarget() {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT, "현재 판매 중인 품절 상품 또는 품절 옵션에만 신청할 수 있습니다.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<View> list(Long userId) {
        var rows = alerts.findByUserIdOrderByIdDesc(userId);
        var byId = products.findAllById(rows.stream().map(RestockAlert::getProductId).distinct().toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        return rows.stream().filter(row -> byId.containsKey(row.getProductId()))
                .map(row -> new View(row, byId.get(row.getProductId()).getName())).toList();
    }

    @Override
    public void cancel(Long userId, Long alertId) {
        var alert = alerts.findByIdForUpdate(alertId).filter(value -> value.getUserId().equals(userId))
                .orElseThrow(NotFoundException.supplier("재입고 알림"));
        alert.cancel(LocalDateTime.now(clock));
        alerts.saveAndFlush(alert);
    }
}
