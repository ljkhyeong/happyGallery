package com.personal.happygallery.adapter.in.web.booking;

import com.personal.happygallery.application.booking.port.in.ClassQueryUseCase;
import com.personal.happygallery.adapter.in.web.booking.dto.ClassResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/classes")
public class ClassController {

    private final ClassQueryUseCase classQueryUseCase;

    public ClassController(ClassQueryUseCase classQueryUseCase) {
        this.classQueryUseCase = classQueryUseCase;
    }

    /** GET /api/v1/classes — 현재 운영 중인 클래스 목록 */
    @GetMapping
    @Operation(operationId = "listPublicClasses")
    public List<ClassResponse> listClasses() {
        return classQueryUseCase.listActive().stream()
                .map(ClassResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getPublicClass")
    public ClassResponse getClass(@PathVariable Long id) {
        return ClassResponse.from(classQueryUseCase.getActive(id));
    }
}
