package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.DefaultShippingAddressUseCase;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.order.ShippingAddressProtector;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.ShippingAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultShippingAddressService implements DefaultShippingAddressUseCase {
    private final UserReaderPort users;
    private final MemberAccountGuard accounts;
    private final ShippingAddressProtector protector;

    public DefaultShippingAddressService(UserReaderPort users, MemberAccountGuard accounts, ShippingAddressProtector protector) {
        this.users = users;
        this.accounts = accounts;
        this.protector = protector;
    }

    @Override
    @Transactional(readOnly = true)
    public View get(Long userId) {
        var user = users.findById(userId).orElseThrow(NotFoundException.supplier("회원"));
        var encrypted = user.getDefaultShippingAddressEnc();
        return new View(user.getShippingAddressVersion(), encrypted == null ? null : protector.decrypt(encrypted));
    }

    @Override
    public void save(Long userId, long version, ShippingAddress address) {
        accounts.requireActiveForUpdate(userId).changeDefaultShippingAddress(version, protector.encrypt(address));
    }

    @Override
    public void delete(Long userId, long version) {
        accounts.requireActiveForUpdate(userId).changeDefaultShippingAddress(version, null);
    }
}
