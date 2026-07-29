import { useEffect, useRef, useState } from "react";
import { Alert, Container } from "react-bootstrap";
import { useNavigate, useSearchParams } from "react-router";
import {
  buildAuthPageHref,
  resolveSafeReturnTo,
} from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { getUserMessage } from "@/shared/lib";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import { LoadingSpinner } from "@/shared/ui";
import {
  startSocialAccountLink,
  unlinkSocialAccount,
} from "@/features/customer-auth/socialAccountApi";
import type { SocialProvider } from "@/features/customer-auth/socialAuth";
import {
  ApiError,
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
  const [signupHref, setSignupHref] = useState("/signup");
  const handled = useRef(false);

  useEffect(() => {
    if (handled.current) {
      return;
    }
    handled.current = true;

    const pendingSocialAccountLink = sessionStorage.getItem(SESSION_KEYS.socialAccountLink);
    sessionStorage.removeItem(SESSION_KEYS.socialAccountLink);
    const pendingSocialReauthentication =
      sessionStorage.getItem(SESSION_KEYS.socialReauthentication);
    sessionStorage.removeItem(SESSION_KEYS.socialReauthentication);
    const continuationOwner = Number(
      sessionStorage.getItem(SESSION_KEYS.customerContinuationOwner),
    );
    sessionStorage.removeItem(SESSION_KEYS.customerContinuationOwner);
    const hasCustomerContinuation =
      pendingSocialAccountLink !== null || pendingSocialReauthentication !== null;
    const clearPendingStepUpActions = () => {
      sessionStorage.removeItem(SESSION_KEYS.socialAccountLinkTarget);
      sessionStorage.removeItem(SESSION_KEYS.socialAccountUnlinkTarget);
      sessionStorage.removeItem(SESSION_KEYS.stepUpReturnAction);
    };

    const errorCode = searchParams.get("error");
    if (errorCode) {
      setLinkCallback(
        pendingSocialAccountLink !== null || pendingSocialReauthentication !== null,
      );
      if (errorCode === "POLICY_CONSENT_REQUIRED") {
        const returnTo = resolveSafeReturnTo(
          sessionStorage.getItem(SESSION_KEYS.socialLoginReturnTo),
        );
        setPolicyConsentRequired(true);
        setSignupHref(buildAuthPageHref("/signup", { redirectTo: returnTo }));
      }
      sessionStorage.removeItem(SESSION_KEYS.socialLoginReturnTo);
      clearPendingStepUpActions();
      setError(getUserMessage(errorCode) ?? "소셜 로그인에 실패했습니다. 다시 시도해 주세요.");
      return;
    }

    void (async () => {
      try {
        const user = await refresh();
        if (
          hasCustomerContinuation
          && (
            !Number.isSafeInteger(continuationOwner)
            || continuationOwner <= 0
            || user?.id !== continuationOwner
          )
        ) {
          clearPendingStepUpActions();
          navigate(
            user ? "/my" : buildAuthPageHref("/login", { redirectTo: "/my" }),
            { replace: true },
          );
          return;
        }

        if (searchParams.get("reauthenticated")) {
          const linkTarget = sessionStorage.getItem(
            SESSION_KEYS.socialAccountLinkTarget,
          ) as SocialProvider | null;
          const unlinkTarget = sessionStorage.getItem(
            SESSION_KEYS.socialAccountUnlinkTarget,
          ) as SocialProvider | null;
          const returnAction = sessionStorage.getItem(SESSION_KEYS.stepUpReturnAction);
          clearPendingStepUpActions();
          if (linkTarget) {
            await runForCurrentCustomer(
              () => startSocialAccountLink(linkTarget),
              ({ authorizationUrl }) => {
                if (!user) throw new CustomerSessionChangedError();
                sessionStorage.setItem(
                  SESSION_KEYS.customerContinuationOwner,
                  String(user.id),
                );
                sessionStorage.setItem(SESSION_KEYS.socialAccountLink, linkTarget);
                window.location.assign(authorizationUrl);
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

        const returnTo = resolveSafeReturnTo(
          sessionStorage.getItem(SESSION_KEYS.socialLoginReturnTo),
        );
        sessionStorage.removeItem(SESSION_KEYS.socialLoginReturnTo);

        if (searchParams.get("newUser") === "true" || user?.phone === null) {
          navigate("/my", { replace: true, state: { phoneOnboarding: true } });
        } else {
          navigate(returnTo, { replace: true });
        }
      } catch (error) {
        if (error instanceof CustomerSessionChangedError) {
          navigate("/my", { replace: true });
          return;
        }
        clearPendingStepUpActions();
        sessionStorage.removeItem(SESSION_KEYS.socialAccountLink);
        sessionStorage.removeItem(SESSION_KEYS.socialLoginReturnTo);
        setLinkCallback(
          pendingSocialAccountLink !== null
          || pendingSocialReauthentication !== null,
        );
        setError(
          error instanceof ApiError
            ? getUserMessage(error.code) ?? error.message
            : "소셜 계정 처리를 완료하지 못했습니다. 다시 시도해 주세요.",
        );
      }
    })();
  }, [navigate, refresh, searchParams]);

  if (error) {
    const errorHref = linkCallback ? "/my" : policyConsentRequired ? signupHref : "/login";
    const errorLinkLabel = linkCallback
      ? "마이페이지로 돌아가기"
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
