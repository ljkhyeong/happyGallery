import { updateMyPhone } from "@/generated/api/customerAuth";
import type { CustomerUser } from "./useCustomerAuth";

export function updateMemberPhone(
  phone: string,
  verificationCode: string,
): Promise<CustomerUser> {
  return updateMyPhone({ phone, verificationCode });
}
