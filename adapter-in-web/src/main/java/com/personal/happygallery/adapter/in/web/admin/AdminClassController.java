package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.CreateClassRequest;
import com.personal.happygallery.adapter.in.web.booking.dto.ClassResponse;
import com.personal.happygallery.application.booking.port.in.ClassManagementUseCase;
import com.personal.happygallery.domain.booking.BookingClass;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/classes", "/admin/classes"})
public class AdminClassController {

    private final ClassManagementUseCase classManagementUseCase;

    public AdminClassController(ClassManagementUseCase classManagementUseCase) {
        this.classManagementUseCase = classManagementUseCase;
    }

    /** POST /admin/classes — 클래스 생성 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClassResponse createClass(@RequestBody @Valid CreateClassRequest request) {
        BookingClass bookingClass = classManagementUseCase.createClass(
                request.name(),
                request.category(),
                request.durationMin(),
                request.price(),
                request.bufferMin()
        );
        return ClassResponse.from(bookingClass);
    }
}
