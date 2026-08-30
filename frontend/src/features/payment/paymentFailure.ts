const PAYMENT_FAILURE_MESSAGE_BY_CODE = new Map<string, string>([
  ["PAY_PROCESS_CANCELED", "결제창에서 결제를 취소했습니다."],
  ["PAY_PROCESS_ABORTED", "결제가 중단되었습니다. 결제 수단 정보를 확인한 뒤 다시 시도해 주세요."],
  ["REJECT_CARD_COMPANY", "카드사에서 결제를 승인하지 않았습니다. 카드사 또는 다른 결제 수단을 확인해 주세요."],
  ["INVALID_CARD_NUMBER", "카드 번호를 확인한 뒤 다시 시도해 주세요."],
  ["INVALID_CARD_EXPIRATION", "카드 유효기간을 확인한 뒤 다시 시도해 주세요."],
  ["INVALID_STOPPED_CARD", "사용이 중지된 카드입니다. 다른 결제 수단을 이용해 주세요."],
  ["EXCEED_MAX_DAILY_PAYMENT_COUNT", "카드의 일일 결제 한도를 초과했습니다. 다른 결제 수단을 이용해 주세요."],
  ["NOT_SUPPORTED_METHOD", "지원하지 않는 결제 수단입니다. 다른 결제 수단을 이용해 주세요."],
  ["NOT_AVAILABLE_PAYMENT", "현재 사용할 수 없는 결제 수단입니다. 다른 결제 수단을 이용해 주세요."],
]);

const DEFAULT_PAYMENT_FAILURE_MESSAGE =
  "결제가 완료되지 않았습니다. 잠시 후 다시 시도해 주세요.";

export function paymentFailureMessage(code: string | null): string {
  if (!code) return DEFAULT_PAYMENT_FAILURE_MESSAGE;
  return PAYMENT_FAILURE_MESSAGE_BY_CODE.get(code) ?? DEFAULT_PAYMENT_FAILURE_MESSAGE;
}
