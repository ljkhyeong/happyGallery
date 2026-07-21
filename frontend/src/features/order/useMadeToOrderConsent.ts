import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError } from "@/shared/api";
import { useOrderPricePolicy } from "./useOrderPricePolicy";

export function isMadeToOrderConsentVersionMismatch(error: unknown): boolean {
  return error instanceof ApiError
    && error.code === "INVALID_INPUT"
    && error.message.includes("주문제작 동의 안내가 변경");
}

export function useMadeToOrderConsent(required: boolean) {
  const [checked, setCheckedState] = useState(false);
  const [versionMismatch, setVersionMismatch] = useState(false);
  const [refreshRequired, setRefreshRequired] = useState(false);
  const previousVersion = useRef<string | null>(null);
  const policyQuery = useOrderPricePolicy(required);
  const refetchPolicy = policyQuery.refetch;
  const version = policyQuery.data?.madeToOrderConsentVersion.trim() || null;

  useEffect(() => {
    if (!required) {
      previousVersion.current = null;
      setCheckedState(false);
      setVersionMismatch(false);
      setRefreshRequired(false);
      return;
    }
    if (!version) return;
    if (previousVersion.current && previousVersion.current !== version) {
      setCheckedState(false);
      setVersionMismatch(true);
    }
    previousVersion.current = version;
  }, [required, version]);

  const setChecked = useCallback((nextChecked: boolean) => {
    if (refreshRequired) return;
    setCheckedState(nextChecked);
    if (nextChecked) {
      setVersionMismatch(false);
    }
  }, [refreshRequired]);

  const handleSubmissionError = useCallback((error: unknown) => {
    if (!isMadeToOrderConsentVersionMismatch(error)) return;
    setCheckedState(false);
    setVersionMismatch(true);
    setRefreshRequired(true);
    void refetchPolicy().then((result) => {
      const refreshedVersion = result.data?.madeToOrderConsentVersion.trim();
      if (result.isSuccess && refreshedVersion) {
        setRefreshRequired(false);
      }
    });
  }, [refetchPolicy]);

  const agreed = required && checked && version !== null;

  return {
    agreed,
    version: required ? version : null,
    ready: (!required || agreed) && !refreshRequired,
    checked,
    setChecked,
    versionMismatch,
    refreshRequired,
    policyQuery,
    handleSubmissionError,
  };
}
