import { generatedApiClient } from '../../shared/api/generatedClient';
export type SocialAccountsResponseLinkedProvidersItem = typeof SocialAccountsResponseLinkedProvidersItem[keyof typeof SocialAccountsResponseLinkedProvidersItem];


export const SocialAccountsResponseLinkedProvidersItem = {
  GOOGLE: 'GOOGLE',
  NAVER: 'NAVER',
} as const;

export interface SocialAccountsResponse {
  linkedProviders: SocialAccountsResponseLinkedProvidersItem[];
}

export interface SocialAccountAuthorizationResponse {
  authorizationUrl: string;
}

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



export const getUnlinkMySocialAccountUrl = (provider: 'google' | 'naver',) => {




  return `/api/v1/me/social-accounts/${provider}`
}

export const unlinkMySocialAccount = async (provider: 'google' | 'naver', options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getUnlinkMySocialAccountUrl(provider),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getStartMySocialAccountLinkUrl = (provider: 'google' | 'naver',) => {




  return `/api/v1/me/social-accounts/${provider}/authorization`
}

export const startMySocialAccountLink = async (provider: 'google' | 'naver', options?: RequestInit): Promise<SocialAccountAuthorizationResponse> => {

  return generatedApiClient<SocialAccountAuthorizationResponse>(getStartMySocialAccountLinkUrl(provider),
  {
    ...options,
    method: 'POST'


  }
);}
