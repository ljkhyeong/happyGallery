export type {
  CurrentPolicyConsentResponse as CurrentPolicyConsent,
  PolicyDocument,
} from "@/generated/api/policyConsent";

export interface PolicyAcceptance {
  termsVersion: string;
  termsAccepted: boolean;
  privacyVersion: string;
  privacyAccepted: boolean;
}
