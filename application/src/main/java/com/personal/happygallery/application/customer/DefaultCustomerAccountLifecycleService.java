package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase;
import com.personal.happygallery.application.inquiry.port.out.GroupInquiryPort;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase.WithdrawCommand;
import com.personal.happygallery.application.customer.port.out.CustomerAccountActivityPort;
import com.personal.happygallery.application.customer.port.out.SocialAccountStorePort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultCustomerAccountLifecycleService implements CustomerAccountLifecycleUseCase {

    private static final String WITHDRAWN_EMAIL_DOMAIN = "@happygallery.invalid";

    private final UserReaderPort userReader;
    private final UserStorePort userStore;
    private final CustomerAccountActivityPort accountActivity;
    private final SocialAccountStorePort socialAccountStore;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final GroupInquiryPort groupInquiries;

    public DefaultCustomerAccountLifecycleService(UserReaderPort userReader,
                                                  UserStorePort userStore,
                                                  CustomerAccountActivityPort accountActivity,
                                                  SocialAccountStorePort socialAccountStore,
                                                  ApplicationEventPublisher eventPublisher,
                                                  Clock clock, GroupInquiryPort groupInquiries) {
        this.userReader = userReader;
        this.userStore = userStore;
        this.accountActivity = accountActivity;
        this.socialAccountStore = socialAccountStore;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.groupInquiries = groupInquiries;
    }

    @Override
    @Transactional
    public void withdraw(WithdrawCommand command) {
        User user = userReader.findByIdForUpdate(command.userId())
                .orElseThrow(NotFoundException.supplier("회원"));
        if (user.getCredentialVersion() != command.credentialVersion()) {
            throw new HappyGalleryException(ErrorCode.UNAUTHORIZED);
        }
        if (!command.recentlyReauthenticated()) {
            throw new HappyGalleryException(ErrorCode.REAUTHENTICATION_REQUIRED);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (accountActivity.hasBlockingActivity(command.userId(), now)) {
            throw new HappyGalleryException(ErrorCode.ACCOUNT_WITHDRAWAL_BLOCKED);
        }

        long invalidatedCredentialVersion = user.getCredentialVersion();
        user.withdraw(
                "withdrawn+" + command.userId() + WITHDRAWN_EMAIL_DOMAIN,
                "탈퇴회원",
                now);
        userStore.save(user);
        socialAccountStore.deleteByUserId(command.userId());
        groupInquiries.deleteByUserId(command.userId());
        eventPublisher.publishEvent(new CustomerCredentialsChangedEvent(
                command.userId(), invalidatedCredentialVersion));
    }
}
