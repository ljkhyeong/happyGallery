package com.personal.happygallery.app.customer;

import com.personal.happygallery.app.customer.port.out.UserReaderPort;
import com.personal.happygallery.app.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.infra.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link UserRepository}(infra) → {@link UserReaderPort} + {@link UserStorePort}(app) 브릿지 어댑터.
 */
@Component
class UserPersistencePortAdapter implements UserReaderPort, UserStorePort {

    private final UserRepository userRepository;

    UserPersistencePortAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public List<User> findAllById(List<Long> ids) {
        return userRepository.findAllById(ids);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }
}
