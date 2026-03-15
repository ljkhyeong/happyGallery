package com.personal.happygallery.app.customer.port.out;

import com.personal.happygallery.domain.user.User;

/**
 * 회원 저장 포트.
 */
public interface UserStorePort {

    User save(User user);
}
