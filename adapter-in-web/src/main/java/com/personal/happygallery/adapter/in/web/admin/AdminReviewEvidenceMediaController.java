package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.config.OpenApiSecuritySchemes;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.media.port.in.ImageMediaUseCase.ImageContent;
import com.personal.happygallery.application.review.port.in.ReviewEvidenceMediaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/review-evidence")
public class AdminReviewEvidenceMediaController {

    private final ReviewEvidenceMediaUseCase evidenceMediaUseCase;

    public AdminReviewEvidenceMediaController(ReviewEvidenceMediaUseCase evidenceMediaUseCase) {
        this.evidenceMediaUseCase = evidenceMediaUseCase;
    }

    @GetMapping("/{evidenceId}/images/{sortOrder}")
    @Operation(
            operationId = "getAdminReviewEvidenceImage",
            security = @SecurityRequirement(name = OpenApiSecuritySchemes.ADMIN_BEARER),
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(
                                    mediaType = "image/jpeg",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(
                                    mediaType = "image/png",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(
                                    mediaType = "image/webp",
                                    schema = @Schema(type = "string", format = "binary"))
                    }))
    public ResponseEntity<byte[]> getImage(
            @PathVariable @Positive Long evidenceId,
            @Parameter(schema = @Schema(type = "integer", minimum = "0", maximum = "4"))
            @PathVariable @PositiveOrZero int sortOrder,
            @AuthenticationPrincipal AdminPrincipal admin) {
        admin.requireBearerAdminUserId();
        ImageContent image =
                evidenceMediaUseCase.getImage(evidenceId, sortOrder);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(image.bytes());
    }
}
