import { generatedApiClient } from '../../shared/api/generatedClient';
export interface PolicyAcceptanceRequest {
  privacyAccepted: boolean;
  /** @minLength 1 */
  privacyVersion: string;
  termsAccepted: boolean;
  /** @minLength 1 */
  termsVersion: string;
}

export interface SocialSignupAuthorizationResponse {
  authorizationUrl: string;
}

export type SocialAccountsResponseLinkedProvidersItem = typeof SocialAccountsResponseLinkedProvidersItem[keyof typeof SocialAccountsResponseLinkedProvidersItem];


export const SocialAccountsResponseLinkedProvidersItem = {
  GOOGLE: 'GOOGLE',
  NAVER: 'NAVER',
  KAKAO: 'KAKAO',
} as const;

export interface SocialAccountsResponse {
  linkedProviders: SocialAccountsResponseLinkedProvidersItem[];
}

export interface SocialAccountAuthorizationResponse {
  authorizationUrl: string;
}

export const getStartSocialSignupUrl = (provider: 'google' | 'naver' | 'kakao',) => {




  return `/api/v1/auth/social/signup-intents/${provider}`
}

export const startSocialSignup = async (provider: 'google' | 'naver' | 'kakao',
    policyAcceptanceRequest: PolicyAcceptanceRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<SocialSignupAuthorizationResponse> => {

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
return generatedApiClient<SocialSignupAuthorizationResponse>(getStartSocialSignupUrl(provider),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(policyAcceptanceRequest)
  }
);}



export const getGetMySocialAccountsUrl = () => {




  return `/api/v1/me/social-accounts`
}

export const getMySocialAccounts = async ( options?: Parameters<typeof generatedApiClient>[1]): Promise<SocialAccountsResponse> => {

  return generatedApiClient<SocialAccountsResponse>(getGetMySocialAccountsUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getUnlinkMySocialAccountUrl = (provider: 'google' | 'naver' | 'kakao',) => {




  return `/api/v1/me/social-accounts/${provider}`
}

export const unlinkMySocialAccount = async (provider: 'google' | 'naver' | 'kakao', options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

  return generatedApiClient<void>(getUnlinkMySocialAccountUrl(provider),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getStartMySocialAccountLinkUrl = (provider: 'google' | 'naver' | 'kakao',) => {




  return `/api/v1/me/social-accounts/${provider}/authorization`
}

export const startMySocialAccountLink = async (provider: 'google' | 'naver' | 'kakao', options?: Parameters<typeof generatedApiClient>[1]): Promise<SocialAccountAuthorizationResponse> => {

  return generatedApiClient<SocialAccountAuthorizationResponse>(getStartMySocialAccountLinkUrl(provider),
  {
    ...options,
    method: 'POST'


  }
);}



export const getStartMySocialReauthenticationUrl = (provider: 'google' | 'naver' | 'kakao',) => {




  return `/api/v1/me/social-accounts/${provider}/reauthentication`
}

export const startMySocialReauthentication = async (provider: 'google' | 'naver' | 'kakao', options?: Parameters<typeof generatedApiClient>[1]): Promise<SocialAccountAuthorizationResponse> => {

  return generatedApiClient<SocialAccountAuthorizationResponse>(getStartMySocialReauthenticationUrl(provider),
  {
    ...options,
    method: 'POST'


  }
);}
