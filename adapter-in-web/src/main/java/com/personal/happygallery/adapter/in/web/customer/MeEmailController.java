package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.RegisterEmailRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.SendEmailVerificationRequest;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerStepUpAuthenticationStore;
import com.personal.happygallery.application.customer.port.in.MemberEmailRegistrationUseCase;
import com.personal.happygallery.application.customer.port.in.MemberEmailRegistrationUseCase.RegisterEmailCommand;
import com.personal.happygallery.application.customer.port.in.MemberEmailRegistrationUseCase.SendVerificationCommand;
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
@RequestMapping("/api/v1/me")
public class MeEmailController {

    private final MemberEmailRegistrationUseCase emailRegistration;
    private final CustomerStepUpAuthenticationStore stepUpAuthenticationStore;
    private final CustomerSessionBinder customerSessionBinder;

    public MeEmailController(
            MemberEmailRegistrationUseCase emailRegistration,
            CustomerStepUpAuthenticationStore stepUpAuthenticationStore,
            CustomerSessionBinder customerSessionBinder
    ) {
        this.emailRegistration = emailRegistration;
        this.stepUpAuthenticationStore = stepUpAuthenticationStore;
        this.customerSessionBinder = customerSessionBinder;
    }

    @PostMapping("/email-verifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "sendMyEmailVerification")
    public void sendVerificationCode(
            @RequestBody @Valid SendEmailVerificationRequest request,
            @AuthenticationPrincipal CustomerPrincipal customer,
            HttpServletRequest httpRequest
    ) {
        emailRegistration.sendVerificationCode(new SendVerificationCommand(
                customer.userId(),
                customer.credentialVersion(),
                request.email(),
                recentlyReauthenticated(httpRequest, customer)));
    }

    @PatchMapping("/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "registerMyVerifiedEmail")
    public void registerEmail(
            @RequestBody @Valid RegisterEmailRequest request,
            @AuthenticationPrincipal CustomerPrincipal customer,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        emailRegistration.registerVerifiedEmail(new RegisterEmailCommand(
                customer.userId(),
                customer.credentialVersion(),
                request.email(),
                request.verificationCode(),
                recentlyReauthenticated(httpRequest, customer)));
        customerSessionBinder.unbindIfBoundTo(
                httpRequest, httpResponse, customer.userId());
    }

    private boolean recentlyReauthenticated(
            HttpServletRequest request,
            CustomerPrincipal customer
    ) {
        return stepUpAuthenticationStore.isRecentlyVerified(
                request, customer.userId(), customer.credentialVersion());
    }
}
