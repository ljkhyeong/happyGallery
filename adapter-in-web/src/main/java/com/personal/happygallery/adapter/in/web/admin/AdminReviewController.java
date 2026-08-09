package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.review.dto.AdminReviewPageResponse;
import com.personal.happygallery.adapter.in.web.review.dto.AdminReviewReportPageResponse;
import com.personal.happygallery.adapter.in.web.review.dto.AdminReviewReportResponse;
import com.personal.happygallery.adapter.in.web.review.dto.AdminReviewResponse;
import com.personal.happygallery.adapter.in.web.review.dto.DecideReviewReportRequest;
import com.personal.happygallery.adapter.in.web.review.dto.ReviewModerationActionResponse;
import com.personal.happygallery.adapter.in.web.review.dto.UpdateReviewStatusRequest;
import com.personal.happygallery.adapter.in.web.review.dto.UpsertReviewReplyRequest;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.review.port.in.ReviewUseCase;
import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminReviewController {

    private final ReviewUseCase reviewUseCase;

    public AdminReviewController(ReviewUseCase reviewUseCase) {
        this.reviewUseCase = reviewUseCase;
    }

    @GetMapping("/reviews")
    @Operation(operationId = "listAdminReviews")
    public AdminReviewPageResponse list(
            @RequestParam(required = false) ReviewTargetType targetType,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) String cursor,
            @Parameter(schema = @Schema(
                    type = "integer", format = "int32", defaultValue = "20",
                    minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size) {
        return AdminReviewPageResponse.from(
                reviewUseCase.listAdminReviews(targetType, status, cursor, size));
    }

    @PatchMapping("/reviews/{reviewId}/status")
    @Operation(operationId = "updateAdminReviewStatus")
    public AdminReviewResponse updateStatus(
            @PathVariable Long reviewId,
            @RequestBody @Valid UpdateReviewStatusRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return AdminReviewResponse.from(reviewUseCase.updateStatus(
                reviewId,
                request.status(),
                request.reason(),
                admin.requireBearerAdminUserId()));
    }

    @GetMapping("/reviews/{reviewId}/moderation-actions")
    @Operation(operationId = "listReviewModerationActions")
    public List<ReviewModerationActionResponse> listModerationActions(
            @PathVariable Long reviewId) {
        return reviewUseCase.listModerationActions(reviewId).stream()
                .map(ReviewModerationActionResponse::from)
                .toList();
    }

    @PutMapping("/reviews/{reviewId}/reply")
    @Operation(operationId = "upsertOfficialReviewReply")
    public AdminReviewResponse upsertReply(
            @PathVariable Long reviewId,
            @RequestBody @Valid UpsertReviewReplyRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return AdminReviewResponse.from(reviewUseCase.upsertOfficialReply(
                reviewId, request.content(), admin.requireBearerAdminUserId()));
    }

    @DeleteMapping("/reviews/{reviewId}/reply")
    @Operation(operationId = "deleteOfficialReviewReply")
    public AdminReviewResponse deleteReply(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return AdminReviewResponse.from(reviewUseCase.deleteOfficialReply(
                reviewId, admin.requireBearerAdminUserId()));
    }

    @GetMapping("/review-reports")
    @Operation(operationId = "listAdminReviewReports")
    public AdminReviewReportPageResponse listReports(
            @RequestParam(required = false) ReviewReportStatus status,
            @RequestParam(required = false) String cursor,
            @Parameter(schema = @Schema(
                    type = "integer", format = "int32", defaultValue = "20",
                    minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size) {
        return AdminReviewReportPageResponse.from(
                reviewUseCase.listAdminReports(status, cursor, size));
    }

    @PatchMapping("/review-reports/{reportId}")
    @Operation(operationId = "decideAdminReviewReport")
    public AdminReviewReportResponse decideReport(
            @PathVariable Long reportId,
            @RequestBody @Valid DecideReviewReportRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return AdminReviewReportResponse.from(reviewUseCase.decideReport(
                reportId,
                request.decision().toStatus(),
                request.note(),
                admin.requireBearerAdminUserId()));
    }
}
