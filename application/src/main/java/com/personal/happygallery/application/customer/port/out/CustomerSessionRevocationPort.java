package com.personal.happygallery.application.customer.port.out;

/** 회원의 특정 자격 증명 버전으로 발급된 Spring Session을 모두 폐기한다. */
public interface CustomerSessionRevocationPort {

    void revokeCredentialVersion(Long userId, long credentialVersion);
}
