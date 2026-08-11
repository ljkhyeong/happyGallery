package com.personal.happygallery.adapter.in.web.product;

import com.personal.happygallery.adapter.in.web.review.dto.PublicReviewPageResponse;
import com.personal.happygallery.application.review.port.in.ReviewUseCase;
import com.personal.happygallery.domain.review.ReviewSort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
@Validated
public class ProductReviewController {

    private final ReviewUseCase reviewUseCase;

    public ProductReviewController(ReviewUseCase reviewUseCase) {
        this.reviewUseCase = reviewUseCase;
    }

    @GetMapping
    @Operation(operationId = "listProductReviews")
    public PublicReviewPageResponse list(
            @PathVariable @Positive Long productId,
            @Parameter(schema = @Schema(type = "integer", format = "int32", minimum = "1", maximum = "5"))
            @RequestParam(required = false) @Min(1) @Max(5) Integer rating,
            @RequestParam(defaultValue = "LATEST") ReviewSort sort,
            @RequestParam(required = false) String cursor,
            @Parameter(schema = @Schema(
                    type = "integer", format = "int32", defaultValue = "20",
                    minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size) {
        return PublicReviewPageResponse.from(
                reviewUseCase.listProductReviews(productId, rating, sort, cursor, size));
    }
}
