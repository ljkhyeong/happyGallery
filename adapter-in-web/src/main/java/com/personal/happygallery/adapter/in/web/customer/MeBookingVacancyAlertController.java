package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.booking.dto.VacancyAlertResponse;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.booking.port.in.BookingVacancyAlertUseCase;
import com.personal.happygallery.domain.booking.BookingVacancyAlert;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeBookingVacancyAlertController {

    private final BookingVacancyAlertUseCase vacancyAlertUseCase;

    public MeBookingVacancyAlertController(BookingVacancyAlertUseCase vacancyAlertUseCase) {
        this.vacancyAlertUseCase = vacancyAlertUseCase;
    }

    @PostMapping("/slots/{slotId}/vacancy-alerts")
    @Operation(operationId = "registerMyVacancyAlert")
    public VacancyAlertResponse register(
            @PathVariable Long slotId,
            @AuthenticationPrincipal CustomerPrincipal customer
    ) {
        BookingVacancyAlert alert = vacancyAlertUseCase.registerMember(slotId, customer.userId());
        return VacancyAlertResponse.from(alert, null);
    }

    @GetMapping("/vacancy-alerts")
    @Operation(operationId = "listMyVacancyAlerts")
    public List<VacancyAlertResponse> list(
            @AuthenticationPrincipal CustomerPrincipal customer
    ) {
        return vacancyAlertUseCase.listMember(customer.userId()).stream()
                .map(alert -> VacancyAlertResponse.from(alert, null))
                .toList();
    }

    @DeleteMapping("/slots/{slotId}/vacancy-alerts")
    @Operation(operationId = "cancelMyVacancyAlert")
    public void cancel(
            @PathVariable Long slotId,
            @AuthenticationPrincipal CustomerPrincipal customer
    ) {
        vacancyAlertUseCase.cancelMember(slotId, customer.userId());
    }
}
