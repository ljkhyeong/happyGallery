package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.CustomerUserResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.UpdateMemberPhoneRequest;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerStepUpAuthenticationStore;
import com.personal.happygallery.application.customer.port.in.MemberPhoneUpdateUseCase;
import com.personal.happygallery.application.customer.port.in.MemberPhoneUpdateUseCase.UpdatePhoneCommand;
import com.personal.happygallery.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/phone")
public class MePhoneController {

    private final MemberPhoneUpdateUseCase phoneUpdate;
    private final CustomerStepUpAuthenticationStore stepUpAuthenticationStore;
    private final CustomerSessionBinder customerSessionBinder;

    public MePhoneController(MemberPhoneUpdateUseCase phoneUpdate,
                             CustomerStepUpAuthenticationStore stepUpAuthenticationStore,
                             CustomerSessionBinder customerSessionBinder) {
        this.phoneUpdate = phoneUpdate;
        this.stepUpAuthenticationStore = stepUpAuthenticationStore;
        this.customerSessionBinder = customerSessionBinder;
    }

    @PatchMapping
    @Operation(operationId = "updateMyPhone")
    public CustomerUserResponse updatePhone(
            @RequestBody @Valid UpdateMemberPhoneRequest request,
            @AuthenticationPrincipal CustomerPrincipal customer,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        boolean recentlyReauthenticated = stepUpAuthenticationStore.isRecentlyVerified(
                httpRequest, customer.userId(), customer.credentialVersion());
        User user = phoneUpdate.update(new UpdatePhoneCommand(
                customer.userId(),
                customer.credentialVersion(),
                request.phone(),
                request.verificationCode(),
                recentlyReauthenticated));
        if (user.getCredentialVersion() != customer.credentialVersion()) {
            customerSessionBinder.unbindIfBoundTo(
                    httpRequest, httpResponse, customer.userId());
        }
        return CustomerUserResponse.from(user);
    }
}
