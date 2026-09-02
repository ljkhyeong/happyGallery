package com.personal.happygallery.application.product;

import com.jayway.jsonpath.JsonPath;
import com.personal.happygallery.domain.error.InventoryNotEnoughException;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.InventoryAdjustment;
import com.personal.happygallery.domain.product.InventoryAdjustmentType;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.ProductOptionType;
import com.personal.happygallery.domain.product.ProductVariant;
import com.personal.happygallery.domain.product.SmartStoreStockMapping;
import com.personal.happygallery.domain.product.SmartStoreStockSyncStatus;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.OptionGroupDefinition;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.OptionValueDefinition;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.SaveProductCommand;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.SelectionDefinition;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.VariantDefinition;
import com.personal.happygallery.application.product.ProductVariantStockService.VariantAdjustment;
import com.personal.happygallery.adapter.out.persistence.product.InventoryRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductVariantRepository;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.DeleteMappingCommand;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.MappingActor;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.SaveMappingCommand;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.VariantMapping;
import com.personal.happygallery.application.product.port.out.SmartStoreStockMappingPort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncPort;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.OptionStock;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [UseCaseIT] 상품 등록 + 재고 차감 + 동시성 방지 검증.
 *
 * <p>Proof (docs/PRD/0001_기준_스펙/spec.md §8.1): 단일 작품(quantity=1) 재고를 순차 차감 시
 * 첫 번째 차감은 성공하고 두 번째는 {@link InventoryNotEnoughException}으로 실패한다.
 */
@UseCaseIT
class ProductInventoryUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository productRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired InventoryService inventoryService;
    @Autowired ProductVariantStockService variantStockService;
    @Autowired ProductAdminUseCase productAdminUseCase;
    @Autowired ProductOptionConfigurationService optionConfigurationService;
    @Autowired ProductVariantRepository variantRepository;
    @Autowired SmartStoreInventoryUseCase smartStoreInventoryUseCase;
    @Autowired SmartStoreInventoryMappingService smartStoreInventoryMappingService;
    @Autowired SmartStoreStockMappingPort mappingPort;
    @Autowired SmartStoreStockSyncPort stockSyncPort;
    @Autowired SmartStoreStockSyncTransactionService stockSyncTransactionService;
    @Autowired Clock clock;
    @Autowired TestCleanupSupport cleanupSupport;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearProductData();
    }

    @Test
    @DisplayName("기본 조합 상품 정보를 수정해도 판매 후 재고를 보존하고 재고 조정은 이력을 남긴다")
    void updateDefaultVariant_preservesStockAfterSale() {
        var draft = List.of(new VariantDefinition(List.of(), 2000L, 5, true));
        var registered = productAdminUseCase.register(madeToOrderCommand(List.of(), draft));
        Long productId = registered.product().getId();
        Long variantId = registered.options().variants().getFirst().id();
        variantStockService.deductAll(List.of(new VariantAdjustment(variantId, 1)));

        var updated = productAdminUseCase.update(productId, madeToOrderCommand(List.of(), draft));
        assertSoftly(softly -> {
            softly.assertThat(updated.quantity()).isEqualTo(4);
            softly.assertThat(updated.options().variants().getFirst().id()).isEqualTo(variantId);
            softly.assertThat(updated.options().variants().getFirst().priceAdjustment()).isEqualTo(2000L);
        });

        var adjustment = productAdminUseCase.adjustInventory(new ProductAdminUseCase.AdjustInventoryCommand(
                productId, variantId, InventoryAdjustmentType.INCREASE, 2,
                "제작 가능 수량 추가", null, "local-api-key"));
        assertSoftly(softly -> {
            softly.assertThat(adjustment.getQuantityBefore()).isEqualTo(4);
            softly.assertThat(adjustment.getQuantityAfter()).isEqualTo(6);
            softly.assertThat(productAdminUseCase.listRecentInventoryAdjustments(productId))
                    .extracting(InventoryAdjustment::getId).containsExactly(adjustment.getId());
        });
    }

    @Test
    @DisplayName("선택형 표시 순서와 가격을 바꿔도 조합 번호와 재고를 보존하고 새 조합만 최초 재고를 받는다")
    void updateSelectVariants_preservesIdentityAndStock() {
        var groups = List.of(selectGroup("size", 0, false, "large"), selectGroup("color", 1, true, "red"));
        var red = List.of(new SelectionDefinition("color", "red"));
        var redLarge = List.of(new SelectionDefinition("color", "red"), new SelectionDefinition("size", "large"));
        var draft = List.of(new VariantDefinition(red, 0L, 3, true),
                new VariantDefinition(redLarge, 1000L, 5, true));
        var registered = productAdminUseCase.register(madeToOrderCommand(groups, draft));
        var existing = registered.options().variants();
        Long largeId = existing.stream().filter(variant -> variant.selections().size() == 2)
                .findFirst().orElseThrow().id();
        variantStockService.deductAll(List.of(new VariantAdjustment(largeId, 1)));

        var reordered = List.of(selectGroup("color", 0, true, "red"), selectGroup("size", 1, false, "large", "small"));
        var updated = productAdminUseCase.update(registered.product().getId(), madeToOrderCommand(reordered,
                List.of(draft.getFirst(), new VariantDefinition(redLarge, 2500L, 5, true),
                        new VariantDefinition(List.of(new SelectionDefinition("color", "red"),
                                new SelectionDefinition("size", "small")), 500L, 7, true))));
        var large = updated.options().variants().stream().filter(variant -> variant.id().equals(largeId))
                .findFirst().orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(updated.options().variants()).hasSize(3);
            softly.assertThat(updated.options().variants()).extracting(ProductOptions.Variant::id)
                    .containsAll(existing.stream().map(ProductOptions.Variant::id).toList());
            softly.assertThat(large.quantity()).isEqualTo(4);
            softly.assertThat(large.priceAdjustment()).isEqualTo(2500L);
            softly.assertThat(large.active()).isTrue();
            softly.assertThat(updated.quantity()).isEqualTo(14);
        });
    }

    @Test
    @DisplayName("옵션 추가와 필수 전환 및 삭제 뒤 재저장해도 과거 조합이 편집 목록에 섞이지 않는다")
    void updateOptionStructure_returnsOnlyCurrentVariants() {
        var registered = productAdminUseCase.register(madeToOrderCommand(List.of(), List.of()));
        Long productId = registered.product().getId();
        Long defaultId = registered.options().variants().getFirst().id();
        var red = List.of(new SelectionDefinition("color", "red"));
        var optionalGroups = List.of(selectGroup("color", 0, false, "red"));
        var optional = productAdminUseCase.update(productId, madeToOrderCommand(optionalGroups, List.of(
                new VariantDefinition(List.of(), 0, 3, true),
                new VariantDefinition(red, 1000, 4, false))));
        var reloaded = optionConfigurationService.get(productId, true);
        var resaved = productAdminUseCase.update(productId, madeToOrderCommand(optionalGroups, variantsFrom(reloaded)));
        assertSoftly(softly -> {
            softly.assertThat(optional.options().variants()).hasSize(2);
            softly.assertThat(reloaded).isEqualTo(optional.options());
            softly.assertThat(resaved.options()).isEqualTo(optional.options());
            softly.assertThat(resaved.options().variants()).extracting(ProductOptions.Variant::id).doesNotContain(defaultId);
            softly.assertThat(resaved.options().variants()).filteredOn(variant -> !variant.active()).hasSize(1);
        });

        var requiredGroups = List.of(selectGroup("color", 0, true, "red"));
        productAdminUseCase.update(productId, madeToOrderCommand(requiredGroups,
                List.of(new VariantDefinition(red, 1000, 4, false))));
        var required = productAdminUseCase.update(productId, madeToOrderCommand(requiredGroups,
                variantsFrom(optionConfigurationService.get(productId, true))));
        assertThat(required.options().variants()).hasSize(1);

        productAdminUseCase.update(productId, madeToOrderCommand(List.of(), List.of()));
        var removed = productAdminUseCase.update(productId, madeToOrderCommand(List.of(),
                variantsFrom(optionConfigurationService.get(productId, true))));
        assertSoftly(softly -> {
            softly.assertThat(removed.options().variants()).extracting(ProductOptions.Variant::id).containsExactly(defaultId);
            softly.assertThat(variantRepository.findWithSelectionsByProductId(productId)).hasSize(3);
        });
    }

    @Test
    @DisplayName("현재 조합만 다시 매핑하고 과거 연결은 재고 0으로 보내며 원격 옵션 재사용 시 중복 전송하지 않는다")
    void saveSmartStoreMapping_preservesRetiredOptionsForZeroStock() {
        var registered = productAdminUseCase.register(madeToOrderCommand(List.of(), List.of()));
        Long productId = registered.product().getId();
        Long defaultId = registered.options().variants().getFirst().id();
        smartStoreInventoryUseCase.saveMapping(productId,
                new SaveMappingCommand(123L, true, List.of(new VariantMapping(defaultId, 100L))));
        variantRepository.saveAndFlush(new ProductVariant(productId, "legacy-variant:old", 10000L,
                0, 2, false, List.of()));

        var groups = List.of(selectGroup("color", 0, true, "red", "blue"));
        var updated = productAdminUseCase.update(productId, madeToOrderCommand(groups, List.of(
                new VariantDefinition(List.of(new SelectionDefinition("color", "red")), 1000, 4, true),
                new VariantDefinition(List.of(new SelectionDefinition("color", "blue")), 2000, 3, false))));
        var current = updated.options().variants();
        Long redId = current.get(0).id();
        Long blueId = current.get(1).id();
        assertThatThrownBy(() -> smartStoreInventoryUseCase.saveMapping(productId,
                mappingCommand(productId, 123L, true, List.of(new VariantMapping(redId, 101L)))))
                .isInstanceOf(IllegalArgumentException.class);
        smartStoreInventoryUseCase.saveMapping(productId, mappingCommand(productId, 123L, true,
                List.of(new VariantMapping(redId, 101L), new VariantMapping(blueId, 102L))));

        var claimed = stockSyncTransactionService.claim(productId, LocalDateTime.now(clock)).orElseThrow();
        var preview = stockSyncTransactionService.productSnapshot(productId);
        assertSoftly(softly -> {
            softly.assertThat(claimed.configurationError()).isNull();
            softly.assertThat(claimed.command().options()).containsExactlyInAnyOrder(
                    new OptionStock(100L, 0), new OptionStock(101L, 4), new OptionStock(102L, 0));
            softly.assertThat(preview.options()).filteredOn(option -> option.productVariantId().equals(defaultId))
                    .allMatch(option -> option.stockQuantity() == 0 && !option.usable());
        });

        variantStockService.restoreAll(List.of(new VariantAdjustment(defaultId, 1)));
        assertThat(stockSyncTransactionService.productSnapshot(productId).options())
                .filteredOn(option -> option.productVariantId().equals(defaultId))
                .allMatch(option -> option.stockQuantity() == 0);
        var replaced = productAdminUseCase.update(productId,
                madeToOrderCommand(List.of(selectGroup("color", 0, true, "blue", "green")), List.of(
                        new VariantDefinition(List.of(new SelectionDefinition("color", "blue")), 2000, 3, false),
                        new VariantDefinition(List.of(new SelectionDefinition("color", "green")), 1000, 2, true))));
        Long greenId = replaced.options().variants().stream().filter(variant -> !variant.id().equals(blueId))
                .findFirst().orElseThrow().id();
        smartStoreInventoryUseCase.saveMapping(productId, mappingCommand(productId, 123L, true,
                List.of(new VariantMapping(greenId, 100L), new VariantMapping(blueId, 102L))));
        assertThat(mappingPort.findByProductIdOrderByProductVariantIdAsc(productId))
                .extracting(SmartStoreStockMapping::getProductVariantId).containsExactlyInAnyOrder(redId, blueId, greenId);
        assertThat(stockSyncTransactionService.productSnapshot(productId).options())
                .extracting(option -> new OptionStock(option.optionId(), option.stockQuantity()))
                .containsExactlyInAnyOrder(new OptionStock(100L, 2), new OptionStock(101L, 0), new OptionStock(102L, 0));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("재등록 전 응답은 진행 중 전송을 다시 요청하되 기존 대기와 재시도는 유지한다")
    void reenableSmartStoreMapping_requeuesInFlightWriteAndPreservesRetries(boolean deleteMapping) {
        var registered = productAdminUseCase.register(madeToOrderCommand(List.of(), List.of()));
        Long productId = registered.product().getId();
        Long variantId = registered.options().variants().getFirst().id();
        var command = new SaveMappingCommand(123L, true, List.of(new VariantMapping(variantId, 100L)));
        smartStoreInventoryUseCase.saveMapping(productId, command);
        LocalDateTime now = LocalDateTime.now(clock);
        var previous = stockSyncTransactionService.claim(productId, now).orElseThrow();

        if (deleteMapping) {
            deleteMapping(productId);
        } else {
            smartStoreInventoryUseCase.saveMapping(productId,
                    mappingCommand(productId, 123L, false, command.variants()));
        }
        smartStoreInventoryUseCase.saveMapping(productId, mappingCommand(
                productId, 123L, true, List.of(new VariantMapping(variantId, 101L))));
        var pending = stockSyncPort.findByProductId(productId).orElseThrow();
        stockSyncTransactionService.finish(productId, previous.generation(), previous.version(), true, null, now);
        stockSyncTransactionService.finish(productId, previous.generation(), previous.version(), false, "이전 전송 실패", now);
        var afterOldResponse = stockSyncPort.findByProductId(productId).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(pending.getGeneration()).isNotEqualTo(previous.generation());
            softly.assertThat(pending.getRequestVersion()).isEqualTo(previous.version());
            softly.assertThat(afterOldResponse.getStatus()).isEqualTo(SmartStoreStockSyncStatus.PENDING);
            softly.assertThat(afterOldResponse.getAttemptCount()).isZero();
            softly.assertThat(afterOldResponse.getNextAttemptAt()).isEqualTo(pending.getNextAttemptAt());
        });

        var current = stockSyncTransactionService.claim(productId, LocalDateTime.now(clock)).orElseThrow();
        assertThat(current.command().options()).contains(new OptionStock(101L, 9));
        stockSyncTransactionService.finish(productId, previous.generation(), previous.version(), true, null, now);
        var correction = stockSyncPort.findByProductId(productId).orElseThrow();
        assertThat(correction.getRequestVersion()).isGreaterThan(current.version());
        stockSyncTransactionService.finish(productId, current.generation(), current.version(), true, null, now);
        assertThat(stockSyncPort.findByProductId(productId).orElseThrow().getStatus())
                .isEqualTo(SmartStoreStockSyncStatus.PENDING);

        var corrected = stockSyncTransactionService.claim(productId, LocalDateTime.now(clock)).orElseThrow();
        stockSyncTransactionService.finish(productId, corrected.generation(), corrected.version(), false, "새 전송 실패", now);
        stockSyncTransactionService.finish(productId, previous.generation(), previous.version(), true, null, now);
        stockSyncTransactionService.finish(productId, previous.generation(), previous.version(), false, "이전 전송 실패", now);
        var retry = stockSyncPort.findByProductId(productId).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(retry.getStatus()).isEqualTo(SmartStoreStockSyncStatus.PENDING);
            softly.assertThat(retry.getAttemptCount()).isEqualTo(1);
            softly.assertThat(retry.getLastError()).isEqualTo("새 전송 실패");
            softly.assertThat(retry.getNextAttemptAt()).isEqualTo(now.plusMinutes(1));
        });
        var retried = stockSyncTransactionService.claim(productId, retry.getNextAttemptAt()).orElseThrow();
        stockSyncTransactionService.finish(productId, retried.generation(), retried.version(), true, null, retry.getNextAttemptAt());
        assertThat(stockSyncPort.findByProductId(productId).orElseThrow().getStatus())
                .isEqualTo(SmartStoreStockSyncStatus.SYNCED);
    }

    @Test
    @DisplayName("같은 조합의 연결을 바꾸면 이전 옵션은 0개로 재시도하고 현재 연결만 편집 응답에 표시한다")
    void remapSmartStoreOption_preservesZeroStockUntilOptionIsReused() {
        var registered = productAdminUseCase.register(madeToOrderCommand(List.of(), List.of()));
        Long productId = registered.product().getId();
        Long variantId = registered.options().variants().getFirst().id();
        smartStoreInventoryUseCase.saveMapping(productId,
                new SaveMappingCommand(123L, true, List.of(new VariantMapping(variantId, 100L))));
        LocalDateTime now = LocalDateTime.now(clock);
        var initial = stockSyncTransactionService.claim(productId, now).orElseThrow();
        stockSyncTransactionService.finish(productId, initial.generation(), initial.version(), true, null, now);

        var saved = smartStoreInventoryUseCase.saveMapping(productId,
                mappingCommand(productId, 123L, true, List.of(new VariantMapping(variantId, 101L))));
        var changed = stockSyncTransactionService.claim(productId, now).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(saved.variants()).containsExactly(new VariantMapping(variantId, 101L));
            softly.assertThat(smartStoreInventoryUseCase.getMapping(productId).orElseThrow().variants())
                    .isEqualTo(saved.variants());
            softly.assertThat(changed.command().options())
                    .containsExactlyInAnyOrder(new OptionStock(100L, 0), new OptionStock(101L, 9));
            softly.assertThat(mappingPort.findByOriginProductNoAndOptionId(123L, 100L).orElseThrow().getProductVariantId())
                    .isEqualTo(variantId);
        });
        stockSyncTransactionService.finish(productId, changed.generation(), changed.version(), false, "연동 지연", now);
        var retry = stockSyncTransactionService.claim(productId, now.plusMinutes(1)).orElseThrow();
        assertThat(retry.command()).isEqualTo(changed.command());
        stockSyncTransactionService.finish(productId, retry.generation(), retry.version(), true, null, now.plusMinutes(1));

        variantStockService.restoreAll(List.of(new VariantAdjustment(variantId, 1)));
        assertThat(stockSyncTransactionService.productSnapshot(productId).options())
                .filteredOn(option -> option.optionId().equals(100L))
                .singleElement().satisfies(option -> assertSoftly(softly -> {
                    softly.assertThat(option.stockQuantity()).isZero();
                    softly.assertThat(option.price()).isZero();
                    softly.assertThat(option.usable()).isFalse();
        }));
        smartStoreInventoryUseCase.saveMapping(productId,
                mappingCommand(productId, 123L, true, List.of(new VariantMapping(variantId, 102L))));
        var reused = smartStoreInventoryUseCase.saveMapping(productId,
                mappingCommand(productId, 123L, true, List.of(new VariantMapping(variantId, 100L))));
        assertThat(reused.variants()).containsExactly(new VariantMapping(variantId, 100L));
        assertThat(stockSyncTransactionService.claim(productId, now.plusMinutes(1)).orElseThrow().command().options())
                .containsExactlyInAnyOrder(new OptionStock(100L, 10), new OptionStock(101L, 0), new OptionStock(102L, 0));
    }

    private static List<VariantDefinition> variantsFrom(ProductOptions options) {
        return options.variants().stream().map(variant -> new VariantDefinition(
                variant.selections().stream().map(selection -> new SelectionDefinition(
                        selection.groupKey(), selection.valueKey())).toList(),
                variant.priceAdjustment(), variant.quantity(), variant.active())).toList();
    }

    private SaveMappingCommand mappingCommand(
            Long productId, Long originProductNo, boolean enabled, List<VariantMapping> variants) {
        var current = smartStoreInventoryUseCase.getMapping(productId);
        boolean originChanged = current.isPresent()
                && !current.orElseThrow().originProductNo().equals(originProductNo);
        return new SaveMappingCommand(
                originProductNo,
                enabled,
                variants,
                current.map(SmartStoreInventoryUseCase.MappingResult::mappingVersion).orElse(null),
                originChanged);
    }

    private void deleteMapping(Long productId) {
        var current = smartStoreInventoryUseCase.getMapping(productId).orElseThrow();
        smartStoreInventoryMappingService.deleteMapping(
                productId,
                new DeleteMappingCommand(current.mappingVersion(), true),
                MappingActor.system());
    }

    private static SaveProductCommand madeToOrderCommand(
            List<OptionGroupDefinition> groups, List<VariantDefinition> variants) {
        return new SaveProductCommand("주문제작 키링", ProductType.MADE_TO_ORDER, null,
                10000L, 9, null, null, "가죽 키링", null, 3, groups, variants);
    }

    private static OptionGroupDefinition selectGroup(String key, int sortOrder, boolean required, String... values) {
        return new OptionGroupDefinition(key, ProductOptionType.SELECT, key, required, sortOrder,
                null, null, null, IntStream.range(0, values.length)
                        .mapToObj(index -> new OptionValueDefinition(values[index], values[index], index)).toList());
    }

    // -----------------------------------------------------------------------
    // Proof: 상품 등록 → 201, DB에 inventory row 생성
    // -----------------------------------------------------------------------

    @DisplayName("상품 등록과 콘텐츠 수정 결과가 공개 상세에 반영된다")
    @Test
    void registerProduct_success_createsInventory() throws Exception {
        String resp = mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "나무 수납함",
                                  "type": "READY_STOCK",
                                  "category": " wood ",
                                  "price": 35000,
                                  "quantity": 1,
                                  "description": "월넛 원목으로 만든 수납함",
                                  "imageUrl": "https://images.example.com/wood-box.jpg"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("나무 수납함"))
                .andExpect(jsonPath("$.type").value("READY_STOCK"))
                .andExpect(jsonPath("$.category").value("WOOD"))
                .andExpect(jsonPath("$.price").value(35000))
                .andExpect(jsonPath("$.description").value("월넛 원목으로 만든 수납함"))
                .andExpect(jsonPath("$.imageUrl").value("https://images.example.com/wood-box.jpg"))
                .andExpect(jsonPath("$.specification").doesNotExist())
                .andExpect(jsonPath("$.careInstructions").doesNotExist())
                .andExpect(jsonPath("$.productionLeadDays").doesNotExist())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.quantity").value(1))
                .andReturn().getResponse().getContentAsString();

        Long productId = ((Number) JsonPath.read(resp, "$.id")).longValue();

        // Proof: DB에 inventory row 생성 확인
        Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(inventory.getQuantity()).isEqualTo(1);
            softly.assertThat(inventory.isAvailable()).isTrue();
            softly.assertThat(productRepository.findById(productId).orElseThrow().getCategory()).isEqualTo("WOOD");
        });

        mockMvc.perform(get("/api/v1/products").param("category", "wood"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(productId))
                .andExpect(jsonPath("$[0].category").value("WOOD"));

        mockMvc.perform(patch("/api/v1/admin/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "나무 수납함 L",
                                  "category": "wood",
                                  "price": 39000,
                                  "description": "크기와 마감 정보를 보완한 설명",
                                  "imageUrl": "https://images.example.com/wood-box-large.jpg"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("나무 수납함 L"))
                .andExpect(jsonPath("$.price").value(39000));

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("크기와 마감 정보를 보완한 설명"))
                .andExpect(jsonPath("$.imageUrl")
                        .value("https://images.example.com/wood-box-large.jpg"));
    }

    @DisplayName("주문제작 상품은 고정 사양과 제작 기간을 필수로 등록하고 공개 상세에 표시한다")
    @Test
    void registerMadeToOrder_requiresAndExposesPurchaseTerms() throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "주문제작 원목 트레이",
                                  "type": "MADE_TO_ORDER",
                                  "category": "wood",
                                  "price": 89000,
                                  "quantity": 3,
                                  "specification": "재료: 월넛\\n크기: 30 x 20 cm\\n사양: 천연 오일 마감",
                                  "careInstructions": "물기를 바로 닦고 직사광선을 피하세요.",
                                  "productionLeadDays": 21
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specification")
                        .value("재료: 월넛\n크기: 30 x 20 cm\n사양: 천연 오일 마감"))
                .andExpect(jsonPath("$.careInstructions")
                        .value("물기를 바로 닦고 직사광선을 피하세요."))
                .andExpect(jsonPath("$.productionLeadDays").value(21))
                .andReturn().getResponse().getContentAsString();
        Long productId = ((Number) JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specification")
                        .value("재료: 월넛\n크기: 30 x 20 cm\n사양: 천연 오일 마감"))
                .andExpect(jsonPath("$.productionLeadDays").value(21));

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "사양 없는 주문제작",
                                  "type": "MADE_TO_ORDER",
                                  "price": 89000,
                                  "quantity": 1,
                                  "productionLeadDays": 21
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "제작 기간이 있는 기성품",
                                  "type": "READY_STOCK",
                                  "price": 89000,
                                  "quantity": 1,
                                  "productionLeadDays": 10
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // Proof: GET /api/v1/products/{id} → available 필드 포함
    // -----------------------------------------------------------------------

    @DisplayName("상품 조회 시 재고 가용 여부가 표시된다")
    @Test
    void getProduct_showsAvailability() throws Exception {
        Product product = productRepository.save(readyStockProduct("향수 키트", 48000L));
        inventoryRepository.save(inventory(product, 1));

        mockMvc.perform(get("/api/v1/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("향수 키트"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @DisplayName("판매 중지 상품은 공개 상세에서 숨기고 관리자 목록에는 남긴다")
    @Test
    void inactiveProduct_isHiddenFromPublicButVisibleToAdmin() throws Exception {
        Product product = productRepository.save(readyStockProduct("판매 중지 작품", 48000L));
        inventoryRepository.save(inventory(product, 1));

        productAdminUseCase.changeStatus(product.getId(), ProductStatus.INACTIVE);

        mockMvc.perform(get("/api/v1/products/{id}", product.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mockMvc.perform(get("/api/v1/admin/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId()))
                .andExpect(jsonPath("$[0].status").value("INACTIVE"))
                .andExpect(jsonPath("$[0].available").value(false));
    }

    @DisplayName("먼저 읽은 상품 정보는 다른 관리자의 수정 결과를 덮어쓰지 못한다")
    @Test
    void updateProduct_withStaleVersion_throwsOptimisticLockFailure() {
        Product staleProduct = productRepository.save(
                readyStockProduct("동시 수정 작품", 48_000L));
        inventoryRepository.save(inventory(staleProduct, 1));

        productAdminUseCase.update(
                staleProduct.getId(),
                new ProductAdminUseCase.SaveProductCommand(
                        "먼저 반영된 이름", ProductType.READY_STOCK, null,
                        50_000L, null, null, null, null, null, null,
                        List.of(), List.of()));
        staleProduct.updateDetails("뒤늦게 저장한 이름", null, 52_000L, null, null);

        assertThatThrownBy(() -> productRepository.saveAndFlush(staleProduct))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(productRepository.findById(staleProduct.getId()).orElseThrow().getName())
                .isEqualTo("먼저 반영된 이름");
    }

    // -----------------------------------------------------------------------
    // Proof: 재고 차감 후 quantity=0, isAvailable=false
    // -----------------------------------------------------------------------

    @DisplayName("재고 1개를 차감하면 수량이 0이 된다")
    @Test
    void deductInventory_once_quantityBecomesZero() {
        Product product = productRepository.save(readyStockProduct("단일 작품", 50000L));
        inventoryRepository.save(inventory(product, 1));

        inventoryService.deduct(product.getId(), 1);

        Inventory updated = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(updated.getQuantity()).isEqualTo(0);
            softly.assertThat(updated.isAvailable()).isFalse();
        });
    }

    // -----------------------------------------------------------------------
    // Proof: 재고 없을 때 차감 → InventoryNotEnoughException (409)
    // -----------------------------------------------------------------------

    @DisplayName("품절 상태에서 재고 차감 시 예외가 발생한다")
    @Test
    void deductInventory_whenOutOfStock_throwsException() {
        Product product = productRepository.save(readyStockProduct("품절 작품", 50000L));
        inventoryRepository.save(inventory(product, 0));

        assertThatThrownBy(() -> inventoryService.deduct(product.getId(), 1))
                .isInstanceOf(InventoryNotEnoughException.class);
    }

    // -----------------------------------------------------------------------
    // Proof (DoD §8.1): 단일 작품 순차 중복 차감 — 1번만 성공, 2번째는 실패
    // -----------------------------------------------------------------------

    @DisplayName("재고 연속 차감 시 두 번째 호출은 실패한다")
    @Test
    void deductInventory_sequential_secondCallFails() {
        Product product = productRepository.save(readyStockProduct("단일 작품(동시성)", 60000L));
        inventoryRepository.save(inventory(product, 1));

        // 첫 번째 차감 성공
        inventoryService.deduct(product.getId(), 1);

        // 두 번째 차감 실패 — 재고 없음
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> inventoryService.deduct(product.getId(), 1))
                    .isInstanceOf(InventoryNotEnoughException.class);

            // 재고가 0으로 유지됨 (음수로 내려가지 않음)
            Inventory inv = inventoryRepository.findByProductId(product.getId()).orElseThrow();
            softly.assertThat(inv.getQuantity()).isEqualTo(0);
        });
    }

    @DisplayName("수동 재고 조정은 수량을 음수로 만들지 않고 성공한 변경만 이력에 남긴다")
    @Test
    void manualAdjustment_preservesQuantityAndHistory() {
        Product product = productRepository.save(readyStockProduct("오프라인 공유 재고", 60000L));
        inventoryRepository.save(inventory(product, 5));

        InventoryAdjustment adjustment = productAdminUseCase.adjustInventory(
                new ProductAdminUseCase.AdjustInventoryCommand(
                        product.getId(),
                        null,
                        InventoryAdjustmentType.DECREASE,
                        2,
                        "오프라인 매장 판매",
                        null,
                        "local-api-key"));

        assertThatThrownBy(() -> productAdminUseCase.adjustInventory(
                new ProductAdminUseCase.AdjustInventoryCommand(
                        product.getId(),
                        null,
                        InventoryAdjustmentType.DECREASE,
                        4,
                        "재고보다 큰 수량 차감",
                        null,
                        "local-api-key")))
                .isInstanceOf(InventoryNotEnoughException.class);

        Inventory current = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(current.getQuantity()).isEqualTo(3);
            softly.assertThat(adjustment.getQuantityBefore()).isEqualTo(5);
            softly.assertThat(adjustment.getQuantityAfter()).isEqualTo(3);
            softly.assertThat(adjustment.getReason()).isEqualTo("오프라인 매장 판매");
            softly.assertThat(adjustment.getAdjustedAt()).isNotNull();
            softly.assertThat(productAdminUseCase.listRecentInventoryAdjustments(product.getId()))
                    .extracting(InventoryAdjustment::getId)
                    .containsExactly(adjustment.getId());
        });
    }
}
