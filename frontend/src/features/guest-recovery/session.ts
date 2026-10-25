import {
  createOwnedRecoverySessionStore,
  type CustomerSessionStorageHandle,
} from "@/shared/storage/customerSessionOwner";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import type { GuestRecordRecoveryResponse } from "./api";

export type GuestRecordRecoverySession =
  CustomerSessionStorageHandle<GuestRecordRecoveryResponse>;

function isGuestRecordRecovery(value: unknown): value is GuestRecordRecoveryResponse {
  const result = value as Partial<GuestRecordRecoveryResponse> | undefined;
  return Boolean(result)
    && typeof result?.accessToken === "string"
    && result.accessToken.length > 0
    && typeof result.expiresAt === "string";
}

export const {
  save: saveGuestRecordRecovery,
  load: loadGuestRecordRecovery,
  clear: clearGuestRecordRecovery,
} = createOwnedRecoverySessionStore({
  key: SESSION_KEYS.guestRecordRecovery,
  isValue: isGuestRecordRecovery,
});
