import type { SocialProvider } from "@/features/customer-auth/socialAuth";
import { startSocialReauthentication } from "@/features/customer-auth/socialAccountApi";
import {
  removeSessionValues,
  writeSessionValues,
} from "@/shared/storage/browserSessionStorage";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import {
  ApiError,
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

export const CONTINUATION_STORAGE_ERROR_MESSAGE =
  "인증 정보를 저장하지 못했습니다. 브라우저 저장소 설정을 확인해 주세요.";

/** 민감한 변경 요청에 서버가 본인 확인을 다시 요구한 오류인지 판별한다. */
export function isReauthenticationRequired(error: unknown): error is ApiError {
  return error instanceof ApiError && error.is("REAUTHENTICATION_REQUIRED");
}

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

/**
 * 외부 인증 화면에서 돌아와 이어 할 작업을 회원 ID와 함께 저장한 뒤 그 화면으로 이동한다.
 * 회원을 확인할 수 없으면 세션 변경으로 보고, 저장하지 못하면 이동하지 않고 false를 돌려준다.
 */
export function storeContinuationAndRedirect(
  url: string,
  entries: ReadonlyArray<readonly [key: string, value: string]>,
  customerId: number | null = currentCustomerSessionUserId(),
): boolean {
  if (customerId === null) throw new CustomerSessionChangedError();
  clearCustomerStepUpContinuation();
  if (!writeSessionValues([
    [SESSION_KEYS.customerContinuationOwner, String(customerId)],
    ...entries,
  ])) {
    return false;
  }
  window.location.assign(url);
  return true;
}

function continuationEntry(continuation: CustomerStepUpContinuation): readonly [string, string] {
  switch (continuation.kind) {
    case "return":
      return [SESSION_KEYS.stepUpReturnAction, continuation.action];
    case "social-link":
      return [SESSION_KEYS.socialAccountLinkTarget, continuation.provider];
    case "social-unlink":
      return [SESSION_KEYS.socialAccountUnlinkTarget, continuation.provider];
  }
}

export async function redirectToSocialStepUp(
  reauthenticationProvider: SocialProvider,
  continuation: CustomerStepUpContinuation,
) {
  await runForCurrentCustomer(
    () => startSocialReauthentication(reauthenticationProvider),
    ({ authorizationUrl }) => {
      const stored = storeContinuationAndRedirect(authorizationUrl, [
        [SESSION_KEYS.socialReauthentication, reauthenticationProvider],
        continuationEntry(continuation),
      ]);
      if (!stored) throw new Error(CONTINUATION_STORAGE_ERROR_MESSAGE);
    },
  );
}
