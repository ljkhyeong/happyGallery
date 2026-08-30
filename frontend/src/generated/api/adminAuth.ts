import { generatedApiClient } from '../../shared/api/generatedClient';
export interface LoginRequest {
  /**
     * @minLength 1
     * @maxLength 72
     */
  password: string;
  /**
     * @minLength 3
     * @maxLength 50
     * @pattern ^[A-Za-z0-9._-]+$
     */
  username: string;
}

export type LoginResponseStatus = typeof LoginResponseStatus[keyof typeof LoginResponseStatus];


export const LoginResponseStatus = {
  AUTHENTICATED: 'AUTHENTICATED',
  MFA_REQUIRED: 'MFA_REQUIRED',
} as const;

export interface LoginResponse {
  /** @nullable */
  challengeToken: string | null;
  status: LoginResponseStatus;
  /** @nullable */
  token: string | null;
}

export interface AdminMfaDisableRequest {
  /**
     * @minLength 0
     * @maxLength 32
     */
  code: string;
  /**
     * @minLength 1
     * @maxLength 72
     */
  currentPassword: string;
}

export interface AdminMfaStatusResponse {
  enabled: boolean;
  enrollmentPending: boolean;
  /** @minimum 0 */
  recoveryCodesRemaining: number;
  recoveryResetAvailable: boolean;
}

export interface AdminMfaEnrollmentResponse {
  provisioningUri: string;
  secret: string;
}

export interface AdminMfaCodeRequest {
  /**
     * @minLength 0
     * @maxLength 32
     */
  code: string;
}

export interface AdminMfaRecoveryCodesResponse {
  recoveryCodes: string[];
}

export interface AdminMfaRecoveryRequest {
  /**
     * @minLength 1
     * @maxLength 72
     */
  currentPassword: string;
}

export interface AdminMfaVerificationRequest {
  /**
     * @minLength 0
     * @maxLength 100
     */
  challengeToken: string;
  /**
     * @minLength 0
     * @maxLength 32
     */
  code: string;
}

export interface AdminPasswordChangeRequest {
  /**
     * @minLength 1
     * @maxLength 72
     */
  currentPassword: string;
  /**
     * @minLength 10
     * @maxLength 72
     */
  newPassword: string;
}

export const getAdminLoginUrl = () => {




  return `/api/v1/admin/auth/login`
}

export const adminLogin = async (loginRequest: LoginRequest, options?: RequestInit): Promise<LoginResponse> => {

  return generatedApiClient<LoginResponse>(getAdminLoginUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(loginRequest)
  }
);}



export const getAdminLogoutUrl = () => {




  return `/api/v1/admin/auth/logout`
}

export const adminLogout = async ( options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getAdminLogoutUrl(),
  {
    ...options,
    method: 'POST'


  }
);}



export const getDisableAdminMfaUrl = () => {




  return `/api/v1/admin/auth/mfa`
}

export const disableAdminMfa = async (adminMfaDisableRequest: AdminMfaDisableRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getDisableAdminMfaUrl(),
  {
    ...options,
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(adminMfaDisableRequest)
  }
);}



export const getGetAdminMfaStatusUrl = () => {




  return `/api/v1/admin/auth/mfa`
}

export const getAdminMfaStatus = async ( options?: RequestInit): Promise<AdminMfaStatusResponse> => {

  return generatedApiClient<AdminMfaStatusResponse>(getGetAdminMfaStatusUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getBeginAdminMfaEnrollmentUrl = () => {




  return `/api/v1/admin/auth/mfa/enrollment`
}

export const beginAdminMfaEnrollment = async ( options?: RequestInit): Promise<AdminMfaEnrollmentResponse> => {

  return generatedApiClient<AdminMfaEnrollmentResponse>(getBeginAdminMfaEnrollmentUrl(),
  {
    ...options,
    method: 'POST'


  }
);}



export const getConfirmAdminMfaEnrollmentUrl = () => {




  return `/api/v1/admin/auth/mfa/enrollment/confirm`
}

export const confirmAdminMfaEnrollment = async (adminMfaCodeRequest: AdminMfaCodeRequest, options?: RequestInit): Promise<AdminMfaRecoveryCodesResponse> => {

  return generatedApiClient<AdminMfaRecoveryCodesResponse>(getConfirmAdminMfaEnrollmentUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(adminMfaCodeRequest)
  }
);}



export const getRecoverAdminMfaUrl = () => {




  return `/api/v1/admin/auth/mfa/recovery`
}

export const recoverAdminMfa = async (adminMfaRecoveryRequest: AdminMfaRecoveryRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getRecoverAdminMfaUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(adminMfaRecoveryRequest)
  }
);}



export const getVerifyAdminMfaUrl = () => {




  return `/api/v1/admin/auth/mfa/verify`
}

export const verifyAdminMfa = async (adminMfaVerificationRequest: AdminMfaVerificationRequest, options?: RequestInit): Promise<LoginResponse> => {

  return generatedApiClient<LoginResponse>(getVerifyAdminMfaUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(adminMfaVerificationRequest)
  }
);}



export const getChangeAdminPasswordUrl = () => {




  return `/api/v1/admin/auth/password`
}

export const changeAdminPassword = async (adminPasswordChangeRequest: AdminPasswordChangeRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getChangeAdminPasswordUrl(),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(adminPasswordChangeRequest)
  }
);}
