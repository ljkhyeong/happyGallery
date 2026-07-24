package com.personal.happygallery.domain.order;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_claim_items")
public class OrderClaimItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "approved_refund_amount")
    private Long approvedRefundAmount;

    protected OrderClaimItem() {}

    public OrderClaimItem(Long claimId, Long orderId, Long orderItemId, int quantity) {
        if (claimId == null || orderId == null || orderItemId == null || quantity <= 0) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "클레임 상품과 수량을 확인해주세요.");
        }
        this.claimId = claimId;
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.quantity = quantity;
    }

    public void allocateApprovedRefundAmount(long amount) {
        if (amount < 0L) {
            throw new IllegalArgumentException("승인 환불 배분액은 0원 이상이어야 합니다.");
        }
        this.approvedRefundAmount = amount;
    }

    public Long getId() { return id; }
    public Long getClaimId() { return claimId; }
    public Long getOrderId() { return orderId; }
    public Long getOrderItemId() { return orderItemId; }
    public int getQuantity() { return quantity; }
    public Long getApprovedRefundAmount() { return approvedRefundAmount; }
}
