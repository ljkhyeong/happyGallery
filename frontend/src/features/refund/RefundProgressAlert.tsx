import { Alert } from "react-bootstrap";
import { formatKRW } from "@/shared/lib";
import { refundContent, refundCouponMessage, refundProgressLabel } from "./refundPresentation";
import type { OrderDetailResponse } from "@/generated/api/order";
import type { RefundProgress } from "@/shared/types";

interface Props {
  refund: RefundProgress | null;
  couponStatus?: OrderDetailResponse["couponStatus"];
  /** 바깥 여백. 주문·예약 상세 카드는 `.my-detail-card > .alert` 여백을 쓰므로 비워 둔다. */
  className?: string;
}

export function RefundProgressAlert({ refund, couponStatus, className }: Props) {
  if (!refund) return null;

  const content = refundContent(refund);
  const progressLabel = refundProgressLabel(refund.status);
  return (
    <Alert variant={content.variant} className={className ? `py-2 ${className}` : "py-2"}>
      <strong>{content.title}</strong>
      <span className="d-block small mt-1">{content.message}</span>
      <ul className="small mb-0 mt-2 ps-3">
        {refund.pgRefundAmount > 0 && (
          <li>결제사 환불 {formatKRW(refund.pgRefundAmount)} · {progressLabel}</li>
        )}
        {refund.rewardRestoreAmount > 0 && (
          <li>적립금 복원 {formatKRW(refund.rewardRestoreAmount)} · {progressLabel}</li>
        )}
        {refund.rewardRevokeAmount > 0 && (
          <li>지급 적립금 회수 {formatKRW(refund.rewardRevokeAmount)} · {progressLabel}</li>
        )}
        {refund.restoreCoupon && <li>{refundCouponMessage(refund.status, couponStatus)}</li>}
      </ul>
    </Alert>
  );
}
