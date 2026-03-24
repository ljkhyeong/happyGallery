package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.notice.port.in.NoticeAdminUseCase;
import com.personal.happygallery.app.notice.port.in.NoticeQueryUseCase;
import com.personal.happygallery.app.web.admin.dto.CreateNoticeRequest;
import com.personal.happygallery.app.web.admin.dto.UpdateNoticeRequest;
import com.personal.happygallery.app.web.notice.dto.NoticeDetailResponse;
import com.personal.happygallery.app.web.notice.dto.NoticeListResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/notices", "/admin/notices"})
public class AdminNoticeController {

    private final NoticeAdminUseCase noticeAdminUseCase;
    private final NoticeQueryUseCase noticeQueryUseCase;

    public AdminNoticeController(NoticeAdminUseCase noticeAdminUseCase,
                                 NoticeQueryUseCase noticeQueryUseCase) {
        this.noticeAdminUseCase = noticeAdminUseCase;
        this.noticeQueryUseCase = noticeQueryUseCase;
    }

    @GetMapping
    public List<NoticeListResponse> list() {
        return noticeQueryUseCase.listAll().stream()
                .map(NoticeListResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoticeDetailResponse create(@RequestBody @Valid CreateNoticeRequest request) {
        return NoticeDetailResponse.from(
                noticeAdminUseCase.create(request.title(), request.content(), request.pinned()));
    }

    @PutMapping("/{id}")
    public NoticeDetailResponse update(@PathVariable Long id,
                                       @RequestBody @Valid UpdateNoticeRequest request) {
        return NoticeDetailResponse.from(
                noticeAdminUseCase.update(id, request.title(), request.content(), request.pinned()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        noticeAdminUseCase.delete(id);
    }
}
