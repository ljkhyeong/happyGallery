import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import naverLoginIcon from "@/assets/naver-login-icon.png";

export const SOCIAL_PROVIDERS = ["google", "naver"] as const;

export type SocialProvider = (typeof SOCIAL_PROVIDERS)[number];

interface SocialProviderDetails {
  label: string;
  stateStorageKey: string;
  buttonClassName?: string;
  iconSrc?: string;
}

export const SOCIAL_PROVIDER_DETAILS: Record<SocialProvider, SocialProviderDetails> = {
  google: {
    label: "Google",
    stateStorageKey: SESSION_KEYS.googleOauthState,
  },
  naver: {
    label: "네이버",
    stateStorageKey: SESSION_KEYS.naverOauthState,
    buttonClassName: "social-login-button-naver",
    iconSrc: naverLoginIcon,
  },
};

export function isSocialProvider(value: string | undefined): value is SocialProvider {
  return SOCIAL_PROVIDERS.some((provider) => provider === value);
}

export function buildSocialRedirectUri(provider: SocialProvider) {
  return `${window.location.origin}/auth/callback/${provider}`;
}
