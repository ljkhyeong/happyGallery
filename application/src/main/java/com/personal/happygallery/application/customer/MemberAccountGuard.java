package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 탈퇴와 회원 거래 생성을 같은 사용자 행 잠금으로 직렬화한다. */
@Component
public class MemberAccountGuard {

    private final UserReaderPort userReader;

    public MemberAccountGuard(UserReaderPort userReader) {
        this.userReader = userReader;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void requireActiveForUpdate(Long userId) {
        userReader.findByIdForUpdate(userId)
                .orElseThrow(NotFoundException.supplier("회원"));
    }
}
