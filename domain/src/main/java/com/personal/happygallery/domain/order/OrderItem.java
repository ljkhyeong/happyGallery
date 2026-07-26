package com.personal.happygallery.domain.order;

import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import jakarta.persistence.Column;
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
                qty, unitPrice, null, null, null);
    }

    public OrderItem(Order order, Long productId, String productName, int qty, long unitPrice,
                     String specification, String careInstructions, Integer productionLeadDays) {
        this(order, productId, productName,
                productionLeadDays == null ? ProductType.READY_STOCK : ProductType.MADE_TO_ORDER,
                qty, unitPrice, specification, careInstructions, productionLeadDays);
    }

    public OrderItem(Order order, Long productId, String productName, ProductType productType,
                     int qty, long unitPrice, String specification,
                     String careInstructions, Integer productionLeadDays) {
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
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.productType = productType;
        this.qty = qty;
        this.unitPrice = unitPrice;
        this.specification = specification;
        this.careInstructions = careInstructions;
        this.productionLeadDays = productionLeadDays;
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public ProductType getProductType() { return productType; }
    public String getSpecification() { return specification; }
    public String getCareInstructions() { return careInstructions; }
    public Integer getProductionLeadDays() { return productionLeadDays; }
    public int getQty() { return qty; }
    public long getUnitPrice() { return unitPrice; }
}
