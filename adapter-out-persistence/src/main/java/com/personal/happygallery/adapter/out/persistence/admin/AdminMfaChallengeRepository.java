package com.personal.happygallery.adapter.out.persistence.admin;

import com.personal.happygallery.domain.admin.AdminMfaChallenge;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminMfaChallengeRepository extends JpaRepository<AdminMfaChallenge, Long> {

    @Query("""
            SELECT challenge.adminUserId
              FROM AdminMfaChallenge challenge
             WHERE challenge.tokenHmac IN :tokenHmacs
            """)
    Optional<Long> findAdminUserIdByTokenHmacCandidates(
            @Param("tokenHmacs") List<String> tokenHmacs);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT challenge
              FROM AdminMfaChallenge challenge
             WHERE challenge.tokenHmac IN :tokenHmacs
            """)
    Optional<AdminMfaChallenge> findByTokenHmacCandidatesForUpdate(
            @Param("tokenHmacs") List<String> tokenHmacs);

    void deleteByAdminUserId(Long adminUserId);
}
