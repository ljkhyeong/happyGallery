package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.application.customer.port.in.FavoriteUseCase;
import com.personal.happygallery.application.customer.port.out.FavoritePort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.booking.BookingClassStatus;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.user.Favorite;
import com.personal.happygallery.domain.user.FavoriteTargetType;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultFavoriteService implements FavoriteUseCase {
    private final FavoritePort favorites;
    private final MemberAccountGuard accounts;
    private final ProductReaderPort products;
    private final ClassReaderPort classes;
    private final Clock clock;
    public DefaultFavoriteService(FavoritePort favorites, MemberAccountGuard accounts, ProductReaderPort products, ClassReaderPort classes, Clock clock) {
        this.favorites = favorites; this.accounts = accounts; this.products = products; this.classes = classes; this.clock = clock;
    }
    @Override
    public void save(Long userId, FavoriteTargetType type, Long targetId) {
        accounts.requireActiveForUpdate(userId);
        switch (type) {
            case PRODUCT -> products.findByIdWithLock(targetId).filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                    .orElseThrow(NotFoundException.supplier("상품"));
            case CLASS -> classes.findByIdForUpdate(targetId).filter(c -> c.getStatus() == BookingClassStatus.ACTIVE)
                    .orElseThrow(NotFoundException.supplier("클래스"));
        }
        if (!favorites.exists(userId, type, targetId)) favorites.save(new Favorite(userId, type, targetId, LocalDateTime.now(clock)));
    }
    @Override
    public void remove(Long userId, FavoriteTargetType type, Long targetId) {
        accounts.requireActiveForUpdate(userId);
        favorites.deleteTarget(userId, type, targetId);
    }
    @Override
    @Transactional(readOnly = true)
    public boolean isSaved(Long userId, FavoriteTargetType type, Long targetId) { return favorites.exists(userId, type, targetId); }
    @Override
    @Transactional(readOnly = true)
    public CursorPage<View> list(Long userId, FavoriteTargetType type, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        var before = cursor == null ? null : CursorUtils.decode(cursor);
        return CursorPage.of(favorites.list(userId, type, before == null ? null : before.timestamp(),
                before == null ? null : before.id(), pageSize + 1), pageSize,
                row -> CursorUtils.encode(row.createdAt(), row.id()));
    }
}
