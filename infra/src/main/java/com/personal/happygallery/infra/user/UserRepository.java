package com.personal.happygallery.infra.user;

import com.personal.happygallery.app.customer.port.out.UserReaderPort;
import com.personal.happygallery.app.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>, UserReaderPort, UserStorePort {

    @Override Optional<User> findById(Long id);
    @Override User save(User user);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Override
    default List<User> findAllById(List<Long> ids) {
        return findAllById((Iterable<Long>) ids);
    }
}
