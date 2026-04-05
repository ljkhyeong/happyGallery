package com.personal.happygallery.app.admin.port.in;

import com.personal.happygallery.app.admin.port.AdminSession;
import java.util.Optional;

public interface AdminAuthUseCase {

    String login(String username, String rawPassword);

    Optional<AdminSession> validateToken(String token);

    void logout(String token);
}
