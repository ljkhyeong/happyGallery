import type { SocialProvider } from "@/features/customer-auth/socialAuth";
import { startSocialReauthentication } from "@/features/customer-auth/socialAccountApi";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import {
  CustomerSessionChangedError,
  currentCustomerSessionUserId,
  runForCurrentCustomer,
} from "@/shared/api";

export type CustomerStepUpReturnAction =
  | "phone-change"
  | "account-withdrawal";

export type CustomerStepUpContinuation =
  | { kind: "return"; action: CustomerStepUpReturnAction }
  | { kind: "social-link"; provider: SocialProvider }
  | { kind: "social-unlink"; provider: SocialProvider };

export function clearCustomerStepUpContinuation() {
  sessionStorage.removeItem(SESSION_KEYS.socialAccountLink);
  sessionStorage.removeItem(SESSION_KEYS.socialAccountLinkTarget);
  sessionStorage.removeItem(SESSION_KEYS.socialAccountUnlinkTarget);
  sessionStorage.removeItem(SESSION_KEYS.socialReauthentication);
  sessionStorage.removeItem(SESSION_KEYS.stepUpReturnAction);
  sessionStorage.removeItem(SESSION_KEYS.customerContinuationOwner);
}

export async function redirectToSocialStepUp(
  reauthenticationProvider: SocialProvider,
  continuation: CustomerStepUpContinuation,
) {
  await runForCurrentCustomer(
    () => startSocialReauthentication(reauthenticationProvider),
    ({ authorizationUrl }) => {
      const customerId = currentCustomerSessionUserId();
      if (customerId === null) throw new CustomerSessionChangedError();
      clearCustomerStepUpContinuation();
      sessionStorage.setItem(
        SESSION_KEYS.customerContinuationOwner,
        String(customerId),
      );
      sessionStorage.setItem(
        SESSION_KEYS.socialReauthentication,
        reauthenticationProvider,
      );
      switch (continuation.kind) {
        case "return":
          sessionStorage.setItem(
            SESSION_KEYS.stepUpReturnAction,
            continuation.action,
          );
          break;
        case "social-link":
          sessionStorage.setItem(
            SESSION_KEYS.socialAccountLinkTarget,
            continuation.provider,
          );
          break;
        case "social-unlink":
          sessionStorage.setItem(
            SESSION_KEYS.socialAccountUnlinkTarget,
            continuation.provider,
          );
          break;
      }
      window.location.assign(authorizationUrl);
    },
  );
}
