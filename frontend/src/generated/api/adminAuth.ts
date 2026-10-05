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

export const adminLogin = async (loginRequest: LoginRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<LoginResponse> => {

    const getHeaders = (h?: NonNullable<RequestInit['headers']>): Record<string, string | readonly string[]> => {
    if (!h) return {};
    if (h instanceof Headers) return Object.fromEntries(h.entries());
    if (Symbol.iterator in h) {
      return Object.fromEntries(
        Array.from(h as Iterable<Iterable<string>>, (entry) => Array.from(entry) as [string, string]),
      );
    }
    const headers: Record<string, string | readonly string[]> = {};
    for (const [name, value] of Object.entries<string | readonly string[] | undefined>(h)) {
      if (value !== undefined) headers[name] = value;
    }
    return headers;
  };
return generatedApiClient<LoginResponse>(getAdminLoginUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(loginRequest)
  }
);}



export const getAdminLogoutUrl = () => {




  return `/api/v1/admin/auth/logout`
}

export const adminLogout = async ( options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

  return generatedApiClient<void>(getAdminLogoutUrl(),
  {
    ...options,
    method: 'POST'


  }
);}



export const getDisableAdminMfaUrl = () => {




  return `/api/v1/admin/auth/mfa`
}

export const disableAdminMfa = async (adminMfaDisableRequest: AdminMfaDisableRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

    const getHeaders = (h?: NonNullable<RequestInit['headers']>): Record<string, string | readonly string[]> => {
    if (!h) return {};
    if (h instanceof Headers) return Object.fromEntries(h.entries());
    if (Symbol.iterator in h) {
      return Object.fromEntries(
        Array.from(h as Iterable<Iterable<string>>, (entry) => Array.from(entry) as [string, string]),
      );
    }
    const headers: Record<string, string | readonly string[]> = {};
    for (const [name, value] of Object.entries<string | readonly string[] | undefined>(h)) {
      if (value !== undefined) headers[name] = value;
    }
    return headers;
  };
return generatedApiClient<void>(getDisableAdminMfaUrl(),
  {
    ...options,
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(adminMfaDisableRequest)
  }
);}



export const getGetAdminMfaStatusUrl = () => {




  return `/api/v1/admin/auth/mfa`
}

export const getAdminMfaStatus = async ( options?: Parameters<typeof generatedApiClient>[1]): Promise<AdminMfaStatusResponse> => {

  return generatedApiClient<AdminMfaStatusResponse>(getGetAdminMfaStatusUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getBeginAdminMfaEnrollmentUrl = () => {




  return `/api/v1/admin/auth/mfa/enrollment`
}

export const beginAdminMfaEnrollment = async ( options?: Parameters<typeof generatedApiClient>[1]): Promise<AdminMfaEnrollmentResponse> => {

  return generatedApiClient<AdminMfaEnrollmentResponse>(getBeginAdminMfaEnrollmentUrl(),
  {
    ...options,
    method: 'POST'


  }
);}



export const getConfirmAdminMfaEnrollmentUrl = () => {




  return `/api/v1/admin/auth/mfa/enrollment/confirm`
}

export const confirmAdminMfaEnrollment = async (adminMfaCodeRequest: AdminMfaCodeRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<AdminMfaRecoveryCodesResponse> => {

    const getHeaders = (h?: NonNullable<RequestInit['headers']>): Record<string, string | readonly string[]> => {
    if (!h) return {};
    if (h instanceof Headers) return Object.fromEntries(h.entries());
    if (Symbol.iterator in h) {
      return Object.fromEntries(
        Array.from(h as Iterable<Iterable<string>>, (entry) => Array.from(entry) as [string, string]),
      );
    }
    const headers: Record<string, string | readonly string[]> = {};
    for (const [name, value] of Object.entries<string | readonly string[] | undefined>(h)) {
      if (value !== undefined) headers[name] = value;
    }
    return headers;
  };
return generatedApiClient<AdminMfaRecoveryCodesResponse>(getConfirmAdminMfaEnrollmentUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(adminMfaCodeRequest)
  }
);}



export const getRecoverAdminMfaUrl = () => {




  return `/api/v1/admin/auth/mfa/recovery`
}

export const recoverAdminMfa = async (adminMfaRecoveryRequest: AdminMfaRecoveryRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

    const getHeaders = (h?: NonNullable<RequestInit['headers']>): Record<string, string | readonly string[]> => {
    if (!h) return {};
    if (h instanceof Headers) return Object.fromEntries(h.entries());
    if (Symbol.iterator in h) {
      return Object.fromEntries(
        Array.from(h as Iterable<Iterable<string>>, (entry) => Array.from(entry) as [string, string]),
      );
    }
    const headers: Record<string, string | readonly string[]> = {};
    for (const [name, value] of Object.entries<string | readonly string[] | undefined>(h)) {
      if (value !== undefined) headers[name] = value;
    }
    return headers;
  };
return generatedApiClient<void>(getRecoverAdminMfaUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(adminMfaRecoveryRequest)
  }
);}



export const getVerifyAdminMfaUrl = () => {




  return `/api/v1/admin/auth/mfa/verify`
}

export const verifyAdminMfa = async (adminMfaVerificationRequest: AdminMfaVerificationRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<LoginResponse> => {

    const getHeaders = (h?: NonNullable<RequestInit['headers']>): Record<string, string | readonly string[]> => {
    if (!h) return {};
    if (h instanceof Headers) return Object.fromEntries(h.entries());
    if (Symbol.iterator in h) {
      return Object.fromEntries(
        Array.from(h as Iterable<Iterable<string>>, (entry) => Array.from(entry) as [string, string]),
      );
    }
    const headers: Record<string, string | readonly string[]> = {};
    for (const [name, value] of Object.entries<string | readonly string[] | undefined>(h)) {
      if (value !== undefined) headers[name] = value;
    }
    return headers;
  };
return generatedApiClient<LoginResponse>(getVerifyAdminMfaUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(adminMfaVerificationRequest)
  }
);}



export const getChangeAdminPasswordUrl = () => {




  return `/api/v1/admin/auth/password`
}

export const changeAdminPassword = async (adminPasswordChangeRequest: AdminPasswordChangeRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

    const getHeaders = (h?: NonNullable<RequestInit['headers']>): Record<string, string | readonly string[]> => {
    if (!h) return {};
    if (h instanceof Headers) return Object.fromEntries(h.entries());
    if (Symbol.iterator in h) {
      return Object.fromEntries(
        Array.from(h as Iterable<Iterable<string>>, (entry) => Array.from(entry) as [string, string]),
      );
    }
    const headers: Record<string, string | readonly string[]> = {};
    for (const [name, value] of Object.entries<string | readonly string[] | undefined>(h)) {
      if (value !== undefined) headers[name] = value;
    }
    return headers;
  };
return generatedApiClient<void>(getChangeAdminPasswordUrl(),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(adminPasswordChangeRequest)
  }
);}
