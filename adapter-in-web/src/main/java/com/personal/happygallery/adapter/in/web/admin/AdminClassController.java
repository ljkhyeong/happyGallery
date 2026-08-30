package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminClassResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.CreateClassRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.UpdateClassRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.UpdateClassStatusRequest;
import com.personal.happygallery.application.booking.port.in.ClassManagementUseCase;
import com.personal.happygallery.application.booking.port.in.ClassManagementUseCase.CreateClassCommand;
import com.personal.happygallery.application.booking.port.in.ClassManagementUseCase.UpdateClassCommand;
import com.personal.happygallery.application.booking.port.in.ClassQueryUseCase;
import com.personal.happygallery.domain.booking.BookingClass;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/classes")
public class AdminClassController {

    private final ClassManagementUseCase classManagementUseCase;
    private final ClassQueryUseCase classQueryUseCase;

    public AdminClassController(ClassManagementUseCase classManagementUseCase,
                                ClassQueryUseCase classQueryUseCase) {
        this.classManagementUseCase = classManagementUseCase;
        this.classQueryUseCase = classQueryUseCase;
    }

    /** POST /api/v1/admin/classes — 클래스 생성 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminClassResponse createClass(@RequestBody @Valid CreateClassRequest request) {
        BookingClass bookingClass = classManagementUseCase.createClass(new CreateClassCommand(
                request.name(), request.category(), request.durationMin(), request.price(), request.bufferMin(),
                request.capacity(), request.passEligible(), request.description(), request.imageUrl(),
                request.preparationInfo(), request.targetAudience()));
        return AdminClassResponse.from(bookingClass);
    }

    @GetMapping
    public List<AdminClassResponse> listClasses() {
        return classQueryUseCase.listAll().stream().map(AdminClassResponse::from).toList();
    }

    @PatchMapping("/{id}")
    public AdminClassResponse updateClass(@PathVariable Long id,
                                          @RequestBody @Valid UpdateClassRequest request) {
        return AdminClassResponse.from(classManagementUseCase.updateClass(new UpdateClassCommand(
                id, request.name(), request.category(), request.price(), request.passEligible(),
                request.description(), request.imageUrl(),
                request.preparationInfo(), request.targetAudience())));
    }

    @PatchMapping("/{id}/status")
    @Operation(operationId = "changeAdminClassStatus")
    public AdminClassResponse changeStatus(@PathVariable Long id,
                                           @RequestBody @Valid UpdateClassStatusRequest request) {
        return AdminClassResponse.from(classManagementUseCase.changeStatus(id, request.status()));
    }
}
