import type { SocialProvider } from "@/features/customer-auth/socialAuth";
import {
  getMySocialAccounts,
  startMySocialAccountLink as startMySocialAccountLinkRequest,
  unlinkMySocialAccount as unlinkMySocialAccountRequest,
} from "@/generated/api/customerAccount";

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
