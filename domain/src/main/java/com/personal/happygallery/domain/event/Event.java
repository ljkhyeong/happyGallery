package com.personal.happygallery.domain.event;

import com.personal.happygallery.domain.content.ContentTextPolicy;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.media.ImageReferencePolicy;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "events")
public class Event {

    public static final int MAX_TITLE_LENGTH = ContentTextPolicy.MAX_TITLE_LENGTH;
    public static final int MAX_SUMMARY_LENGTH = 500;
    public static final int MAX_CONTENT_LENGTH = ContentTextPolicy.MAX_BODY_LENGTH;
    public static final int MAX_IMAGE_URL_LENGTH = ImageReferencePolicy.MAX_LENGTH;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(nullable = false, length = MAX_SUMMARY_LENGTH)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", length = MAX_IMAGE_URL_LENGTH)
    private String imageUrl;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private boolean featured;

    @Column(name = "coupon_definition_id")
    private Long couponDefinitionId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "event_products", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "product_id", nullable = false)
    @OrderBy
    private Set<Long> relatedProductIds = new LinkedHashSet<>();

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Event() {}

    public Event(
            String title,
            String summary,
            String content,
            String imageUrl,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean published,
            boolean featured,
            Long couponDefinitionId,
            Set<Long> relatedProductIds
    ) {
        apply(
                title,
                summary,
                content,
                imageUrl,
                startAt,
                endAt,
                published,
                featured,
                couponDefinitionId,
                relatedProductIds);
    }

    public void update(
            String title,
            String summary,
            String content,
            String imageUrl,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean published,
            boolean featured,
            Long couponDefinitionId,
            Set<Long> relatedProductIds
    ) {
        apply(
                title,
                summary,
                content,
                imageUrl,
                startAt,
                endAt,
                published,
                featured,
                couponDefinitionId,
                relatedProductIds);
    }

    private void apply(
            String title,
            String summary,
            String content,
            String imageUrl,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean published,
            boolean featured,
            Long couponDefinitionId,
            Set<Long> relatedProductIds
    ) {
        this.title = ContentTextPolicy.requireTitle(title, "이벤트 제목");
        this.summary = requireSummary(summary);
        this.content = ContentTextPolicy.requireBody(content, "이벤트 내용");
        this.imageUrl = ImageReferencePolicy.optional(imageUrl);
        requirePeriod(startAt, endAt);
        this.startAt = startAt;
        this.endAt = endAt;
        this.published = published;
        this.featured = featured;
        this.couponDefinitionId = optionalCouponDefinitionId(couponDefinitionId);
        replaceRelatedProductIds(relatedProductIds);
    }

    private static String requireSummary(String value) {
        if (value == null || value.isBlank()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이벤트 요약은 필수입니다.");
        }
        String normalized = value.strip();
        if (normalized.length() > MAX_SUMMARY_LENGTH) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "이벤트 요약은 " + MAX_SUMMARY_LENGTH + "자 이하여야 합니다.");
        }
        return normalized;
    }

    private static void requirePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이벤트 시작과 종료 시각은 필수입니다.");
        }
        if (!startAt.isBefore(endAt)) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "이벤트 시작 시각은 종료 시각보다 빨라야 합니다.");
        }
    }

    private void replaceRelatedProductIds(Set<Long> productIds) {
        LinkedHashSet<Long> normalizedIds = new LinkedHashSet<>();
        if (productIds != null) {
            productIds.forEach(Event::requireProductId);
            productIds.stream()
                    .sorted()
                    .forEach(normalizedIds::add);
        }
        relatedProductIds.clear();
        relatedProductIds.addAll(normalizedIds);
    }

    private static void requireProductId(Long productId) {
        if (productId == null || productId < 1L) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "연관 상품 ID는 1 이상이어야 합니다.");
        }
    }

    private static Long optionalCouponDefinitionId(Long couponDefinitionId) {
        if (couponDefinitionId != null && couponDefinitionId < 1L) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "연결 쿠폰 ID는 1 이상이어야 합니다.");
        }
        return couponDefinitionId;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public boolean isPublished() { return published; }
    public boolean isFeatured() { return featured; }
    public Long getCouponDefinitionId() { return couponDefinitionId; }
    public Set<Long> getRelatedProductIds() {
        return Collections.unmodifiableSet(relatedProductIds);
    }
    public long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
