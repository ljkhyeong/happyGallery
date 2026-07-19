package com.personal.happygallery.adapter.out.persistence.cart;

import com.personal.happygallery.application.cart.port.out.CartItemReaderPort;
import com.personal.happygallery.application.cart.port.out.CartItemStorePort;
import com.personal.happygallery.domain.cart.CartItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface CartItemRepository extends JpaRepository<CartItem, Long>,
        CartItemReaderPort, CartItemStorePort {

    @Override
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    @Override
    @Lock(PESSIMISTIC_WRITE)
    @Query("""
            select item
              from CartItem item
             where item.userId = :userId
               and item.productId = :productId
            """)
    Optional<CartItem> findByUserIdAndProductIdForUpdate(@Param("userId") Long userId,
                                                        @Param("productId") Long productId);

    @Override
    @Lock(PESSIMISTIC_WRITE)
    List<CartItem> findAllByUserIdAndIdInOrderByIdAsc(Long userId, Collection<Long> cartItemIds);
}
