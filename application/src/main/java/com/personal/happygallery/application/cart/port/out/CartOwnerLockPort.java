package com.personal.happygallery.application.cart.port.out;

/** 같은 회원의 장바구니 변경이 동시에 실행되지 않도록 잠근다. */
public interface CartOwnerLockPort {

    void lock(Long userId);
}
