package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.application.product.port.in.RestockDemandUseCase.Demand;
import java.util.List;

public interface RestockDemandPort {
    List<Demand> list(Long productId, int offset, int size);
    long count(Long productId);
}
