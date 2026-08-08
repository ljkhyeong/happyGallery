package com.personal.happygallery.application.pass.port.in;

import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.util.List;

/**
 * 8회권 조회 유스케이스.
 *
 * <p>회원이 자기 8회권 목록·상세를 조회한다.
 */
public interface PassQueryUseCase {

    List<PassView> listMyPasses(Long userId);

    CursorPage<PassView> listMyPasses(Long userId, String cursor, int size);

    PassView findMyPass(Long id, Long userId);

    record PassView(PassPurchase pass, Refund refund) {}
}
