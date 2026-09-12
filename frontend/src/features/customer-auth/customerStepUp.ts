import type { SocialProvider } from "@/features/customer-auth/socialAuth";
import { startSocialReauthentication } from "@/features/customer-auth/socialAccountApi";
import {
  removeSessionValues,
  writeSessionValues,
} from "@/shared/storage/browserSessionStorage";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import {
  CustomerSessionChangedError,
  currentCustomerSessionUserId,
  runForCurrentCustomer,
} from "@/shared/api";

export type CustomerStepUpReturnAction =
  | "phone-change"
  | "email-registration"
  | "account-withdrawal";

export type CustomerStepUpContinuation =
  | { kind: "return"; action: CustomerStepUpReturnAction }
  | { kind: "social-link"; provider: SocialProvider }
  | { kind: "social-unlink"; provider: SocialProvider };

export function clearCustomerStepUpContinuation() {
  removeSessionValues(
    SESSION_KEYS.socialAccountLink,
    SESSION_KEYS.socialAccountLinkTarget,
    SESSION_KEYS.socialAccountUnlinkTarget,
    SESSION_KEYS.socialReauthentication,
    SESSION_KEYS.stepUpReturnAction,
    SESSION_KEYS.customerContinuationOwner,
  );
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
      const entries: Array<readonly [string, string]> = [
        [SESSION_KEYS.customerContinuationOwner, String(customerId)],
        [SESSION_KEYS.socialReauthentication, reauthenticationProvider],
      ];
      switch (continuation.kind) {
        case "return":
          entries.push([SESSION_KEYS.stepUpReturnAction, continuation.action]);
          break;
        case "social-link":
          entries.push([
            SESSION_KEYS.socialAccountLinkTarget,
            continuation.provider,
          ]);
          break;
        case "social-unlink":
          entries.push([
            SESSION_KEYS.socialAccountUnlinkTarget,
            continuation.provider,
          ]);
          break;
      }
      if (!writeSessionValues(entries)) {
        throw new Error("인증 정보를 저장하지 못했습니다. 브라우저 저장소 설정을 확인해 주세요.");
      }
      window.location.assign(authorizationUrl);
    },
  );
}
