import type { CustomerPaymentStatus } from "./types";

export function shouldPollPaymentStatus(status: CustomerPaymentStatus | undefined): boolean {
  return status === "CONFIRMING" || status === "REFUNDING";
}

export function isTerminalPaymentStatus(status: CustomerPaymentStatus): boolean {
  return status === "COMPLETED"
    || status === "REFUNDED"
    || status === "FAILED"
    || status === "EXPIRED";
}
