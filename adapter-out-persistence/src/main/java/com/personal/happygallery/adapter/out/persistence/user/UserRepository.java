package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.domain.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    @Override Optional<User> findById(Long id);

    Optional<User> findByEmailHmac(String emailHmac);

    boolean existsByEmailHmac(String emailHmac);
}
