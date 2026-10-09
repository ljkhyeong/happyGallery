import type { RefundStatus } from "@/shared/types";

/** 관리자 화면의 환불 처리 상태 이름 */
export const ADMIN_REFUND_STATUS_LABEL: Record<RefundStatus, string> = {
  REQUESTED: "요청 접수",
  PROCESSING: "처리 중",
  RETRYABLE: "자동으로 다시 처리 예정",
  RECONCILIATION_REQUIRED: "결제사 확인 필요",
  SUCCEEDED: "환불 완료",
  FAILED: "환불 실패",
};
