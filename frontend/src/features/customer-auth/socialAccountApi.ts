import type { SocialProvider } from "@/features/customer-auth/socialAuth";
import {
  getMySocialAccounts,
  startMySocialAccountLink as startMySocialAccountLinkRequest,
  unlinkMySocialAccount as unlinkMySocialAccountRequest,
} from "@/generated/api/customerAccount";
import { api } from "@/shared/api";

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
  return api("/me/reauthentication/password", {
    method: "POST",
    body: { currentPassword },
  });
}

export function startSocialReauthentication(
  provider: SocialProvider,
): Promise<{ authorizationUrl: string }> {
  return api(`/me/social-accounts/${provider}/reauthentication`, {
    method: "POST",
  });
}
