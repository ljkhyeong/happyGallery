package com.personal.happygallery.domain.store;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "workshop_profiles")
public class WorkshopProfile {

    public static final long SINGLETON_ID = 1L;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_PHONE_LENGTH = 30;
    public static final int MAX_POSTAL_CODE_LENGTH = 20;
    public static final int MAX_ADDRESS_LENGTH = 200;
    public static final int MAX_BUSINESS_HOURS_LENGTH = 1_000;
    public static final int MAX_URL_LENGTH = 500;
    public static final int MAX_PARKING_INFO_LENGTH = 1_000;
    public static final int MAX_BUSINESS_REGISTRATION_NUMBER_LENGTH = 20;
    public static final int MAX_REPRESENTATIVE_NAME_LENGTH = 100;
    public static final int MAX_EMAIL_LENGTH = 254;
    public static final int MAX_MAIL_ORDER_REGISTRATION_NUMBER_LENGTH = 100;
    public static final int MAX_INTRODUCTION_LENGTH = 2_000;
    public static final int MAX_KAKAO_TALK_ID_LENGTH = 100;
    private static final String BUSINESS_REGISTRATION_NUMBER_PATTERN = "^\\d{3}-\\d{2}-\\d{5}$";

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

    @Column(name = "map_url", length = MAX_URL_LENGTH)
    private String mapUrl;

    @Column(name = "parking_info", length = MAX_PARKING_INFO_LENGTH)
    private String parkingInfo;

    @Column(name = "business_registration_number", length = MAX_BUSINESS_REGISTRATION_NUMBER_LENGTH)
    private String businessRegistrationNumber;

    @Column(name = "representative_name", length = MAX_REPRESENTATIVE_NAME_LENGTH)
    private String representativeName;

    @Column(length = MAX_EMAIL_LENGTH)
    private String email;

    @Column(name = "mail_order_registration_number", length = MAX_MAIL_ORDER_REGISTRATION_NUMBER_LENGTH)
    private String mailOrderRegistrationNumber;

    @Column(length = MAX_INTRODUCTION_LENGTH)
    private String introduction;

    @Column(name = "kakao_talk_id", length = MAX_KAKAO_TALK_ID_LENGTH)
    private String kakaoTalkId;

    @Column(name = "naver_talk_url", length = MAX_URL_LENGTH)
    private String naverTalkUrl;

    @Column(name = "naver_blog_url", length = MAX_URL_LENGTH)
    private String naverBlogUrl;

    @Column(name = "instagram_url", length = MAX_URL_LENGTH)
    private String instagramUrl;

    @Column(name = "smart_store_url", length = MAX_URL_LENGTH)
    private String smartStoreUrl;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected WorkshopProfile() {}

    public WorkshopProfile(String name) {
        this.id = SINGLETON_ID;
        this.name = required(name, "공방명", MAX_NAME_LENGTH);
    }

    public void update(String name, String phone, String postalCode,
                       String addressLine1, String addressLine2, String businessHours,
                       String mapUrl, String parkingInfo, String businessRegistrationNumber,
                       String representativeName, String email, String mailOrderRegistrationNumber,
                       String introduction, String kakaoTalkId,
                       String naverTalkUrl, String naverBlogUrl,
                       String instagramUrl, String smartStoreUrl,
                       LocalDateTime updatedAt) {
        this.name = required(name, "공방명", MAX_NAME_LENGTH);
        this.phone = optional(phone, "연락처", MAX_PHONE_LENGTH);
        this.postalCode = optional(postalCode, "우편번호", MAX_POSTAL_CODE_LENGTH);
        this.addressLine1 = optional(addressLine1, "기본 주소", MAX_ADDRESS_LENGTH);
        this.addressLine2 = optional(addressLine2, "상세 주소", MAX_ADDRESS_LENGTH);
        this.businessHours = optional(businessHours, "운영시간", MAX_BUSINESS_HOURS_LENGTH);
        this.mapUrl = optionalHttpUrl(mapUrl, "지도 URL");
        this.parkingInfo = optional(parkingInfo, "주차 안내", MAX_PARKING_INFO_LENGTH);
        this.businessRegistrationNumber = optionalBusinessRegistrationNumber(businessRegistrationNumber);
        this.representativeName = optional(
                representativeName, "대표자명", MAX_REPRESENTATIVE_NAME_LENGTH);
        this.email = optionalEmail(email);
        this.mailOrderRegistrationNumber = optional(
                mailOrderRegistrationNumber,
                "통신판매업 신고번호",
                MAX_MAIL_ORDER_REGISTRATION_NUMBER_LENGTH);
        this.introduction = optional(introduction, "공방 소개", MAX_INTRODUCTION_LENGTH);
        this.kakaoTalkId = optional(kakaoTalkId, "카카오톡 ID", MAX_KAKAO_TALK_ID_LENGTH);
        this.naverTalkUrl = optionalHttpUrl(naverTalkUrl, "네이버톡톡 URL");
        this.naverBlogUrl = optionalHttpUrl(naverBlogUrl, "네이버 블로그 URL");
        this.instagramUrl = optionalHttpUrl(instagramUrl, "인스타그램 URL");
        this.smartStoreUrl = optionalHttpUrl(smartStoreUrl, "스마트스토어 URL");
        this.updatedAt = updatedAt;
    }

    private static String optionalBusinessRegistrationNumber(String value) {
        String normalized = optional(
                value, "사업자등록번호", MAX_BUSINESS_REGISTRATION_NUMBER_LENGTH);
        if (normalized == null || normalized.matches(BUSINESS_REGISTRATION_NUMBER_PATTERN)) {
            return normalized;
        }
        throw new HappyGalleryException(
                ErrorCode.INVALID_INPUT, "사업자등록번호는 000-00-00000 형식이어야 합니다.");
    }

    private static String optionalEmail(String value) {
        String normalized = optional(value, "전자우편주소", MAX_EMAIL_LENGTH);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
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

    private static String optionalHttpUrl(String value, String fieldName) {
        String normalized = optional(value, fieldName, MAX_URL_LENGTH);
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
        throw new HappyGalleryException(
                ErrorCode.INVALID_INPUT, fieldName + "은 http(s) 주소여야 합니다.");
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
    public String getBusinessRegistrationNumber() { return businessRegistrationNumber; }
    public String getRepresentativeName() { return representativeName; }
    public String getEmail() { return email; }
    public String getMailOrderRegistrationNumber() { return mailOrderRegistrationNumber; }
    public String getIntroduction() { return introduction; }
    public String getKakaoTalkId() { return kakaoTalkId; }
    public String getNaverTalkUrl() { return naverTalkUrl; }
    public String getNaverBlogUrl() { return naverBlogUrl; }
    public String getInstagramUrl() { return instagramUrl; }
    public String getSmartStoreUrl() { return smartStoreUrl; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public boolean hasCompleteOnlineSalesDisclosure() {
        return phone != null
                && addressLine1 != null
                && businessRegistrationNumber != null
                && representativeName != null
                && email != null
                && mailOrderRegistrationNumber != null;
    }
}
