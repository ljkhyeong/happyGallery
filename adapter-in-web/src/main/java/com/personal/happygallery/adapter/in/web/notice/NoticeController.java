package com.personal.happygallery.adapter.in.web.notice;

import com.personal.happygallery.application.notice.port.in.NoticeQueryUseCase;
import com.personal.happygallery.adapter.in.web.notice.dto.NoticeDetailResponse;
import com.personal.happygallery.adapter.in.web.notice.dto.NoticeListResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeQueryUseCase noticeQueryUseCase;

    public NoticeController(NoticeQueryUseCase noticeQueryUseCase) {
        this.noticeQueryUseCase = noticeQueryUseCase;
    }

    @GetMapping
    @Operation(operationId = "listPublicNotices")
    public List<NoticeListResponse> list() {
        return noticeQueryUseCase.listAll().stream()
                .map(NoticeListResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getPublicNotice")
    public ResponseEntity<NoticeDetailResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(NoticeDetailResponse.from(noticeQueryUseCase.getDetail(id)));
    }
}
