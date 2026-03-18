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
import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.HappyGalleryException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 주문 이행 정보 — fulfillments 테이블.
 *
 * <p>MADE_TO_ORDER 승인 시(SHIPPING), 또는 픽업 준비 완료 시(PICKUP) 생성된다.
 * 관리자가 {@link #setExpectedShipDate(LocalDate)}로 예상 출고일을,
 * {@link #getPickupDeadlineAt()}로 픽업 마감 시각을 관리한다.
 *
 * <p>주문 상태는 {@link Order#getStatus()}가 단일 소스이다.
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
     */
    public Fulfillment(Long orderId, FulfillmentType type) {
        this.orderId = orderId;
        this.type = type;
    }

    /**
     * 픽업 이행 레코드 생성 (PICKUP).
     *
     * @param orderId          주문 ID
     * @param pickupDeadlineAt 픽업 마감 시각
     */
    public Fulfillment(Long orderId, LocalDateTime pickupDeadlineAt) {
        this.orderId = orderId;
        this.type = FulfillmentType.PICKUP;
        this.pickupDeadlineAt = pickupDeadlineAt;
    }

    /** 예상 출고일을 갱신한다. */
    public void setExpectedShipDate(LocalDate expectedShipDate) {
        this.expectedShipDate = expectedShipDate;
    }

    /** SHIPPING 타입인지 확인한다. 픽업 이행에서 출고일 갱신 시 호출. */
    public void requireShippingType() {
        if (this.type != FulfillmentType.SHIPPING) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "배송 이행에서만 출고일을 설정할 수 있습니다.");
        }
    }

    /** 기존 이행 레코드를 픽업용으로 전환한다 (MADE_TO_ORDER 제작 완료 후 픽업 시). */
    public void convertToPickup(LocalDateTime pickupDeadlineAt) {
        this.type = FulfillmentType.PICKUP;
        this.expectedShipDate = null;
        this.pickupDeadlineAt = pickupDeadlineAt;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public FulfillmentType getType() { return type; }
    public LocalDate getExpectedShipDate() { return expectedShipDate; }
    public LocalDateTime getPickupDeadlineAt() { return pickupDeadlineAt; }
    public long getVersion() { return version; }
}
