package com.personal.happygallery.application.customer.port.out;

import java.time.LocalDateTime;

/** 회원 탈퇴를 막아야 하는 미완료 거래가 있는지 조회한다. */
public interface CustomerAccountActivityPort {

    boolean hasBlockingActivity(Long userId, LocalDateTime now);
}
