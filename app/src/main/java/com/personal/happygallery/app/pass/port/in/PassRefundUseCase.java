package com.personal.happygallery.app.pass.port.in;

import com.personal.happygallery.app.pass.PassRefundService.PassRefundResult;

/**
 * 8회권 환불 유스케이스.
 *
 * <p>관리자가 수동으로 전체 환불을 처리한다.
 */
public interface PassRefundUseCase {

    PassRefundResult refundPass(Long passId);
}
