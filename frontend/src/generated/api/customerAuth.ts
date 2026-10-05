import { generatedApiClient } from '../../shared/api/generatedClient';
export interface CsrfToken {
  headerName?: string;
  parameterName?: string;
  token?: string;
}

export interface CsrfTokenResponse {
  cookieName?: string;
  headerName?: string;
}

export interface CustomerLoginRequest {
  /** @minLength 1 */
  email: string;
  /** @minLength 1 */
  password: string;
}

export interface CustomerUserResponse {
  /** @nullable */
  email: string | null;
  id: number;
  localPasswordEnabled: boolean;
  name: string;
  /** @nullable */
  phone: string | null;
  phoneVerified: boolean;
}

export interface ResetPasswordRequest {
  /** @minLength 1 */
  email: string;
  /**
     * @minLength 8
     * @maxLength 72
     */
  newPassword: string;
  /** @minLength 1 */
  phone: string;
  /**
     * @minLength 1
     * @pattern ^[0-9]{6}$
     */
  verificationCode: string;
}

export interface PolicyAcceptanceRequest {
  privacyAccepted: boolean;
  /** @minLength 1 */
  privacyVersion: string;
  termsAccepted: boolean;
  /** @minLength 1 */
  termsVersion: string;
}

export interface SignupRequest {
  /** @minLength 1 */
  email: string;
  /** @minLength 1 */
  name: string;
  /**
     * @minLength 8
     * @maxLength 72
     */
  password: string;
  /** @minLength 1 */
  phone: string;
  policyAcceptance: PolicyAcceptanceRequest;
  /**
     * @minLength 1
     * @pattern ^[0-9]{6}$
     */
  verificationCode: string;
}

export interface RegisterEmailRequest {
  /**
     * @minLength 0
     * @maxLength 254
     */
  email: string;
  /**
     * @minLength 1
     * @pattern ^[0-9]{6}$
     */
  verificationCode: string;
}

export interface SendEmailVerificationRequest {
  /**
     * @minLength 0
     * @maxLength 254
     */
  email: string;
}

export interface ChangePasswordRequest {
  /**
     * @minLength 8
     * @maxLength 72
     */
  currentPassword: string;
  /**
     * @minLength 8
     * @maxLength 72
     */
  newPassword: string;
}

export interface UpdateMemberPhoneRequest {
  /** @minLength 1 */
  phone: string;
  /**
     * @minLength 1
     * @pattern ^[0-9]{6}$
     */
  verificationCode: string;
}

export interface PasswordReauthenticationRequest {
  /**
     * @minLength 1
     * @maxLength 72
     */
  currentPassword: string;
}

export type CsrfParams = {
csrfToken: CsrfToken;
};

export const getCsrfUrl = (params: CsrfParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/auth/csrf?${stringifiedParams}` : `/api/v1/auth/csrf`
}

export const csrf = async (params: CsrfParams, options?: Parameters<typeof generatedApiClient>[1]): Promise<CsrfTokenResponse> => {

  return generatedApiClient<CsrfTokenResponse>(getCsrfUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getLoginCustomerUrl = () => {




  return `/api/v1/auth/login`
}

export const loginCustomer = async (customerLoginRequest: CustomerLoginRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<CustomerUserResponse> => {

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
return generatedApiClient<CustomerUserResponse>(getLoginCustomerUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(customerLoginRequest)
  }
);}



export const getLogoutCustomerUrl = () => {




  return `/api/v1/auth/logout`
}

export const logoutCustomer = async ( options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

  return generatedApiClient<void>(getLogoutCustomerUrl(),
  {
    ...options,
    method: 'POST'


  }
);}



export const getResetCustomerPasswordUrl = () => {




  return `/api/v1/auth/password/reset`
}

export const resetCustomerPassword = async (resetPasswordRequest: ResetPasswordRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

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
return generatedApiClient<void>(getResetCustomerPasswordUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(resetPasswordRequest)
  }
);}



export const getSignupCustomerUrl = () => {




  return `/api/v1/auth/signup`
}

export const signupCustomer = async (signupRequest: SignupRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<CustomerUserResponse> => {

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
return generatedApiClient<CustomerUserResponse>(getSignupCustomerUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(signupRequest)
  }
);}



export const getWithdrawMyAccountUrl = () => {




  return `/api/v1/me`
}

export const withdrawMyAccount = async ( options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

  return generatedApiClient<void>(getWithdrawMyAccountUrl(),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getGetCurrentCustomerUrl = () => {




  return `/api/v1/me`
}

export const getCurrentCustomer = async ( options?: Parameters<typeof generatedApiClient>[1]): Promise<CustomerUserResponse> => {

  return generatedApiClient<CustomerUserResponse>(getGetCurrentCustomerUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getRegisterMyVerifiedEmailUrl = () => {




  return `/api/v1/me/email`
}

export const registerMyVerifiedEmail = async (registerEmailRequest: RegisterEmailRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

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
return generatedApiClient<void>(getRegisterMyVerifiedEmailUrl(),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(registerEmailRequest)
  }
);}



export const getSendMyEmailVerificationUrl = () => {




  return `/api/v1/me/email-verifications`
}

export const sendMyEmailVerification = async (sendEmailVerificationRequest: SendEmailVerificationRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

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
return generatedApiClient<void>(getSendMyEmailVerificationUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(sendEmailVerificationRequest)
  }
);}



export const getChangeMyPasswordUrl = () => {




  return `/api/v1/me/password`
}

export const changeMyPassword = async (changePasswordRequest: ChangePasswordRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

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
return generatedApiClient<void>(getChangeMyPasswordUrl(),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(changePasswordRequest)
  }
);}



export const getUpdateMyPhoneUrl = () => {




  return `/api/v1/me/phone`
}

export const updateMyPhone = async (updateMemberPhoneRequest: UpdateMemberPhoneRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<CustomerUserResponse> => {

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
return generatedApiClient<CustomerUserResponse>(getUpdateMyPhoneUrl(),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(updateMemberPhoneRequest)
  }
);}



export const getReauthenticateMyPasswordUrl = () => {




  return `/api/v1/me/reauthentication/password`
}

export const reauthenticateMyPassword = async (passwordReauthenticationRequest: PasswordReauthenticationRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

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
return generatedApiClient<void>(getReauthenticateMyPasswordUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(passwordReauthenticationRequest)
  }
);}
