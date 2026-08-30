package com.personal.happygallery.adapter.out.persistence.reward;

import com.personal.happygallery.domain.reward.RewardAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardAccountRepository extends JpaRepository<RewardAccount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM RewardAccount a WHERE a.userId = :userId")
    Optional<RewardAccount> findByUserIdForUpdate(@Param("userId") Long userId);

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM RewardAccount a
            WHERE a.userId = :userId
              AND (a.reservedBalance > 0 OR a.debtBalance > 0)
            """)
    boolean existsBlockingWithdrawal(@Param("userId") Long userId);
}
