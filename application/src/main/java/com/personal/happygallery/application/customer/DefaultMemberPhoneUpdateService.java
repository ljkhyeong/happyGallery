package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.customer.port.in.MemberPhoneUpdateUseCase;
import com.personal.happygallery.application.customer.port.in.MemberPhoneUpdateUseCase.UpdatePhoneCommand;
import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.user.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultMemberPhoneUpdateService implements MemberPhoneUpdateUseCase {

    private final UserReaderPort userReader;
    private final UserStorePort userStore;
    private final BookingStorePort bookingStore;
    private final PhoneOwnershipVerificationUseCase phoneOwnershipVerification;
    private final ApplicationEventPublisher eventPublisher;

    public DefaultMemberPhoneUpdateService(UserReaderPort userReader,
                                           UserStorePort userStore,
                                           BookingStorePort bookingStore,
                                           PhoneOwnershipVerificationUseCase phoneOwnershipVerification,
                                           ApplicationEventPublisher eventPublisher) {
        this.userReader = userReader;
        this.userStore = userStore;
        this.bookingStore = bookingStore;
        this.phoneOwnershipVerification = phoneOwnershipVerification;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public User update(UpdatePhoneCommand command) {
        User user = userReader.findByIdForUpdate(command.userId())
                .orElseThrow(NotFoundException.supplier("회원"));
        if (user.getCredentialVersion() != command.credentialVersion()) {
            throw new HappyGalleryException(ErrorCode.UNAUTHORIZED);
        }
        boolean firstRegistration = user.getPhone() == null;
        if (!command.recentlyReauthenticated()) {
            throw new HappyGalleryException(ErrorCode.REAUTHENTICATION_REQUIRED);
        }
        PhoneVerificationPurpose purpose = firstRegistration
                ? PhoneVerificationPurpose.MEMBER_PHONE_REGISTRATION
                : PhoneVerificationPurpose.MEMBER_PHONE_CHANGE;
        phoneOwnershipVerification.verify(
                command.phone(), command.verificationCode(), purpose);
        if (userReader.existsByPhoneAndIdNot(command.phone(), command.userId())) {
            throw new HappyGalleryException(ErrorCode.PHONE_ALREADY_IN_USE);
        }

        long invalidatedCredentialVersion = user.getCredentialVersion();
        user.registerVerifiedPhone(command.phone());
        if (!firstRegistration) {
            user.markAuthenticationMethodsChanged();
        }
        User savedUser = userStore.save(user);
        bookingStore.updateBookedOwnerPhoneHmacByUserId(
                command.userId(), savedUser.getPhoneHmac());
        if (!firstRegistration) {
            eventPublisher.publishEvent(new CustomerCredentialsChangedEvent(
                    command.userId(), invalidatedCredentialVersion));
        }
        return savedUser;
    }
}
