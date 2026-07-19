import { adminHeaders, api } from "@/shared/api";

interface AdminPasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
}

export function changeAdminPassword(
  adminKey: string,
  request: AdminPasswordChangeRequest,
): Promise<void> {
  return api("/admin/auth/password", {
    method: "PATCH",
    headers: adminHeaders(adminKey),
    body: request,
  });
}
