import { generatedApiClient } from '../../shared/api/generatedClient';
export interface PolicyDocument {
  documentPath: string;
  version: string;
}

export interface CurrentPolicyConsentResponse {
  privacy: PolicyDocument;
  terms: PolicyDocument;
}

export const getGetCurrentPolicyConsentUrl = () => {




  return `/api/v1/policies/current`
}

export const getCurrentPolicyConsent = async ( options?: RequestInit): Promise<CurrentPolicyConsentResponse> => {

  return generatedApiClient<CurrentPolicyConsentResponse>(getGetCurrentPolicyConsentUrl(),
  {
    ...options,
    method: 'GET'


  }
);}
