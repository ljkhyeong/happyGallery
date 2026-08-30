import {
  changeMyPassword,
  resetCustomerPassword,
} from "@/generated/api/customerAuth";
import { normalizePhone } from "@/shared/validation/phone";

export function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  return changeMyPassword({ currentPassword, newPassword });
}

export function resetPassword(
  email: string,
  phone: string,
  verificationCode: string,
  newPassword: string,
): Promise<void> {
  return resetCustomerPassword({
    email,
    phone: normalizePhone(phone),
    verificationCode,
    newPassword,
  });
}
