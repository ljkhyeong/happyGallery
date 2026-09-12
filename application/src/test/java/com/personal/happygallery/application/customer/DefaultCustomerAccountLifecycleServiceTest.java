package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase.WithdrawCommand;
import com.personal.happygallery.application.customer.port.out.CustomerAccountActivityPort;
import com.personal.happygallery.application.customer.port.out.CustomerAccountActivityPort.BlockingActivity;
import com.personal.happygallery.application.customer.port.out.FavoritePort;
import com.personal.happygallery.application.customer.port.out.SocialAccountStorePort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.inquiry.port.out.GroupInquiryPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultCustomerAccountLifecycleServiceTest {
    @Test
    @DisplayName("탈퇴 제한 사유를 모두 반환하고 회원 정보를 변경하지 않는다")
    void reportsOnlyActualBlockingReasons() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-08T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        UserReaderPort users = mock(UserReaderPort.class);
        UserStorePort store = mock(UserStorePort.class);
        CustomerAccountActivityPort activities = mock(CustomerAccountActivityPort.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(mock(User.class)));
        when(activities.findBlockingActivities(1L, LocalDateTime.now(clock)))
                .thenReturn(List.of(BlockingActivity.PASS, BlockingActivity.REFUND));
        var service = new DefaultCustomerAccountLifecycleService(users, store, activities,
                mock(SocialAccountStorePort.class), events, clock, mock(GroupInquiryPort.class), mock(FavoritePort.class));

        var error = catchThrowableOfType(HappyGalleryException.class,
                () -> service.withdraw(new WithdrawCommand(1L, 0, true)));

        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_WITHDRAWAL_BLOCKED);
        assertThat(error.getMessage().lines().toList()).containsExactly(
                "사용 가능한 이용권이 있습니다.", "처리 중인 환불이 있습니다.");
        verifyNoInteractions(store, events);
    }
}
