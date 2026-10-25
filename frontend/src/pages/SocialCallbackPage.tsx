import { useEffect, useRef, useState } from "react";
import { Alert, Container } from "react-bootstrap";
import { useNavigate, useSearchParams } from "react-router";
import {
  buildAuthPageHref,
  resolveSafeReturnTo,
} from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { getUserMessage } from "@/shared/lib";
import {
  readSessionValue,
  removeSessionValues,
} from "@/shared/storage/browserSessionStorage";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import { describeError, LoadingSpinner } from "@/shared/ui";
import {
  clearCustomerStepUpContinuation,
  readCustomerStepUpContinuation,
  storeContinuationAndRedirect,
} from "@/features/customer-auth/customerStepUp";
import {
  startSocialAccountLink,
  unlinkSocialAccount,
} from "@/features/customer-auth/socialAccountApi";
import { SocialSignupConsent } from "@/features/customer-auth/SocialSignupConsent";
import {
  CustomerSessionChangedError,
  runForCurrentCustomer,
} from "@/shared/api";

export function SocialCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { refresh } = useCustomerAuth();
  const [error, setError] = useState("");
  const [linkCallback, setLinkCallback] = useState(false);
  const [policyConsentRequired, setPolicyConsentRequired] = useState(false);
  const [returnTo, setReturnTo] = useState("/");
  const handled = useRef(false);

  useEffect(() => {
    if (handled.current) {
      return;
    }
    handled.current = true;

    const savedReturnTo = resolveSafeReturnTo(
      readSessionValue(SESSION_KEYS.socialLoginReturnTo),
    );
    setReturnTo(savedReturnTo);
    const continuation = readCustomerStepUpContinuation();
    const hasCustomerContinuation =
      continuation.socialAccountLink !== null || continuation.reauthenticationProvider !== null;

    if (searchParams.get("signupAttempt") && !hasCustomerContinuation) {
      clearCustomerStepUpContinuation();
      return;
    }

    const errorCode = searchParams.get("error");
    if (errorCode) {
      setLinkCallback(hasCustomerContinuation);
      if (errorCode === "POLICY_CONSENT_REQUIRED") {
        setPolicyConsentRequired(true);
      }
      removeSessionValues(SESSION_KEYS.socialLoginReturnTo);
      clearCustomerStepUpContinuation();
      setError(getUserMessage(errorCode) ?? "소셜 로그인에 실패했습니다. 다시 시도해 주세요.");
      return;
    }

    void (async () => {
      try {
        const user = await refresh();
        if (hasCustomerContinuation && user?.id !== continuation.owner) {
          clearCustomerStepUpContinuation();
          navigate(
            user ? "/my" : buildAuthPageHref("/login", { redirectTo: "/my" }),
            { replace: true },
          );
          return;
        }

        if (searchParams.get("reauthenticated")) {
          const { linkTarget, unlinkTarget, returnAction } = continuation;
          clearCustomerStepUpContinuation();
          if (linkTarget) {
            await runForCurrentCustomer(
              () => startSocialAccountLink(linkTarget),
              ({ authorizationUrl }) => {
                const continuationStored = storeContinuationAndRedirect(
                  authorizationUrl,
                  [[SESSION_KEYS.socialAccountLink, linkTarget]],
                  user?.id ?? null,
                );
                if (!continuationStored) navigate("/my", { replace: true });
              },
            );
            return;
          }
          if (unlinkTarget) {
            await runForCurrentCustomer(
              () => unlinkSocialAccount(unlinkTarget),
              () => {
                window.location.assign(buildAuthPageHref("/login", { redirectTo: "/my" }));
              },
            );
            return;
          }
          navigate("/my", {
            replace: true,
            state: {
              phoneChangeRequested: returnAction === "phone-change",
              emailRegistrationRequested:
                returnAction === "email-registration",
              accountWithdrawalRequested:
                returnAction === "account-withdrawal",
            },
          });
          return;
        }

        const linkedProvider = searchParams.get("linked");
        if (linkedProvider) {
          navigate(
            user ? "/my" : buildAuthPageHref("/login", { redirectTo: "/my" }),
            { replace: true, state: user ? { socialAccountLinked: linkedProvider } : null },
          );
          return;
        }

        removeSessionValues(SESSION_KEYS.socialLoginReturnTo);

        navigate(user?.phone === null ? "/my" : savedReturnTo, { replace: true });
      } catch (error) {
        if (error instanceof CustomerSessionChangedError) {
          navigate("/my", { replace: true });
          return;
        }
        clearCustomerStepUpContinuation();
        removeSessionValues(SESSION_KEYS.socialLoginReturnTo);
        setLinkCallback(hasCustomerContinuation);
        setError(describeError(error, "소셜 계정 처리를 완료하지 못했습니다. 다시 시도해 주세요."));
      }
    })();
  }, [navigate, refresh, searchParams]);

  const signupAttempt = searchParams.get("signupAttempt");
  if (signupAttempt && !error) {
    return (
      <Container className="page-container" style={{ maxWidth: 480 }}>
        <SocialSignupConsent attemptId={signupAttempt} returnTo={returnTo} />
      </Container>
    );
  }

  if (error) {
    const errorHref = linkCallback
      ? "/my"
      : buildAuthPageHref(policyConsentRequired ? "/signup" : "/login", { redirectTo: returnTo });
    const errorLinkLabel = linkCallback
      ? "내 정보로 돌아가기"
      : policyConsentRequired
        ? "동의하고 회원가입하기"
        : "로그인 페이지로 돌아가기";

    return (
      <Container className="page-container" style={{ maxWidth: 480 }}>
        <Alert variant="danger" className="mt-5">{error}</Alert>
        <a href={errorHref}>{errorLinkLabel}</a>
      </Container>
    );
  }

  return (
    <Container className="page-container d-flex justify-content-center align-items-center" style={{ minHeight: 300 }}>
      <LoadingSpinner />
    </Container>
  );
}
