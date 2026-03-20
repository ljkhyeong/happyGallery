package com.personal.happygallery.app.web.admin.port.in;

import com.personal.happygallery.app.admin.port.out.AdminSessionPort.AdminSession;
import java.util.Optional;

public interface AdminAuthUseCase {

    String login(String username, String rawPassword);

    Optional<AdminSession> validateToken(String token);

    void logout(String token);
}
