package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
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
    private final ProductOptionConfigurationService optionConfigurationService;

    public DefaultProductQueryService(ProductReaderPort productReaderPort,
                                      InventoryReaderPort inventoryReaderPort,
                                      ProductOptionConfigurationService optionConfigurationService) {
        this.productReaderPort = productReaderPort;
        this.inventoryReaderPort = inventoryReaderPort;
        this.optionConfigurationService = optionConfigurationService;
    }

    /** 상품 단건 조회 */
    @Override
    public ProductView getProduct(Long productId) {
        Product product = productReaderPort.findActiveById(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        return toProductViews(List.of(product), false).getFirst();
    }

    /** ACTIVE 상품 목록 조회 — 최신 등록순 (N+1 방지: 재고 일괄 조회) */
    @Override
    public List<ProductView> listActiveProducts() {
        List<Product> products = productReaderPort.findActiveProductsByCreatedAtDesc();
        return toProductViews(products, false);
    }

    @Override
    public List<ProductView> listAllProducts() {
        return toProductViews(productReaderPort.findAllProductsByCreatedAtDesc(), true);
    }

    /** 필터 조건에 따른 ACTIVE 상품 목록 조회. */
    @Override
    public List<ProductView> listActiveProducts(ProductFilter filter) {
        if (filter.isDefault()) {
            return listActiveProducts();
        }
        List<Product> products = productReaderPort.findActiveByFilter(filter);
        return toProductViews(products, false);
    }

    /** ACTIVE 상품에 존재하는 카테고리 목록. */
    @Override
    public List<String> listActiveCategories() {
        return productReaderPort.findDistinctActiveCategories();
    }

    private List<ProductView> toProductViews(List<Product> products, boolean adminView) {
        if (products.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();
        Map<Long, Inventory> inventoryMap = inventoryReaderPort.findByProductIdIn(productIds)
                .stream()
                .collect(toMap(Inventory::getProductId, Function.identity()));

        Map<Long, ProductOptions> optionsByProductId = optionConfigurationService
                .getAll(productIds, adminView);
        return products.stream()
                .map(p -> {
                    Inventory inv = inventoryMap.get(p.getId());
                    if (inv == null) {
                        throw new NotFoundException("재고");
                    }
                    if (p.getType() == ProductType.READY_STOCK) {
                        return new ProductView(
                                p, inv.getQuantity(), inv.isAvailable(), ProductOptions.EMPTY);
                    }
                    ProductOptions options = optionsByProductId.getOrDefault(
                            p.getId(), ProductOptions.EMPTY);
                    return new ProductView(p, options.quantity(), options.available(), options);
                })
                .toList();
    }
}
