package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.pass.PassExpiryBatchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/passes")
public class AdminPassController {

    private final PassExpiryBatchService passExpiryBatchService;

    public AdminPassController(PassExpiryBatchService passExpiryBatchService) {
        this.passExpiryBatchService = passExpiryBatchService;
    }

    /** 만료 배치 수동 트리거 — 스케줄러 미구현 시 운영자가 직접 호출 */
    @PostMapping("/expire")
    public Map<String, Integer> triggerExpiry() {
        int count = passExpiryBatchService.expireAll();
        return Map.of("expiredCount", count);
    }
}
