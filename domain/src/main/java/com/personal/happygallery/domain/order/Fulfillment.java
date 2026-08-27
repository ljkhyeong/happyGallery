package com.personal.happygallery.domain.order;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
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
import java.util.Objects;

/**
 * 주문 이행 정보 — fulfillments 테이블.
 *
 * <p>결제 확정으로 주문을 생성할 때 고객이 선택한 SHIPPING 또는 PICKUP 타입으로 함께 생성된다.
 * 관리자가 {@link #setExpectedShipDate(LocalDate)}로 예상 출고일을,
 * {@link #setPickupDeadline(LocalDateTime)}으로 픽업 마감 시각을 관리한다.
 *
 * <p>주문 상태는 {@link Order#getStatus()}가 단일 소스이다.
 */
@Entity
@Table(name = "fulfillments")
public class Fulfillment {

    public static final int MAX_CARRIER_LENGTH = 50;
    public static final int MAX_TRACKING_NUMBER_LENGTH = 100;

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

    @Column(name = "shipping_address_enc", length = 4096)
    private String shippingAddressEnc;

    @Column(length = MAX_CARRIER_LENGTH)
    private String carrier;

    @Column(name = "tracking_number", length = MAX_TRACKING_NUMBER_LENGTH)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "carrier_code", length = 30)
    private ShippingCarrier carrierCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_registration_status", length = 20)
    private TrackingRegistrationStatus trackingRegistrationStatus;

    @Column(name = "tracking_request_id", length = 100)
    private String trackingRequestId;

    @Column(name = "tracking_registration_attempts", nullable = false)
    private int trackingRegistrationAttempts;

    @Column(name = "tracking_next_attempt_at")
    private LocalDateTime trackingNextAttemptAt;

    @Column(name = "tracking_registration_started_at")
    private LocalDateTime trackingRegistrationStartedAt;

    @Column(name = "tracking_last_error", length = 500)
    private String trackingLastError;

    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_status", length = 30)
    private ShipmentTrackingStatus trackingStatus;

    @Column(name = "tracking_status_text", length = 100)
    private String trackingStatusText;

    @Column(name = "tracking_updated_at")
    private LocalDateTime trackingUpdatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Fulfillment() {}

    private Fulfillment(Long orderId) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
    }

    /** 배송 이행 레코드를 생성한다. */
    public static Fulfillment shipping(Long orderId, String shippingAddressEnc) {
        Fulfillment fulfillment = new Fulfillment(orderId);
        fulfillment.type = FulfillmentType.SHIPPING;
        fulfillment.shippingAddressEnc = Objects.requireNonNull(
                shippingAddressEnc, "shippingAddressEnc must not be null");
        return fulfillment;
    }

    /** 픽업 이행 레코드를 생성한다. 픽업 마감은 준비 완료 시점에 설정한다. */
    public static Fulfillment pickup(Long orderId) {
        Fulfillment fulfillment = new Fulfillment(orderId);
        fulfillment.type = FulfillmentType.PICKUP;
        return fulfillment;
    }

    /** 예상 출고일을 갱신한다. */
    public void setExpectedShipDate(LocalDate expectedShipDate) {
        requireShippingType();
        this.expectedShipDate = expectedShipDate;
    }

    public void requireShippingType() {
        if (this.type != FulfillmentType.SHIPPING) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "배송 이행에서만 출고일을 설정할 수 있습니다.");
        }
    }

    /** 배송 출발 시 고객에게 노출할 택배사와 운송장 번호를 함께 기록한다. */
    public void recordShipment(String carrier, String trackingNumber) {
        requireShippingType();
        this.carrier = requireTrackingText(carrier, "택배사", MAX_CARRIER_LENGTH);
        this.trackingNumber = requireTrackingText(
                trackingNumber, "운송장 번호", MAX_TRACKING_NUMBER_LENGTH);
        ShippingCarrier.fromDisplayName(this.carrier).ifPresent(this::setTrackingRegistrationPending);
    }

    /** 지원 택배사의 운송장을 기록하고 배송조회 등록 대기 상태로 전환한다. */
    public void recordShipment(ShippingCarrier carrier, String trackingNumber, LocalDateTime now) {
        requireShippingType();
        this.carrier = Objects.requireNonNull(carrier, "carrier must not be null").displayName();
        this.trackingNumber = requireTrackingText(
                trackingNumber, "운송장 번호", MAX_TRACKING_NUMBER_LENGTH);
        setTrackingRegistrationPending(carrier);
        this.trackingNextAttemptAt = Objects.requireNonNull(now, "now must not be null");
    }

    private void setTrackingRegistrationPending(ShippingCarrier carrier) {
        this.carrierCode = carrier;
        this.trackingRegistrationStatus = TrackingRegistrationStatus.PENDING;
        this.trackingRegistrationAttempts = 0;
        this.trackingRequestId = null;
        this.trackingRegistrationStartedAt = null;
        this.trackingNextAttemptAt = null;
        this.trackingLastError = null;
        this.trackingStatus = ShipmentTrackingStatus.PENDING;
        this.trackingStatusText = "배송조회 등록 대기";
    }

    public boolean claimTrackingRegistration(LocalDateTime now, LocalDateTime processingStaleBefore) {
        boolean pending = trackingRegistrationStatus == TrackingRegistrationStatus.PENDING
                && (trackingNextAttemptAt == null || !trackingNextAttemptAt.isAfter(now));
        boolean stale = trackingRegistrationStatus == TrackingRegistrationStatus.PROCESSING
                && trackingRegistrationStartedAt != null
                && !trackingRegistrationStartedAt.isAfter(processingStaleBefore);
        if (!pending && !stale) {
            return false;
        }
        trackingRegistrationStatus = TrackingRegistrationStatus.PROCESSING;
        trackingRegistrationStartedAt = now;
        trackingRegistrationAttempts++;
        return true;
    }

    public void completeTrackingRegistration(String requestId, LocalDateTime now) {
        trackingRegistrationStatus = trackingStatus == ShipmentTrackingStatus.DELIVERED
                ? TrackingRegistrationStatus.COMPLETED
                : TrackingRegistrationStatus.ACTIVE;
        trackingRequestId = requestId;
        trackingRegistrationStartedAt = null;
        trackingNextAttemptAt = null;
        trackingLastError = null;
        if (trackingStatus == null || trackingStatus == ShipmentTrackingStatus.PENDING) {
            trackingStatus = ShipmentTrackingStatus.REGISTERED;
            trackingStatusText = "배송조회 등록 완료";
            trackingUpdatedAt = now;
        }
    }

    public void failTrackingRegistration(String reason, LocalDateTime retryAt, int maxAttempts) {
        trackingRegistrationStartedAt = null;
        trackingLastError = abbreviate(reason, 500);
        if (trackingRegistrationAttempts >= maxAttempts || retryAt == null) {
            trackingRegistrationStatus = TrackingRegistrationStatus.FAILED;
            trackingNextAttemptAt = null;
            return;
        }
        trackingRegistrationStatus = TrackingRegistrationStatus.PENDING;
        trackingNextAttemptAt = retryAt;
    }

    public void applyTrackingUpdate(ShipmentTrackingStatus status,
                                    String statusText,
                                    LocalDateTime updatedAt) {
        trackingStatus = Objects.requireNonNull(status, "status must not be null");
        trackingStatusText = abbreviate(statusText, 100);
        trackingUpdatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        trackingRegistrationStatus = status == ShipmentTrackingStatus.DELIVERED
                ? TrackingRegistrationStatus.COMPLETED
                : TrackingRegistrationStatus.ACTIVE;
        trackingRegistrationStartedAt = null;
        trackingNextAttemptAt = null;
        trackingLastError = null;
    }

    public boolean matchesTracking(ShippingCarrier carrier, String trackingNumber) {
        return carrierCode == carrier && Objects.equals(this.trackingNumber, trackingNumber);
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public void setPickupDeadline(LocalDateTime pickupDeadlineAt) {
        requirePickupType();
        this.pickupDeadlineAt = requirePickupDeadlineAt(pickupDeadlineAt);
    }

    public void requirePickupType() {
        if (this.type != FulfillmentType.PICKUP) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "픽업 주문에서만 픽업 처리를 할 수 있습니다.");
        }
    }

    private static LocalDateTime requirePickupDeadlineAt(LocalDateTime pickupDeadlineAt) {
        if (pickupDeadlineAt == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "픽업 마감 시각은 필수입니다.");
        }
        return pickupDeadlineAt;
    }

    private static String requireTrackingText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, fieldName + "는 필수입니다.");
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, fieldName + "는 " + maxLength + "자 이하여야 합니다.");
        }
        return normalized;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public FulfillmentType getType() { return type; }
    public LocalDate getExpectedShipDate() { return expectedShipDate; }
    public LocalDateTime getPickupDeadlineAt() { return pickupDeadlineAt; }
    public String getShippingAddressEnc() { return shippingAddressEnc; }
    public String getCarrier() { return carrier; }
    public String getTrackingNumber() { return trackingNumber; }
    public ShippingCarrier getCarrierCode() { return carrierCode; }
    public TrackingRegistrationStatus getTrackingRegistrationStatus() { return trackingRegistrationStatus; }
    public String getTrackingRequestId() { return trackingRequestId; }
    public int getTrackingRegistrationAttempts() { return trackingRegistrationAttempts; }
    public LocalDateTime getTrackingNextAttemptAt() { return trackingNextAttemptAt; }
    public LocalDateTime getTrackingRegistrationStartedAt() { return trackingRegistrationStartedAt; }
    public String getTrackingLastError() { return trackingLastError; }
    public ShipmentTrackingStatus getTrackingStatus() { return trackingStatus; }
    public String getTrackingStatusText() { return trackingStatusText; }
    public LocalDateTime getTrackingUpdatedAt() { return trackingUpdatedAt; }
    public long getVersion() { return version; }
}
