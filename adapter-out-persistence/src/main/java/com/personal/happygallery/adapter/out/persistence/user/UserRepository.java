package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.user.AuthProvider;
import com.personal.happygallery.domain.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>, UserReaderPort, UserStorePort {

    @Override Optional<User> findById(Long id);
    @Override User save(User user);

    @Override Optional<User> findByEmail(String email);

    /** 블라인드 인덱스로 회원 조회 */
    @Override Optional<User> findByEmailHmac(String emailHmac);

    @Override boolean existsByEmail(String email);

    @Override boolean existsByEmailHmac(String emailHmac);

    @Override Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
