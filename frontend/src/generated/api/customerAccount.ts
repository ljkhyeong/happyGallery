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
    policyAcceptanceRequest: PolicyAcceptanceRequest, options?: RequestInit): Promise<SocialSignupAuthorizationResponse> => {

  return generatedApiClient<SocialSignupAuthorizationResponse>(getStartSocialSignupUrl(provider),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(policyAcceptanceRequest)
  }
);}



export const getGetMySocialAccountsUrl = () => {




  return `/api/v1/me/social-accounts`
}

export const getMySocialAccounts = async ( options?: RequestInit): Promise<SocialAccountsResponse> => {

  return generatedApiClient<SocialAccountsResponse>(getGetMySocialAccountsUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getUnlinkMySocialAccountUrl = (provider: 'google' | 'naver' | 'kakao',) => {




  return `/api/v1/me/social-accounts/${provider}`
}

export const unlinkMySocialAccount = async (provider: 'google' | 'naver' | 'kakao', options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getUnlinkMySocialAccountUrl(provider),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getStartMySocialAccountLinkUrl = (provider: 'google' | 'naver' | 'kakao',) => {




  return `/api/v1/me/social-accounts/${provider}/authorization`
}

export const startMySocialAccountLink = async (provider: 'google' | 'naver' | 'kakao', options?: RequestInit): Promise<SocialAccountAuthorizationResponse> => {

  return generatedApiClient<SocialAccountAuthorizationResponse>(getStartMySocialAccountLinkUrl(provider),
  {
    ...options,
    method: 'POST'


  }
);}



export const getStartMySocialReauthenticationUrl = (provider: 'google' | 'naver' | 'kakao',) => {




  return `/api/v1/me/social-accounts/${provider}/reauthentication`
}

export const startMySocialReauthentication = async (provider: 'google' | 'naver' | 'kakao', options?: RequestInit): Promise<SocialAccountAuthorizationResponse> => {

  return generatedApiClient<SocialAccountAuthorizationResponse>(getStartMySocialReauthenticationUrl(provider),
  {
    ...options,
    method: 'POST'


  }
);}
