package com.personal.happygallery.domain.store;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.net.URI;
import java.time.LocalDateTime;

@Entity
@Table(name = "workshop_profiles")
public class WorkshopProfile {

    public static final long SINGLETON_ID = 1L;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_PHONE_LENGTH = 30;
    public static final int MAX_POSTAL_CODE_LENGTH = 20;
    public static final int MAX_ADDRESS_LENGTH = 200;
    public static final int MAX_BUSINESS_HOURS_LENGTH = 1_000;
    public static final int MAX_MAP_URL_LENGTH = 500;
    public static final int MAX_PARKING_INFO_LENGTH = 1_000;

    @Id
    private Long id;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(length = MAX_PHONE_LENGTH)
    private String phone;

    @Column(name = "postal_code", length = MAX_POSTAL_CODE_LENGTH)
    private String postalCode;

    @Column(name = "address_line1", length = MAX_ADDRESS_LENGTH)
    private String addressLine1;

    @Column(name = "address_line2", length = MAX_ADDRESS_LENGTH)
    private String addressLine2;

    @Column(name = "business_hours", length = MAX_BUSINESS_HOURS_LENGTH)
    private String businessHours;

    @Column(name = "map_url", length = MAX_MAP_URL_LENGTH)
    private String mapUrl;

    @Column(name = "parking_info", length = MAX_PARKING_INFO_LENGTH)
    private String parkingInfo;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;

    protected WorkshopProfile() {}

    public WorkshopProfile(String name) {
        this.id = SINGLETON_ID;
        this.name = required(name, "공방명", MAX_NAME_LENGTH);
    }

    public void update(String name, String phone, String postalCode,
                       String addressLine1, String addressLine2, String businessHours,
                       String mapUrl, String parkingInfo, LocalDateTime updatedAt) {
        this.name = required(name, "공방명", MAX_NAME_LENGTH);
        this.phone = optional(phone, "연락처", MAX_PHONE_LENGTH);
        this.postalCode = optional(postalCode, "우편번호", MAX_POSTAL_CODE_LENGTH);
        this.addressLine1 = optional(addressLine1, "기본 주소", MAX_ADDRESS_LENGTH);
        this.addressLine2 = optional(addressLine2, "상세 주소", MAX_ADDRESS_LENGTH);
        this.businessHours = optional(businessHours, "운영시간", MAX_BUSINESS_HOURS_LENGTH);
        this.mapUrl = optionalHttpUrl(mapUrl);
        this.parkingInfo = optional(parkingInfo, "주차 안내", MAX_PARKING_INFO_LENGTH);
        this.updatedAt = updatedAt;
    }

    private static String required(String value, String fieldName, int maxLength) {
        String normalized = optional(value, fieldName, maxLength);
        if (normalized == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, fieldName + "은 필수입니다.");
        }
        return normalized;
    }

    private static String optional(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.codePointCount(0, normalized.length()) > maxLength) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, fieldName + "은 " + maxLength + "자 이하여야 합니다.");
        }
        return normalized;
    }

    private static String optionalHttpUrl(String value) {
        String normalized = optional(value, "지도 URL", MAX_MAP_URL_LENGTH);
        if (normalized == null) {
            return null;
        }
        try {
            URI uri = URI.create(normalized);
            if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null) {
                return normalized;
            }
        } catch (IllegalArgumentException ignored) {
            // 아래의 일관된 도메인 오류로 변환한다.
        }
        throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "지도 URL은 http(s) 주소여야 합니다.");
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getPostalCode() { return postalCode; }
    public String getAddressLine1() { return addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public String getBusinessHours() { return businessHours; }
    public String getMapUrl() { return mapUrl; }
    public String getParkingInfo() { return parkingInfo; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
