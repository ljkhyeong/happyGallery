package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.EmailAddress;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.PersonalName;
import com.personal.happygallery.domain.user.User;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
class JpaUserPersistenceAdapter implements UserReaderPort, UserStorePort {

    private static final String DUPLICATE_EMAIL_CONSTRAINT = "uq_users_email_hmac";

    private final UserRepository userRepository;
    private final FieldEncryptor fieldEncryptor;
    private final BlindIndexer blindIndexer;

    JpaUserPersistenceAdapter(UserRepository userRepository,
                              FieldEncryptor fieldEncryptor,
                              BlindIndexer blindIndexer) {
        this.userRepository = userRepository;
        this.fieldEncryptor = fieldEncryptor;
        this.blindIndexer = blindIndexer;
    }

    @Override
    public Optional<User> findById(Long id) {
        return active(userRepository.findById(id));
    }

    @Override
    public Optional<User> findByIdForUpdate(Long id) {
        return active(userRepository.findByIdForUpdate(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return active(userRepository.findByEmailHmac(indexEmail(email)));
    }

    @Override
    public Optional<LoginSnapshot> findLoginSnapshotByEmail(String email) {
        return userRepository.findLoginSnapshotByEmailHmac(indexEmail(email))
                .map(snapshot -> new LoginSnapshot(
                        snapshot.getUserId(),
                        snapshot.getPasswordHash(),
                        snapshot.getWithdrawnAt() == null));
    }

    @Override
    public Optional<User> findByEmailForUpdate(String email) {
        return active(userRepository.findByEmailHmacForUpdate(indexEmail(email)));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailHmac(indexEmail(email));
    }

    @Override
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhoneHmac(
                blindIndexer.index(KoreanPhoneNumber.required(phone)));
    }

    @Override
    public boolean existsByPhoneAndIdNot(String phone, Long excludedUserId) {
        return userRepository.existsByPhoneHmacAndIdNot(
                blindIndexer.index(KoreanPhoneNumber.required(phone)), excludedUserId);
    }

    @Override
    public List<User> findAllById(Iterable<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .map(this::restore)
                .filter(User::isActive)
                .toList();
    }

    @Override
    public List<User> findAllByIdForAdminHistory(Iterable<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .map(this::restore)
                .toList();
    }

    @Override
    public User save(User user) {
        protect(user);
        try {
            return restore(userRepository.save(user));
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, DUPLICATE_EMAIL_CONSTRAINT)) {
                throw new HappyGalleryException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            throw exception;
        }
    }

    @Override
    public User saveAndFlush(User user) {
        protect(user);
        try {
            return restore(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, DUPLICATE_EMAIL_CONSTRAINT)) {
                throw new HappyGalleryException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            throw exception;
        }
    }

    private void protect(User user) {
        String email = EmailAddress.optional(user.getEmail());
        String name = PersonalName.required(user.getName());
        String phone = user.getPhone();
        user.protect(
                email == null ? null : fieldEncryptor.encrypt(email),
                email == null ? null : blindIndexer.index(email),
                fieldEncryptor.encrypt(name), blindIndexer.index(name),
                phone == null ? null : fieldEncryptor.encrypt(phone),
                phone == null ? null : blindIndexer.index(phone));
    }

    private User restore(User user) {
        user.restoreProtectedFields(
                user.getEmailEnc() == null ? null : fieldEncryptor.decrypt(user.getEmailEnc()),
                fieldEncryptor.decrypt(user.getNameEnc()),
                user.getPhoneEnc() == null ? null : fieldEncryptor.decrypt(user.getPhoneEnc()));
        return user;
    }

    private String indexEmail(String email) {
        return blindIndexer.index(EmailAddress.required(email));
    }

    private Optional<User> active(Optional<User> user) {
        return user.map(this::restore).filter(User::isActive);
    }

    private static boolean hasConstraint(Throwable throwable, String expectedConstraint) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                    && StringUtils.hasText(violation.getConstraintName())) {
                String constraint = StringUtils.unqualify(violation.getConstraintName()
                        .toLowerCase(Locale.ROOT)
                        .replace("`", "")
                        .replace("\"", "")
                        .replace("'", ""));
                return expectedConstraint.equals(constraint);
            }
            current = current.getCause();
        }
        return false;
    }
}
