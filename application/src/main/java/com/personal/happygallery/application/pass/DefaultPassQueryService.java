package com.personal.happygallery.application.pass;

import com.personal.happygallery.application.pass.port.in.PassQueryUseCase;
import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;

@Service
@Transactional(readOnly = true)
public class DefaultPassQueryService implements PassQueryUseCase {

    private final PassPurchaseReaderPort passPurchaseReader;
    private final RefundPort refundPort;

    public DefaultPassQueryService(PassPurchaseReaderPort passPurchaseReader,
                                   RefundPort refundPort) {
        this.passPurchaseReader = passPurchaseReader;
        this.refundPort = refundPort;
    }

    /** 회원 — 자기 8회권 목록 조회 */
    @Override
    public List<PassView> listMyPasses(Long userId) {
        return listMyPasses(userId, null, PageParams.MAX_SIZE).content();
    }

    @Override
    public CursorPage<PassView> listMyPasses(Long userId, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        int fetchSize = pageSize + 1;
        List<PassPurchase> passes;
        if (cursor == null) {
            passes = passPurchaseReader.findByUserIdOrderByPurchasedAtDesc(userId, fetchSize);
        } else {
            var cursorParam = CursorUtils.decode(cursor);
            passes = passPurchaseReader.findByUserIdOrderByPurchasedAtDescAfter(
                    userId, cursorParam.timestamp(), cursorParam.id(), fetchSize);
        }
        List<PassView> views = withRefunds(passes);
        return CursorPage.of(
                views,
                pageSize,
                view -> CursorUtils.encode(
                        view.pass().getPurchasedAt(), view.pass().getId()));
    }

    private List<PassView> withRefunds(List<PassPurchase> passes) {
        if (passes.isEmpty()) {
            return List.of();
        }
        List<Long> passIds = passes.stream()
                .map(PassPurchase::getId)
                .toList();
        Map<Long, Refund> refundsByPassId = refundPort.findByPassPurchaseIdIn(passIds)
                .stream()
                .collect(toMap(Refund::getPassPurchaseId, Function.identity()));
        return passes.stream()
                .map(pass -> new PassView(pass, refundsByPassId.get(pass.getId())))
                .toList();
    }

    /** 회원 — 자기 8회권 상세 조회 (소유권 검증 포함) */
    @Override
    public PassView findMyPass(Long id, Long userId) {
        PassPurchase pass = passPurchaseReader.findById(id)
                .filter(p -> Objects.equals(p.getUserId(), userId))
                .orElseThrow(NotFoundException.supplier("8회권"));
        return new PassView(pass, refundPort.findByPassPurchaseId(id).orElse(null));
    }
}
