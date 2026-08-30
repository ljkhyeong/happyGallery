package com.personal.happygallery.domain.order;

import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.OrderBy;
import java.util.List;

/** 주문 상품 라인 — order_items 테이블 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    public static final int MAX_PRODUCT_NAME_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_variant_id")
    private Long productVariantId;

    @Column(name = "product_name", nullable = false, length = MAX_PRODUCT_NAME_LENGTH)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", length = 20)
    private ProductType productType;

    @Column(length = 2_000)
    private String specification;

    @Column(name = "care_instructions", length = 2_000)
    private String careInstructions;

    @Column(name = "production_lead_days")
    private Integer productionLeadDays;

    @Column(nullable = false)
    private int qty;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(name = "base_price", nullable = false)
    private long basePrice;

    @Column(name = "variant_price_adjustment", nullable = false)
    private long variantPriceAdjustment;

    @Column(name = "text_option_price_adjustment", nullable = false)
    private long textOptionPriceAdjustment;

    @ElementCollection
    @CollectionTable(
            name = "order_item_option_snapshots",
            joinColumns = @JoinColumn(name = "order_item_id"))
    @OrderBy("sortOrder ASC")
    private List<OrderOptionSnapshot> optionSnapshots = List.of();

    @Column(name = "gross_amount", nullable = false)
    private long grossAmount;

    @Column(name = "coupon_discount_amount", nullable = false)
    private long couponDiscountAmount;

    @Column(name = "reward_used_amount", nullable = false)
    private long rewardUsedAmount;

    @Column(name = "net_paid_amount", nullable = false)
    private long netPaidAmount;

    protected OrderItem() {}

    /**
     * 주문 상품 라인 생성.
     *
     * @param order     소속 주문
     * @param productId 상품 ID
     * @param productName 결제 준비 시점 상품명
     * @param qty         수량
     * @param unitPrice   단가 (원)
     */
    public OrderItem(Order order, Long productId, String productName, int qty, long unitPrice) {
        this(order, productId, productName, ProductType.READY_STOCK,
                qty, unitPrice, null, null, null,
                OrderItemPricing.fullPrice(qty, unitPrice));
    }

    public OrderItem(Order order, Long productId, String productName, int qty, long unitPrice,
                     String specification, String careInstructions, Integer productionLeadDays) {
        this(order, productId, productName,
                productionLeadDays == null ? ProductType.READY_STOCK : ProductType.MADE_TO_ORDER,
                qty, unitPrice, specification, careInstructions, productionLeadDays,
                OrderItemPricing.fullPrice(qty, unitPrice));
    }

    public OrderItem(Order order, Long productId, String productName, ProductType productType,
                     int qty, long unitPrice, String specification,
                     String careInstructions, Integer productionLeadDays) {
        this(order, productId, productName, productType, qty, unitPrice,
                specification, careInstructions, productionLeadDays,
                OrderItemPricing.fullPrice(qty, unitPrice));
    }

    public OrderItem(Order order, Long productId, String productName, ProductType productType,
                     int qty, long unitPrice, String specification,
                     String careInstructions, Integer productionLeadDays,
                     OrderItemPricing pricing) {
        this(order, productId, null, productName, productType,
                qty, unitPrice, unitPrice, 0L, 0L, List.of(),
                specification, careInstructions, productionLeadDays, pricing);
    }

    public OrderItem(Order order, Long productId, Long productVariantId,
                     String productName, ProductType productType,
                     int qty, long unitPrice, long basePrice,
                     long variantPriceAdjustment, long textOptionPriceAdjustment,
                     List<OrderOptionSnapshot> optionSnapshots,
                     String specification, String careInstructions,
                     Integer productionLeadDays, OrderItemPricing pricing) {
        if (productType == null) {
            throw new IllegalArgumentException("신규 주문 상품의 상품 유형은 필수입니다.");
        }
        if (productType == ProductType.MADE_TO_ORDER
                && (productionLeadDays == null
                    || productionLeadDays < Product.MIN_PRODUCTION_LEAD_DAYS
                    || productionLeadDays > Product.MAX_PRODUCTION_LEAD_DAYS
                    || specification == null
                    || specification.isBlank())) {
            throw new IllegalArgumentException("주문제작 스냅샷의 상품 사양과 제작 기간이 올바르지 않습니다.");
        }
        if (productType == ProductType.READY_STOCK && productionLeadDays != null) {
            throw new IllegalArgumentException("기성품 주문에는 제작 기간을 저장할 수 없습니다.");
        }
        long expectedGrossAmount = OrderAmountCalculator.addLine(0L, qty, unitPrice);
        if (pricing == null || pricing.grossAmount() != expectedGrossAmount) {
            throw new IllegalArgumentException("주문 상품 금액과 혜택 배분이 일치하지 않습니다.");
        }
        long expectedUnitPrice;
        try {
            expectedUnitPrice = Math.addExact(
                    Math.addExact(basePrice, variantPriceAdjustment),
                    textOptionPriceAdjustment);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("주문 옵션 금액이 너무 큽니다.", exception);
        }
        if (basePrice < 1 || textOptionPriceAdjustment < 0 || unitPrice != expectedUnitPrice) {
            throw new IllegalArgumentException("주문 상품 기본가와 옵션 금액이 일치하지 않습니다.");
        }
        this.order = order;
        this.productId = productId;
        this.productVariantId = productVariantId;
        this.productName = productName;
        this.productType = productType;
        this.qty = qty;
        this.unitPrice = unitPrice;
        this.basePrice = basePrice;
        this.variantPriceAdjustment = variantPriceAdjustment;
        this.textOptionPriceAdjustment = textOptionPriceAdjustment;
        this.optionSnapshots = List.copyOf(optionSnapshots);
        this.specification = specification;
        this.careInstructions = careInstructions;
        this.productionLeadDays = productionLeadDays;
        this.grossAmount = pricing.grossAmount();
        this.couponDiscountAmount = pricing.couponDiscountAmount();
        this.rewardUsedAmount = pricing.rewardUsedAmount();
        this.netPaidAmount = pricing.netPaidAmount();
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public Long getProductId() { return productId; }
    public Long getProductVariantId() { return productVariantId; }
    public String getProductName() { return productName; }
    public ProductType getProductType() { return productType; }
    public String getSpecification() { return specification; }
    public String getCareInstructions() { return careInstructions; }
    public Integer getProductionLeadDays() { return productionLeadDays; }
    public int getQty() { return qty; }
    public long getUnitPrice() { return unitPrice; }
    public long getBasePrice() { return basePrice; }
    public long getVariantPriceAdjustment() { return variantPriceAdjustment; }
    public long getTextOptionPriceAdjustment() { return textOptionPriceAdjustment; }
    public List<OrderOptionSnapshot> getOptionSnapshots() { return List.copyOf(optionSnapshots); }
    public long getGrossAmount() { return grossAmount; }
    public long getCouponDiscountAmount() { return couponDiscountAmount; }
    public long getRewardUsedAmount() { return rewardUsedAmount; }
    public long getNetPaidAmount() { return netPaidAmount; }
}
