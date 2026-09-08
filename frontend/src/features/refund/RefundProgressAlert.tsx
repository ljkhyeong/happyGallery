import { Alert } from "react-bootstrap";
import { formatKRW } from "@/shared/lib";
import { refundContent, refundCouponMessage, refundProgressLabel } from "./refundPresentation";
import type { OrderDetailResponse, RefundProgress } from "@/shared/types";

interface Props {
  refund: RefundProgress | null;
  couponStatus?: OrderDetailResponse["couponStatus"];
}

export function RefundProgressAlert({ refund, couponStatus }: Props) {
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
        {refund.restoreCoupon && <li>{refundCouponMessage(refund.status, couponStatus)}</li>}
      </ul>
    </Alert>
  );
}
