package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.InitialMemberPhoneRegistrationUseCase;
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
public class DefaultInitialMemberPhoneRegistrationService
        implements InitialMemberPhoneRegistrationUseCase {

    private final UserReaderPort userReader;
    private final UserStorePort userStore;
    private final PhoneOwnershipVerificationUseCase phoneOwnershipVerification;

    public DefaultInitialMemberPhoneRegistrationService(
            UserReaderPort userReader,
            UserStorePort userStore,
            PhoneOwnershipVerificationUseCase phoneOwnershipVerification) {
        this.userReader = userReader;
        this.userStore = userStore;
        this.phoneOwnershipVerification = phoneOwnershipVerification;
    }

    @Override
    @Transactional
    public User register(Long userId, String phone, String verificationCode) {
        User user = userReader.findByIdForUpdate(userId)
                .orElseThrow(NotFoundException.supplier("회원"));
        if (user.getPhone() != null) {
            throw new HappyGalleryException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }

        phoneOwnershipVerification.verify(phone, verificationCode);
        user.registerVerifiedPhone(phone);
        return userStore.save(user);
    }
}
