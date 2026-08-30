package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.ProductOptions.OptionGroup;
import com.personal.happygallery.application.product.ProductOptions.OptionSnapshot;
import com.personal.happygallery.application.product.ProductOptions.OptionValue;
import com.personal.happygallery.application.product.ProductOptions.PurchaseRequest;
import com.personal.happygallery.application.product.ProductOptions.ResolvedLine;
import com.personal.happygallery.application.product.ProductOptions.ResolvedPurchase;
import com.personal.happygallery.application.product.ProductOptions.ResolvedTextInput;
import com.personal.happygallery.application.product.ProductOptions.Selection;
import com.personal.happygallery.application.product.ProductOptions.TextInput;
import com.personal.happygallery.application.product.ProductOptions.Variant;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.OptionGroupDefinition;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.OptionValueDefinition;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.SelectionDefinition;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.VariantDefinition;
import com.personal.happygallery.application.product.port.out.ProductOptionGroupPort;
import com.personal.happygallery.application.product.port.out.ProductOptionValuePort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.ProductVariantReaderPort;
import com.personal.happygallery.application.product.port.out.ProductVariantStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductOptionGroup;
import com.personal.happygallery.domain.product.ProductOptionPolicy;
import com.personal.happygallery.domain.product.ProductOptionType;
import com.personal.happygallery.domain.product.ProductOptionValue;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.ProductVariant;
import com.personal.happygallery.domain.product.ProductVariantSelection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductOptionConfigurationService {

    private final ProductOptionGroupPort groupPort;
    private final ProductOptionValuePort valuePort;
    private final ProductVariantReaderPort variantReaderPort;
    private final ProductVariantStorePort variantStorePort;
    private final ProductReaderPort productReaderPort;

    public ProductOptionConfigurationService(ProductOptionGroupPort groupPort,
                                             ProductOptionValuePort valuePort,
                                             ProductVariantReaderPort variantReaderPort,
                                             ProductVariantStorePort variantStorePort,
                                             ProductReaderPort productReaderPort) {
        this.groupPort = groupPort;
        this.valuePort = valuePort;
        this.variantReaderPort = variantReaderPort;
        this.variantStorePort = variantStorePort;
        this.productReaderPort = productReaderPort;
    }

    public ProductOptions configure(Product product, Integer defaultQuantity,
                                    List<OptionGroupDefinition> groupDefinitions,
                                    List<VariantDefinition> variantDefinitions) {
        if (product.getType() == ProductType.READY_STOCK) {
            if (!groupDefinitions.isEmpty() || !variantDefinitions.isEmpty()) {
                throw invalid("선택 옵션과 직접입력 옵션은 주문제작 상품에만 등록할 수 있습니다.");
            }
            return ProductOptions.EMPTY;
        }

        validateGroupDefinitions(groupDefinitions);
        List<ProductOptionGroup> groups = syncGroups(product.getId(), groupDefinitions);
        Map<String, ProductOptionGroup> groupsByKey = activeGroupsByKey(groups);
        List<ProductOptionValue> values = syncValues(groupDefinitions, groupsByKey);
        Map<Long, List<ProductOptionValue>> valuesByGroupId = activeValuesByGroupId(values);
        List<ProductVariant> variants = syncVariants(
                product,
                defaultQuantity,
                groupDefinitions,
                variantDefinitions,
                groupsByKey,
                valuesByGroupId);
        return toOptions(groups, values, variants, true);
    }

    @Transactional(readOnly = true)
    public ProductOptions get(Long productId, boolean includeInactiveVariants) {
        return getAll(List.of(productId), includeInactiveVariants)
                .getOrDefault(productId, ProductOptions.EMPTY);
    }

    @Transactional(readOnly = true)
    public Map<Long, ProductOptions> getAll(Collection<Long> productIds,
                                            boolean includeInactiveVariants) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductOptionGroup> groups = groupPort
                .findByProductIdInOrderByProductIdAscSortOrderAscIdAsc(productIds);
        List<Long> groupIds = groups.stream().map(ProductOptionGroup::getId).toList();
        List<ProductOptionValue> values = groupIds.isEmpty()
                ? List.of()
                : valuePort.findByGroupIdInOrderByGroupIdAscSortOrderAscIdAsc(groupIds);
        List<ProductVariant> variants = variantReaderPort
                .findWithSelectionsByProductIdIn(productIds);

        Map<Long, List<ProductOptionGroup>> groupsByProduct = groups.stream()
                .collect(Collectors.groupingBy(ProductOptionGroup::getProductId));
        Map<Long, ProductOptionGroup> groupsById = groups.stream()
                .collect(Collectors.toMap(ProductOptionGroup::getId, Function.identity()));
        Map<Long, List<ProductOptionValue>> valuesByProduct = values.stream()
                .filter(value -> groupsById.containsKey(value.getGroupId()))
                .collect(Collectors.groupingBy(value -> groupsById.get(value.getGroupId()).getProductId()));
        Map<Long, List<ProductVariant>> variantsByProduct = variants.stream()
                .collect(Collectors.groupingBy(ProductVariant::getProductId));

        Map<Long, ProductOptions> result = new HashMap<>();
        for (Long productId : productIds) {
            result.put(productId, toOptions(
                    groupsByProduct.getOrDefault(productId, List.of()),
                    valuesByProduct.getOrDefault(productId, List.of()),
                    variantsByProduct.getOrDefault(productId, List.of()),
                    includeInactiveVariants));
        }
        return Map.copyOf(result);
    }

    @Transactional(readOnly = true)
    public ResolvedPurchase resolvePurchase(Product product, Long variantId,
                                            List<TextInput> requestedInputs) {
        return resolvePurchases(List.of(new PurchaseRequest(
                0, product.getId(), variantId, requestedInputs))).getFirst().purchase();
    }

    @Transactional(readOnly = true)
    public List<ResolvedLine> resolvePurchases(List<PurchaseRequest> requests) {
        return resolvePurchases(requests, false);
    }

    @Transactional(readOnly = true)
    public List<ResolvedLine> resolvePurchasesForCart(List<PurchaseRequest> requests) {
        return resolvePurchases(requests, true);
    }

    private List<ResolvedLine> resolvePurchases(
            List<PurchaseRequest> requests, boolean ignoreUnavailableSelections) {
        if (requests.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = requests.stream().map(PurchaseRequest::productId).distinct().toList();
        Map<Long, Product> productsById = productReaderPort.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<ProductOptionGroup> groups = groupPort
                .findByProductIdInOrderByProductIdAscSortOrderAscIdAsc(productIds);
        Map<Long, List<ProductOptionGroup>> groupsByProductId = groups.stream()
                .collect(Collectors.groupingBy(ProductOptionGroup::getProductId));
        List<Long> groupIds = groups.stream().map(ProductOptionGroup::getId).toList();
        List<ProductOptionValue> values = groupIds.isEmpty()
                ? List.of()
                : valuePort.findByGroupIdInOrderByGroupIdAscSortOrderAscIdAsc(groupIds);
        Map<Long, List<ProductOptionValue>> valuesByGroupId = values.stream()
                .collect(Collectors.groupingBy(ProductOptionValue::getGroupId));
        Map<Long, List<ProductVariant>> variantsByProductId = variantReaderPort
                .findWithSelectionsByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(ProductVariant::getProductId));

        List<ResolvedLine> resolved = new ArrayList<>(requests.size());
        for (PurchaseRequest request : requests) {
            Product product = productsById.get(request.productId());
            if (product == null) {
                if (ignoreUnavailableSelections) {
                    continue;
                }
                throw new NotFoundException("상품");
            }
            ProductVariant variant = null;
            if (product.getType() == ProductType.MADE_TO_ORDER) {
                variant = variantsByProductId.getOrDefault(product.getId(), List.of()).stream()
                        .filter(candidate -> request.variantId() == null
                                ? candidate.getCombinationKey().equals(ProductVariant.DEFAULT_COMBINATION_KEY)
                                : candidate.getId().equals(request.variantId()))
                        .findFirst()
                        .orElse(null);
                if (variant == null) {
                    if (ignoreUnavailableSelections) {
                        continue;
                    }
                    throw new NotFoundException("상품 옵션 조합");
                }
            }
            try {
                resolved.add(new ResolvedLine(
                        request.index(),
                        product,
                        resolvePurchase(
                                product,
                                variant,
                                request.textInputs(),
                                groupsByProductId.getOrDefault(product.getId(), List.of()),
                                valuesByGroupId)));
            } catch (HappyGalleryException | IllegalArgumentException exception) {
                if (!ignoreUnavailableSelections) {
                    throw exception;
                }
            }
        }
        return List.copyOf(resolved);
    }

    private ResolvedPurchase resolvePurchase(
            Product product,
            ProductVariant variant,
            List<TextInput> requestedInputs,
            List<ProductOptionGroup> groups,
            Map<Long, List<ProductOptionValue>> valuesByGroupId) {
        if (product.getType() != ProductType.MADE_TO_ORDER) {
            if (variant != null || !requestedInputs.isEmpty()) {
                throw invalid("기성품에는 주문제작 옵션을 지정할 수 없습니다.");
            }
            return new ResolvedPurchase(
                    null, product.getPrice(), 0L, 0L, product.getPrice(), true,
                    Integer.MAX_VALUE, List.of(), List.of());
        }
        if (!variant.getProductId().equals(product.getId()) || !variant.isActive()) {
            throw invalid("판매 중인 상품 옵션 조합만 선택할 수 있습니다.");
        }
        Map<Long, ProductOptionGroup> groupsById = groups.stream()
                .collect(Collectors.toMap(ProductOptionGroup::getId, Function.identity()));
        Map<Long, ProductOptionValue> valuesById = valuesByGroupId.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(ProductOptionValue::getId, Function.identity()));

        List<OptionSnapshot> snapshots = new ArrayList<>();
        for (ProductVariantSelection selection : variant.getSelections()) {
            ProductOptionGroup group = groupsById.get(selection.getOptionGroupId());
            ProductOptionValue value = valuesById.get(selection.getOptionValueId());
            if (group == null || value == null || !group.isActive() || !value.isActive()) {
                throw invalid("현재 판매하지 않는 상품 옵션이 포함되어 있습니다.");
            }
            snapshots.add(new OptionSnapshot(
                    ProductOptionType.SELECT,
                    group.getName(),
                    value.getName(),
                    0L,
                    group.getSortOrder()));
        }

        Map<String, TextInput> inputsByGroupKey = new LinkedHashMap<>();
        for (TextInput input : requestedInputs) {
            if (input == null || input.groupKey() == null) {
                throw invalid("직접입력형 옵션이 올바르지 않습니다.");
            }
            if (inputsByGroupKey.putIfAbsent(input.groupKey(), input) != null) {
                throw invalid("같은 직접입력형 옵션을 두 번 입력할 수 없습니다.");
            }
        }

        long textPriceAdjustment = 0L;
        Set<String> acceptedInputKeys = new HashSet<>();
        List<ResolvedTextInput> resolvedTextInputs = new ArrayList<>();
        for (ProductOptionGroup group : groups) {
            if (!group.isActive() || group.getType() != ProductOptionType.TEXT) {
                continue;
            }
            TextInput input = inputsByGroupKey.get(group.getKey());
            String normalized = input == null
                    ? null
                    : ProductOptionPolicy.optionalText(
                            input.value(), group.getName(), group.getInputMaxLength());
            if (normalized == null) {
                if (group.isRequired()) {
                    throw invalid(group.getName() + "을 입력해 주세요.");
                }
                continue;
            }
            acceptedInputKeys.add(group.getKey());
            long adjustment = group.getInputPriceAdjustment();
            textPriceAdjustment = addPrice(textPriceAdjustment, adjustment);
            snapshots.add(new OptionSnapshot(
                    ProductOptionType.TEXT,
                    group.getName(),
                    normalized,
                    adjustment,
                    group.getSortOrder()));
            resolvedTextInputs.add(new ResolvedTextInput(
                    group.getId(), group.getKey(), normalized, group.getSortOrder()));
        }
        if (!acceptedInputKeys.equals(inputsByGroupKey.keySet())) {
            throw invalid("등록되지 않은 직접입력형 옵션이 포함되어 있습니다.");
        }

        snapshots.sort(Comparator.comparingInt(OptionSnapshot::sortOrder));
        long unitPrice = variant.unitPrice(product.getPrice(), textPriceAdjustment);
        if (unitPrice > PaymentAmountPolicy.MAX_AMOUNT) {
            throw invalid("옵션을 반영한 상품 가격이 허용 범위를 벗어났습니다.");
        }
        return new ResolvedPurchase(
                variant.getId(),
                product.getPrice(),
                variant.getPriceAdjustment(),
                textPriceAdjustment,
                unitPrice,
                variant.isActive(),
                variant.getQuantity(),
                snapshots,
                resolvedTextInputs);
    }

    private List<ProductOptionGroup> syncGroups(
            Long productId, List<OptionGroupDefinition> definitions) {
        List<ProductOptionGroup> existing = groupPort
                .findByProductIdOrderBySortOrderAscIdAsc(productId);
        Map<String, ProductOptionGroup> existingByKey = existing.stream()
                .collect(Collectors.toMap(ProductOptionGroup::getKey, Function.identity()));
        Set<String> requestedKeys = definitions.stream()
                .map(OptionGroupDefinition::key)
                .collect(Collectors.toSet());

        List<ProductOptionGroup> changed = new ArrayList<>();
        for (OptionGroupDefinition definition : definitions) {
            ProductOptionGroup group = existingByKey.get(definition.key());
            if (group == null) {
                group = new ProductOptionGroup(
                        productId,
                        definition.key(),
                        definition.type(),
                        definition.name(),
                        definition.required(),
                        definition.sortOrder(),
                        definition.inputPlaceholder(),
                        definition.inputMaxLength(),
                        definition.inputPriceAdjustment());
            } else {
                if (group.getType() != definition.type()) {
                    throw invalid("기존 옵션의 유형은 변경할 수 없습니다.");
                }
                group.update(
                        definition.name(),
                        definition.required(),
                        definition.sortOrder(),
                        definition.inputPlaceholder(),
                        definition.inputMaxLength(),
                        definition.inputPriceAdjustment());
            }
            changed.add(group);
        }
        for (ProductOptionGroup group : existing) {
            if (!requestedKeys.contains(group.getKey())) {
                group.deactivate();
                changed.add(group);
            }
        }
        return List.copyOf(groupPort.saveAll(changed));
    }

    private List<ProductOptionValue> syncValues(
            List<OptionGroupDefinition> definitions,
            Map<String, ProductOptionGroup> groupsByKey) {
        List<Long> groupIds = groupsByKey.values().stream()
                .map(ProductOptionGroup::getId)
                .toList();
        List<ProductOptionValue> existing = groupIds.isEmpty()
                ? List.of()
                : valuePort.findByGroupIdInOrderByGroupIdAscSortOrderAscIdAsc(groupIds);
        Map<Long, Map<String, ProductOptionValue>> existingByGroup = existing.stream()
                .collect(Collectors.groupingBy(
                        ProductOptionValue::getGroupId,
                        Collectors.toMap(ProductOptionValue::getKey, Function.identity())));
        List<ProductOptionValue> changed = new ArrayList<>();

        for (OptionGroupDefinition definition : definitions) {
            ProductOptionGroup group = groupsByKey.get(definition.key());
            Map<String, ProductOptionValue> existingByKey = existingByGroup
                    .getOrDefault(group.getId(), Map.of());
            Set<String> requestedKeys = definition.values().stream()
                    .map(OptionValueDefinition::key)
                    .collect(Collectors.toSet());
            for (OptionValueDefinition valueDefinition : definition.values()) {
                ProductOptionValue value = existingByKey.get(valueDefinition.key());
                if (value == null) {
                    value = new ProductOptionValue(
                            group.getId(),
                            valueDefinition.key(),
                            valueDefinition.name(),
                            valueDefinition.sortOrder());
                } else {
                    value.update(valueDefinition.name(), valueDefinition.sortOrder());
                }
                changed.add(value);
            }
            for (ProductOptionValue value : existingByKey.values()) {
                if (!requestedKeys.contains(value.getKey())) {
                    value.deactivate();
                    changed.add(value);
                }
            }
        }
        return changed.isEmpty() ? existing : List.copyOf(valuePort.saveAll(changed));
    }

    private List<ProductVariant> syncVariants(
            Product product,
            Integer defaultQuantity,
            List<OptionGroupDefinition> groupDefinitions,
            List<VariantDefinition> requestedVariants,
            Map<String, ProductOptionGroup> groupsByKey,
            Map<Long, List<ProductOptionValue>> valuesByGroupId) {
        List<OptionGroupDefinition> selectDefinitions = groupDefinitions.stream()
                .filter(group -> group.type() == ProductOptionType.SELECT)
                .sorted(Comparator.comparingInt(OptionGroupDefinition::sortOrder))
                .toList();
        List<VariantDefinition> definitions = requestedVariants;
        if (selectDefinitions.isEmpty() && requestedVariants.isEmpty()) {
            if (defaultQuantity == null || defaultQuantity < 0) {
                throw invalid("주문제작 상품의 기본 조합 재고는 0 이상이어야 합니다.");
            }
            definitions = List.of(new VariantDefinition(List.of(), 0L, defaultQuantity, true));
        }

        Map<String, Map<String, ProductOptionValue>> valuesByGroupKey = new HashMap<>();
        for (OptionGroupDefinition definition : selectDefinitions) {
            ProductOptionGroup group = groupsByKey.get(definition.key());
            Map<String, ProductOptionValue> valuesByKey = valuesByGroupId
                    .getOrDefault(group.getId(), List.of())
                    .stream()
                    .collect(Collectors.toMap(ProductOptionValue::getKey, Function.identity()));
            if (valuesByKey.isEmpty()) {
                throw invalid("선택형 옵션에는 하나 이상의 옵션값이 필요합니다.");
            }
            valuesByGroupKey.put(definition.key(), valuesByKey);
        }

        List<String> expectedKeys = expectedCombinationKeys(selectDefinitions);
        if (expectedKeys.size() > ProductOptionPolicy.MAX_COMBINATIONS) {
            throw invalid("옵션 조합은 최대 " + ProductOptionPolicy.MAX_COMBINATIONS + "개까지 만들 수 있습니다.");
        }
        Map<String, VariantDefinition> definitionsByKey = new LinkedHashMap<>();
        for (VariantDefinition definition : definitions) {
            String key = combinationKey(definition.selections(), selectDefinitions, valuesByGroupKey);
            if (definitionsByKey.putIfAbsent(key, definition) != null) {
                throw invalid("같은 옵션 조합이 중복되었습니다.");
            }
        }
        if (!new HashSet<>(expectedKeys).equals(definitionsByKey.keySet())) {
            throw invalid("모든 선택형 옵션 조합의 가격과 재고를 입력해 주세요.");
        }

        long maximumTextAdjustment = groupDefinitions.stream()
                .filter(group -> group.type() == ProductOptionType.TEXT)
                .map(OptionGroupDefinition::inputPriceAdjustment)
                .filter(value -> value != null)
                .reduce(0L, ProductOptionConfigurationService::addPrice);

        List<ProductVariant> existing = variantStorePort.findByProductIdWithLock(product.getId());
        Map<String, ProductVariant> existingByKey = existing.stream()
                .collect(Collectors.toMap(ProductVariant::getCombinationKey, Function.identity()));
        List<ProductVariant> changed = new ArrayList<>();
        for (String key : expectedKeys) {
            VariantDefinition definition = definitionsByKey.get(key);
            ProductOptionPolicy.requireVariantPrice(product.getPrice(), definition.priceAdjustment());
            addPrice(Math.addExact(product.getPrice(), definition.priceAdjustment()), maximumTextAdjustment);
            ProductVariant variant = existingByKey.get(key);
            if (variant == null) {
                variant = new ProductVariant(
                        product.getId(),
                        key,
                        product.getPrice(),
                        definition.priceAdjustment(),
                        definition.quantity(),
                        definition.active(),
                        selections(definition.selections(), groupsByKey, valuesByGroupKey));
            } else {
                variant.update(
                        product.getPrice(),
                        definition.priceAdjustment(),
                        definition.quantity(),
                        definition.active());
            }
            changed.add(variant);
        }
        for (ProductVariant variant : existing) {
            if (!definitionsByKey.containsKey(variant.getCombinationKey())) {
                variant.deactivate();
                changed.add(variant);
            }
        }
        return List.copyOf(variantStorePort.saveAll(changed));
    }

    private static List<ProductVariantSelection> selections(
            List<SelectionDefinition> definitions,
            Map<String, ProductOptionGroup> groupsByKey,
            Map<String, Map<String, ProductOptionValue>> valuesByGroupKey) {
        List<ProductVariantSelection> selections = new ArrayList<>();
        for (SelectionDefinition definition : definitions) {
            ProductOptionGroup group = groupsByKey.get(definition.groupKey());
            ProductOptionValue value = valuesByGroupKey
                    .getOrDefault(definition.groupKey(), Map.of())
                    .get(definition.valueKey());
            if (group == null || value == null) {
                throw invalid("옵션 조합에 등록되지 않은 옵션값이 포함되어 있습니다.");
            }
            selections.add(new ProductVariantSelection(
                    group.getId(), value.getId(), group.getSortOrder()));
        }
        selections.sort(Comparator.comparingInt(ProductVariantSelection::getSortOrder));
        return List.copyOf(selections);
    }

    private static List<String> expectedCombinationKeys(List<OptionGroupDefinition> groups) {
        if (groups.isEmpty()) {
            return List.of(ProductVariant.DEFAULT_COMBINATION_KEY);
        }
        List<String> keys = new ArrayList<>();
        appendCombinationKey(groups, 0, new ArrayList<>(), keys);
        return List.copyOf(keys);
    }

    private static void appendCombinationKey(
            List<OptionGroupDefinition> groups,
            int index,
            List<String> parts,
            List<String> result) {
        if (index == groups.size()) {
            result.add(String.join("|", parts));
            return;
        }
        OptionGroupDefinition group = groups.get(index);
        if (!group.required()) {
            parts.add(group.key() + "=-");
            appendCombinationKey(groups, index + 1, parts, result);
            parts.removeLast();
        }
        for (OptionValueDefinition value : group.values().stream()
                .sorted(Comparator.comparingInt(OptionValueDefinition::sortOrder))
                .toList()) {
            parts.add(group.key() + "=" + value.key());
            appendCombinationKey(groups, index + 1, parts, result);
            parts.removeLast();
        }
    }

    private static String combinationKey(
            List<SelectionDefinition> selections,
            List<OptionGroupDefinition> groups,
            Map<String, Map<String, ProductOptionValue>> valuesByGroupKey) {
        if (groups.isEmpty()) {
            if (!selections.isEmpty()) {
                throw invalid("선택형 옵션이 없는 상품에는 조합 선택값을 보낼 수 없습니다.");
            }
            return ProductVariant.DEFAULT_COMBINATION_KEY;
        }
        Map<String, SelectionDefinition> selectionsByGroup = new HashMap<>();
        for (SelectionDefinition selection : selections) {
            if (selection == null || selection.groupKey() == null || selection.valueKey() == null
                    || selectionsByGroup.putIfAbsent(selection.groupKey(), selection) != null) {
                throw invalid("옵션 조합 선택값이 올바르지 않습니다.");
            }
        }
        List<String> parts = new ArrayList<>();
        for (OptionGroupDefinition group : groups) {
            SelectionDefinition selection = selectionsByGroup.remove(group.key());
            if (selection == null) {
                if (group.required()) {
                    throw invalid(group.name() + " 옵션은 필수입니다.");
                }
                parts.add(group.key() + "=-");
                continue;
            }
            if (!valuesByGroupKey.getOrDefault(group.key(), Map.of())
                    .containsKey(selection.valueKey())) {
                throw invalid("옵션 조합에 등록되지 않은 옵션값이 포함되어 있습니다.");
            }
            parts.add(group.key() + "=" + selection.valueKey());
        }
        if (!selectionsByGroup.isEmpty()) {
            throw invalid("옵션 조합에 등록되지 않은 옵션 그룹이 포함되어 있습니다.");
        }
        return String.join("|", parts);
    }

    private static void validateGroupDefinitions(List<OptionGroupDefinition> definitions) {
        long selectCount = definitions.stream()
                .filter(group -> group.type() == ProductOptionType.SELECT)
                .count();
        long textCount = definitions.stream()
                .filter(group -> group.type() == ProductOptionType.TEXT)
                .count();
        if (selectCount > ProductOptionPolicy.MAX_SELECT_GROUPS
                || textCount > ProductOptionPolicy.MAX_TEXT_GROUPS) {
            throw invalid("선택형 옵션은 최대 3개, 직접입력형 옵션은 최대 5개까지 등록할 수 있습니다.");
        }
        Set<String> groupKeys = new HashSet<>();
        Set<Integer> sortOrders = new HashSet<>();
        for (OptionGroupDefinition group : definitions) {
            if (group == null || group.type() == null
                    || !groupKeys.add(ProductOptionPolicy.requireKey(group.key(), "옵션"))
                    || !sortOrders.add(group.sortOrder())) {
                throw invalid("옵션 그룹 식별자와 정렬 순서는 중복될 수 없습니다.");
            }
            ProductOptionPolicy.requireName(group.name(), "옵션명");
            if (group.type() == ProductOptionType.TEXT && !group.values().isEmpty()) {
                throw invalid("직접입력형 옵션에는 선택값을 등록할 수 없습니다.");
            }
            Set<String> valueKeys = new HashSet<>();
            Set<Integer> valueSortOrders = new HashSet<>();
            for (OptionValueDefinition value : group.values()) {
                if (value == null
                        || !valueKeys.add(ProductOptionPolicy.requireKey(value.key(), "옵션값"))
                        || !valueSortOrders.add(value.sortOrder())) {
                    throw invalid("옵션값 식별자와 정렬 순서는 한 그룹 안에서 중복될 수 없습니다.");
                }
                ProductOptionPolicy.requireName(value.name(), "옵션값");
            }
        }
    }

    private static Map<String, ProductOptionGroup> activeGroupsByKey(
            List<ProductOptionGroup> groups) {
        return groups.stream()
                .filter(ProductOptionGroup::isActive)
                .collect(Collectors.toMap(ProductOptionGroup::getKey, Function.identity()));
    }

    private static Map<Long, List<ProductOptionValue>> activeValuesByGroupId(
            List<ProductOptionValue> values) {
        return values.stream()
                .filter(ProductOptionValue::isActive)
                .collect(Collectors.groupingBy(ProductOptionValue::getGroupId));
    }

    private static ProductOptions toOptions(
            List<ProductOptionGroup> allGroups,
            List<ProductOptionValue> allValues,
            List<ProductVariant> allVariants,
            boolean includeInactiveVariants) {
        List<ProductOptionGroup> groups = allGroups.stream()
                .filter(ProductOptionGroup::isActive)
                .sorted(Comparator.comparingInt(ProductOptionGroup::getSortOrder))
                .toList();
        Map<Long, ProductOptionGroup> groupsById = groups.stream()
                .collect(Collectors.toMap(ProductOptionGroup::getId, Function.identity()));
        Map<Long, List<ProductOptionValue>> valuesByGroup = allValues.stream()
                .filter(ProductOptionValue::isActive)
                .filter(value -> groupsById.containsKey(value.getGroupId()))
                .collect(Collectors.groupingBy(ProductOptionValue::getGroupId));
        Map<Long, ProductOptionValue> valuesById = allValues.stream()
                .filter(ProductOptionValue::isActive)
                .collect(Collectors.toMap(ProductOptionValue::getId, Function.identity()));

        List<OptionGroup> groupViews = groups.stream()
                .map(group -> new OptionGroup(
                        group.getKey(),
                        group.getType(),
                        group.getName(),
                        group.isRequired(),
                        group.getSortOrder(),
                        group.getInputPlaceholder(),
                        group.getInputMaxLength(),
                        group.getInputPriceAdjustment(),
                        valuesByGroup.getOrDefault(group.getId(), List.of()).stream()
                                .sorted(Comparator.comparingInt(ProductOptionValue::getSortOrder))
                                .map(value -> new OptionValue(
                                        value.getKey(), value.getName(), value.getSortOrder()))
                                .toList()))
                .toList();

        List<Variant> variantViews = allVariants.stream()
                .filter(variant -> includeInactiveVariants || variant.isActive())
                .filter(variant -> variant.getSelections().stream()
                        .allMatch(selection -> groupsById.containsKey(selection.getOptionGroupId())
                                && valuesById.containsKey(selection.getOptionValueId())))
                .map(variant -> new Variant(
                        variant.getId(),
                        variant.getPriceAdjustment(),
                        variant.getQuantity(),
                        variant.isActive(),
                        variant.getSelections().stream()
                                .map(selection -> new Selection(
                                        groupsById.get(selection.getOptionGroupId()).getKey(),
                                        valuesById.get(selection.getOptionValueId()).getKey()))
                                .toList()))
                .toList();
        return new ProductOptions(groupViews, variantViews);
    }

    private static long addPrice(long left, long right) {
        try {
            long result = Math.addExact(left, right);
            if (result > PaymentAmountPolicy.MAX_AMOUNT) {
                throw invalid("옵션을 반영한 상품 가격이 허용 범위를 벗어났습니다.");
            }
            return result;
        } catch (ArithmeticException exception) {
            throw invalid("옵션을 반영한 상품 가격이 허용 범위를 벗어났습니다.");
        }
    }

    private static HappyGalleryException invalid(String message) {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT, message);
    }
}
