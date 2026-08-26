package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.review.dto.CreateClassReviewRequest;
import com.personal.happygallery.adapter.in.web.review.dto.CreateProductReviewRequest;
import com.personal.happygallery.adapter.in.web.review.dto.CreateReviewReportRequest;
import com.personal.happygallery.adapter.in.web.review.dto.MemberReviewReportResponse;
import com.personal.happygallery.adapter.in.web.review.dto.MemberReviewPageResponse;
import com.personal.happygallery.adapter.in.web.review.dto.MemberReviewResponse;
import com.personal.happygallery.adapter.in.web.review.dto.ReviewHelpfulResponse;
import com.personal.happygallery.adapter.in.web.review.dto.ReviewImageResponse;
import com.personal.happygallery.adapter.in.web.review.dto.ReviewCreationStateResponse;
import com.personal.happygallery.adapter.in.web.review.dto.ReviewOpportunityPageResponse;
import com.personal.happygallery.adapter.in.web.review.dto.ReviewReactionResponse;
import com.personal.happygallery.adapter.in.web.review.dto.UpdateReviewRequest;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.adapter.in.web.config.OpenApiSecuritySchemes;
import com.personal.happygallery.adapter.in.web.error.ErrorResponse;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.review.port.in.ReviewUseCase;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/me/reviews")
@SecurityRequirement(name = OpenApiSecuritySchemes.CUSTOMER_SESSION)
public class MeReviewController {

    private final ReviewUseCase reviewUseCase;
    private final SubjectRateLimitGuard rateLimitGuard;

    public MeReviewController(
            ReviewUseCase reviewUseCase,
            SubjectRateLimitGuard rateLimitGuard
    ) {
        this.reviewUseCase = reviewUseCase;
        this.rateLimitGuard = rateLimitGuard;
    }

    @PostMapping("/products")
    @Operation(operationId = "createProductReview")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberReviewResponse createProduct(
            @RequestBody @Valid CreateProductReviewRequest request,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        rateLimitGuard.checkReviewMutation(customer.userId());
        return MemberReviewResponse.from(reviewUseCase.createProductReview(
                customer.userId(), request.orderItemId(), request.rating(), request.content()));
    }

    @PostMapping("/classes")
    @Operation(operationId = "createClassReview")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberReviewResponse createClass(
            @RequestBody @Valid CreateClassReviewRequest request,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        rateLimitGuard.checkReviewMutation(customer.userId());
        return MemberReviewResponse.from(reviewUseCase.createClassReview(
                customer.userId(), request.bookingId(), request.rating(), request.content()));
    }

    @GetMapping("/products/{orderItemId}/creation-state")
    @Operation(operationId = "getProductReviewCreationState")
    public ReviewCreationStateResponse getProductCreationState(
            @PathVariable @Positive Long orderItemId,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        return ReviewCreationStateResponse.from(
                reviewUseCase.getProductReviewCreationState(customer.userId(), orderItemId));
    }

    @GetMapping("/classes/{bookingId}/creation-state")
    @Operation(operationId = "getClassReviewCreationState")
    public ReviewCreationStateResponse getClassCreationState(
            @PathVariable @Positive Long bookingId,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        return ReviewCreationStateResponse.from(
                reviewUseCase.getClassReviewCreationState(customer.userId(), bookingId));
    }

    @GetMapping
    @Operation(operationId = "listMyReviews")
    public MemberReviewPageResponse list(
            @AuthenticationPrincipal CustomerPrincipal customer,
            @RequestParam(required = false) String cursor,
            @Parameter(schema = @Schema(
                    type = "integer", format = "int32", defaultValue = "20",
                    minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size) {
        return MemberReviewPageResponse.from(
                reviewUseCase.listMyReviews(customer.userId(), cursor, size));
    }

    @GetMapping("/opportunities")
    @Operation(operationId = "listMyReviewOpportunities")
    public ReviewOpportunityPageResponse listOpportunities(
            @AuthenticationPrincipal CustomerPrincipal customer,
            @RequestParam(required = false) String cursor,
            @Parameter(schema = @Schema(
                    type = "integer", format = "int32", defaultValue = "20",
                    minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size) {
        return ReviewOpportunityPageResponse.from(
                reviewUseCase.listMyReviewOpportunities(customer.userId(), cursor, size));
    }

    @GetMapping("/reactions")
    @Operation(operationId = "listMyReviewReactions")
    public List<ReviewReactionResponse> listReactions(
            @AuthenticationPrincipal CustomerPrincipal customer,
            @Parameter(
                    required = true,
                    array = @ArraySchema(
                            minItems = 1,
                            maxItems = 100,
                            schema = @Schema(
                                    type = "integer",
                                    format = "int64",
                                    minimum = "1")))
            @RequestParam
            @Size(min = 1, max = 100)
            List<@Positive Long> reviewIds) {
        return reviewUseCase.listMyReviewReactions(customer.userId(), reviewIds).stream()
                .map(ReviewReactionResponse::from)
                .toList();
    }

    @GetMapping("/orders/{orderId}")
    @Operation(operationId = "listMyOrderReviews")
    public List<MemberReviewResponse> listByOrder(
            @PathVariable @Positive Long orderId,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        return reviewUseCase.listMyOrderReviews(customer.userId(), orderId).stream()
                .map(MemberReviewResponse::from)
                .toList();
    }

    @GetMapping("/bookings/{bookingId}")
    @Operation(operationId = "listMyBookingReviews")
    public List<MemberReviewResponse> listByBooking(
            @PathVariable @Positive Long bookingId,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        return reviewUseCase.listMyBookingReviews(customer.userId(), bookingId).stream()
                .map(MemberReviewResponse::from)
                .toList();
    }

    @PatchMapping("/{reviewId}")
    @Operation(operationId = "updateMyReview")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정된 후기",
                    content = @Content(schema = @Schema(implementation = MemberReviewResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "후기 콘텐츠 revision 충돌",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public MemberReviewResponse update(
            @PathVariable @Positive Long reviewId,
            @RequestBody @Valid UpdateReviewRequest request,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        rateLimitGuard.checkReviewMutation(customer.userId());
        return MemberReviewResponse.from(reviewUseCase.updateReview(
                customer.userId(),
                reviewId,
                request.expectedContentRevision(),
                request.rating(),
                request.content()));
    }

    @PutMapping("/{reviewId}/helpful")
    @Operation(operationId = "markReviewHelpful")
    public ReviewHelpfulResponse markHelpful(
            @PathVariable @Positive Long reviewId,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        rateLimitGuard.checkReviewHelpful(customer.userId());
        return ReviewHelpfulResponse.from(
                reviewUseCase.markHelpful(customer.userId(), reviewId));
    }

    @DeleteMapping("/{reviewId}/helpful")
    @Operation(operationId = "unmarkReviewHelpful")
    public ReviewHelpfulResponse unmarkHelpful(
            @PathVariable @Positive Long reviewId,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        rateLimitGuard.checkReviewHelpful(customer.userId());
        return ReviewHelpfulResponse.from(
                reviewUseCase.unmarkHelpful(customer.userId(), reviewId));
    }

    @PostMapping("/{reviewId}/reports")
    @Operation(operationId = "reportReview")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberReviewReportResponse report(
            @PathVariable @Positive Long reviewId,
            @RequestBody @Valid CreateReviewReportRequest request,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        rateLimitGuard.checkReviewReport(customer.userId());
        return MemberReviewReportResponse.from(reviewUseCase.createReport(
                customer.userId(), reviewId, request.reason(), request.detail()));
    }

    @PostMapping(
            path = "/{reviewId}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "addMyReviewImage",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true))
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewImageResponse addImage(
            @PathVariable @Positive Long reviewId,
            @Parameter(
                    required = true,
                    description = "JPEG 또는 PNG 후기 이미지",
                    schema = @Schema(type = "string", format = "binary"))
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        rateLimitGuard.checkReviewImageUpload(customer.userId());
        try {
            return ReviewImageResponse.from(reviewUseCase.addReviewImage(
                    customer.userId(), reviewId, file.getBytes(), file.getContentType()));
        } catch (IOException exception) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이미지 파일을 읽을 수 없습니다.");
        }
    }

    @DeleteMapping("/{reviewId}/images/{imageId}")
    @Operation(operationId = "deleteMyReviewImage")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(
            @PathVariable @Positive Long reviewId,
            @PathVariable @Positive Long imageId,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        reviewUseCase.deleteReviewImage(customer.userId(), reviewId, imageId);
    }

    @DeleteMapping("/{reviewId}")
    @Operation(operationId = "deleteMyReview")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable @Positive Long reviewId,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        rateLimitGuard.checkReviewMutation(customer.userId());
        reviewUseCase.deleteReview(customer.userId(), reviewId);
    }
}
