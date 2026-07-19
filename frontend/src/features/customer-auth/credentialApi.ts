import { api } from "@/shared/api";
import { normalizePhone } from "@/shared/validation/phone";

export function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  return api("/me/password", {
    method: "PATCH",
    body: { currentPassword, newPassword },
  });
}

export function resetPassword(
  email: string,
  phone: string,
  verificationCode: string,
  newPassword: string,
): Promise<void> {
  return api("/auth/password/reset", {
    method: "POST",
    body: {
      email,
      phone: normalizePhone(phone),
      verificationCode,
      newPassword,
    },
  });
}
