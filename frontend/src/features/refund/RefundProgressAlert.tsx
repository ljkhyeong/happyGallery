import { Alert } from "react-bootstrap";
import { formatKRW } from "@/shared/lib";
import type { RefundProgress } from "@/shared/types";

export function RefundProgressAlert({ refund }: { refund: RefundProgress | null }) {
  if (!refund) return null;

  const content = refundContent(refund);
  const progressLabel = refundProgressLabel(refund.status);
  return (
    <Alert variant={content.variant} className="mt-3 mb-0 py-2">
      <strong>{content.title}</strong>
      <span className="d-block small mt-1">{content.message}</span>
      <ul className="small mb-0 mt-2 ps-3">
        {refund.pgRefundAmount > 0 && (
          <li>결제사 환불 {formatKRW(refund.pgRefundAmount)} · {progressLabel}</li>
        )}
        {refund.rewardRestoreAmount > 0 && (
          <li>적립금 복원 {refund.rewardRestoreAmount.toLocaleString("ko-KR")}P · {progressLabel}</li>
        )}
        {refund.rewardRevokeAmount > 0 && (
          <li>지급 적립금 회수 {refund.rewardRevokeAmount.toLocaleString("ko-KR")}P · {progressLabel}</li>
        )}
        {refund.restoreCoupon && <li>쿠폰 사용 상태 정리 · {progressLabel}</li>}
      </ul>
    </Alert>
  );
}

function refundProgressLabel(status: RefundProgress["status"]): string {
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

function refundContent(refund: RefundProgress) {
  switch (refund.status) {
    case "REQUESTED":
    case "PROCESSING":
      return {
        variant: "info",
        title: "환불 처리 중",
        message: refund.amount > 0
          ? `${formatKRW(refund.amount)}의 고객 반환 절차를 처리하고 있습니다. 완료되면 알림으로 안내합니다.`
          : "금액 반환 없이 쿠폰 등 주문 혜택의 후속 처리를 진행하고 있습니다.",
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
          ? `${formatKRW(refund.amount)}의 고객 반환 처리가 완료되었습니다.`
          : refund.restoreCoupon
            ? "결제 금액 반환 없이 쿠폰 사용 상태 정리가 완료되었습니다."
            : "금액 반환이 없는 환불 후속 처리가 완료되었습니다.",
      };
    case "FAILED":
      return {
        variant: "warning",
        title: "환불 확인 필요",
        message: "환불 처리가 완료되지 않아 공방에서 확인하고 있습니다.",
      };
  }
}
