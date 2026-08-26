package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.review.dto.AdminReviewPageResponse;
import com.personal.happygallery.adapter.in.web.review.dto.AdminReviewReportPageResponse;
import com.personal.happygallery.adapter.in.web.review.dto.AdminReviewReportResponse;
import com.personal.happygallery.adapter.in.web.review.dto.AdminReviewResponse;
import com.personal.happygallery.adapter.in.web.review.dto.DecideReviewReportRequest;
import com.personal.happygallery.adapter.in.web.review.dto.ReviewModerationActionResponse;
import com.personal.happygallery.adapter.in.web.review.dto.UpdateReviewStatusRequest;
import com.personal.happygallery.adapter.in.web.review.dto.UpsertReviewReplyRequest;
import com.personal.happygallery.adapter.in.web.config.OpenApiSecuritySchemes;
import com.personal.happygallery.adapter.in.web.error.ErrorResponse;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.review.port.in.ReviewUseCase;
import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
    @Operation(
            operationId = "listAdminReviews",
            security = {
                    @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_BEARER),
                    @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_API_KEY)
            })
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

    @GetMapping("/reviews/{reviewId}")
    @Operation(
            operationId = "getAdminReview",
            security = {
                    @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_BEARER),
                    @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_API_KEY)
            })
    public AdminReviewResponse get(@PathVariable @Positive Long reviewId) {
        return AdminReviewResponse.from(reviewUseCase.getAdminReview(reviewId));
    }

    @PatchMapping("/reviews/{reviewId}/status")
    @Operation(
            operationId = "updateAdminReviewStatus",
            security = @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_BEARER))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "변경된 후기",
                    content = @Content(schema = @Schema(implementation = AdminReviewResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "후기 콘텐츠 revision 또는 운영 version 충돌",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AdminReviewResponse updateStatus(
            @PathVariable @Positive Long reviewId,
            @RequestBody @Valid UpdateReviewStatusRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return AdminReviewResponse.from(reviewUseCase.updateStatus(
                reviewId,
                request.status(),
                request.reason(),
                request.expectedContentRevision(),
                request.expectedVersion(),
                admin.requireBearerAdminUserId()));
    }

    @GetMapping("/reviews/{reviewId}/moderation-actions")
    @Operation(
            operationId = "listReviewModerationActions",
            security = @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_BEARER))
    public List<ReviewModerationActionResponse> listModerationActions(
            @PathVariable @Positive Long reviewId,
            @AuthenticationPrincipal AdminPrincipal admin) {
        admin.requireBearerAdminUserId();
        return reviewUseCase.listModerationActions(reviewId).stream()
                .map(ReviewModerationActionResponse::from)
                .toList();
    }

    @PutMapping("/reviews/{reviewId}/reply")
    @Operation(
            operationId = "upsertOfficialReviewReply",
            security = @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_BEARER))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "답글이 반영된 후기",
                    content = @Content(schema = @Schema(implementation = AdminReviewResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "후기 운영 version 충돌",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AdminReviewResponse upsertReply(
            @PathVariable @Positive Long reviewId,
            @RequestBody @Valid UpsertReviewReplyRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return AdminReviewResponse.from(reviewUseCase.upsertOfficialReply(
                reviewId,
                request.content(),
                request.expectedVersion(),
                admin.requireBearerAdminUserId()));
    }

    @DeleteMapping("/reviews/{reviewId}/reply")
    @Operation(
            operationId = "deleteOfficialReviewReply",
            security = @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_BEARER))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "답글이 삭제된 후기",
                    content = @Content(schema = @Schema(implementation = AdminReviewResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "후기 운영 version 충돌",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AdminReviewResponse deleteReply(
            @PathVariable @Positive Long reviewId,
            @RequestParam @PositiveOrZero long expectedVersion,
            @AuthenticationPrincipal AdminPrincipal admin) {
        admin.requireBearerAdminUserId();
        return AdminReviewResponse.from(reviewUseCase.deleteOfficialReply(
                reviewId, expectedVersion));
    }

    @GetMapping("/review-reports")
    @Operation(
            operationId = "listAdminReviewReports",
            security = {
                    @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_BEARER),
                    @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_API_KEY)
            })
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

    @GetMapping("/review-reports/{reportId}")
    @Operation(
            operationId = "getAdminReviewReport",
            security = @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_BEARER))
    public AdminReviewReportResponse getReport(
            @PathVariable @Positive Long reportId,
            @AuthenticationPrincipal AdminPrincipal admin) {
        admin.requireBearerAdminUserId();
        return AdminReviewReportResponse.from(reviewUseCase.getAdminReport(reportId));
    }

    @PatchMapping("/review-reports/{reportId}")
    @Operation(
            operationId = "decideAdminReviewReport",
            security = @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_BEARER))
    public AdminReviewReportResponse decideReport(
            @PathVariable @Positive Long reportId,
            @RequestBody @Valid DecideReviewReportRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return AdminReviewReportResponse.from(reviewUseCase.decideReport(
                reportId,
                request.decision().toStatus(),
                request.note(),
                admin.requireBearerAdminUserId()));
    }
}
