package com.personal.happygallery.app.pass.port.in;

import com.personal.happygallery.domain.pass.PassPurchase;
import java.util.List;

/**
 * 8회권 조회 유스케이스.
 *
 * <p>회원이 자기 8회권 목록·상세를 조회한다.
 */
public interface PassQueryUseCase {

    List<PassPurchase> listMyPasses(Long userId);

    PassPurchase findMyPass(Long id, Long userId);
}
