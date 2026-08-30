package com.personal.happygallery.adapter.in.web.booking;

import com.personal.happygallery.adapter.in.web.booking.dto.GuestVacancyAlertRequest;
import com.personal.happygallery.adapter.in.web.booking.dto.VacancyAlertResponse;
import com.personal.happygallery.application.booking.port.in.BookingVacancyAlertUseCase;
import com.personal.happygallery.application.booking.port.in.BookingVacancyAlertUseCase.GuestAlertCommand;
import com.personal.happygallery.application.booking.port.in.BookingVacancyAlertUseCase.GuestAlertResult;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/slots/{slotId}/vacancy-alerts")
public class BookingVacancyAlertController {

    private final BookingVacancyAlertUseCase vacancyAlertUseCase;

    public BookingVacancyAlertController(BookingVacancyAlertUseCase vacancyAlertUseCase) {
        this.vacancyAlertUseCase = vacancyAlertUseCase;
    }

    @PostMapping
    @Operation(operationId = "registerGuestVacancyAlert")
    public VacancyAlertResponse register(
            @PathVariable Long slotId,
            @RequestBody @Valid GuestVacancyAlertRequest request
    ) {
        GuestAlertResult result = vacancyAlertUseCase.registerGuest(new GuestAlertCommand(
                slotId, request.name(), request.phone(), request.verificationCode()));
        return VacancyAlertResponse.from(result.alert(), result.accessToken());
    }

    @DeleteMapping
    @Operation(operationId = "cancelGuestVacancyAlert")
    public void cancel(
            @PathVariable Long slotId,
            @RequestHeader("X-Access-Token") String accessToken
    ) {
        vacancyAlertUseCase.cancelGuest(slotId, accessToken);
    }
}
