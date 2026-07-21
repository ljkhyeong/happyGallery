package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.user.User;
import java.util.List;
import java.util.Optional;

/**
 * 회원 조회 포트.
 */
public interface UserReaderPort {

    Optional<User> findById(Long id);

    Optional<User> findByIdForUpdate(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailForUpdate(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long excludedUserId);

    List<User> findAllById(Iterable<Long> ids);
}
