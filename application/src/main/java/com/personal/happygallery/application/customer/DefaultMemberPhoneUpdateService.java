package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.MemberPhoneUpdateUseCase;
import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultMemberPhoneUpdateService implements MemberPhoneUpdateUseCase {

    private final UserReaderPort userReader;
    private final UserStorePort userStore;
    private final PhoneOwnershipVerificationUseCase phoneOwnershipVerification;

    public DefaultMemberPhoneUpdateService(UserReaderPort userReader,
                                           UserStorePort userStore,
                                           PhoneOwnershipVerificationUseCase phoneOwnershipVerification) {
        this.userReader = userReader;
        this.userStore = userStore;
        this.phoneOwnershipVerification = phoneOwnershipVerification;
    }

    @Override
    @Transactional
    public User update(Long userId, String phone, String verificationCode) {
        User user = userReader.findByIdForUpdate(userId)
                .orElseThrow(NotFoundException.supplier("회원"));
        phoneOwnershipVerification.verify(phone, verificationCode);
        if (userReader.existsByPhoneAndIdNot(phone, userId)) {
            throw new HappyGalleryException(ErrorCode.PHONE_ALREADY_IN_USE);
        }

        user.registerVerifiedPhone(phone);
        return userStore.save(user);
    }
}
