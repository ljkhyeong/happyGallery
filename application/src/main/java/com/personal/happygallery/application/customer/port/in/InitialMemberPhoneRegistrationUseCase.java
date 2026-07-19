package com.personal.happygallery.application.customer.port.in;

import com.personal.happygallery.domain.user.User;

/** 전화번호가 없는 회원에게 SMS로 확인한 최초 연락처를 등록한다. */
public interface InitialMemberPhoneRegistrationUseCase {

    User register(Long userId, String phone, String verificationCode);
}
