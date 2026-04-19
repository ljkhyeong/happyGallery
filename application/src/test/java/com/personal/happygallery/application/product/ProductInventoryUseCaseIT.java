package com.personal.happygallery.application.product;

import com.jayway.jsonpath.JsonPath;
import com.personal.happygallery.domain.error.InventoryNotEnoughException;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.adapter.out.persistence.product.InventoryRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;
import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static com.personal.happygallery.support.TestDataCleaner.clearProductData;
import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @BeforeEach
    void setUp() {
        clearProductData(inventoryRepository, productRepository);
    }

    // -----------------------------------------------------------------------
    // Proof: 상품 등록 → 201, DB에 inventory row 생성
    // -----------------------------------------------------------------------

    @DisplayName("상품 등록 성공 시 재고 레코드가 함께 생성된다")
    @Test
    void registerProduct_success_createsInventory() throws Exception {
        String resp = mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "나무 수납함",
                                  "type": "READY_STOCK",
                                  "price": 35000,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("나무 수납함"))
                .andExpect(jsonPath("$.type").value("READY_STOCK"))
                .andExpect(jsonPath("$.price").value(35000))
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
        });
    }

    // -----------------------------------------------------------------------
    // Proof: GET /products/{id} → available 필드 포함
    // -----------------------------------------------------------------------

    @DisplayName("상품 조회 시 재고 가용 여부가 표시된다")
    @Test
    void getProduct_showsAvailability() throws Exception {
        Product product = productRepository.save(readyStockProduct("향수 키트", 48000L));
        inventoryRepository.save(inventory(product, 1));

        mockMvc.perform(get("/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("향수 키트"))
                .andExpect(jsonPath("$.available").value(true));
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
        assertThatThrownBy(() -> inventoryService.deduct(product.getId(), 1))
                .isInstanceOf(InventoryNotEnoughException.class);

        // 재고가 0으로 유지됨 (음수로 내려가지 않음)
        Inventory inv = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        assertThat(inv.getQuantity()).isEqualTo(0);
    }
}
