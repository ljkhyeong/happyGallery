package com.personal.happygallery.app.product;

import com.personal.happygallery.app.product.port.out.InventoryReaderPort;
import com.personal.happygallery.app.product.port.out.ProductReaderPort;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductQueryService {

    private final ProductReaderPort productReaderPort;
    private final InventoryReaderPort inventoryReaderPort;

    public ProductQueryService(ProductReaderPort productReaderPort,
                               InventoryReaderPort inventoryReaderPort) {
        this.productReaderPort = productReaderPort;
        this.inventoryReaderPort = inventoryReaderPort;
    }

    public record ProductWithInventory(Product product, Inventory inventory) {}

    /** 상품 단건 조회 */
    public ProductWithInventory getProduct(Long productId) {
        Product product = productReaderPort.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품"));
        Inventory inventory = inventoryReaderPort.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException("재고"));
        return new ProductWithInventory(product, inventory);
    }

    /** ACTIVE 상품 목록 조회 — 최신 등록순 */
    public List<ProductWithInventory> listActiveProducts() {
        return productReaderPort.findByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE)
                .stream()
                .map(p -> {
                    Inventory inv = inventoryReaderPort.findByProductId(p.getId())
                            .orElseThrow(() -> new NotFoundException("재고"));
                    return new ProductWithInventory(p, inv);
                })
                .toList();
    }
}
