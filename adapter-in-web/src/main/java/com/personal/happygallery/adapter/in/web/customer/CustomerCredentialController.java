package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.ChangePasswordRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.ResetPasswordRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.PasswordReauthenticationRequest;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerStepUpAuthenticationStore;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.application.customer.port.in.CustomerCredentialUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerCredentialUseCase.ChangePasswordCommand;
import com.personal.happygallery.application.customer.port.in.CustomerCredentialUseCase.ResetPasswordCommand;
import io.swagger.v3.oas.annotations.Operation;
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
    private final CustomerStepUpAuthenticationStore stepUpAuthenticationStore;
    private final SubjectRateLimitGuard rateLimitGuard;

    public CustomerCredentialController(CustomerCredentialUseCase credentials,
                                        CustomerSessionBinder customerSessionBinder,
                                        CustomerStepUpAuthenticationStore stepUpAuthenticationStore,
                                        SubjectRateLimitGuard rateLimitGuard) {
        this.credentials = credentials;
        this.customerSessionBinder = customerSessionBinder;
        this.stepUpAuthenticationStore = stepUpAuthenticationStore;
        this.rateLimitGuard = rateLimitGuard;
    }

    @PostMapping("/me/reauthentication/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "reauthenticateMyPassword")
    public void reauthenticatePassword(
            @RequestBody @Valid PasswordReauthenticationRequest request,
            @AuthenticationPrincipal CustomerPrincipal customer,
            HttpServletRequest httpRequest) {
        rateLimitGuard.checkCustomerReauthentication(customer.userId());
        credentials.verifyPassword(customer.userId(), request.currentPassword());
        stepUpAuthenticationStore.markVerified(
                httpRequest, customer.userId(), customer.credentialVersion());
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "changeMyPassword")
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
    @Operation(operationId = "resetCustomerPassword")
    public void resetPassword(@RequestBody @Valid ResetPasswordRequest request,
                              HttpServletRequest httpRequest,
                              HttpServletResponse httpResponse) {
        Long userId = credentials.resetPassword(new ResetPasswordCommand(
                request.email(), request.phone(), request.verificationCode(), request.newPassword()));
        customerSessionBinder.unbindIfBoundTo(
                httpRequest, httpResponse, userId);
    }
}
