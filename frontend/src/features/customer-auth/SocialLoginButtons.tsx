import { useState } from "react";
import { Button } from "react-bootstrap";
import { resolveSafeReturnTo } from "@/features/customer-auth/navigation";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import {
  SOCIAL_PROVIDER_DETAILS,
  SOCIAL_PROVIDERS,
  type SocialProvider,
} from "@/features/customer-auth/socialAuth";
import { startSocialSignup } from "@/generated/api/customerAccount";
import type { PolicyAcceptance } from "@/features/policy-consent/types";
import { ErrorAlert } from "@/shared/ui";

interface SocialLoginButtonsProps {
  action: "로그인" | "회원가입";
  returnTo: string;
  policyAcceptance?: PolicyAcceptance | null;
}

export function SocialLoginButtons({
  action,
  returnTo,
  policyAcceptance,
}: SocialLoginButtonsProps) {
  const [startingProvider, setStartingProvider] = useState<SocialProvider | null>(null);
  const [error, setError] = useState<unknown>(null);

  async function startSocialLogin(provider: SocialProvider) {
    const signupAcceptance = action === "회원가입" ? policyAcceptance : null;
    if (action === "회원가입" && !signupAcceptance) {
      return;
    }

    setError(null);
    setStartingProvider(provider);
    try {
      let authorizationUrl = `/api/v1/auth/social/authorization/${provider}`;
      if (signupAcceptance) {
        authorizationUrl = (await startSocialSignup(provider, signupAcceptance)).authorizationUrl;
      }
      sessionStorage.setItem(SESSION_KEYS.socialLoginReturnTo, resolveSafeReturnTo(returnTo));
      window.location.assign(authorizationUrl);
    } catch (requestError) {
      setError(requestError);
      setStartingProvider(null);
    }
  }

  return (
    <div className="d-grid gap-2">
      <ErrorAlert error={error} />
      {SOCIAL_PROVIDERS.map((provider) => {
        const details = SOCIAL_PROVIDER_DETAILS[provider];
        const isStarting = startingProvider === provider;

        return (
          <Button
            key={provider}
            type="button"
            variant="outline-dark"
            className={details.buttonClassName}
            disabled={startingProvider !== null || (action === "회원가입" && !policyAcceptance)}
            onClick={() => void startSocialLogin(provider)}
          >
            <span className="social-login-button-content">
              {details.iconSrc && (
                <img className="social-login-button-icon" src={details.iconSrc} alt="" aria-hidden="true" />
              )}
              <span>{isStarting ? `${details.label} 연결 중...` : `${details.label}로 ${action}`}</span>
            </span>
          </Button>
        );
      })}
    </div>
  );
}
