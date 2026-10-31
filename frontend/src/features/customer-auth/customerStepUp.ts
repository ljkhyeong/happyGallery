import type { SocialProvider } from "@/features/customer-auth/socialAuth";
import { startSocialReauthentication } from "@/features/customer-auth/socialAccountApi";
import {
  readSessionValue,
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

type CustomerStepUpContinuation =
  | { kind: "return"; action: CustomerStepUpReturnAction }
  | { kind: "social-link"; provider: SocialProvider }
  | { kind: "social-unlink"; provider: SocialProvider };

/** 콜백 화면이 읽는 저장된 이어가기 값. 저장되지 않은 값은 null이다. */
interface StoredCustomerStepUpContinuation {
  /** 이어가기를 저장한 회원 ID. 저장값이 양의 정수가 아니면 null */
  owner: number | null;
  /** 소셜 계정 연결 인증에서 돌아왔으면 그 provider */
  socialAccountLink: SocialProvider | null;
  /** 소셜 재인증에서 돌아왔으면 그 provider */
  reauthenticationProvider: SocialProvider | null;
  linkTarget: SocialProvider | null;
  unlinkTarget: SocialProvider | null;
  returnAction: CustomerStepUpReturnAction | null;
}

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
 * 외부 인증에서 돌아온 콜백이 저장된 이어가기 값을 한 번에 읽는다.
 * 돌아온 경로(연결·재인증)와 소유자는 콜백 한 번에만 쓰여 읽는 즉시 지우고,
 * 이어 할 작업(연결·해제 대상, 복귀 동작)은 분기가 처리할 때 clearCustomerStepUpContinuation으로 지운다.
 */
export function readCustomerStepUpContinuation(): StoredCustomerStepUpContinuation {
  const owner = Number(readSessionValue(SESSION_KEYS.customerContinuationOwner));
  const continuation: StoredCustomerStepUpContinuation = {
    owner: Number.isSafeInteger(owner) && owner > 0 ? owner : null,
    socialAccountLink: readSessionValue(SESSION_KEYS.socialAccountLink) as SocialProvider | null,
    reauthenticationProvider:
      readSessionValue(SESSION_KEYS.socialReauthentication) as SocialProvider | null,
    linkTarget: readSessionValue(SESSION_KEYS.socialAccountLinkTarget) as SocialProvider | null,
    unlinkTarget: readSessionValue(SESSION_KEYS.socialAccountUnlinkTarget) as SocialProvider | null,
    returnAction:
      readSessionValue(SESSION_KEYS.stepUpReturnAction) as CustomerStepUpReturnAction | null,
  };
  removeSessionValues(
    SESSION_KEYS.socialAccountLink,
    SESSION_KEYS.socialReauthentication,
    SESSION_KEYS.customerContinuationOwner,
  );
  return continuation;
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
