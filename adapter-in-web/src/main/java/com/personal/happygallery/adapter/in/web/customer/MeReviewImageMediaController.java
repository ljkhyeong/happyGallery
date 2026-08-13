package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.config.OpenApiSecuritySchemes;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.media.port.in.ImageMediaUseCase.ImageContent;
import com.personal.happygallery.application.review.port.in.ReviewImageMediaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Positive;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/reviews")
@Validated
@SecurityRequirement(name = OpenApiSecuritySchemes.CUSTOMER_SESSION)
public class MeReviewImageMediaController {

    private final ReviewImageMediaUseCase reviewImageMediaUseCase;

    public MeReviewImageMediaController(ReviewImageMediaUseCase reviewImageMediaUseCase) {
        this.reviewImageMediaUseCase = reviewImageMediaUseCase;
    }

    @GetMapping("/{reviewId}/images/{imageId}")
    @Operation(
            operationId = "getMyReviewImage",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = {
                            @Content(
                                    mediaType = "image/jpeg",
                                    schema = @Schema(type = "string", format = "binary")),
                            @Content(
                                    mediaType = "image/png",
                                    schema = @Schema(type = "string", format = "binary"))
                    }))
    public ResponseEntity<byte[]> getImage(
            @PathVariable @Positive Long reviewId,
            @PathVariable @Positive Long imageId,
            @AuthenticationPrincipal CustomerPrincipal customer
    ) {
        ImageContent image =
                reviewImageMediaUseCase.getOwnedImage(customer.userId(), reviewId, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(image.bytes());
    }
}
