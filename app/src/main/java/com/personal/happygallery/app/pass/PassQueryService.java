package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PassQueryService {

    private final PassPurchaseReaderPort passPurchaseReader;

    public PassQueryService(PassPurchaseReaderPort passPurchaseReader) {
        this.passPurchaseReader = passPurchaseReader;
    }

    /** 회원 — 자기 8회권 목록 조회 */
    public List<PassPurchase> listMyPasses(Long userId) {
        return passPurchaseReader.findByUserIdOrderByPurchasedAtDesc(userId);
    }

    /** 회원 — 자기 8회권 상세 조회 (소유권 검증 포함) */
    public PassPurchase findMyPass(Long id, Long userId) {
        return passPurchaseReader.findById(id)
                .filter(p -> Objects.equals(p.getUserId(), userId))
                .orElseThrow(() -> new NotFoundException("8회권"));
    }
}
