import { getCurrentPolicyConsent } from "@/generated/api/policyConsent";
import type { CurrentPolicyConsent } from "./types";

export function fetchCurrentPolicyConsent(): Promise<CurrentPolicyConsent> {
  return getCurrentPolicyConsent();
}
