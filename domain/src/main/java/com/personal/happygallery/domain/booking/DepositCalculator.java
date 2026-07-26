package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;

/**
 * 예약금 산출 — 클래스 가격의 10%.
 *
 * <p>근거: {@code docs/PRD/0001_기준_스펙/spec.md} "예약금: 클래스 가격의 10%".
 * 클라이언트 입력값을 신뢰하지 않고 서버가 슬롯의 클래스 가격으로 직접 산출한다.
 */
public final class DepositCalculator {

    private DepositCalculator() {}

    /** 슬롯의 클래스 가격 기준 10% 예약금. */
    public static long of(Slot slot) {
        return calculate(slot, 1).depositAmount();
    }

    /** 예약 인원 전체의 예약금과 잔금을 overflow 검출 산술로 계산한다. */
    public static BookingAmounts calculate(Slot slot, int participantCount) {
        SlotCapacity.requireValidParticipantCount(participantCount);
        try {
            long totalAmount = Math.multiplyExact(
                    slot.getBookingClass().getPrice(), participantCount);
            PaymentAmountPolicy.requireValid(totalAmount);
            long depositAmount = totalAmount / 10;
            return new BookingAmounts(
                    depositAmount,
                    Math.subtractExact(totalAmount, depositAmount));
        } catch (ArithmeticException e) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "예약 금액이 허용 범위를 초과했습니다.");
        }
    }

    public record BookingAmounts(long depositAmount, long balanceAmount) {}
}
