package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.user.User;
import java.util.List;
import java.util.Optional;

/**
 * 회원 조회 포트.
 *
 * <p>{@link User}를 돌려주는 조회는 활성 회원만 반환하므로 호출자가 활성 여부를 다시 검사하지 않는다.
 * 익명화된 탈퇴 회원은 {@link #findAllByIdForAdminHistory}만 포함한다.
 */
public interface UserReaderPort {

    /** 활성 회원만 반환한다. 탈퇴 회원은 {@code Optional.empty()}다. */
    Optional<User> findById(Long id);

    /** 활성 회원만 행 잠금과 함께 반환한다. 탈퇴 회원은 {@code Optional.empty()}다. */
    Optional<User> findByIdForUpdate(Long id);

    Optional<User> findByEmail(String email);

    /** BCrypt 사전 검증용 비잠금 값이며 영속 엔티티를 1차 캐시에 올리지 않는다. */
    Optional<LoginSnapshot> findLoginSnapshotByEmail(String email);

    Optional<User> findByEmailForUpdate(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long excludedUserId);

    List<User> findAllById(Iterable<Long> ids);

    /** 관리자 과거 이력 표시용 조회. 익명화된 탈퇴 회원도 포함한다. */
    List<User> findAllByIdForAdminHistory(Iterable<Long> ids);

    record LoginSnapshot(Long userId, String passwordHash, boolean active) {

        public boolean hasLocalPassword() {
            return passwordHash != null;
        }
    }
}
