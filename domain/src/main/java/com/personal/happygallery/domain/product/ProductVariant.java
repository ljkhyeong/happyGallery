package com.personal.happygallery.domain.product;

import com.personal.happygallery.domain.error.InventoryNotEnoughException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Entity
@Table(name = "product_variants")
public class ProductVariant {

    public static final String DEFAULT_COMBINATION_KEY = "DEFAULT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "combination_key", nullable = false, length = 512)
    private String combinationKey;

    @Column(name = "price_adjustment", nullable = false)
    private long priceAdjustment;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private boolean active;

    @Version
    @Column(nullable = false)
    private long version;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "product_variant_selections",
            joinColumns = @JoinColumn(name = "variant_id"))
    @OrderBy("sortOrder ASC")
    private List<ProductVariantSelection> selections = List.of();

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ProductVariant() {}

    public ProductVariant(Long productId, String combinationKey, long basePrice,
                          long priceAdjustment, int quantity, boolean active,
                          List<ProductVariantSelection> selections) {
        if (productId == null || combinationKey == null || combinationKey.isBlank()) {
            throw new IllegalArgumentException("상품과 옵션 조합 식별자는 필수입니다.");
        }
        requireSelections(selections);
        this.productId = productId;
        this.combinationKey = combinationKey;
        this.selections = List.copyOf(selections);
        update(basePrice, priceAdjustment, quantity, active);
    }

    public void update(long basePrice, long priceAdjustment, int quantity, boolean active) {
        if (quantity < 0) {
            throw new IllegalArgumentException("옵션 조합 재고는 0 이상이어야 합니다.");
        }
        this.priceAdjustment = ProductOptionPolicy.requireVariantPrice(basePrice, priceAdjustment);
        this.quantity = quantity;
        this.active = active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void requireSufficient(int qty) {
        requirePositive(qty);
        if (!active || quantity < qty) {
            throw new InventoryNotEnoughException();
        }
    }

    public void deduct(int qty) {
        requireSufficient(qty);
        quantity -= qty;
    }

    public void restore(int qty) {
        requirePositive(qty);
        quantity = Math.addExact(quantity, qty);
    }

    public boolean isAvailable() {
        return active && quantity > 0;
    }

    public long unitPrice(long basePrice, long textOptionPriceAdjustment) {
        try {
            long price = Math.addExact(Math.addExact(basePrice, priceAdjustment), textOptionPriceAdjustment);
            if (price < 1) {
                throw new IllegalArgumentException("옵션을 반영한 상품 가격은 1원 이상이어야 합니다.");
            }
            return price;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("옵션을 반영한 상품 가격이 너무 큽니다.", exception);
        }
    }

    private static void requireSelections(List<ProductVariantSelection> selections) {
        if (selections == null || selections.size() > ProductOptionPolicy.MAX_SELECT_GROUPS) {
            throw new IllegalArgumentException("옵션 조합 선택값이 올바르지 않습니다.");
        }
        if (new HashSet<>(selections.stream()
                .map(ProductVariantSelection::getOptionGroupId)
                .toList()).size() != selections.size()) {
            throw new IllegalArgumentException("한 옵션 그룹에서 둘 이상의 값을 선택할 수 없습니다.");
        }
    }

    private static void requirePositive(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("재고 변경 수량은 1 이상이어야 합니다.");
        }
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getCombinationKey() { return combinationKey; }
    public long getPriceAdjustment() { return priceAdjustment; }
    public int getQuantity() { return quantity; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
    public List<ProductVariantSelection> getSelections() { return List.copyOf(selections); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
