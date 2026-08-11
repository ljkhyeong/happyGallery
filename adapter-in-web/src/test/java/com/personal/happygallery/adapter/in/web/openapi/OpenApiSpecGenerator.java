package com.personal.happygallery.adapter.in.web.openapi;

import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import com.personal.happygallery.support.UseCaseIT;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springdoc.core.customizers.OpenApiCustomizer;
import tools.jackson.databind.ObjectMapper;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("openapi")
@UseCaseIT
@Import(OpenApiSpecGenerator.OpenApiConfiguration.class)
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.paths-to-match=/api/v1/**"
})
class OpenApiSpecGenerator {

    private static final Pattern UNSTABLE_NUMERIC_SUFFIX = Pattern.compile(".+_\\d+$");
    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "put", "post", "delete", "patch", "head", "options", "trace");
    private static final Set<String> CURSOR_PAGE_OPERATION_IDS = Set.of(
            "listProductQnaPage",
            "listMyProductQnaPage",
            "listMyInquiriesPage",
            "listMyOrdersPage",
            "listMyBookingsPage",
            "listMyPassesPage",
            "listAdminProductQnaPage",
            "listRecoveredGuestOrders",
            "listRecoveredGuestBookings",
            "listProductReviews",
            "listClassReviews",
            "listMyReviews",
            "listMyReviewOpportunities",
            "listAdminReviews",
            "listAdminReviewReports");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Controller와 DTO에서 OpenAPI 명세를 생성한다")
    void generateOpenApi() throws Exception {
        String openApi = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        Map<?, ?> document = objectMapper.readValue(openApi, Map.class);
        assertStableOperationIds(document);
        assertPaymentAndCartRequestContracts(document);
        assertReviewContracts(document);
        assertReviewSecurityContracts(document);
        assertCursorPageSizeContracts(document);

        String canonicalOpenApi = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(sortObjectKeys(objectMapper.readValue(openApi, Object.class))) + "\n";
        Path output = Path.of(System.getProperty("openapi.output"));
        Files.createDirectories(output.getParent());
        Files.writeString(output, canonicalOpenApi, StandardCharsets.UTF_8);
    }

    private void assertStableOperationIds(Map<?, ?> document) {
        Map<?, ?> paths = (Map<?, ?>) document.get("paths");
        Set<String> operationIds = new HashSet<>();
        for (Map.Entry<?, ?> pathEntry : paths.entrySet()) {
            Map<?, ?> operations = (Map<?, ?>) pathEntry.getValue();
            for (Map.Entry<?, ?> operationEntry : operations.entrySet()) {
                if (!HTTP_METHODS.contains(operationEntry.getKey())
                        || !(operationEntry.getValue() instanceof Map<?, ?> operation)) {
                    continue;
                }
                Object operationId = operation.get("operationId");
                if (!(operationId instanceof String id) || id.isBlank()) {
                    throw new IllegalStateException(
                            "OpenAPI operationId가 없습니다: "
                                    + pathEntry.getKey() + " " + operationEntry.getKey());
                }
                if (!operationIds.add(id)) {
                    throw new IllegalStateException("중복 OpenAPI operationId입니다: " + id);
                }
                if (UNSTABLE_NUMERIC_SUFFIX.matcher(id).matches()) {
                    throw new IllegalStateException("불안정한 OpenAPI operationId입니다: " + id);
                }
            }
        }
    }

    private void assertPaymentAndCartRequestContracts(Map<?, ?> document) {
        assertRequiredProperties(document, "ConfirmPaymentRequest", "orderId", "amount");
        assertRequiredProperties(document, "AddCartItemRequest", "productId", "qty");
        assertRequiredProperties(document, "UpdateCartItemRequest", "qty");
        assertRequiredProperties(document, "MergeCartItemRequest", "productId", "qty");
        assertRequiredProperties(document, "CartResponse", "items", "totalAmount", "cartVersion");

        assertRequiredProperties(
                document,
                "OrderPayload",
                "type",
                "items",
                "cartCheckout",
                "fulfillmentType",
                "madeToOrderConsent");
        assertRequiredProperties(
                document,
                "BookingPayload",
                "type",
                "slotId",
                "participantCount");
        assertRequiredProperties(document, "PassPayload", "type", "userId");
        assertRequiredProperties(document, "OrderItemRef", "productId", "qty");
        assertRequiredProperties(
                document,
                "ShippingAddress",
                "recipientName",
                "phone",
                "postalCode",
                "addressLine1");
        assertRequiredProperties(
                document,
                "OrderDetailResponse",
                "productAmount",
                "couponDiscountAmount",
                "rewardUsedAmount",
                "pgPaidAmount",
                "rewardEarnBase",
                "issuedCouponId");
        assertRequiredProperties(
                document,
                "RefundProgressResponse",
                "amount",
                "pgRefundAmount",
                "rewardRestoreAmount",
                "rewardRevokeAmount",
                "restoreCoupon",
                "status");
        assertRequiredProperties(
                document,
                "EventResponse",
                "id",
                "title",
                "startAt",
                "endAt",
                "published",
                "featured",
                "relatedProductIds",
                "version");
        assertRequiredProperties(
                document,
                "RewardWalletResponse",
                "availableBalance",
                "reservedBalance",
                "debtBalance",
                "history");
        assertPaymentPayloadDiscriminator(document);
        assertEnumProperty(document, "OrderPayload", "type", "ORDER");
        assertEnumProperty(document, "BookingPayload", "type", "BOOKING");
        assertEnumProperty(document, "BookingPayload", "paymentMethod", "CARD", "EASY_PAY");
        assertEnumProperty(document, "PassPayload", "type", "PASS");
        assertPatternProperty(
                document, "OrderPayload", "expectedCartVersion", "^[0-9a-f]{64}$");
        assertIntegerRangeProperty(
                document,
                "OrderPayload",
                "rewardAmount",
                0L,
                9_007_199_254_740_991L);
        assertMaximumProperty(
                document, "OrderPayload", "rewardAmount", PaymentAmountPolicy.MAX_AMOUNT);
        assertNullableReferenceProperty(
                document, "OrderPayload", "shippingAddress", "ShippingAddress");
        assertNullableReferenceProperty(
                document, "OrderPayload", "policyAcceptance", "PolicyAcceptanceRequest");
        assertNullableReferenceProperty(
                document, "BookingPayload", "policyAcceptance", "PolicyAcceptanceRequest");
    }

    private void assertReviewContracts(Map<?, ?> document) {
        assertRequiredProperties(
                document, "CreateProductReviewRequest", "orderItemId", "rating", "content");
        assertRequiredProperties(
                document, "CreateClassReviewRequest", "bookingId", "rating", "content");
        assertRequiredProperties(
                document, "UpdateReviewRequest", "expectedContentRevision", "rating", "content");
        assertRequiredProperties(
                document,
                "UpdateReviewStatusRequest",
                "status",
                "expectedContentRevision",
                "expectedVersion");
        assertRequiredProperties(document, "UpsertReviewReplyRequest", "expectedVersion", "content");
        assertRequiredProperties(document, "CreateReviewReportRequest", "reason");
        assertRequiredProperties(document, "DecideReviewReportRequest", "decision");
        assertRequiredProperties(
                document,
                "PublicReviewPageResponse",
                "summary",
                "filteredCount",
                "content",
                "nextCursor",
                "hasMore");
        assertRequiredProperties(
                document, "ReviewSummaryResponse", "reviewCount", "averageRating", "histogram");
        assertRequiredProperties(
                document,
                "ReviewRatingHistogramResponse",
                "rating1",
                "rating2",
                "rating3",
                "rating4",
                "rating5");
        assertRequiredProperties(
                document,
                "PublicReviewResponse",
                "id",
                "rating",
                "content",
                "authorName",
                "sourceType",
                "verifiedTransaction",
                "createdAt",
                "updatedAt",
                "edited",
                "editedAt",
                "officialReply",
                "helpfulCount",
                "images");
        assertRequiredProperties(
                document,
                "MemberReviewResponse",
                "id",
                "targetType",
                "targetId",
                "targetName",
                "sourceType",
                "sourceId",
                "rating",
                "content",
                "status",
                "hiddenReason",
                "createdAt",
                "updatedAt",
                "edited",
                "editedAt",
                "verifiedTransaction",
                "officialReply",
                "helpfulCount",
                "images");
        assertRequiredProperties(
                document, "MemberReviewPageResponse", "content", "nextCursor", "hasMore");
        assertRequiredProperties(
                document,
                "AdminReviewResponse",
                "id",
                "userId",
                "authorName",
                "targetType",
                "targetId",
                "targetName",
                "sourceType",
                "sourceId",
                "rating",
                "content",
                "status",
                "contentRevision",
                "hiddenReason",
                "hiddenAt",
                "hiddenByAdminId",
                "createdAt",
                "updatedAt",
                "edited",
                "editedAt",
                "verifiedTransaction",
                "officialReply",
                "helpfulCount",
                "images");
        assertRequiredProperties(
                document, "AdminReviewPageResponse", "content", "nextCursor", "hasMore");
        assertRequiredProperties(
                document,
                "OfficialReviewReplyResponse",
                "content",
                "createdAt",
                "editedAt",
                "edited");
        assertRequiredProperties(
                document,
                "AdminOfficialReviewReplyResponse",
                "content",
                "adminUserId",
                "createdAt",
                "editedAt",
                "edited");
        assertRequiredProperties(
                document, "ReviewImageResponse", "id", "imageUrl", "sortOrder", "createdAt");
        assertRequiredProperties(
                document,
                "ReviewOpportunityResponse",
                "targetType",
                "sourceType",
                "sourceId",
                "targetId",
                "targetName",
                "orderId",
                "bookingId",
                "completedAt");
        assertRequiredProperties(
                document,
                "ReviewOpportunityPageResponse",
                "content",
                "nextCursor",
                "hasMore");
        assertRequiredProperties(
                document,
                "ReviewCreationStateResponse",
                "targetType",
                "sourceType",
                "sourceId",
                "status");
        assertRequiredProperties(
                document,
                "ReviewReactionResponse",
                "reviewId",
                "helpfulByMe",
                "reportedByMe",
                "ownedByMe",
                "canInteract");
        assertRequiredProperties(
                document, "ReviewHelpfulResponse", "reviewId", "helpfulCount", "helpfulByMe");
        assertRequiredProperties(
                document,
                "MemberReviewReportResponse",
                "id",
                "reviewId",
                "reason",
                "detail",
                "status",
                "createdAt");
        assertRequiredProperties(
                document,
                "AdminReviewReportResponse",
                "id",
                "reviewId",
                "reporterUserId",
                "reason",
                "detail",
                "snapshotStatus",
                "evidence",
                "status",
                "decisionNote",
                "decidedByAdminId",
                "decidedAt",
                "createdAt");
        assertRequiredProperties(
                document, "AdminReviewReportPageResponse", "content", "nextCursor", "hasMore");
        assertRequiredProperties(
                document,
                "ReviewModerationActionResponse",
                "id",
                "reviewId",
                "action",
                "previousStatus",
                "newStatus",
                "reason",
                "adminUserId",
                "evidence",
                "createdAt");
        assertRequiredProperties(
                document,
                "ReviewEvidenceResponse",
                "id",
                "contentRevision",
                "rating",
                "content",
                "editedAt",
                "provenance",
                "imagesComplete",
                "imageUrls",
                "capturedAt");

        assertEnumProperty(
                document, "MemberReviewResponse", "targetType", "PRODUCT", "CLASS");
        assertEnumProperty(
                document, "MemberReviewResponse", "sourceType", "ORDER_ITEM", "BOOKING");
        assertEnumProperty(
                document, "MemberReviewResponse", "status", "PUBLISHED", "HIDDEN");
        assertEnumProperty(
                document, "AdminReviewResponse", "targetType", "PRODUCT", "CLASS");
        assertEnumProperty(
                document, "AdminReviewResponse", "sourceType", "ORDER_ITEM", "BOOKING");
        assertEnumProperty(
                document, "AdminReviewResponse", "status", "PUBLISHED", "HIDDEN");
        assertEnumProperty(
                document, "UpdateReviewStatusRequest", "status", "PUBLISHED", "HIDDEN");
        assertEnumProperty(
                document,
                "CreateReviewReportRequest",
                "reason",
                "SPAM",
                "ABUSIVE",
                "PRIVACY",
                "FALSE_INFORMATION",
                "OTHER");
        assertEnumProperty(
                document, "DecideReviewReportRequest", "decision", "ACCEPTED", "REJECTED");
        assertEnumProperty(
                document,
                "ReviewCreationStateResponse",
                "status",
                "AVAILABLE",
                "NOT_REVIEWABLE",
                "REVIEW_EXISTS",
                "RECREATION_BLOCKED");
        assertNumericRangeProperty(document, "CreateProductReviewRequest", "rating", 1, 5);
        assertNumericRangeProperty(document, "CreateClassReviewRequest", "rating", 1, 5);
        assertNumericRangeProperty(document, "UpdateReviewRequest", "rating", 1, 5);
        assertStringLengthRangeProperty(
                document, "CreateProductReviewRequest", "content", 1, 16_000);
        assertStringLengthRangeProperty(
                document, "CreateClassReviewRequest", "content", 1, 16_000);
        assertStringLengthRangeProperty(
                document, "UpdateReviewRequest", "content", 1, 16_000);
        assertStringLengthRangeProperty(
                document, "UpsertReviewReplyRequest", "content", 1, 16_000);
        assertNullableProperty(document, "PublicReviewPageResponse", "nextCursor");
        assertNullableProperty(document, "PublicReviewResponse", "editedAt");
        assertNullableProperty(document, "PublicReviewResponse", "officialReply");
        assertNullableProperty(document, "MemberReviewResponse", "hiddenReason");
        assertNullableProperty(document, "MemberReviewResponse", "editedAt");
        assertNullableProperty(document, "MemberReviewResponse", "officialReply");
        assertNullableProperty(document, "MemberReviewPageResponse", "nextCursor");
        assertNullableProperty(document, "AdminReviewResponse", "hiddenReason");
        assertNullableProperty(document, "AdminReviewResponse", "hiddenAt");
        assertNullableProperty(document, "AdminReviewResponse", "hiddenByAdminId");
        assertNullableProperty(document, "AdminReviewResponse", "editedAt");
        assertNullableProperty(document, "AdminReviewResponse", "officialReply");
        assertNullableProperty(document, "AdminReviewPageResponse", "nextCursor");
        assertNullableProperty(document, "OfficialReviewReplyResponse", "editedAt");
        assertNullableProperty(document, "AdminOfficialReviewReplyResponse", "editedAt");
        assertNullableProperty(document, "ReviewOpportunityResponse", "orderId");
        assertNullableProperty(document, "ReviewOpportunityResponse", "bookingId");
        assertNullableProperty(document, "ReviewOpportunityPageResponse", "nextCursor");
        assertNullableProperty(document, "MemberReviewReportResponse", "detail");
        assertNullableProperty(document, "AdminReviewReportResponse", "detail");
        assertNullableProperty(document, "AdminReviewReportResponse", "evidence");
        assertNullableProperty(document, "AdminReviewReportResponse", "decisionNote");
        assertNullableProperty(document, "AdminReviewReportResponse", "decidedByAdminId");
        assertNullableProperty(document, "AdminReviewReportResponse", "decidedAt");
        assertNullableProperty(document, "AdminReviewReportPageResponse", "nextCursor");
        assertNullableProperty(document, "ReviewModerationActionResponse", "reason");
        assertNullableProperty(document, "ReviewModerationActionResponse", "evidence");
        assertNullableProperty(document, "ReviewEvidenceResponse", "editedAt");
        assertNullableProperty(document, "UpdateReviewStatusRequest", "reason");
        assertNullableProperty(document, "CreateReviewReportRequest", "detail");
        assertNullableProperty(document, "DecideReviewReportRequest", "note");
        assertPropertyAbsent(document, "PublicReviewResponse", "helpfulByMe");
        assertPropertyAbsent(document, "PublicReviewResponse", "reportedByMe");
        assertPropertyAbsent(document, "PublicReviewResponse", "ownedByMe");
        assertPropertyAbsent(document, "PublicReviewResponse", "canInteract");
        assertPropertyAbsent(document, "MemberReviewReportResponse", "reporterUserId");
        assertPropertyAbsent(document, "MemberReviewReportResponse", "evidence");
        assertRequiredProperties(document, "MemberReviewResponse", "contentRevision");
        assertRequiredProperties(document, "AdminReviewResponse", "contentRevision", "version");
        assertRequiredProperties(document, "ErrorResponse", "code", "message");
        assertOptionalProperty(document, "ErrorResponse", "requestId");
        assertOperationId(
                document,
                "/api/v1/me/reviews/opportunities",
                "get",
                "listMyReviewOpportunities");
        assertOperationId(
                document, "/api/v1/admin/reviews/{reviewId}", "get", "getAdminReview");
        assertOperationId(
                document,
                "/api/v1/admin/review-evidence/{evidenceId}/images/{sortOrder}",
                "get",
                "getAdminReviewEvidenceImage");
        assertOperationId(
                document,
                "/api/v1/me/reviews/{reviewId}/images/{imageId}",
                "get",
                "getMyReviewImage");
        assertOperationId(
                document,
                "/api/v1/admin/reviews/{reviewId}/images/{imageId}",
                "get",
                "getAdminReviewImage");
        assertErrorResponse(
                document,
                "/api/v1/admin/reviews/{reviewId}/status",
                "patch",
                "409");
        assertResponseSchema(
                document,
                "/api/v1/me/reviews/{reviewId}",
                "patch",
                "200",
                "MemberReviewResponse");
        assertResponseSchema(
                document,
                "/api/v1/admin/reviews/{reviewId}/status",
                "patch",
                "200",
                "AdminReviewResponse");
        assertResponseSchema(
                document,
                "/api/v1/admin/reviews/{reviewId}/reply",
                "put",
                "200",
                "AdminReviewResponse");
        assertResponseSchema(
                document,
                "/api/v1/admin/reviews/{reviewId}/reply",
                "delete",
                "200",
                "AdminReviewResponse");
        assertRequiredRequestBody(
                document,
                "/api/v1/me/reviews/{reviewId}/images",
                "post",
                "addMyReviewImage");
        assertReviewListQueryContracts(document);
    }

    private void assertReviewSecurityContracts(Map<?, ?> document) {
        Map<?, ?> components = (Map<?, ?>) document.get("components");
        Map<?, ?> schemes = components == null ? null : (Map<?, ?>) components.get("securitySchemes");
        if (schemes == null
                || !schemes.keySet().containsAll(
                        Set.of("CustomerSession", "AdminBearer", "AdminApiKey"))) {
            throw new IllegalStateException("후기 인증 OpenAPI scheme이 누락되었습니다: " + schemes);
        }
        assertSecurityRequirement(
                document, "/api/v1/me/reviews", "get", "CustomerSession");
        assertSecurityRequirement(
                document,
                "/api/v1/admin/reviews/{reviewId}/status",
                "patch",
                "AdminBearer");
        assertSecurityRequirement(
                document,
                "/api/v1/admin/review-evidence/{evidenceId}/images/{sortOrder}",
                "get",
                "AdminBearer");
        assertSecurityRequirement(
                document,
                "/api/v1/me/reviews/{reviewId}/images/{imageId}",
                "get",
                "CustomerSession");
        assertSecurityRequirement(
                document,
                "/api/v1/admin/reviews/{reviewId}/images/{imageId}",
                "get",
                "AdminBearer");
    }

    private void assertRequiredRequestBody(
            Map<?, ?> document,
            String path,
            String method,
            String operationId
    ) {
        Map<?, ?> operation = operation(document, path, method);
        if (!(operation.get("requestBody") instanceof Map<?, ?> requestBody)
                || !Boolean.TRUE.equals(requestBody.get("required"))) {
            throw new IllegalStateException(
                    operationId + " requestBody는 필수여야 합니다: "
                            + operation.get("requestBody"));
        }
    }

    private void assertReviewListQueryContracts(Map<?, ?> document) {
        assertReviewListQueryContract(
                document, "/api/v1/products/{productId}/reviews", "listProductReviews");
        assertReviewListQueryContract(
                document, "/api/v1/classes/{classId}/reviews", "listClassReviews");
    }

    private void assertReviewListQueryContract(
            Map<?, ?> document,
            String path,
            String operationId
    ) {
        Map<?, ?> operation = operation(document, path, "get");
        Map<?, ?> rating = queryParameter(operation, "rating");
        Map<?, ?> ratingSchema = (Map<?, ?>) rating.get("schema");
        if (!(ratingSchema.get("minimum") instanceof Number minimum)
                || !(ratingSchema.get("maximum") instanceof Number maximum)
                || minimum.intValue() != 1
                || maximum.intValue() != 5) {
            throw new IllegalStateException(operationId + " rating query 범위가 1..5가 아닙니다.");
        }
        Map<?, ?> sort = queryParameter(operation, "sort");
        Object sortSchemaValue = sort.get("schema");
        if (!(sortSchemaValue instanceof Map<?, ?> sortSchema)
                || !(sortSchema.get("enum") instanceof List<?> sortValues)
                || !Set.copyOf(sortValues).equals(Set.of("LATEST", "RATING_HIGH", "RATING_LOW"))) {
            throw new IllegalStateException(operationId + " sort enum이 올바르지 않습니다: " + sort);
        }
    }

    private void assertCursorPageSizeContracts(Map<?, ?> document) {
        Set<String> missingOperationIds = new HashSet<>(CURSOR_PAGE_OPERATION_IDS);
        Map<?, ?> paths = (Map<?, ?>) document.get("paths");
        for (Object pathValue : paths.values()) {
            if (!(pathValue instanceof Map<?, ?> pathItem)) {
                continue;
            }
            for (Object operationValue : pathItem.values()) {
                if (!(operationValue instanceof Map<?, ?> operation)
                        || !(operation.get("operationId") instanceof String operationId)
                        || !CURSOR_PAGE_OPERATION_IDS.contains(operationId)) {
                    continue;
                }
                assertPageSizeParameter(operationId, operation);
                missingOperationIds.remove(operationId);
            }
        }
        if (!missingOperationIds.isEmpty()) {
            throw new IllegalStateException(
                    "커서 페이지 OpenAPI operation이 누락되었습니다: " + missingOperationIds);
        }
    }

    private void assertPageSizeParameter(String operationId, Map<?, ?> operation) {
        Object parametersValue = operation.get("parameters");
        if (!(parametersValue instanceof List<?> parameters)) {
            throw new IllegalStateException(operationId + "의 query parameter가 없습니다.");
        }
        Map<?, ?> sizeParameter = parameters.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .filter(parameter -> "size".equals(parameter.get("name")))
                .filter(parameter -> "query".equals(parameter.get("in")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        operationId + "의 size query parameter가 없습니다."));
        if (!(sizeParameter.get("schema") instanceof Map<?, ?> schema)
                || !(schema.get("minimum") instanceof Number minimum)
                || !(schema.get("maximum") instanceof Number maximum)
                || !"integer".equals(schema.get("type"))
                || !"int32".equals(schema.get("format"))
                || !(schema.get("default") instanceof Number defaultValue)
                || minimum.intValue() != 1
                || maximum.intValue() != 100
                || defaultValue.intValue() != 20) {
            throw new IllegalStateException(
                    operationId + "의 size 계약은 int32 기본값 20, 범위 1~100이어야 합니다: "
                            + sizeParameter);
        }
    }

    private void assertRequiredProperties(
            Map<?, ?> document,
            String schemaName,
            String... expectedProperties
    ) {
        Map<?, ?> schema = schema(document, schemaName);
        Object requiredValue = schema.get("required");
        if (!(requiredValue instanceof List<?> required)
                || !required.containsAll(List.of(expectedProperties))) {
            throw new IllegalStateException(
                    "%s 필수 필드가 OpenAPI에서 누락되었습니다. expected=%s, actual=%s"
                            .formatted(schemaName, List.of(expectedProperties), requiredValue));
        }
    }

    private void assertOptionalProperty(
            Map<?, ?> document,
            String schemaName,
            String propertyName
    ) {
        property(document, schemaName, propertyName);
        Object requiredValue = schema(document, schemaName).get("required");
        if (requiredValue instanceof List<?> required && required.contains(propertyName)) {
            throw new IllegalStateException(
                    "%s.%s는 OpenAPI 선택 필드여야 합니다. actual=%s"
                            .formatted(schemaName, propertyName, required));
        }
    }

    private void assertPaymentPayloadDiscriminator(Map<?, ?> document) {
        Map<?, ?> payloadSchema = property(document, "PreparePaymentRequest", "payload");
        Object oneOfValue = payloadSchema.get("oneOf");
        if (!(oneOfValue instanceof List<?> oneOf) || oneOf.size() != 3) {
            throw new IllegalStateException(
                    "PreparePaymentRequest.payload oneOf는 ORDER/BOOKING/PASS 3개여야 합니다: "
                            + oneOfValue);
        }

        Set<String> references = oneOf.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> String.valueOf(item.get("$ref")))
                .collect(toUnmodifiableSet());
        Set<String> expectedReferences = Set.of(
                "#/components/schemas/OrderPayload",
                "#/components/schemas/BookingPayload",
                "#/components/schemas/PassPayload");
        if (!references.equals(expectedReferences)) {
            throw new IllegalStateException(
                    "PreparePaymentRequest.payload oneOf 참조가 올바르지 않습니다: " + references);
        }

        Map<?, ?> baseSchema = schema(document, "PaymentPayload");
        if (baseSchema.containsKey("oneOf")) {
            throw new IllegalStateException(
                    "PaymentPayload base가 subtype oneOf를 가지면 allOf 상속과 순환합니다.");
        }
        Object discriminatorValue = baseSchema.get("discriminator");
        if (!(discriminatorValue instanceof Map<?, ?> discriminator)
                || !"type".equals(discriminator.get("propertyName"))) {
            throw new IllegalStateException(
                    "PaymentPayload discriminator propertyName은 type이어야 합니다: "
                            + discriminatorValue);
        }
        Map<String, String> expectedMapping = Map.of(
                "ORDER", "#/components/schemas/OrderPayload",
                "BOOKING", "#/components/schemas/BookingPayload",
                "PASS", "#/components/schemas/PassPayload");
        if (!expectedMapping.equals(discriminator.get("mapping"))) {
            throw new IllegalStateException(
                    "PaymentPayload discriminator mapping이 올바르지 않습니다: "
                            + discriminator.get("mapping"));
        }
    }

    private void assertEnumProperty(
            Map<?, ?> document,
            String schemaName,
            String propertyName,
            String... expectedValues
    ) {
        Map<?, ?> property = property(document, schemaName, propertyName);
        Object enumValue = property.get("enum");
        if (!(enumValue instanceof List<?> values)
                || !Set.copyOf(values).equals(Set.of(expectedValues))) {
            throw new IllegalStateException(
                    "%s.%s enum이 올바르지 않습니다. expected=%s, actual=%s"
                            .formatted(schemaName, propertyName, List.of(expectedValues), enumValue));
        }
    }

    private void assertNumericRangeProperty(
            Map<?, ?> document,
            String schemaName,
            String propertyName,
            int expectedMinimum,
            int expectedMaximum
    ) {
        Map<?, ?> property = property(document, schemaName, propertyName);
        if (!(property.get("minimum") instanceof Number minimum)
                || !(property.get("maximum") instanceof Number maximum)
                || minimum.intValue() != expectedMinimum
                || maximum.intValue() != expectedMaximum) {
            throw new IllegalStateException(
                    "%s.%s 범위가 올바르지 않습니다. expected=%d..%d, actual=%s"
                            .formatted(
                                    schemaName,
                                    propertyName,
                                    expectedMinimum,
                                    expectedMaximum,
                                    property));
        }
    }

    private void assertStringLengthRangeProperty(
            Map<?, ?> document,
            String schemaName,
            String propertyName,
            int expectedMinimumLength,
            int expectedMaximumLength
    ) {
        Map<?, ?> property = property(document, schemaName, propertyName);
        if (!(property.get("minLength") instanceof Number minimumLength)
                || minimumLength.intValue() != expectedMinimumLength
                || !(property.get("maxLength") instanceof Number maximumLength)
                || maximumLength.intValue() != expectedMaximumLength) {
            throw new IllegalStateException(
                    "%s.%s 문자열 길이 범위가 올바르지 않습니다. expected=%d..%d, actual=%s"
                            .formatted(
                                    schemaName,
                                    propertyName,
                                    expectedMinimumLength,
                                    expectedMaximumLength,
                                    property));
        }
    }

    private void assertNullableProperty(
            Map<?, ?> document,
            String schemaName,
            String propertyName
    ) {
        Map<?, ?> property = property(document, schemaName, propertyName);
        Object type = property.get("type");
        boolean nullableType = "null".equals(type)
                || type instanceof List<?> types && types.contains("null");
        boolean nullableOneOf = property.get("oneOf") instanceof List<?> oneOf
                && oneOf.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .anyMatch(item -> "null".equals(item.get("type")));
        if (!Boolean.TRUE.equals(property.get("nullable"))
                && !nullableType
                && !nullableOneOf) {
            throw new IllegalStateException(
                    "%s.%s는 nullable이어야 합니다: %s"
                            .formatted(schemaName, propertyName, property));
        }
    }

    private void assertPatternProperty(
            Map<?, ?> document,
            String schemaName,
            String propertyName,
            String expectedPattern
    ) {
        Map<?, ?> property = property(document, schemaName, propertyName);
        if (!expectedPattern.equals(property.get("pattern"))) {
            throw new IllegalStateException(
                    "%s.%s pattern이 올바르지 않습니다. expected=%s, actual=%s"
                            .formatted(
                                    schemaName,
                                    propertyName,
                                    expectedPattern,
                                    property.get("pattern")));
        }
    }

    private void assertIntegerRangeProperty(
            Map<?, ?> document,
            String schemaName,
            String propertyName,
            long expectedMinimum,
            long expectedMaximum
    ) {
        Map<?, ?> property = property(document, schemaName, propertyName);
        Object type = property.get("type");
        boolean nullableInteger = "integer".equals(type)
                || (type instanceof List<?> types
                && types.contains("integer")
                && types.contains("null"));
        if (!nullableInteger
                || !"int64".equals(property.get("format"))
                || !(property.get("minimum") instanceof Number minimum)
                || !(property.get("maximum") instanceof Number maximum)
                || minimum.longValue() != expectedMinimum
                || maximum.longValue() != expectedMaximum) {
            throw new IllegalStateException(
                    "%s.%s 범위가 올바르지 않습니다. expected=%d..%d, actual=%s"
                            .formatted(
                                    schemaName,
                                    propertyName,
                                    expectedMinimum,
                                    expectedMaximum,
                                    property));
        }
    }

    private void assertNullableReferenceProperty(
            Map<?, ?> document,
            String schemaName,
            String propertyName,
            String referenceSchemaName
    ) {
        Map<?, ?> property = property(document, schemaName, propertyName);
        Object oneOfValue = property.get("oneOf");
        if (!(oneOfValue instanceof List<?> oneOf)) {
            throw new IllegalStateException(
                    "%s.%s nullable oneOf가 없습니다: %s"
                            .formatted(schemaName, propertyName, property));
        }
        boolean hasReference = oneOf.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .anyMatch(item -> ("#/components/schemas/" + referenceSchemaName)
                        .equals(item.get("$ref")));
        boolean hasNull = oneOf.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .anyMatch(item -> "null".equals(item.get("type")));
        if (!hasReference || !hasNull) {
            throw new IllegalStateException(
                    "%s.%s는 %s 참조와 null을 모두 허용해야 합니다: %s"
                            .formatted(schemaName, propertyName, referenceSchemaName, oneOfValue));
        }
    }

    private void assertMaximumProperty(
            Map<?, ?> document,
            String schemaName,
            String propertyName,
            long expectedMaximum) {
        Map<?, ?> property = property(document, schemaName, propertyName);
        Object maximum = property.get("maximum");
        if (maximum == null
                || new BigDecimal(maximum.toString()).compareTo(BigDecimal.valueOf(expectedMaximum)) != 0) {
            throw new IllegalStateException(
                    "%s.%s maximum이 올바르지 않습니다. expected=%s, actual=%s"
                            .formatted(schemaName, propertyName, expectedMaximum, maximum));
        }
    }

    private void assertPropertyAbsent(
            Map<?, ?> document,
            String schemaName,
            String propertyName
    ) {
        Map<?, ?> schema = schema(document, schemaName);
        if (propertyFromSchema(schema, propertyName) != null) {
            throw new IllegalStateException(
                    "%s.%s는 공개 계약에 노출되면 안 됩니다."
                            .formatted(schemaName, propertyName));
        }
    }

    private void assertOperationId(
            Map<?, ?> document,
            String path,
            String method,
            String expectedOperationId
    ) {
        Object actual = operation(document, path, method).get("operationId");
        if (!expectedOperationId.equals(actual)) {
            throw new IllegalStateException(
                    "%s %s operationId가 올바르지 않습니다. expected=%s, actual=%s"
                            .formatted(method, path, expectedOperationId, actual));
        }
    }

    private void assertSecurityRequirement(
            Map<?, ?> document,
            String path,
            String method,
            String schemeName
    ) {
        Object securityValue = operation(document, path, method).get("security");
        boolean present = securityValue instanceof List<?> security
                && security.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .anyMatch(requirement -> requirement.containsKey(schemeName));
        if (!present) {
            throw new IllegalStateException(
                    "%s %s에 %s 인증 계약이 없습니다: %s"
                            .formatted(method, path, schemeName, securityValue));
        }
    }

    private void assertErrorResponse(
            Map<?, ?> document,
            String path,
            String method,
            String responseCode
    ) {
        assertResponseSchema(document, path, method, responseCode, "ErrorResponse");
    }

    private void assertResponseSchema(
            Map<?, ?> document,
            String path,
            String method,
            String responseCode,
            String schemaName
    ) {
        Map<?, ?> operation = operation(document, path, method);
        Object responsesValue = operation.get("responses");
        if (!(responsesValue instanceof Map<?, ?> responses)
                || !(responses.get(responseCode) instanceof Map<?, ?> response)
                || !(response.get("content") instanceof Map<?, ?> content)) {
            throw new IllegalStateException(
                    "%s %s의 %s 오류 응답이 없습니다: %s"
                            .formatted(method, path, responseCode, responsesValue));
        }
        boolean expectedSchema = content.values().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(media -> media.get("schema"))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .anyMatch(schema -> ("#/components/schemas/" + schemaName)
                        .equals(schema.get("$ref")));
        if (!expectedSchema) {
            throw new IllegalStateException(
                    "%s %s의 %s 응답이 %s가 아닙니다: %s"
                            .formatted(method, path, responseCode, schemaName, content));
        }
    }

    private Map<?, ?> operation(
            Map<?, ?> document,
            String path,
            String method
    ) {
        Object pathsValue = document.get("paths");
        if (!(pathsValue instanceof Map<?, ?> paths)
                || !(paths.get(path) instanceof Map<?, ?> pathItem)
                || !(pathItem.get(method) instanceof Map<?, ?> operation)) {
            throw new IllegalStateException("OpenAPI operation이 없습니다: " + method + " " + path);
        }
        return operation;
    }

    private Map<?, ?> queryParameter(Map<?, ?> operation, String name) {
        Object parametersValue = operation.get("parameters");
        if (parametersValue instanceof List<?> parameters) {
            return parameters.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .filter(parameter -> name.equals(parameter.get("name")))
                    .filter(parameter -> "query".equals(parameter.get("in")))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "OpenAPI query parameter가 없습니다: " + name));
        }
        throw new IllegalStateException("OpenAPI parameters가 없습니다: " + name);
    }

    private Map<?, ?> property(Map<?, ?> document, String schemaName, String propertyName) {
        Map<?, ?> schema = schema(document, schemaName);
        Map<?, ?> property = propertyFromSchema(schema, propertyName);
        if (property == null) {
            throw new IllegalStateException(
                    "%s.%s OpenAPI property가 없습니다.".formatted(schemaName, propertyName));
        }
        return property;
    }

    private Map<?, ?> propertyFromSchema(Map<?, ?> schema, String propertyName) {
        Object propertiesValue = schema.get("properties");
        if (propertiesValue instanceof Map<?, ?> properties
                && properties.get(propertyName) instanceof Map<?, ?> property) {
            return property;
        }
        Object allOfValue = schema.get("allOf");
        if (!(allOfValue instanceof List<?> allOf)) {
            return null;
        }
        for (Object item : allOf) {
            if (item instanceof Map<?, ?> child) {
                Map<?, ?> property = propertyFromSchema(child, propertyName);
                if (property != null) {
                    return property;
                }
            }
        }
        return null;
    }

    private Map<?, ?> schema(Map<?, ?> document, String schemaName) {
        Object componentsValue = document.get("components");
        if (!(componentsValue instanceof Map<?, ?> components)) {
            throw new IllegalStateException("OpenAPI components가 없습니다.");
        }
        Object schemasValue = components.get("schemas");
        if (!(schemasValue instanceof Map<?, ?> schemas)) {
            throw new IllegalStateException("OpenAPI schemas가 없습니다.");
        }
        Object schemaValue = schemas.get(schemaName);
        if (!(schemaValue instanceof Map<?, ?> schema)) {
            throw new IllegalStateException("OpenAPI schema가 없습니다: " + schemaName);
        }
        return schema;
    }

    private Object sortObjectKeys(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((key, child) -> sorted.put(String.valueOf(key), sortObjectKeys(child)));
            return sorted;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::sortObjectKeys).toList();
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    @TestConfiguration(proxyBeanMethods = false)
    static class OpenApiConfiguration {

        @Bean
        OpenAPI happyGalleryOpenApi() {
            return new OpenAPI()
                    .info(new Info().title("happyGallery API").version("v1"))
                    .servers(List.of(new Server().url("/").description("Same-origin API")));
        }

        @Bean
        OpenApiCustomizer nullableReferenceCustomizer() {
            return openApi -> openApi.getComponents().getSchemas().values()
                    .forEach(this::normalizeNullableReferences);
        }

        private void normalizeNullableReferences(Schema<?> schema) {
            if (schema.getProperties() != null) {
                schema.getProperties().values().forEach(property -> {
                    if (property instanceof Schema<?> propertySchema) {
                        normalizeNullableReference(propertySchema);
                        normalizeNullableReferences(propertySchema);
                    }
                });
            }
            if (schema.getAllOf() != null) {
                schema.getAllOf().forEach(this::normalizeNullableReferences);
            }
            if (schema.getOneOf() != null) {
                schema.getOneOf().forEach(this::normalizeNullableReferences);
            }
            if (schema.getAnyOf() != null) {
                schema.getAnyOf().forEach(this::normalizeNullableReferences);
            }
            if (schema.getItems() != null) {
                normalizeNullableReferences(schema.getItems());
            }
        }

        private void normalizeNullableReference(Schema<?> schema) {
            if (schema.get$ref() == null || !isNullable(schema)) {
                return;
            }

            String reference = schema.get$ref();
            schema.set$ref(null);
            schema.setType(null);
            schema.setTypes(null);
            schema.setNullable(null);
            schema.setOneOf(List.of(
                    new Schema<>().$ref(reference),
                    new Schema<>().types(Set.of("null"))));
        }

        private boolean isNullable(Schema<?> schema) {
            return Boolean.TRUE.equals(schema.getNullable())
                    || "null".equals(schema.getType())
                    || schema.getTypes() != null && schema.getTypes().contains("null");
        }
    }
}
