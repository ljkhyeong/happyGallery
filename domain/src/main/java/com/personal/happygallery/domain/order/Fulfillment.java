package com.personal.happygallery.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * 주문 이행 정보 — fulfillments 테이블.
 *
 * <p>MADE_TO_ORDER 승인 시 생성된다.
 * 관리자가 {@link #setExpectedShipDate(LocalDate)}로 예상 출고일을 갱신한다.
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

    protected Fulfillment() {}

    /**
     * 제작 이행 레코드 생성.
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

    /** 예상 출고일을 갱신한다. */
    public void setExpectedShipDate(LocalDate expectedShipDate) {
        this.expectedShipDate = expectedShipDate;
    }

    /** 주문 상태와 동기화한다. */
    public void syncStatus(OrderStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public FulfillmentType getType() { return type; }
    public OrderStatus getStatus() { return status; }
    public LocalDate getExpectedShipDate() { return expectedShipDate; }
}
