package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.LatestVerificationCodeResponse;
import com.personal.happygallery.application.customer.port.in.DevEmailVerificationQueryUseCase;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** local/dev E2E 전용 이메일 인증 코드 조회 API. */
@Profile({"local", "dev"})
@RestController
@RequestMapping("/api/v1/admin/dev/email-verifications")
public class LocalEmailVerificationController {

    private final DevEmailVerificationQueryUseCase verificationQuery;

    public LocalEmailVerificationController(DevEmailVerificationQueryUseCase verificationQuery) {
        this.verificationQuery = verificationQuery;
    }

    @GetMapping("/latest")
    public ResponseEntity<LatestVerificationCodeResponse> latestCode(
            @RequestParam Long userId,
            @RequestParam String email
    ) {
        return ResponseEntity.of(verificationQuery.findLatestUnverifiedCode(userId, email)
                .map(LatestVerificationCodeResponse::new));
    }
}
