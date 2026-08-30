import { useCallback, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { fetchCurrentPolicyConsent } from "./api";
import type { PolicyAcceptance } from "./types";

export function usePolicyAcceptance() {
  const [capturedAcceptance, setCapturedAcceptance] = useState<PolicyAcceptance | null>(null);
  const policyQuery = useQuery({
    queryKey: ["policy-consent", "current"],
    queryFn: fetchCurrentPolicyConsent,
    staleTime: PUBLIC_DATA_STALE_TIME,
  });
  const policy = policyQuery.data;
  const termsVersion = policy?.terms.version;
  const privacyVersion = policy?.privacy.version;
  const acceptsCurrentPolicy = capturedAcceptance !== null
    && capturedAcceptance.termsVersion === termsVersion
    && capturedAcceptance.privacyVersion === privacyVersion;
  const acceptance = acceptsCurrentPolicy ? capturedAcceptance : null;

  const setAccepted = useCallback((accepted: boolean) => {
    if (!accepted || termsVersion === undefined || privacyVersion === undefined) {
      setCapturedAcceptance(null);
      return;
    }
    setCapturedAcceptance({
      termsVersion,
      termsAccepted: true,
      privacyVersion,
      privacyAccepted: true,
    });
  }, [privacyVersion, termsVersion]);

  return {
    policyQuery,
    accepted: acceptsCurrentPolicy,
    setAccepted,
    acceptance,
    ready: acceptance !== null,
  };
}
