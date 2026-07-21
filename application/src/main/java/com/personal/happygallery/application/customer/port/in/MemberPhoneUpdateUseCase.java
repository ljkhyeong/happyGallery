package com.personal.happygallery.application.customer.port.in;

import com.personal.happygallery.domain.user.User;

/** SMS로 소유권을 확인한 번호를 회원의 연락처로 등록하거나 변경한다. */
public interface MemberPhoneUpdateUseCase {

    User update(Long userId, String phone, String verificationCode);
}
