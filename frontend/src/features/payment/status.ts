import type { PaymentStatusResponse } from "@/generated/api/paymentQuery";
import type { CustomerPaymentStatus } from "./types";

/** 결제 확인·환불 진행 중인 상태를 다시 조회하는 간격. */
export const PAYMENT_STATUS_POLL_INTERVAL_MS = 3_000;

export function shouldPollPaymentStatus(status: CustomerPaymentStatus | undefined): boolean {
  return status === "CONFIRMING" || status === "REFUNDING";
}

export function paymentStatusPollingInterval(status: CustomerPaymentStatus | undefined): number | false {
  return shouldPollPaymentStatus(status) ? PAYMENT_STATUS_POLL_INTERVAL_MS : false;
}

export function isTerminalPaymentStatus(status: CustomerPaymentStatus): boolean {
  return status === "COMPLETED"
    || status === "REFUNDED"
    || status === "FAILED"
    || status === "EXPIRED";
}

/** 승인되지 않았거나 종료·환불되어 남은 결제가 없는 상태. 구매 화면에서 다시 시작할 수 있다. */
export function isEndedWithoutPayment(status: CustomerPaymentStatus): boolean {
  return isTerminalPaymentStatus(status) && status !== "COMPLETED";
}

/** 결제가 끝나 주문·예약·이용권이 만들어진 상태. 결과 화면의 다음 행동을 이 값으로 정한다. */
export function isCompletedPayment(status: PaymentStatusResponse): boolean {
  return status.status === "COMPLETED" && status.domainId != null;
}

/** 결과 카드 아이콘. 완료는 체크, 승인 실패만 빨간 경고이고 확인·환불 진행과 만료는 정보 아이콘이다. */
export function paymentResultTone(status: PaymentStatusResponse): "success" | "danger" | "notice" {
  if (isCompletedPayment(status)) return "success";
  return status.status === "FAILED" ? "danger" : "notice";
}
