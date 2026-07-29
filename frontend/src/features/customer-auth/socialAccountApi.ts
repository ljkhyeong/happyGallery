import type { SocialProvider } from "@/features/customer-auth/socialAuth";
import {
  getMySocialAccounts,
  startMySocialAccountLink as startMySocialAccountLinkRequest,
  startMySocialReauthentication as startMySocialReauthenticationRequest,
  unlinkMySocialAccount as unlinkMySocialAccountRequest,
  type SocialAccountAuthorizationResponse,
} from "@/generated/api/customerAccount";
import { reauthenticateMyPassword } from "@/generated/api/customerAuth";

export async function fetchLinkedSocialProviders(): Promise<SocialProvider[]> {
  const response = await getMySocialAccounts();
  return response.linkedProviders.map((provider) => provider.toLowerCase() as SocialProvider);
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
