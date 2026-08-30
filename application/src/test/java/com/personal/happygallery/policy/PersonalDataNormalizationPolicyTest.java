package com.personal.happygallery.policy;

import com.personal.happygallery.application.crypto.SpringSecurityFieldEncryptor;
import com.personal.happygallery.application.customer.GuestPersonalDataProtector;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("policy")
class PersonalDataNormalizationPolicyTest {

    private final GuestPersonalDataProtector protector = new GuestPersonalDataProtector(
            new SpringSecurityFieldEncryptor(new byte[32]),
            new BlindIndexer(new byte[32]));

    @DisplayName("Unicode 공백이 포함된 휴대폰 번호도 저장과 조회에서 같은 HMAC을 사용한다")
    @Test
    void phoneWithUnicodeWhitespace_hasSameHmacForStorageAndLookup() {
        Guest guest = protector.newGuest("홍길동", "010\u20031234\u00a05678");

        assertThat(guest.getPhoneHmac()).isEqualTo(protector.indexPhone("010-1234-5678"));
    }
}
