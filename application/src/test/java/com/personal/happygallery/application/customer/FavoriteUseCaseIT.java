package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.FavoriteUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.adapter.out.persistence.booking.ClassRepository;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.domain.user.FavoriteTargetType;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UseCaseIT
class FavoriteUseCaseIT {
    @Autowired FavoriteUseCase favorites;
    @Autowired CustomerAccountLifecycleUseCase lifecycle;
    @Autowired UserStorePort users;
    @Autowired ProductStorePort products;
    @Autowired ClassRepository classes;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestCleanupSupport cleanup;

    @AfterEach
    void clean() { cleanup.clearBookingData(); cleanup.clearProductData(); cleanup.clearUsers(); }

    @Test
    @DisplayName("상품과 클래스 찜은 중복 저장되지 않고 본인 목록만 종류·커서로 조회한다")
    void savesDistinctOwnedTargetsAndPaginates() {
        var user = users.save(new User("favorite@example.com", "hash", "회원", "01012345678"));
        var other = users.save(new User("other-favorite@example.com", "hash", "다른회원", "01012345679"));
        var product = products.save(new Product("찜 상품", ProductType.READY_STOCK, 12000));
        var bookingClass = classes.save(new BookingClass("찜 클래스", "WOOD", 60, 30000, 30));
        favorites.save(user.getId(), FavoriteTargetType.PRODUCT, product.getId());
        favorites.save(user.getId(), FavoriteTargetType.PRODUCT, product.getId());
        favorites.save(user.getId(), FavoriteTargetType.CLASS, bookingClass.getId());
        assertThat(favorites.list(user.getId(), null, null, 20).content()).hasSize(2);
        assertThat(favorites.list(other.getId(), null, null, 20).content()).isEmpty();
        favorites.remove(other.getId(), FavoriteTargetType.PRODUCT, product.getId());
        assertThat(favorites.isSaved(user.getId(), FavoriteTargetType.PRODUCT, product.getId())).isTrue();
        var first = favorites.list(user.getId(), null, null, 1);
        assertThat(first.content()).singleElement().satisfies(row -> assertThat(row.targetType()).isEqualTo(FavoriteTargetType.CLASS));
        assertThat(favorites.list(user.getId(), null, first.nextCursor(), 1).content()).singleElement()
                .satisfies(row -> assertThat(row.targetId()).isEqualTo(product.getId()));
        assertThat(favorites.list(user.getId(), FavoriteTargetType.PRODUCT, null, 20).content()).hasSize(1);
        favorites.remove(user.getId(), FavoriteTargetType.PRODUCT, product.getId());
        favorites.remove(user.getId(), FavoriteTargetType.PRODUCT, product.getId());
        assertThat(favorites.isSaved(user.getId(), FavoriteTargetType.PRODUCT, product.getId())).isFalse();
    }

    @Test
    @DisplayName("중지한 상품은 기존 찜에서 상태를 표시하며 탈퇴하면 찜을 삭제하고 재등록을 막는다")
    void inactiveTargetsAndWithdrawal() {
        var user = users.save(new User("withdraw-favorite@example.com", "hash", "회원", "01012345678"));
        var product = products.save(new Product("중지할 상품", ProductType.READY_STOCK, 12000));
        favorites.save(user.getId(), FavoriteTargetType.PRODUCT, product.getId());
        jdbc.update("UPDATE products SET status = 'INACTIVE' WHERE id = ?", product.getId());
        assertThat(favorites.list(user.getId(), null, null, 20).content()).singleElement().satisfies(row -> assertThat(row.active()).isFalse());
        assertThatThrownBy(() -> favorites.save(user.getId(), FavoriteTargetType.PRODUCT, product.getId())).isInstanceOf(NotFoundException.class);
        lifecycle.withdraw(new CustomerAccountLifecycleUseCase.WithdrawCommand(user.getId(), user.getCredentialVersion(), true));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM member_favorites WHERE user_id = ?", Integer.class, user.getId())).isZero();
        assertThatThrownBy(() -> favorites.save(user.getId(), FavoriteTargetType.PRODUCT, product.getId())).isInstanceOf(NotFoundException.class);
    }
}
