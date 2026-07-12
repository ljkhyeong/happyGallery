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
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

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

    private Fulfillment(Long orderId) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
    }

    /** 배송 이행 레코드를 생성한다. */
    public static Fulfillment shipping(Long orderId) {
        Fulfillment fulfillment = new Fulfillment(orderId);
        fulfillment.type = FulfillmentType.SHIPPING;
        return fulfillment;
    }

    /** 픽업 이행 레코드를 생성한다. */
    public static Fulfillment pickup(Long orderId, LocalDateTime pickupDeadlineAt) {
        Fulfillment fulfillment = new Fulfillment(orderId);
        fulfillment.type = FulfillmentType.PICKUP;
        fulfillment.pickupDeadlineAt = requirePickupDeadlineAt(pickupDeadlineAt);
        return fulfillment;
    }

    /** 예상 출고일을 갱신한다. */
    public void setExpectedShipDate(LocalDate expectedShipDate) {
        requireShippingType();
        this.expectedShipDate = expectedShipDate;
    }

    private void requireShippingType() {
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
        this.pickupDeadlineAt = requirePickupDeadlineAt(pickupDeadlineAt);
    }

    private static LocalDateTime requirePickupDeadlineAt(LocalDateTime pickupDeadlineAt) {
        if (pickupDeadlineAt == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "픽업 마감 시각은 필수입니다.");
        }
        return pickupDeadlineAt;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public FulfillmentType getType() { return type; }
    public LocalDate getExpectedShipDate() { return expectedShipDate; }
    public LocalDateTime getPickupDeadlineAt() { return pickupDeadlineAt; }
    public long getVersion() { return version; }
}
