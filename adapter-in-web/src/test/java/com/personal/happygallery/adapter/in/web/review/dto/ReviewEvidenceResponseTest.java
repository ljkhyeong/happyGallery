package com.personal.happygallery.adapter.in.web.review.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.personal.happygallery.application.review.port.in.ReviewUseCase;
import com.personal.happygallery.domain.review.ReviewEvidenceProvenance;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewEvidenceResponseTest {

    @Test
    @DisplayName("증거 응답은 원본 공개 URL 대신 Bearer 관리자 증거 이미지 경로를 순번대로 제공한다")
    void exposeAdminEvidenceImageUrls() {
        ReviewUseCase.ReviewEvidenceItem evidence = new ReviewUseCase.ReviewEvidenceItem(
                81L,
                3L,
                5,
                "증거 본문",
                null,
                ReviewEvidenceProvenance.LIVE,
                true,
                List.of(
                        "/api/v1/media/images/first.jpg",
                        "/api/v1/media/images/second.png"),
                LocalDateTime.of(2026, 8, 9, 0, 0));

        ReviewEvidenceResponse response = ReviewEvidenceResponse.from(evidence);

        assertThat(response.imageUrls()).containsExactly(
                "/api/v1/admin/review-evidence/81/images/0",
                "/api/v1/admin/review-evidence/81/images/1");
    }
}
