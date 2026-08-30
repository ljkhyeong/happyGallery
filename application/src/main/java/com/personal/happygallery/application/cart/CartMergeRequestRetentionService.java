package com.personal.happygallery.application.cart;

import com.personal.happygallery.application.cart.port.out.CartMergeRequestStorePort;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartMergeRequestRetentionService {

    private final CartMergeRequestStorePort requestStore;

    public CartMergeRequestRetentionService(CartMergeRequestStorePort requestStore) {
        this.requestStore = requestStore;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteBatchBefore(LocalDateTime cutoff, int batchSize) {
        return requestStore.deleteCreatedBefore(cutoff, batchSize);
    }
}
