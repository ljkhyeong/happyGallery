package com.personal.happygallery.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 주문 이행 정보 — fulfillments 테이블.
 *
 * <p>MADE_TO_ORDER 승인 시(SHIPPING), 또는 픽업 준비 완료 시(PICKUP) 생성된다.
 * 관리자가 {@link #setExpectedShipDate(LocalDate)}로 예상 출고일을,
 * {@link #getPickupDeadlineAt()}로 픽업 마감 시각을 관리한다.
 */
@Entity
@Table(name = "fulfillments")
public class Fulfillment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FulfillmentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "expected_ship_date")
    private LocalDate expectedShipDate;

    @Column(name = "pickup_deadline_at")
    private LocalDateTime pickupDeadlineAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Fulfillment() {}

    /**
     * 제작 이행 레코드 생성 (SHIPPING).
     *
     * @param orderId 주문 ID
     * @param type    이행 유형 (SHIPPING | PICKUP)
     * @param status  초기 주문 상태 (IN_PRODUCTION)
     */
    public Fulfillment(Long orderId, FulfillmentType type, OrderStatus status) {
        this.orderId = orderId;
        this.type = type;
        this.status = status;
    }

    /**
     * 픽업 이행 레코드 생성 (PICKUP).
     *
     * @param orderId          주문 ID
     * @param status           초기 주문 상태 (PICKUP_READY)
     * @param pickupDeadlineAt 픽업 마감 시각
     */
    public Fulfillment(Long orderId, OrderStatus status, LocalDateTime pickupDeadlineAt) {
        this.orderId = orderId;
        this.type = FulfillmentType.PICKUP;
        this.status = status;
        this.pickupDeadlineAt = pickupDeadlineAt;
    }

    /** 예상 출고일을 갱신한다. */
    public void setExpectedShipDate(LocalDate expectedShipDate) {
        this.expectedShipDate = expectedShipDate;
    }

    /**
     * 배치에서 픽업 만료 대상인지 판단한다.
     * 픽업 준비 상태이고, 픽업 마감 시각이 {@code now} 이전이면 {@code true}.
     */
    public boolean canExpirePickup(LocalDateTime now) {
        return this.status == OrderStatus.PICKUP_READY
                && this.pickupDeadlineAt != null
                && this.pickupDeadlineAt.isBefore(now);
    }

    /** 주문 상태와 동기화한다. null이면 무시한다. */
    public void syncStatus(OrderStatus status) {
        if (status == null) {
            return;
        }
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public FulfillmentType getType() { return type; }
    public OrderStatus getStatus() { return status; }
    public LocalDate getExpectedShipDate() { return expectedShipDate; }
    public LocalDateTime getPickupDeadlineAt() { return pickupDeadlineAt; }
    public long getVersion() { return version; }
}
