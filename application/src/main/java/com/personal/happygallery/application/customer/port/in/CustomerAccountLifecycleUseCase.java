package com.personal.happygallery.application.customer.port.in;

/** 로그인 회원의 계정 탈퇴와 개인정보 폐기를 처리한다. */
public interface CustomerAccountLifecycleUseCase {

    record WithdrawCommand(
            Long userId,
            long credentialVersion,
            boolean recentlyReauthenticated) {}

    void withdraw(WithdrawCommand command);
}
