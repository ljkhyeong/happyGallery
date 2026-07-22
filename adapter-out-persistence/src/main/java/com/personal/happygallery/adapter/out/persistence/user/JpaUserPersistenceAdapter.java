package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.user.EmailAddress;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.PersonalName;
import com.personal.happygallery.domain.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaUserPersistenceAdapter implements UserReaderPort, UserStorePort {

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
    public User save(User user) {
        protect(user);
        return restore(userRepository.save(user));
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
}
