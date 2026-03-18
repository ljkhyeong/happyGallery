package com.personal.happygallery.app.web.booking;

import com.personal.happygallery.app.booking.port.in.ClassQueryUseCase;
import com.personal.happygallery.app.web.booking.dto.ClassResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/classes", "/classes"})
public class ClassController {

    private final ClassQueryUseCase classQueryService;

    public ClassController(ClassQueryUseCase classQueryService) {
        this.classQueryService = classQueryService;
    }

    /** GET /classes — 전체 클래스 목록 */
    @GetMapping
    public List<ClassResponse> listClasses() {
        return classQueryService.listAll().stream()
                .map(ClassResponse::from)
                .toList();
    }
}
