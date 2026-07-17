import { useState } from "react";
import { Button } from "react-bootstrap";
import { api } from "@/shared/api";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import {
  buildSocialRedirectUri,
  SOCIAL_PROVIDER_DETAILS,
  SOCIAL_PROVIDERS,
  type SocialProvider,
} from "@/features/customer-auth/socialAuth";

interface AuthorizationUrlResponse {
  url: string;
  state: string;
}

interface SocialLoginButtonsProps {
  action: "로그인" | "회원가입";
  returnTo: string;
  onError: (message: string) => void;
}

export function SocialLoginButtons({ action, returnTo, onError }: SocialLoginButtonsProps) {
  const [startingProvider, setStartingProvider] = useState<SocialProvider | null>(null);

  async function startSocialLogin(provider: SocialProvider) {
    const details = SOCIAL_PROVIDER_DETAILS[provider];
    const redirectUri = buildSocialRedirectUri(provider);

    onError("");
    setStartingProvider(provider);

    try {
      const authorization = await api<AuthorizationUrlResponse>(`/auth/social/${provider}/url`, {
        params: { redirectUri },
      });

      sessionStorage.setItem(SESSION_KEYS.socialLoginReturnTo, returnTo);
      sessionStorage.setItem(details.stateStorageKey, authorization.state);
      window.location.assign(authorization.url);
    } catch {
      sessionStorage.removeItem(details.stateStorageKey);
      setStartingProvider(null);
      onError(`${details.label} ${action} 준비에 실패했습니다.`);
    }
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
