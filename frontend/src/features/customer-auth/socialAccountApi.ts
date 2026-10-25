import type { SocialProvider } from "@/features/customer-auth/socialAuth";
import {
  getMySocialAccounts,
  startMySocialAccountLink as startMySocialAccountLinkRequest,
  startMySocialReauthentication as startMySocialReauthenticationRequest,
  unlinkMySocialAccount as unlinkMySocialAccountRequest,
  type SocialAccountAuthorizationResponse,
} from "@/generated/api/customerAccount";
import { reauthenticateMyPassword } from "@/generated/api/customerAuth";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";

export async function fetchLinkedSocialProviders(): Promise<SocialProvider[]> {
  const response = await getMySocialAccounts();
  return response.linkedProviders.map((provider) => provider.toLowerCase() as SocialProvider);
}

/** 계정 설정과 재인증 안내가 같은 연결된 소셜 계정 캐시를 쓴다. */
export function linkedSocialProvidersQuery() {
  return {
    queryKey: queryKeys.member.socialAccounts,
    queryFn: () => runForCurrentCustomer(fetchLinkedSocialProviders),
  };
}

export function startSocialAccountLink(provider: SocialProvider) {
  return startMySocialAccountLinkRequest(provider);
}

export function unlinkSocialAccount(provider: SocialProvider) {
  return unlinkMySocialAccountRequest(provider);
}

export function reauthenticateCustomerPassword(currentPassword: string): Promise<void> {
  return reauthenticateMyPassword({ currentPassword });
}

export function startSocialReauthentication(
  provider: SocialProvider,
): Promise<SocialAccountAuthorizationResponse> {
  return startMySocialReauthenticationRequest(provider);
}
