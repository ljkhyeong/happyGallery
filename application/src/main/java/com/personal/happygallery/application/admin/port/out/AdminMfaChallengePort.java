package com.personal.happygallery.application.admin.port.out;

import com.personal.happygallery.domain.admin.AdminMfaChallenge;
import java.util.List;
import java.util.Optional;

public interface AdminMfaChallengePort {

    AdminMfaChallenge save(AdminMfaChallenge challenge);

    Optional<Long> findAdminUserIdByTokenHmacCandidates(List<String> tokenHmacs);

    Optional<AdminMfaChallenge> findByTokenHmacCandidatesForUpdate(List<String> tokenHmacs);

    void deleteByAdminUserId(Long adminUserId);
}
