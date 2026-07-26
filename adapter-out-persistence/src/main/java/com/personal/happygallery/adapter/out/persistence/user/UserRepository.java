package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.domain.user.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface UserRepository extends JpaRepository<User, Long> {

    @Override Optional<User> findById(Long id);

    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    Optional<User> findByEmailHmac(String emailHmac);

    @Query("""
            SELECT u.id AS userId,
                   u.passwordHash AS passwordHash,
                   u.withdrawnAt AS withdrawnAt
            FROM User u
            WHERE u.emailHmac = :emailHmac
            """)
    Optional<LoginSnapshotProjection> findLoginSnapshotByEmailHmac(
            @Param("emailHmac") String emailHmac);

    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.emailHmac = :emailHmac")
    Optional<User> findByEmailHmacForUpdate(@Param("emailHmac") String emailHmac);

    boolean existsByEmailHmac(String emailHmac);

    boolean existsByPhoneHmac(String phoneHmac);

    boolean existsByPhoneHmacAndIdNot(String phoneHmac, Long excludedUserId);

    interface LoginSnapshotProjection {

        Long getUserId();

        String getPasswordHash();

        LocalDateTime getWithdrawnAt();
    }
}
