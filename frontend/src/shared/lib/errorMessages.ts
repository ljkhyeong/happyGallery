import type { ErrorCode } from "@/shared/types/error";

const ERROR_MESSAGES: Record<ErrorCode, string> = {
  INVALID_INPUT: "입력값이 올바르지 않습니다. 다시 확인해 주세요.",
  PHONE_VERIFICATION_FAILED: "휴대폰 인증에 실패했습니다. 인증코드를 확인해 주세요.",
  UNAUTHORIZED: "인증이 필요합니다. 다시 로그인해 주세요.",
  NOT_FOUND: "요청하신 정보를 찾을 수 없습니다.",
  ALREADY_REFUNDED: "이미 환불 처리된 항목입니다.",
  INVENTORY_NOT_ENOUGH: "재고가 부족합니다.",
  CAPACITY_EXCEEDED: "정원이 초과되었습니다. 다른 시간을 선택해 주세요.",
  DUPLICATE_BOOKING: "이미 해당 슬롯에 예약이 있습니다.",
  SLOT_NOT_AVAILABLE: "선택하신 슬롯은 예약할 수 없습니다.",
  BOOKING_CONFLICT: "예약 처리 중 충돌이 발생했습니다. 다시 시도해 주세요.",
  CONFLICT: "처리 중 충돌이 감지되었습니다. 잠시 후 다시 시도해 주세요.",
  TOO_MANY_REQUESTS: "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.",
  REFUND_NOT_ALLOWED: "현재 상태에서는 환불할 수 없습니다.",
  PRODUCTION_REFUND_NOT_ALLOWED: "제작 진행 중에는 환불할 수 없습니다.",
  CHANGE_NOT_ALLOWED: "현재 상태에서는 변경할 수 없습니다.",
  PASS_EXPIRED: "이용권이 만료되었습니다.",
  PASS_CREDIT_INSUFFICIENT: "이용권 잔여 횟수가 부족합니다.",
  PAYMENT_METHOD_NOT_ALLOWED: "허용되지 않는 결제 수단입니다.",
  INTERNAL_ERROR: "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
};

export function getUserMessage(code: string): string | undefined {
  return ERROR_MESSAGES[code as ErrorCode];
}
