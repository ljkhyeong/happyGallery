package com.personal.happygallery.adapter.out.persistence.admin;

import com.personal.happygallery.application.admin.port.out.AdminMfaChallengePort;
import com.personal.happygallery.domain.admin.AdminMfaChallenge;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaAdminMfaChallengePersistenceAdapter implements AdminMfaChallengePort {

    private final AdminMfaChallengeRepository repository;

    JpaAdminMfaChallengePersistenceAdapter(AdminMfaChallengeRepository repository) {
        this.repository = repository;
    }

    @Override
    public AdminMfaChallenge save(AdminMfaChallenge challenge) {
        return repository.save(challenge);
    }

    @Override
    public Optional<Long> findAdminUserIdByTokenHmacCandidates(List<String> tokenHmacs) {
        return repository.findAdminUserIdByTokenHmacCandidates(tokenHmacs);
    }

    @Override
    public Optional<AdminMfaChallenge> findByTokenHmacCandidatesForUpdate(List<String> tokenHmacs) {
        return repository.findByTokenHmacCandidatesForUpdate(tokenHmacs);
    }

    @Override
    public void deleteByAdminUserId(Long adminUserId) {
        repository.deleteByAdminUserId(adminUserId);
    }
}
