import { api } from "@/shared/api";
import type { CustomerUser } from "./useCustomerAuth";

export function registerInitialPhone(phone: string, verificationCode: string) {
  return api<CustomerUser>("/me/phone", {
    method: "PATCH",
    body: { phone, verificationCode },
  });
}
