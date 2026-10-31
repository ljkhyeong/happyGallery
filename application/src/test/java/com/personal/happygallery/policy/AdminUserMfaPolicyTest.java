package com.personal.happygallery.policy;

import com.personal.happygallery.domain.admin.AdminUser;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("policy")
class AdminUserMfaPolicyTest {

    @DisplayName("활성 MFA는 등록 재시작·재활성화를 거부하고 현재 등록 상태를 유지한다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("activeMfaRejectedCommands")
    void activeMfa_rejectsReenrollment_withoutChangingMfaState(Consumer<AdminUser> command) {
        AdminUser admin = enabledAdmin();
        admin.acceptTotpStep(123L);
        long credentialVersion = admin.getCredentialVersion();

        assertThatThrownBy(() -> command.accept(admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MFA가 이미 활성화되어 있습니다.");

        assertSoftly(softly -> {
            softly.assertThat(admin.getCredentialVersion()).isEqualTo(credentialVersion);
            softly.assertThat(admin.isMfaEnabled()).isTrue();
            softly.assertThat(admin.getTotpSecretEnc()).isEqualTo("encrypted-secret");
            softly.assertThat(admin.getLastAcceptedTotpStep()).isEqualTo(123L);
        });
    }

    private static Stream<Named<Consumer<AdminUser>>> activeMfaRejectedCommands() {
        return Stream.of(
                Named.named("새 등록 시작", admin -> admin.beginMfaEnrollment("replacement-secret")),
                Named.named("다시 활성화", AdminUser::enableMfa));
    }

    @DisplayName("이미 비활성인 MFA는 다시 비활성화할 수 없고 자격 증명 버전을 유지한다")
    @Test
    void disableMfa_rejectsAlreadyDisabledState_withoutChangingCredentialVersion() {
        AdminUser admin = enabledAdmin();
        admin.disableMfa();
        long credentialVersion = admin.getCredentialVersion();

        assertThatThrownBy(admin::disableMfa)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MFA가 활성화되어 있지 않습니다.");

        assertSoftly(softly -> {
            softly.assertThat(admin.getCredentialVersion()).isEqualTo(credentialVersion);
            softly.assertThat(admin.isMfaEnabled()).isFalse();
            softly.assertThat(admin.getTotpSecretEnc()).isNull();
        });
    }

    private AdminUser enabledAdmin() {
        AdminUser admin = new AdminUser("admin", "password-hash");
        admin.beginMfaEnrollment("encrypted-secret");
        admin.enableMfa();
        return admin;
    }
}
