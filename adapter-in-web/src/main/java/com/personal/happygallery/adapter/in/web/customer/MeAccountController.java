package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerStepUpAuthenticationStore;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase.WithdrawCommand;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeAccountController {

    private final CustomerAccountLifecycleUseCase accountLifecycle;
    private final CustomerSessionBinder customerSessionBinder;
    private final CustomerStepUpAuthenticationStore stepUpAuthenticationStore;

    public MeAccountController(CustomerAccountLifecycleUseCase accountLifecycle,
                               CustomerSessionBinder customerSessionBinder,
                               CustomerStepUpAuthenticationStore stepUpAuthenticationStore) {
        this.accountLifecycle = accountLifecycle;
        this.customerSessionBinder = customerSessionBinder;
        this.stepUpAuthenticationStore = stepUpAuthenticationStore;
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "withdrawMyAccount")
    public void withdraw(@AuthenticationPrincipal CustomerPrincipal customer,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        accountLifecycle.withdraw(new WithdrawCommand(
                customer.userId(),
                customer.credentialVersion(),
                stepUpAuthenticationStore.isRecentlyVerified(
                        request, customer.userId(), customer.credentialVersion())));
        customerSessionBinder.unbindIfBoundTo(request, response, customer.userId());
    }
}
