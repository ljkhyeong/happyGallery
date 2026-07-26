import {
  adminLogin,
  adminLogout,
  beginAdminMfaEnrollment,
  changeAdminPassword as changeAdminPasswordRequest,
  confirmAdminMfaEnrollment as confirmAdminMfaEnrollmentRequest,
  disableAdminMfa as disableAdminMfaRequest,
  getAdminMfaStatus as getAdminMfaStatusRequest,
  verifyAdminMfa as verifyAdminMfaRequest,
  type AdminMfaEnrollmentResponse,
  type AdminMfaRecoveryCodesResponse,
  type AdminMfaStatusResponse,
  type AdminPasswordChangeRequest,
  type LoginResponse,
} from "@/generated/api/adminAuth";
import { adminHeaders } from "@/shared/api";

export type AdminAuthResponse = LoginResponse;
export type {
  AdminMfaEnrollmentResponse,
  AdminMfaStatusResponse,
} from "@/generated/api/adminAuth";

export function loginAdmin(username: string, password: string): Promise<AdminAuthResponse> {
  return adminLogin({ username, password });
}

export function verifyAdminMfa(
  challengeToken: string,
  code: string,
): Promise<AdminAuthResponse> {
  return verifyAdminMfaRequest({ challengeToken, code });
}

export function logoutAdmin(adminKey: string): Promise<void> {
  return adminLogout({ headers: adminHeaders(adminKey) });
}

export function changeAdminPassword(
  adminKey: string,
  request: AdminPasswordChangeRequest,
): Promise<void> {
  return changeAdminPasswordRequest(request, {
    headers: adminHeaders(adminKey),
  });
}

export function getAdminMfaStatus(adminKey: string): Promise<AdminMfaStatusResponse> {
  return getAdminMfaStatusRequest({
    headers: adminHeaders(adminKey),
  });
}

export function startAdminMfaEnrollment(
  adminKey: string,
): Promise<AdminMfaEnrollmentResponse> {
  return beginAdminMfaEnrollment({
    headers: adminHeaders(adminKey),
  });
}

export function confirmAdminMfaEnrollment(
  adminKey: string,
  code: string,
): Promise<AdminMfaRecoveryCodesResponse> {
  return confirmAdminMfaEnrollmentRequest({ code }, {
    headers: adminHeaders(adminKey),
  });
}

export function disableAdminMfa(
  adminKey: string,
  currentPassword: string,
  code: string,
): Promise<void> {
  return disableAdminMfaRequest({ currentPassword, code }, {
    headers: adminHeaders(adminKey),
  });
}
