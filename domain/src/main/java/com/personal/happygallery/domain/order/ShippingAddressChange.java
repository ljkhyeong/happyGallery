package com.personal.happygallery.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 배송지 변경 전후의 암호문과 변경한 고객을 보관한다. */
@Entity
@Table(name = "shipping_address_changes")
public class ShippingAddressChange {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "guest_id")
    private Long guestId;
    @Column(name = "before_address_enc", nullable = false, length = 4096)
    private String beforeAddressEnc;
    @Column(name = "after_address_enc", nullable = false, length = 4096)
    private String afterAddressEnc;
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    protected ShippingAddressChange() {}

    public ShippingAddressChange(Order order, String beforeAddressEnc, String afterAddressEnc,
                                 LocalDateTime changedAt) {
        this.orderId = order.getId();
        this.userId = order.getUserId();
        this.guestId = order.getGuestId();
        this.beforeAddressEnc = beforeAddressEnc;
        this.afterAddressEnc = afterAddressEnc;
        this.changedAt = changedAt;
    }
}
