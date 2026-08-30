package com.personal.happygallery.bootstrap.migration;

import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.user.EmailAddress;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.PersonalName;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class V46__ProtectPlaintextPersonalData extends BaseJavaMigration {

    private final FieldEncryptor fieldEncryptor;
    private final BlindIndexer blindIndexer;

    public V46__ProtectPlaintextPersonalData(FieldEncryptor fieldEncryptor, BlindIndexer blindIndexer) {
        this.fieldEncryptor = fieldEncryptor;
        this.blindIndexer = blindIndexer;
    }

    @Override
    public MigrationVersion getVersion() {
        return MigrationVersion.fromVersion("46");
    }

    @Override
    public String getDescription() {
        return "protect plaintext personal data";
    }

    @Override
    public boolean canExecuteInTransaction() {
        return false;
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        List<UserData> users = readUsers(connection);
        List<GuestData> guests = readGuests(connection);
        List<PaymentPayloadData> paymentPayloads = readPaymentPayloads(connection);
        List<SocialIdentityData> socialIdentities = readSocialIdentities(connection);
        requireUniqueUserEmails(users);
        requireUniqueGuestPhones(guests);
        requireUniqueSocialIdentities(socialIdentities);

        prepareProtectedColumns(connection);
        updateUsers(connection, users);
        updateGuests(connection, guests);
        updatePaymentPayloads(connection, paymentPayloads);
        updateSocialIdentities(connection, socialIdentities);
        replacePhoneVerifications(connection);
        removePlaintextColumns(connection);
    }

    private List<UserData> readUsers(Connection connection) throws SQLException {
        List<UserData> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT id, email, name, phone
                     FROM users
                     ORDER BY id
                     """)) {
            while (result.next()) {
                long id = result.getLong("id");
                String email = EmailAddress.required(result.getString("email"));
                String name = PersonalName.required(result.getString("name"));
                String phone = KoreanPhoneNumber.optional(result.getString("phone"));
                rows.add(new UserData(
                        id,
                        fieldEncryptor.encrypt(email), blindIndexer.index(email),
                        fieldEncryptor.encrypt(name), blindIndexer.index(name),
                        phone == null ? null : fieldEncryptor.encrypt(phone),
                        phone == null ? null : blindIndexer.index(phone)));
            }
        }
        return rows;
    }

    private List<GuestData> readGuests(Connection connection) throws SQLException {
        List<GuestData> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT id, name, phone_enc
                     FROM guests
                     ORDER BY id
                     """)) {
            while (result.next()) {
                long id = result.getLong("id");
                String name = PersonalName.required(result.getString("name"));
                String phone = KoreanPhoneNumber.required(
                        fieldEncryptor.decrypt(result.getString("phone_enc")));
                rows.add(new GuestData(
                        id,
                        fieldEncryptor.encrypt(name), blindIndexer.index(name),
                        fieldEncryptor.encrypt(phone), blindIndexer.index(phone)));
            }
        }
        return rows;
    }

    private List<PaymentPayloadData> readPaymentPayloads(Connection connection) throws SQLException {
        List<PaymentPayloadData> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT id, payload_json
                     FROM payment_attempt
                     ORDER BY id
                     """)) {
            while (result.next()) {
                String stored = result.getString("payload_json");
                rows.add(new PaymentPayloadData(result.getLong("id"), protectPayload(stored)));
            }
        }
        return rows;
    }

    private List<SocialIdentityData> readSocialIdentities(Connection connection) throws SQLException {
        List<SocialIdentityData> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT id, provider, provider_id
                     FROM user_social_accounts
                     ORDER BY id
                     """)) {
            while (result.next()) {
                String providerId = result.getString("provider_id");
                if (!StringUtils.hasText(providerId)) {
                    throw new IllegalStateException("소셜 식별자가 비어 있습니다. socialAccountId=" + result.getLong("id"));
                }
                rows.add(new SocialIdentityData(
                        result.getLong("id"),
                        result.getString("provider"),
                        blindIndexer.index(providerId)));
            }
        }
        return rows;
    }

    private String protectPayload(String stored) {
        if (!StringUtils.hasText(stored)) {
            throw new IllegalStateException("결제 준비 payload가 비어 있습니다.");
        }
        if (stored.stripLeading().startsWith("{")) {
            return fieldEncryptor.encrypt(stored);
        }
        fieldEncryptor.decrypt(stored);
        return stored;
    }

    private void requireUniqueUserEmails(List<UserData> users) {
        Map<String, Long> seen = new HashMap<>();
        for (UserData user : users) {
            Long duplicateId = seen.putIfAbsent(user.emailHmac(), user.id());
            if (duplicateId != null) {
                throw new IllegalStateException(
                        "정규화 후 이메일이 중복됩니다. userIds=" + duplicateId + "," + user.id());
            }
        }
    }

    private void requireUniqueGuestPhones(List<GuestData> guests) {
        Map<String, Long> seen = new HashMap<>();
        for (GuestData guest : guests) {
            Long duplicateId = seen.putIfAbsent(guest.phoneHmac(), guest.id());
            if (duplicateId != null) {
                throw new IllegalStateException(
                        "정규화 후 게스트 전화번호가 중복됩니다. guestIds=" + duplicateId + "," + guest.id());
            }
        }
    }

    private void requireUniqueSocialIdentities(List<SocialIdentityData> identities) {
        Map<String, Long> seen = new HashMap<>();
        for (SocialIdentityData identity : identities) {
            String key = identity.provider() + ":" + identity.providerIdHmac();
            Long duplicateId = seen.putIfAbsent(key, identity.id());
            if (duplicateId != null) {
                throw new IllegalStateException(
                        "소셜 식별자가 중복됩니다. socialAccountIds=" + duplicateId + "," + identity.id());
            }
        }
    }

    private void prepareProtectedColumns(Connection connection) throws SQLException {
        execute(connection, """
                ALTER TABLE users
                    MODIFY COLUMN email_enc VARCHAR(512) NULL,
                    ADD COLUMN name_enc VARCHAR(1024) NULL AFTER name,
                    ADD COLUMN name_hmac CHAR(64) NULL AFTER name_enc
                """);
        execute(connection, """
                ALTER TABLE guests
                    ADD COLUMN name_enc VARCHAR(1024) NULL AFTER name,
                    ADD COLUMN name_hmac CHAR(64) NULL AFTER name_enc
                """);
        execute(connection, """
                ALTER TABLE payment_attempt
                    MODIFY COLUMN payload_json MEDIUMTEXT NOT NULL
                """);
        execute(connection, """
                ALTER TABLE user_social_accounts
                    ADD COLUMN provider_id_hmac CHAR(64) NULL AFTER provider_id
                """);
    }

    private void updateUsers(Connection connection, List<UserData> users) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE users
                SET email_enc = ?, email_hmac = ?,
                    name_enc = ?, name_hmac = ?,
                    phone_enc = ?, phone_hmac = ?
                WHERE id = ?
                """)) {
            for (UserData user : users) {
                statement.setString(1, user.emailEnc());
                statement.setString(2, user.emailHmac());
                statement.setString(3, user.nameEnc());
                statement.setString(4, user.nameHmac());
                statement.setString(5, user.phoneEnc());
                statement.setString(6, user.phoneHmac());
                statement.setLong(7, user.id());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void updateGuests(Connection connection, List<GuestData> guests) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE guests
                SET name_enc = ?, name_hmac = ?, phone_enc = ?, phone_hmac = ?
                WHERE id = ?
                """)) {
            for (GuestData guest : guests) {
                statement.setString(1, guest.nameEnc());
                statement.setString(2, guest.nameHmac());
                statement.setString(3, guest.phoneEnc());
                statement.setString(4, guest.phoneHmac());
                statement.setLong(5, guest.id());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void updatePaymentPayloads(Connection connection, List<PaymentPayloadData> payloads) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE payment_attempt SET payload_json = ? WHERE id = ?
                """)) {
            for (PaymentPayloadData payload : payloads) {
                statement.setString(1, payload.payloadEnc());
                statement.setLong(2, payload.id());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void updateSocialIdentities(Connection connection,
                                        List<SocialIdentityData> identities) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE user_social_accounts SET provider_id_hmac = ? WHERE id = ?
                """)) {
            for (SocialIdentityData identity : identities) {
                statement.setString(1, identity.providerIdHmac());
                statement.setLong(2, identity.id());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void replacePhoneVerifications(Connection connection) throws SQLException {
        execute(connection, "DROP TABLE phone_verifications");
        execute(connection, """
                CREATE TABLE phone_verifications
                (
                    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                    phone_hmac CHAR(64)     NOT NULL,
                    code_hmac  CHAR(64)     NOT NULL,
                    code_enc   VARCHAR(255) NOT NULL,
                    verified   BOOLEAN      NOT NULL DEFAULT FALSE,
                    expires_at DATETIME(6)  NOT NULL,
                    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    INDEX idx_pv_phone_created (phone_hmac, id)
                )
                """);
    }

    private void removePlaintextColumns(Connection connection) throws SQLException {
        execute(connection, """
                ALTER TABLE users
                    DROP INDEX uq_users_email,
                    DROP INDEX uq_users_provider_provider_id,
                    DROP INDEX idx_users_email_hmac,
                    MODIFY COLUMN email_enc VARCHAR(512) NOT NULL,
                    MODIFY COLUMN email_hmac CHAR(64) NOT NULL,
                    MODIFY COLUMN name_enc VARCHAR(1024) NOT NULL,
                    MODIFY COLUMN name_hmac CHAR(64) NOT NULL,
                    DROP COLUMN email,
                    DROP COLUMN name,
                    DROP COLUMN phone,
                    DROP COLUMN provider,
                    DROP COLUMN provider_id,
                    ADD CONSTRAINT uq_users_email_hmac UNIQUE (email_hmac),
                    ADD INDEX idx_users_name_hmac (name_hmac)
                """);
        execute(connection, """
                ALTER TABLE guests
                    MODIFY COLUMN name_enc VARCHAR(1024) NOT NULL,
                    MODIFY COLUMN name_hmac CHAR(64) NOT NULL,
                    DROP COLUMN name,
                    ADD INDEX idx_guests_name_hmac (name_hmac)
                """);
        execute(connection, """
                ALTER TABLE payment_attempt
                    CHANGE COLUMN payload_json payload_enc MEDIUMTEXT NOT NULL
                """);
        execute(connection, """
                ALTER TABLE user_social_accounts
                    DROP INDEX uq_user_social_accounts_provider_identity,
                    MODIFY COLUMN provider_id_hmac CHAR(64) NOT NULL,
                    DROP COLUMN provider_id,
                    ADD CONSTRAINT uq_user_social_accounts_provider_identity
                        UNIQUE (provider, provider_id_hmac)
                """);
        execute(connection, """
                ALTER TABLE fulfillments
                    DROP COLUMN address,
                    DROP COLUMN pickup_store
                """);
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private record UserData(
            long id,
            String emailEnc,
            String emailHmac,
            String nameEnc,
            String nameHmac,
            String phoneEnc,
            String phoneHmac) {}

    private record GuestData(
            long id,
            String nameEnc,
            String nameHmac,
            String phoneEnc,
            String phoneHmac) {}

    private record PaymentPayloadData(long id, String payloadEnc) {}

    private record SocialIdentityData(long id, String provider, String providerIdHmac) {}
}
