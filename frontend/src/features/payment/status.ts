import type { CustomerPaymentStatus } from "./types";

export function shouldPollPaymentStatus(status: CustomerPaymentStatus | undefined): boolean {
  return status === "CONFIRMING" || status === "REFUNDING";
}
