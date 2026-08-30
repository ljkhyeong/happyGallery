import naverLoginIcon from "@/assets/naver-login-icon.png";

export const SOCIAL_PROVIDERS = ["google", "naver", "kakao"] as const;

export type SocialProvider = (typeof SOCIAL_PROVIDERS)[number];

interface SocialProviderDetails {
  label: string;
  buttonClassName?: string;
  iconSrc?: string;
}

export const SOCIAL_PROVIDER_DETAILS: Record<SocialProvider, SocialProviderDetails> = {
  google: {
    label: "Google",
  },
  naver: {
    label: "네이버",
    buttonClassName: "social-login-button-naver",
    iconSrc: naverLoginIcon,
  },
  kakao: {
    label: "카카오",
    buttonClassName: "social-login-button-kakao",
  },
};
