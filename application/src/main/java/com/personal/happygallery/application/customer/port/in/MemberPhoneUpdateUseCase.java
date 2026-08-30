package com.personal.happygallery.application.customer.port.in;

import com.personal.happygallery.domain.user.User;

/** SMS로 소유권을 확인한 번호를 회원의 연락처로 등록하거나 변경한다. */
public interface MemberPhoneUpdateUseCase {

    record UpdatePhoneCommand(
            Long userId,
            long credentialVersion,
            String phone,
            String verificationCode,
            boolean recentlyReauthenticated) {}

    User update(UpdatePhoneCommand command);
}
