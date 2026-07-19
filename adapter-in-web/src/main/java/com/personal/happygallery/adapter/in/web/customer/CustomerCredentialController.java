package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.ChangePasswordRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.ResetPasswordRequest;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.customer.port.in.CustomerCredentialUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerCredentialUseCase.ChangePasswordCommand;
import com.personal.happygallery.application.customer.port.in.CustomerCredentialUseCase.ResetPasswordCommand;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CustomerCredentialController {

    private final CustomerCredentialUseCase credentials;
    private final CustomerSessionBinder customerSessionBinder;
    private final SubjectRateLimitGuard rateLimitGuard;

    public CustomerCredentialController(CustomerCredentialUseCase credentials,
                                        CustomerSessionBinder customerSessionBinder,
                                        SubjectRateLimitGuard rateLimitGuard) {
        this.credentials = credentials;
        this.customerSessionBinder = customerSessionBinder;
        this.rateLimitGuard = rateLimitGuard;
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestBody @Valid ChangePasswordRequest request,
                               @AuthenticationPrincipal CustomerPrincipal customer,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        credentials.changePassword(new ChangePasswordCommand(
                customer.userId(), request.currentPassword(), request.newPassword()));
        customerSessionBinder.unbind(httpRequest, httpResponse);
    }

    @PostMapping("/auth/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@RequestBody @Valid ResetPasswordRequest request,
                              HttpServletRequest httpRequest,
                              HttpServletResponse httpResponse) {
        String phone = KoreanPhoneNumber.required(request.phone());
        rateLimitGuard.checkPhoneVerificationAttempt(phone);
        Long userId = credentials.resetPassword(new ResetPasswordCommand(
                request.email(), phone, request.verificationCode(), request.newPassword()));
        customerSessionBinder.unbindIfBoundTo(
                httpRequest, httpResponse, userId);
    }
}
