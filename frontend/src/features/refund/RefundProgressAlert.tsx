import { Alert } from "react-bootstrap";
import { formatKRW } from "@/shared/lib";
import type { RefundProgress } from "@/shared/types";

export function RefundProgressAlert({ refund }: { refund: RefundProgress | null }) {
  if (!refund) return null;

  const content = refundContent(refund);
  return (
    <Alert variant={content.variant} className="mt-3 mb-0 py-2">
      <strong>{content.title}</strong>
      <span className="d-block small mt-1">{content.message}</span>
    </Alert>
  );
}

function refundContent(refund: RefundProgress) {
  switch (refund.status) {
    case "REQUESTED":
    case "PROCESSING":
      return {
        variant: "info",
        title: "환불 처리 중",
        message: `${formatKRW(refund.amount)} 환불을 처리하고 있습니다. 완료되면 알림으로 안내합니다.`,
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
        message: `${formatKRW(refund.amount)} 환불이 완료되었습니다.`,
      };
    case "FAILED":
      return {
        variant: "warning",
        title: "환불 확인 필요",
        message: "환불 처리가 완료되지 않아 운영자가 확인하고 있습니다.",
      };
  }
}
