package com.personal.happygallery.app.booking;

import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.infra.booking.ClassRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClassQueryService {

    private final ClassRepository classRepository;

    public ClassQueryService(ClassRepository classRepository) {
        this.classRepository = classRepository;
    }

    /** 전체 클래스 목록 조회 */
    public List<BookingClass> listAll() {
        return classRepository.findAll();
    }
}
