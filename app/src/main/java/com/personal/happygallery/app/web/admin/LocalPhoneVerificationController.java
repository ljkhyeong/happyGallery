package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.customer.port.in.DevPhoneVerificationQueryUseCase;
import com.personal.happygallery.app.web.admin.dto.LatestVerificationCodeResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * local/E2E 전용 — 가장 최근 미소모 인증 코드를 조회한다.
 * 프로덕션에서는 빈 등록되지 않는다.
 */
@Profile("local")
@RestController
@RequestMapping({"/api/v1/admin/dev/phone-verifications", "/admin/dev/phone-verifications"})
public class LocalPhoneVerificationController {

    private final DevPhoneVerificationQueryUseCase phoneVerificationQuery;

    public LocalPhoneVerificationController(DevPhoneVerificationQueryUseCase phoneVerificationQuery) {
        this.phoneVerificationQuery = phoneVerificationQuery;
    }

    @GetMapping("/latest")
    public ResponseEntity<LatestVerificationCodeResponse> latestCode(@RequestParam String phone) {
        return phoneVerificationQuery.findLatestUnverifiedCode(phone)
                .map(code -> ResponseEntity.ok(new LatestVerificationCodeResponse(code)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
