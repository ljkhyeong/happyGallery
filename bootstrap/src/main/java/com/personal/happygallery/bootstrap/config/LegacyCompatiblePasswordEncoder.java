package com.personal.happygallery.bootstrap.config;

import java.util.Map;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

final class LegacyCompatiblePasswordEncoder implements PasswordEncoder {

    private static final String BCRYPT_ID = "bcrypt";
    private static final String BCRYPT_PREFIX = "{bcrypt}";

    private final BCryptPasswordEncoder bcrypt;
    private final DelegatingPasswordEncoder delegating;

    LegacyCompatiblePasswordEncoder(BCryptPasswordEncoder bcrypt) {
        this.bcrypt = bcrypt;
        this.delegating = new DelegatingPasswordEncoder(BCRYPT_ID, Map.of(BCRYPT_ID, bcrypt));
        this.delegating.setDefaultPasswordEncoderForMatches(bcrypt);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return delegating.matches(rawPassword, encodedPassword);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        String bcryptHash = encodedPassword.startsWith(BCRYPT_PREFIX)
                ? encodedPassword.substring(BCRYPT_PREFIX.length())
                : encodedPassword;
        return bcrypt.upgradeEncoding(bcryptHash);
    }
}
