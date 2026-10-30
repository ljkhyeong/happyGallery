import type { ReactNode } from "react";
import { Badge } from "react-bootstrap";
import { getStatusLabel, type StatusAudience } from "@/shared/lib/labels";


const VARIANT_MAP: Record<string, string> = {
  // Booking
  BOOKED: "primary",
  CANCELED: "secondary",
  NO_SHOW: "danger",
  COMPLETED: "success",
  // Order
  PAID_APPROVAL_PENDING: "warning",
  APPROVED_FULFILLMENT_PENDING: "info",
  REJECTED: "secondary",
  CUSTOMER_CANCELED: "secondary",
  AUTO_REFUND_TIMEOUT: "secondary",
  IN_PRODUCTION: "info",
  DELAY_CONSENT_PENDING: "warning",
  DELAY_ACCEPTED: "warning",
  DELAY_REJECTED_CANCELED: "secondary",
  PICKUP_READY: "info",
  PICKED_UP: "success",
  PICKUP_EXPIRED: "secondary",
  PICKUP_FORFEITED: "secondary",
  SHIPPING_PREPARING: "info",
  SHIPPED: "primary",
  DELIVERED: "success",
  // Payment
  READY: "secondary",
  CONFIRMING: "info",
  RETRYABLE: "warning",
  FAILED: "danger",
  REVIEW_REQUIRED: "warning",
  REFUNDING: "info",
  REFUNDED: "success",
  SUPPORT_REQUIRED: "warning",
  EXPIRED: "secondary",
  // Pass
  ACTIVE: "success",
  USED_UP: "secondary",
  REFUND_PENDING: "warning",
  REFUND_FAILED: "danger",
};

/**
 * 고객 화면 배지 색(README 프론트엔드 디자인 기준): 진행 중은 먹색, 끝난 상태와 이름표는 회색 면,
 * 고객이 답하거나 확인할 일은 노랑(먹색 글자), 실패·오류만 빨강이다. 관리자 화면은 위 색을 그대로 쓴다.
 */
export type CustomerBadgeTone = "progress" | "done" | "action" | "error";

const TONE_BG: Record<CustomerBadgeTone, string> = {
  progress: "dark",
  done: "light",
  action: "warning",
  error: "danger",
};

const VARIANT_TONE: Record<string, CustomerBadgeTone> = {
  primary: "progress",
  info: "progress",
  success: "done",
  secondary: "done",
  warning: "action",
  danger: "error",
};

// 관리자 색과 고객 색이 다른 상태. 공방·결제사 차례인 대기와 이미 동의한 지연은 진행 중이고,
// 불참은 오류가 아니라 끝난 상태다. 노랑은 지연 동의처럼 고객이 답할 상태에만 남긴다.
const CUSTOMER_STATUS_TONE: Record<string, CustomerBadgeTone> = {
  NO_SHOW: "done",
  ACTIVE: "progress",
  PAID_APPROVAL_PENDING: "progress",
  DELAY_ACCEPTED: "progress",
  REVIEW_REQUIRED: "progress",
  REFUND_PENDING: "progress",
};

interface ToneBadgeProps {
  tone: CustomerBadgeTone;
  className?: string;
  children: ReactNode;
}

/** 서버 상태 이름이 아닌 기능별 상태(문의 답변·알림 신청 등)를 고객 화면 색 기준으로 보여 준다. */
export function ToneBadge({ tone, className = "badge-status", children }: ToneBadgeProps) {
  const bg = TONE_BG[tone];
  return (
    <Badge bg={bg} text={bg === "light" || bg === "warning" ? "dark" : undefined} className={className}>
      {children}
    </Badge>
  );
}

interface Props {
  status: string;
  audience?: StatusAudience;
}

export function StatusBadge({ status, audience = "customer" }: Props) {
  const variant = VARIANT_MAP[status] ?? "secondary";
  if (audience === "customer") {
    return (
      <ToneBadge tone={CUSTOMER_STATUS_TONE[status] ?? VARIANT_TONE[variant] ?? "done"}>
        {getStatusLabel(status, audience)}
      </ToneBadge>
    );
  }
  return (
    <Badge bg={variant} text={variant === "light" || variant === "warning" ? "dark" : undefined} className="badge-status">
      {getStatusLabel(status, audience)}
    </Badge>
  );
}
