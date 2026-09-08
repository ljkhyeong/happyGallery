import { formatKRW } from "../../shared/lib/format.ts";
import type { OrderDetailResponse, RefundProgress } from "../../shared/types/index.ts";

export function refundProgressLabel(status: RefundProgress["status"]): string {
  switch (status) {
    case "REQUESTED":
      return "요청됨";
    case "PROCESSING":
      return "처리 중";
    case "RETRYABLE":
    case "RECONCILIATION_REQUIRED":
      return "결과 확인 중";
    case "SUCCEEDED":
      return "완료";
    case "FAILED":
      return "공방에서 확인 중";
  }
}

export function refundContent(refund: RefundProgress) {
  switch (refund.status) {
    case "REQUESTED":
    case "PROCESSING":
      return {
        variant: "info",
        title: "환불 처리 중",
        message: refund.amount > 0
          ? `${formatKRW(refund.amount)}을 환불하고 있습니다. 완료되면 알림으로 안내합니다.`
          : "결제 금액 환불 없이 아래 항목을 처리하고 있습니다.",
      };
    case "RETRYABLE":
    case "RECONCILIATION_REQUIRED":
      return {
        variant: "warning",
        title: "환불 상태 확인 중",
        message: "결제사 처리 결과를 확인하고 있습니다. 완료되면 알림으로 안내합니다.",
      };
    case "SUCCEEDED":
      return {
        variant: "success",
        title: "환불 완료",
        message: refund.amount > 0
          ? `${formatKRW(refund.amount)} 환불이 완료되었습니다.`
          : "환불할 결제 금액은 없습니다.",
      };
    case "FAILED":
      return {
        variant: "warning",
        title: "환불 확인 필요",
        message: "환불 처리가 완료되지 않아 공방에서 확인하고 있습니다.",
      };
  }
}

export function refundCouponMessage(status: RefundProgress["status"], couponStatus: OrderDetailResponse["couponStatus"] | undefined): string {
  if (status !== "SUCCEEDED") return `쿠폰 사용 취소 · ${refundProgressLabel(status)}`;
  switch (couponStatus) {
    case "AVAILABLE": return "쿠폰을 다시 사용할 수 있습니다.";
    case "EXPIRED": return "쿠폰 유효기간이 지나 다시 사용할 수 없습니다.";
    case "RESERVED": return "쿠폰을 다른 결제에 사용 중입니다.";
    case "REDEEMED": return "이미 사용한 쿠폰입니다.";
    case "CANCELED": return "사용이 중지된 쿠폰입니다.";
    default: return "쿠폰 상태는 내 쿠폰에서 확인해 주세요.";
  }
}
