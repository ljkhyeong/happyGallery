package com.personal.happygallery.domain.booking;

/**
 * 예약금 산출 — 클래스 가격의 10%.
 *
 * <p>근거: {@code docs/PRD/0001_기준_스펙/spec.md} "예약금: 클래스 가격의 10%".
 * 클라이언트 입력값을 신뢰하지 않고 서버가 슬롯의 클래스 가격으로 직접 산출한다.
 */
public final class DepositCalculator {

    private static final int DEPOSIT_PERCENT = 10;

    private DepositCalculator() {}

    /** 슬롯의 클래스 가격 기준 10% 예약금. */
    public static long of(Slot slot) {
        long classPrice = slot.getBookingClass().getPrice();
        return classPrice * DEPOSIT_PERCENT / 100;
    }
}
