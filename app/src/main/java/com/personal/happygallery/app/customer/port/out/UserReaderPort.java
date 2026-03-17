package com.personal.happygallery.app.customer.port.out;

import com.personal.happygallery.domain.user.User;
import java.util.List;
import java.util.Optional;

/**
 * 회원 조회 포트.
 */
public interface UserReaderPort {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllById(List<Long> ids);
}
