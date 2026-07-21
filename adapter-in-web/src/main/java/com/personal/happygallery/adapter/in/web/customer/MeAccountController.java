package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase;
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

    public MeAccountController(CustomerAccountLifecycleUseCase accountLifecycle,
                               CustomerSessionBinder customerSessionBinder) {
        this.accountLifecycle = accountLifecycle;
        this.customerSessionBinder = customerSessionBinder;
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal CustomerPrincipal customer,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        accountLifecycle.withdraw(customer.userId());
        customerSessionBinder.unbindIfBoundTo(request, response, customer.userId());
    }
}
