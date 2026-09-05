package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.in.RestockDemandUseCase;
import com.personal.happygallery.application.product.port.out.RestockDemandPort;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.application.shared.page.PageParams;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultRestockDemandService implements RestockDemandUseCase {
    private final RestockDemandPort demand;
    public DefaultRestockDemandService(RestockDemandPort demand) { this.demand = demand; }
    @Override
    public OffsetPage<Demand> list(Long productId, int page, int size) {
        int offset = PageParams.offset(page, size);
        return OffsetPage.of(demand.list(productId, offset, size), page, size, demand.count(productId));
    }
}
