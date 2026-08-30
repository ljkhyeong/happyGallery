import { useEffect, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Button, Form } from "react-bootstrap";
import {
  ApiError,
  CustomerSessionChangedError,
  runForCurrentCustomer,
} from "@/shared/api";
import { getUserMessage } from "@/shared/lib";
import { ErrorAlert, useToast } from "@/shared/ui";
import { isPasswordWithinByteLimit } from "@/shared/validation/password";
import {
  fetchLinkedSocialProviders,
  reauthenticateCustomerPassword,
} from "@/features/customer-auth/socialAccountApi";
import {
  redirectToSocialStepUp,
  type CustomerStepUpReturnAction,
} from "@/features/customer-auth/customerStepUp";

interface Props {
  localPasswordEnabled: boolean;
  returnAction: CustomerStepUpReturnAction;
  onVerified: () => void;
  onBusyChange?: (busy: boolean) => void;
}

export function CustomerStepUpPrompt({
  localPasswordEnabled,
  returnAction,
  onVerified,
  onBusyChange,
}: Props) {
  const toast = useToast();
  const [password, setPassword] = useState("");
  const [socialPending, setSocialPending] = useState(false);
  const linkedProviders = useQuery({
    queryKey: ["me", "social-accounts"],
    queryFn: fetchLinkedSocialProviders,
  });
  const passwordReauthentication = useMutation({
    mutationFn: () =>
      runForCurrentCustomer(
        () => reauthenticateCustomerPassword(password),
        () => {
          setPassword("");
          onVerified();
        },
      ),
  });
  const busy = passwordReauthentication.isPending || socialPending;

  useEffect(() => {
    onBusyChange?.(busy);
    return () => onBusyChange?.(false);
  }, [busy, onBusyChange]);

  async function startSocialStepUp() {
    const provider = linkedProviders.data?.[0];
    if (!provider) return;
    setSocialPending(true);
    try {
      await redirectToSocialStepUp(provider, {
        kind: "return",
        action: returnAction,
      });
    } catch (error) {
      if (error instanceof CustomerSessionChangedError) return;
      const message = error instanceof ApiError
        ? getUserMessage(error.code) ?? "소셜 계정 본인 확인을 시작하지 못했습니다."
        : "소셜 계정 본인 확인을 시작하지 못했습니다.";
      toast.show(message, "danger");
    } finally {
      setSocialPending(false);
    }
  }

  return (
    <>
      <ErrorAlert error={passwordReauthentication.error} />
      <ErrorAlert
        error={linkedProviders.error}
        onRetry={() => { void linkedProviders.refetch(); }}
        retrying={linkedProviders.isFetching}
      />
      {localPasswordEnabled && (
        <Form
          onSubmit={(event) => {
            event.preventDefault();
            if (!isPasswordWithinByteLimit(password)) return;
            passwordReauthentication.mutate();
          }}
        >
          <Form.Group controlId={`${returnAction}-current-password`}>
            <Form.Label>현재 비밀번호</Form.Label>
            <Form.Control
              type="password"
              autoComplete="current-password"
              maxLength={72}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              autoFocus
            />
          </Form.Group>
          <Button
            type="submit"
            className="mt-3 w-100"
            disabled={
              !password
              || !isPasswordWithinByteLimit(password)
              || passwordReauthentication.isPending
            }
          >
            본인 확인
          </Button>
        </Form>
      )}
      {linkedProviders.data !== undefined
        && !linkedProviders.error
        && (!localPasswordEnabled || linkedProviders.data.length > 0) && (
        <Button
          type="button"
          variant={localPasswordEnabled ? "outline-primary" : "primary"}
          className={localPasswordEnabled ? "mt-2 w-100" : "w-100"}
          disabled={
            linkedProviders.isLoading
            || linkedProviders.data?.length === 0
            || socialPending
          }
          onClick={() => void startSocialStepUp()}
        >
          연결된 소셜 계정으로 본인 확인
        </Button>
      )}
    </>
  );
}
