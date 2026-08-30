package com.personal.happygallery.adapter.out.persistence.cart;

import com.personal.happygallery.application.cart.port.out.CartItemDetail;
import com.personal.happygallery.application.cart.port.out.CartItemReaderPort;
import com.personal.happygallery.application.cart.port.out.CartItemStorePort;
import com.personal.happygallery.application.cart.port.out.CartReadModelPort;
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
        CartItemReaderPort,
        CartItemStorePort,
        CartReadModelPort {

    @Override
    <S extends CartItem> S save(S item);

    @Override
    <S extends CartItem> List<S> saveAll(Iterable<S> items);

    @Override
    void delete(CartItem item);

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
    @Query("""
            select item
              from CartItem item
             where item.userId = :userId
               and item.id = :cartItemId
            """)
    Optional<CartItem> findByUserIdAndIdForUpdate(
            @Param("userId") Long userId,
            @Param("cartItemId") Long cartItemId);

    @Override
    @Query("""
            select distinct item
              from CartItem item
              left join fetch item.textInputs
             where item.userId = :userId
             order by item.createdAt, item.id
            """)
    List<CartItem> findAllWithTextInputsByUserId(@Param("userId") Long userId);

    @Override
    @Lock(PESSIMISTIC_WRITE)
    @Query("""
            select distinct item
              from CartItem item
              left join fetch item.textInputs
             where item.userId = :userId
               and item.productId in :productIds
             order by item.productId, item.id
            """)
    List<CartItem> findAllByUserIdAndProductIdInForUpdate(
            @Param("userId") Long userId,
            @Param("productIds") Collection<Long> productIds);

    @Override
    @Lock(PESSIMISTIC_WRITE)
    List<CartItem> findAllByUserIdAndIdInOrderByIdAsc(Long userId, Collection<Long> cartItemIds);

    @Override
    @Query("""
            select new com.personal.happygallery.application.cart.port.out.CartItemDetail(
                       item.id,
                       product.id,
                       product.name,
                       product.type,
                       product.price,
                       product.specification,
                       product.careInstructions,
                       product.productionLeadDays,
                       item.qty,
                       product.status,
                       inventory.quantity
                   )
              from CartItem item
              join Product product on product.id = item.productId
              left join Inventory inventory on inventory.productId = item.productId
             where item.userId = :userId
             order by item.createdAt, item.id
            """)
    List<CartItemDetail> findDetailsByUserId(@Param("userId") Long userId);
}
