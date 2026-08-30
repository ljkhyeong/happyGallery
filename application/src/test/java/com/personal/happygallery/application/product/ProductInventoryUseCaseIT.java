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
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.OptionGroupDefinition;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.OptionValueDefinition;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.SaveProductCommand;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.SelectionDefinition;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.VariantDefinition;
import com.personal.happygallery.application.product.ProductVariantStockService.VariantAdjustment;
import com.personal.happygallery.adapter.out.persistence.product.InventoryRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
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
