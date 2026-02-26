package com.personal.happygallery.domain.booking;

/**
 * 예약금 결제 수단.
 * <ul>
 *   <li>{@link #CARD} — 신용/체크카드 (허용)</li>
 *   <li>{@link #EASY_PAY} — 간편결제(카카오페이, 네이버페이 등) (허용)</li>
 *   <li>{@link #BANK_TRANSFER} — 계좌이체 (예약금 결제 불가)</li>
 * </ul>
 */
public enum DepositPaymentMethod {
    CARD,
    EASY_PAY,
    BANK_TRANSFER
}
