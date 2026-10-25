package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.User;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaUserPersistenceAdapterTest {

    @DisplayName("회원 이메일 유일 제약 충돌은 명시적인 이메일 중복 오류로 번역한다")
    @Test
    void save_duplicateEmail_mapsToEmailAlreadyExists() {
        UserRepository repository = mock(UserRepository.class);
        FieldEncryptor fieldEncryptor = mock(FieldEncryptor.class);
        User user = new User("member@example.com", "hash", "회원", "01012345678");
        when(repository.save(user)).thenThrow(constraintViolation("users.uq_users_email_hmac"));
        JpaUserPersistenceAdapter adapter = new JpaUserPersistenceAdapter(
                repository, fieldEncryptor, new BlindIndexer(new byte[32]));

        assertThatThrownBy(() -> adapter.save(user))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @DisplayName("즉시 flush한 회원 이메일 유일 제약 충돌도 이메일 중복 오류로 번역한다")
    @Test
    void saveAndFlush_duplicateEmail_mapsToEmailAlreadyExists() {
        UserRepository repository = mock(UserRepository.class);
        FieldEncryptor fieldEncryptor = mock(FieldEncryptor.class);
        User user = new User("member@example.com", "hash", "회원", "01012345678");
        when(repository.saveAndFlush(user))
                .thenThrow(constraintViolation("users.uq_users_email_hmac"));
        JpaUserPersistenceAdapter adapter = new JpaUserPersistenceAdapter(
                repository, fieldEncryptor, new BlindIndexer(new byte[32]));

        assertThatThrownBy(() -> adapter.saveAndFlush(user))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    private static DataIntegrityViolationException constraintViolation(String constraintName) {
        return new DataIntegrityViolationException(
                "DB constraint violation",
                new ConstraintViolationException("Duplicate entry", new SQLException(), constraintName));
    }
}
