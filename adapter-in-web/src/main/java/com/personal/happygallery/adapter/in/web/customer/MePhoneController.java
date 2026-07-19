package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.CustomerUserResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.RegisterInitialPhoneRequest;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.customer.port.in.InitialMemberPhoneRegistrationUseCase;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.User;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/phone")
public class MePhoneController {

    private final InitialMemberPhoneRegistrationUseCase phoneRegistration;
    private final SubjectRateLimitGuard rateLimitGuard;

    public MePhoneController(InitialMemberPhoneRegistrationUseCase phoneRegistration,
                             SubjectRateLimitGuard rateLimitGuard) {
        this.phoneRegistration = phoneRegistration;
        this.rateLimitGuard = rateLimitGuard;
    }

    @PatchMapping
    public CustomerUserResponse registerInitialPhone(
            @RequestBody @Valid RegisterInitialPhoneRequest request,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        String phone = KoreanPhoneNumber.required(request.phone());
        rateLimitGuard.checkPhoneVerificationAttempt(phone);
        User user = phoneRegistration.register(
                customer.userId(), phone, request.verificationCode());
        return CustomerUserResponse.from(user);
    }
}
