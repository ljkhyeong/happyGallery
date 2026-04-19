package com.personal.happygallery.adapter.in.web.booking;

import com.personal.happygallery.application.booking.port.in.ClassQueryUseCase;
import com.personal.happygallery.adapter.in.web.booking.dto.ClassResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/classes", "/classes"})
public class ClassController {

    private final ClassQueryUseCase classQueryUseCase;

    public ClassController(ClassQueryUseCase classQueryUseCase) {
        this.classQueryUseCase = classQueryUseCase;
    }

    /** GET /classes — 전체 클래스 목록 */
    @GetMapping
    public List<ClassResponse> listClasses() {
        return classQueryUseCase.listAll().stream()
                .map(ClassResponse::from)
                .toList();
    }
}
