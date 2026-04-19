package com.personal.happygallery.application.admin.port.in;

/**
 * 최초 관리자 계정 one-time bootstrap.
 * <p>상태 조회와 생성 두 가지 동작만 제공한다. 계정이 하나라도 존재하면 항상 disabled 이다.
 */
public interface AdminSetupUseCase {

    /**
     * setup 이 현재 시점에 수행 가능한지.
     * <p>{@code admin_user} 가 비어 있어야 true 를 반환한다.
     */
    boolean isAvailable();

    /**
     * 최초 관리자 계정을 생성한다.
     *
     * @throws com.personal.happygallery.domain.error.HappyGalleryException
     *         setup 이 비활성 상태이거나(이미 계정 존재), username 이 이미 존재하는 경우
     */
    void setup(String username, String rawPassword);
}
