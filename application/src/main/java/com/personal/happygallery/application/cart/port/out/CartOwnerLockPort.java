package com.personal.happygallery.application.cart.port.out;

/** 같은 회원의 장바구니 변경을 직렬화하는 안정적인 잠금 경계. */
public interface CartOwnerLockPort {

    void lock(Long userId);
}
