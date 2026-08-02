import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Badge, Button, Form, Modal, Spinner } from "react-bootstrap";
import {
  ApiError,
  CustomerSessionChangedError,
  currentCustomerSessionUserId,
  runForCurrentCustomer,
} from "@/shared/api";
import { getUserMessage } from "@/shared/lib";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import { ErrorAlert, useToast } from "@/shared/ui";
import { isPasswordWithinByteLimit } from "@/shared/validation/password";
import {
  SOCIAL_PROVIDER_DETAILS,
  SOCIAL_PROVIDERS,
  type SocialProvider,
} from "@/features/customer-auth/socialAuth";
import {
  fetchLinkedSocialProviders,
  reauthenticateCustomerPassword,
  startSocialAccountLink,
  unlinkSocialAccount,
} from "@/features/customer-auth/socialAccountApi";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import {
  clearCustomerStepUpContinuation,
  redirectToSocialStepUp,
} from "@/features/customer-auth/customerStepUp";

interface Props {
  localPasswordEnabled: boolean;
}

type SensitiveAction = {
  kind: "link" | "unlink";
  provider: SocialProvider;
};

export function SocialAccountSection({ localPasswordEnabled }: Props) {
  const toast = useToast();
  const [startingProvider, setStartingProvider] = useState<SocialProvider | null>(null);
  const [password, setPassword] = useState("");
  const [pendingAction, setPendingAction] = useState<SensitiveAction | null>(null);
  const {
    data: linkedProviders,
    error: linkedProvidersError,
    isLoading,
    isFetching: linkedProvidersFetching,
    refetch: refetchLinkedProviders,
  } = useQuery({
    queryKey: ["me", "social-accounts"],
    queryFn: fetchLinkedSocialProviders,
  });
  const unlinkMutation = useMutation({
    mutationFn: (provider: SocialProvider) =>
      runForCurrentCustomer(
        () => unlinkSocialAccount(provider),
        () => {
          window.location.assign(buildAuthPageHref("/login", { redirectTo: "/my" }));
        },
      ),
  });

  const showActionError = (error: unknown, fallback: string) => {
    if (error instanceof CustomerSessionChangedError) return;
    const message = error instanceof ApiError
      ? getUserMessage(error.code) ?? error.message
      : fallback;
    toast.show(message, "danger");
  };

  const beginLink = async (provider: SocialProvider) => {
    await runForCurrentCustomer(
      () => startSocialAccountLink(provider),
      ({ authorizationUrl }) => {
        const customerId = currentCustomerSessionUserId();
        if (customerId === null) throw new CustomerSessionChangedError();
        clearCustomerStepUpContinuation();
        sessionStorage.setItem(
          SESSION_KEYS.customerContinuationOwner,
          String(customerId),
        );
        sessionStorage.setItem(SESSION_KEYS.socialAccountLink, provider);
        window.location.assign(authorizationUrl);
      },
    );
  };

  const beginUnlink = async (provider: SocialProvider) => {
    await unlinkMutation.mutateAsync(provider);
  };

  const redirectForSensitiveAction = async (action: SensitiveAction) => {
    const reauthenticationProvider = linkedProviders?.[0];
    if (!reauthenticationProvider) {
      return false;
    }
    setStartingProvider(action.provider);
    try {
      await redirectToSocialStepUp(
        reauthenticationProvider,
        action.kind === "link"
          ? { kind: "social-link", provider: action.provider }
          : { kind: "social-unlink", provider: action.provider },
      );
    } catch (error) {
      showActionError(error, "소셜 계정 본인 확인을 시작하지 못했습니다.");
    } finally {
      setStartingProvider(null);
    }
    return true;
  };

  const startLink = async (provider: SocialProvider) => {
    setStartingProvider(provider);
    try {
      await beginLink(provider);
    } catch (error) {
      if (error instanceof ApiError && error.code === "REAUTHENTICATION_REQUIRED") {
        if (localPasswordEnabled) {
          setPendingAction({ kind: "link", provider });
          return;
        }
        if (await redirectForSensitiveAction({ kind: "link", provider })) {
          return;
        }
      }
      showActionError(error, "소셜 계정 연결을 시작하지 못했습니다.");
    } finally {
      setStartingProvider(null);
    }
  };

  const requestUnlink = async (provider: SocialProvider) => {
    setStartingProvider(provider);
    try {
      await beginUnlink(provider);
    } catch (error) {
      if (error instanceof ApiError && error.code === "REAUTHENTICATION_REQUIRED") {
        if (localPasswordEnabled) {
          setPendingAction({ kind: "unlink", provider });
          return;
        }
        if (await redirectForSensitiveAction({ kind: "unlink", provider })) {
          return;
        }
      }
      showActionError(error, "소셜 계정 연결을 해제하지 못했습니다.");
    } finally {
      setStartingProvider(null);
    }
  };

  const handlePasswordReauthentication = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!pendingAction || !isPasswordWithinByteLimit(password)) return;
    setStartingProvider(pendingAction.provider);
    try {
      await runForCurrentCustomer(
        () => reauthenticateCustomerPassword(password),
        async () => {
          const action = pendingAction;
          setPendingAction(null);
          setPassword("");
          if (action.kind === "link") {
            await beginLink(action.provider);
          } else {
            await beginUnlink(action.provider);
          }
        },
      );
    } catch (error) {
      showActionError(error, "본인 확인을 완료하지 못했습니다.");
    } finally {
      setStartingProvider(null);
    }
  };

  const canUnlink = localPasswordEnabled || (linkedProviders?.length ?? 0) > 1;
  const sensitiveActionPending = startingProvider !== null;
  const closePasswordReauthentication = () => {
    if (sensitiveActionPending) {
      return;
    }
    setPendingAction(null);
    setPassword("");
  };

  return (
    <div className="border-top mt-3 pt-3">
      <div className="d-flex justify-content-between align-items-center gap-3 mb-2">
        <div>
          <h6 className="mb-1">소셜 로그인</h6>
        </div>
        {isLoading && (
          <span role="status" aria-live="polite">
            <Spinner animation="border" size="sm" aria-hidden="true" />
            <span className="visually-hidden">연결된 소셜 계정을 확인하는 중...</span>
          </span>
        )}
      </div>

      <ErrorAlert
        error={linkedProvidersError}
        onRetry={() => void refetchLinkedProviders()}
        retrying={linkedProvidersFetching}
      />

      {linkedProviders && (
        <div className="d-grid gap-2">
          {SOCIAL_PROVIDERS.map((provider) => {
            const linked = linkedProviders.includes(provider);
            const details = SOCIAL_PROVIDER_DETAILS[provider];
            return (
              <div key={provider} className="d-flex justify-content-between align-items-center gap-3">
                <div className="d-flex align-items-center gap-2">
                  <span>{details.label}</span>
                  {linked && <Badge bg="success">연결됨</Badge>}
                </div>
                {linked ? (
                  <Button
                    type="button"
                    variant="outline-danger"
                    size="sm"
                    disabled={
                      !canUnlink
                      || unlinkMutation.isPending
                      || sensitiveActionPending
                    }
                    onClick={() => void requestUnlink(provider)}
                  >
                    해제
                  </Button>
                ) : (
                  <Button
                    type="button"
                    variant="outline-primary"
                    size="sm"
                    disabled={startingProvider !== null}
                    onClick={() => void startLink(provider)}
                  >
                    {startingProvider === provider ? "연결 중..." : "연결"}
                  </Button>
                )}
              </div>
            );
          })}
        </div>
      )}

      <Modal
        show={pendingAction !== null}
        aria-labelledby="social-account-password-confirm-title"
        onHide={closePasswordReauthentication}
        backdrop={sensitiveActionPending ? "static" : true}
        keyboard={!sensitiveActionPending}
        centered
      >
        <Form onSubmit={(event) => void handlePasswordReauthentication(event)}>
          <Modal.Header closeButton={!sensitiveActionPending}>
            <Modal.Title id="social-account-password-confirm-title" className="fs-6">
              비밀번호로 본인 확인
            </Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <Form.Control
              type="password"
              autoComplete="current-password"
              maxLength={72}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              autoFocus
            />
          </Modal.Body>
          <Modal.Footer>
            {(linkedProviders?.length ?? 0) > 0 && pendingAction && (
              <Button
                type="button"
                variant="outline-primary"
                onClick={() => void redirectForSensitiveAction(pendingAction)}
                disabled={sensitiveActionPending}
              >
                연결된 소셜 계정으로 본인 확인
              </Button>
            )}
            <Button
              type="button"
              variant="secondary"
              onClick={closePasswordReauthentication}
              disabled={sensitiveActionPending}
            >
              취소
            </Button>
            <Button
              type="submit"
              disabled={
                !password
                || !isPasswordWithinByteLimit(password)
                || sensitiveActionPending
              }
            >
              {pendingAction?.kind === "unlink" ? "확인하고 해제" : "확인하고 연결"}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  );
}
