package com.personal.happygallery.application.customer;

import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.PersonalName;
import org.springframework.stereotype.Component;

@Component
public class GuestPersonalDataProtector {

    private final FieldEncryptor fieldEncryptor;
    private final BlindIndexer blindIndexer;

    public GuestPersonalDataProtector(FieldEncryptor fieldEncryptor, BlindIndexer blindIndexer) {
        this.fieldEncryptor = fieldEncryptor;
        this.blindIndexer = blindIndexer;
    }

    public Guest newGuest(String name, String phone) {
        String normalizedName = PersonalName.required(name);
        String normalizedPhone = KoreanPhoneNumber.required(phone);
        return new Guest(
                fieldEncryptor.encrypt(normalizedName), blindIndexer.index(normalizedName),
                fieldEncryptor.encrypt(normalizedPhone), blindIndexer.index(normalizedPhone));
    }

    public String indexPhone(String phone) {
        return blindIndexer.index(KoreanPhoneNumber.required(phone));
    }

    public String decryptPhone(Guest guest) {
        if (guest == null) {
            throw new IllegalStateException("게스트 정보가 없습니다.");
        }
        return decrypt(guest.getPhoneEnc(), "게스트 전화번호 암호문이 없습니다.");
    }

    public String decryptName(Guest guest) {
        if (guest == null) {
            throw new IllegalStateException("게스트 정보가 없습니다.");
        }
        return decrypt(guest.getNameEnc(), "게스트 이름 암호문이 없습니다.");
    }

    private String decrypt(String encrypted, String missingMessage) {
        if (encrypted == null) {
            throw new IllegalStateException(missingMessage);
        }
        return fieldEncryptor.decrypt(encrypted);
    }
}
