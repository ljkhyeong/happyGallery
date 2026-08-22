package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;

@Service
@Transactional(readOnly = true)
public class DefaultProductQueryService implements ProductQueryUseCase {

    private final ProductReaderPort productReaderPort;
    private final InventoryReaderPort inventoryReaderPort;

    public DefaultProductQueryService(ProductReaderPort productReaderPort,
                                      InventoryReaderPort inventoryReaderPort) {
        this.productReaderPort = productReaderPort;
        this.inventoryReaderPort = inventoryReaderPort;
    }

    /** 상품 단건 조회 */
    @Override
    public ProductWithInventory getProduct(Long productId) {
        Product product = productReaderPort.findActiveById(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        Inventory inventory = inventoryReaderPort.findByProductId(productId)
                .orElseThrow(NotFoundException.supplier("재고"));
        return new ProductWithInventory(product, inventory);
    }

    /** ACTIVE 상품 목록 조회 — 최신 등록순 (N+1 방지: 재고 일괄 조회) */
    @Override
    public List<ProductWithInventory> listActiveProducts() {
        List<Product> products = productReaderPort.findActiveProductsByCreatedAtDesc();
        return toProductWithInventoryList(products);
    }

    @Override
    public List<ProductWithInventory> listAllProducts() {
        return toProductWithInventoryList(productReaderPort.findAllProductsByCreatedAtDesc());
    }

    /** 필터 조건에 따른 ACTIVE 상품 목록 조회. */
    @Override
    public List<ProductWithInventory> listActiveProducts(ProductFilter filter) {
        if (filter.isDefault()) {
            return listActiveProducts();
        }
        List<Product> products = productReaderPort.findActiveByFilter(filter);
        return toProductWithInventoryList(products);
    }

    /** ACTIVE 상품에 존재하는 카테고리 목록. */
    @Override
    public List<String> listActiveCategories() {
        return productReaderPort.findDistinctActiveCategories();
    }

    private List<ProductWithInventory> toProductWithInventoryList(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, Inventory> inventoryMap = inventoryReaderPort.findByProductIdIn(productIds)
                .stream()
                .collect(toMap(Inventory::getProductId, Function.identity()));

        return products.stream()
                .map(p -> {
                    Inventory inv = inventoryMap.get(p.getId());
                    if (inv == null) {
                        throw new NotFoundException("재고");
                    }
                    return new ProductWithInventory(p, inv);
                })
                .toList();
    }
}
