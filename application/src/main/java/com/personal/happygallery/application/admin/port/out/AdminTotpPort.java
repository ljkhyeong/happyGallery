package com.personal.happygallery.application.admin.port.out;

import java.util.List;
import java.util.OptionalLong;

public interface AdminTotpPort {

    Enrollment generateEnrollment(String username);

    OptionalLong findMatchingTimeStep(String secret, String code);

    List<String> generateRecoveryCodes(int count);

    record Enrollment(String secret, String provisioningUri) {}
}
