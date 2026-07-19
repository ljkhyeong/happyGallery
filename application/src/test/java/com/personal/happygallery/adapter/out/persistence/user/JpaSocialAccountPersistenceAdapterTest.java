package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.domain.crypto.BlindIndexKeyRing;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.user.SocialAccount;
import com.personal.happygallery.domain.user.SocialProvider;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaSocialAccountPersistenceAdapterTest {

    private static final byte[] ACTIVE_KEY = filledKey((byte) 1);
    private static final byte[] PREVIOUS_KEY = filledKey((byte) 2);

    @DisplayName("이전 HMAC 키로 찾은 소셜 계정은 활성 키와 암호문으로 즉시 갱신한다")
    @Test
    void findByPreviousHmac_backfillsActiveProtection() {
        String providerId = "legacy-provider-id";
        BlindIndexKeyRing keyRing = new BlindIndexKeyRing("v2", keyRing());
        FieldEncryptor fieldEncryptor = mock(FieldEncryptor.class);
        SocialAccountRepository repository = mock(SocialAccountRepository.class);
        SocialAccount legacyAccount = new SocialAccount(1L, SocialProvider.NAVER, providerId);
        legacyAccount.protect(null, new BlindIndexer(PREVIOUS_KEY).index(providerId));
        when(repository.findByProviderAndProviderIdHmacIn(
                SocialProvider.NAVER, keyRing.indexCandidates(providerId)))
                .thenReturn(List.of(legacyAccount));
        when(fieldEncryptor.encrypt(providerId)).thenReturn("hg:v2:encrypted-provider-id");
        when(repository.save(legacyAccount)).thenReturn(legacyAccount);
        JpaSocialAccountPersistenceAdapter adapter = new JpaSocialAccountPersistenceAdapter(
                repository, fieldEncryptor, keyRing);

        SocialAccount found = adapter.findByProviderAndProviderId(SocialProvider.NAVER, providerId)
                .orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(found.getProviderId()).isEqualTo(providerId);
            softly.assertThat(found.getProviderIdEnc()).isEqualTo("hg:v2:encrypted-provider-id");
            softly.assertThat(found.getProviderIdHmac()).isEqualTo(keyRing.index(providerId));
        });
        verify(repository).save(legacyAccount);
    }

    private static Map<String, byte[]> keyRing() {
        LinkedHashMap<String, byte[]> keys = new LinkedHashMap<>();
        keys.put("v2", ACTIVE_KEY);
        keys.put("v1", PREVIOUS_KEY);
        return keys;
    }

    private static byte[] filledKey(byte value) {
        byte[] key = new byte[32];
        Arrays.fill(key, value);
        return key;
    }
}
