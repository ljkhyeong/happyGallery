package com.personal.happygallery.application.admin.port.out;

import java.util.List;

public interface AdminTotpPort {

    Enrollment generateEnrollment(String username);

    boolean verify(String secret, String code);

    List<String> generateRecoveryCodes(int count);

    record Enrollment(String secret, String provisioningUri) {}
}
