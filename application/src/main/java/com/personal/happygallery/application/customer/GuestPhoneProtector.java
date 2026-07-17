package com.personal.happygallery.application.customer;

import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import org.springframework.stereotype.Component;

@Component
public class GuestPhoneProtector {

    private final FieldEncryptor fieldEncryptor;
    private final BlindIndexer blindIndexer;

    public GuestPhoneProtector(FieldEncryptor fieldEncryptor, BlindIndexer blindIndexer) {
        this.fieldEncryptor = fieldEncryptor;
        this.blindIndexer = blindIndexer;
    }

    public Guest newGuest(String name, String phone) {
        return new Guest(name, fieldEncryptor.encrypt(phone), index(phone));
    }

    public String index(String phone) {
        return blindIndexer.index(phone);
    }

    public String decrypt(Guest guest) {
        if (guest == null || guest.getPhoneEnc() == null) {
            throw new IllegalStateException("게스트 전화번호 암호문이 없습니다.");
        }
        return decryptEncryptedPhone(guest.getPhoneEnc());
    }

    public String decryptEncryptedPhone(String phoneEnc) {
        if (phoneEnc == null) {
            throw new IllegalStateException("게스트 전화번호 암호문이 없습니다.");
        }
        return fieldEncryptor.decrypt(phoneEnc);
    }
}
