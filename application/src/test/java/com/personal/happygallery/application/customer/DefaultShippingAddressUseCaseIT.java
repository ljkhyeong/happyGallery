package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase;
import com.personal.happygallery.application.customer.port.in.DefaultShippingAddressUseCase;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.user.User;
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
class DefaultShippingAddressUseCaseIT {
    @Autowired DefaultShippingAddressUseCase addresses;
    @Autowired UserStorePort users;
    @Autowired CustomerAccountLifecycleUseCase lifecycle;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestCleanupSupport cleanup;

    @AfterEach
    void clean() { cleanup.clearUsers(); }

    @Test
    @DisplayName("기본 배송지는 본인에게만 반환하고 암호화 저장하며 오래된 수정과 삭제를 거절한다")
    void savesEncryptedAddressAndRejectsStaleEdits() {
        var user = users.save(new User("address@example.com", "hash", "회원", "01012345678"));
        var other = users.save(new User("other-address@example.com", "hash", "다른회원", "01012345679"));
        var address = new ShippingAddress("수령인", "01012345678", "12345", "서울시 테스트로 10", "101호");
        assertThat(addresses.get(user.getId()).shippingAddress()).isNull();
        addresses.save(user.getId(), 0, address);
        assertThat(addresses.get(user.getId()).shippingAddress()).isEqualTo(address);
        assertThat(addresses.get(other.getId()).shippingAddress()).isNull();
        assertThat(jdbc.queryForObject("SELECT default_shipping_address_enc FROM users WHERE id = ?", String.class, user.getId()))
                .startsWith("hg:").doesNotContain("수령인", "서울시", "01012345678");
        assertThatThrownBy(() -> addresses.save(user.getId(), 0, address)).isInstanceOf(HappyGalleryException.class);
        assertThatThrownBy(() -> addresses.delete(user.getId(), 0)).isInstanceOf(HappyGalleryException.class);
        addresses.delete(user.getId(), 1);
        assertThat(addresses.get(user.getId()).shippingAddress()).isNull();
        assertThat(addresses.get(user.getId()).version()).isEqualTo(2);
    }

    @Test
    @DisplayName("탈퇴하면 기본 배송지를 폐기하고 탈퇴한 계정의 재저장을 막는다")
    void withdrawalDeletesAddress() {
        var user = users.save(new User("withdraw-address@example.com", "hash", "회원", "01012345678"));
        var address = new ShippingAddress("수령인", "01012345678", "12345", "서울시 테스트로 10", null);
        addresses.save(user.getId(), 0, address);
        lifecycle.withdraw(new CustomerAccountLifecycleUseCase.WithdrawCommand(user.getId(), user.getCredentialVersion(), true));
        assertThat(jdbc.queryForObject("SELECT default_shipping_address_enc FROM users WHERE id = ?", String.class, user.getId())).isNull();
        assertThatThrownBy(() -> addresses.save(user.getId(), 1, address)).isInstanceOf(HappyGalleryException.class);
    }
}
