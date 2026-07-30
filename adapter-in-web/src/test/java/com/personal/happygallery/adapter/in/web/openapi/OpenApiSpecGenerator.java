package com.personal.happygallery.adapter.in.web.openapi;

import com.personal.happygallery.support.UseCaseIT;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

        String canonicalOpenApi = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(sortObjectKeys(objectMapper.readValue(openApi, Object.class))) + "\n";
        Path output = Path.of(System.getProperty("openapi.output"));
        Files.createDirectories(output.getParent());
        Files.writeString(output, canonicalOpenApi, StandardCharsets.UTF_8);
    }

    private void assertStableOperationIds(Map<?, ?> document) {
        Map<?, ?> paths = (Map<?, ?>) document.get("paths");
        for (Object pathValue : paths.values()) {
            Map<?, ?> operations = (Map<?, ?>) pathValue;
            for (Object operationValue : operations.values()) {
                if (!(operationValue instanceof Map<?, ?> operation)) {
                    continue;
                }
                Object operationId = operation.get("operationId");
                if (operationId instanceof String id && UNSTABLE_NUMERIC_SUFFIX.matcher(id).matches()) {
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
        assertPaymentPayloadDiscriminator(document);
        assertEnumProperty(document, "OrderPayload", "type", "ORDER");
        assertEnumProperty(document, "BookingPayload", "type", "BOOKING");
        assertEnumProperty(document, "BookingPayload", "paymentMethod", "CARD", "EASY_PAY");
        assertEnumProperty(document, "PassPayload", "type", "PASS");
        assertNullableReferenceProperty(
                document, "OrderPayload", "shippingAddress", "ShippingAddress");
        assertNullableReferenceProperty(
                document, "OrderPayload", "policyAcceptance", "PolicyAcceptanceRequest");
        assertNullableReferenceProperty(
                document, "BookingPayload", "policyAcceptance", "PolicyAcceptanceRequest");
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
