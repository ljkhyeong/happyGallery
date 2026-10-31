import {
  getCurrentPolicyConsent,
  type CurrentPolicyConsentResponse,
} from "@/generated/api/policyConsent";

export function fetchCurrentPolicyConsent(): Promise<CurrentPolicyConsentResponse> {
  return getCurrentPolicyConsent();
}
