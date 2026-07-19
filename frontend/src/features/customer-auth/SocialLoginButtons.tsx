import { useState } from "react";
import { Button } from "react-bootstrap";
import { resolveSafeReturnTo } from "@/features/customer-auth/navigation";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import {
  SOCIAL_PROVIDER_DETAILS,
  SOCIAL_PROVIDERS,
  type SocialProvider,
} from "@/features/customer-auth/socialAuth";

interface SocialLoginButtonsProps {
  action: "로그인" | "회원가입";
  returnTo: string;
}

export function SocialLoginButtons({ action, returnTo }: SocialLoginButtonsProps) {
  const [startingProvider, setStartingProvider] = useState<SocialProvider | null>(null);

  function startSocialLogin(provider: SocialProvider) {
    setStartingProvider(provider);
    sessionStorage.setItem(SESSION_KEYS.socialLoginReturnTo, resolveSafeReturnTo(returnTo));
    window.location.assign(`/api/v1/auth/social/authorization/${provider}`);
  }

  return (
    <div className="d-grid gap-2">
      {SOCIAL_PROVIDERS.map((provider) => {
        const details = SOCIAL_PROVIDER_DETAILS[provider];
        const isStarting = startingProvider === provider;

        return (
          <Button
            key={provider}
            type="button"
            variant="outline-dark"
            className={details.buttonClassName}
            disabled={startingProvider !== null}
            onClick={() => startSocialLogin(provider)}
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
