package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminSetupRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminSetupStatusResponse;
import com.personal.happygallery.adapter.in.web.config.properties.AdminSetupProperties;
import com.personal.happygallery.application.admin.port.in.AdminSetupUseCase;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 최초 관리자 계정 one-time setup.
 *
 * <p>가드: {@link AdminSetupProperties#enabled()} && {@link AdminSetupUseCase#isAvailable()}
 * <p>두 조건을 모두 만족하지 않으면 404 를 반환하여 엔드포인트 존재 자체를 감춘다.
 */
@RestController
@RequestMapping({"/api/v1/admin/setup", "/admin/setup"})
public class AdminSetupController {

    private final AdminSetupProperties setupProperties;
    private final AdminSetupUseCase adminSetupUseCase;

    public AdminSetupController(AdminSetupProperties setupProperties, AdminSetupUseCase adminSetupUseCase) {
        this.setupProperties = setupProperties;
        this.adminSetupUseCase = adminSetupUseCase;
    }

    @GetMapping("/status")
    public AdminSetupStatusResponse status() {
        boolean required = setupProperties.enabled() && adminSetupUseCase.isAvailable();
        return new AdminSetupStatusResponse(required);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void setup(@RequestBody @Valid AdminSetupRequest request) {
        if (!setupProperties.enabled() || !adminSetupUseCase.isAvailable()) {
            throw new HappyGalleryException(ErrorCode.NOT_FOUND, "setup 이 비활성 상태입니다.");
        }
        if (!tokenMatches(request.token())) {
            throw new HappyGalleryException(ErrorCode.UNAUTHORIZED, "setup 토큰이 일치하지 않습니다.");
        }
        adminSetupUseCase.setup(request.username(), request.password());
    }

    private boolean tokenMatches(String submitted) {
        byte[] expected = setupProperties.token().getBytes(StandardCharsets.UTF_8);
        byte[] given = submitted.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, given);
    }
}
