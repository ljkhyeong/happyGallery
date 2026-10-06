/** 예약금은 수업료 합계의 10%(원 미만 버림)이고, 나머지는 수업 당일 공방에서 잔금으로 받는다. */
export function splitBookingAmount(totalAmount: number): { depositAmount: number; balanceAmount: number } {
  const depositAmount = Math.floor(totalAmount / 10);
  return { depositAmount, balanceAmount: totalAmount - depositAmount };
}
