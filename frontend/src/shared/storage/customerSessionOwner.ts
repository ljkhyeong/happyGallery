import {
  captureCustomerSession,
  isCurrentCustomerSession,
  type CustomerSessionSnapshot,
} from "@/shared/api/customerSession";

export interface CustomerSessionStorageOwner {
  boundaryEpoch: string | null;
  boundaryCustomerId: number | null;
}

export interface CustomerSessionStorageHandle<T> {
  owner: CustomerSessionStorageOwner;
  value: T;
}

export function currentCustomerSessionStorageOwner(
  expectedSnapshot: CustomerSessionSnapshot = captureCustomerSession(),
): CustomerSessionStorageOwner | null {
  if (!isCurrentCustomerSession(expectedSnapshot)) return null;
  return {
    boundaryEpoch: expectedSnapshot.boundaryEpoch,
    boundaryCustomerId: expectedSnapshot.boundaryCustomerId,
  };
}

export function isCurrentCustomerSessionStorageOwner(
  owner: CustomerSessionStorageOwner,
  expectedSnapshot?: CustomerSessionSnapshot,
): boolean {
  const currentOwner = currentCustomerSessionStorageOwner(expectedSnapshot);
  return currentOwner !== null && sameCustomerSessionStorageOwner(owner, currentOwner);
}

export function isCustomerSessionStorageOwner(
  value: unknown,
): value is CustomerSessionStorageOwner {
  if (typeof value !== "object" || value === null) return false;
  const owner = value as Partial<CustomerSessionStorageOwner>;
  return (
    owner.boundaryEpoch === null
    || (
      typeof owner.boundaryEpoch === "string"
      && owner.boundaryEpoch.length > 0
    )
  ) && (
    owner.boundaryCustomerId === null
    || (
      typeof owner.boundaryCustomerId === "number"
      && Number.isSafeInteger(owner.boundaryCustomerId)
      && owner.boundaryCustomerId > 0
    )
  );
}

export function sameCustomerSessionStorageOwner(
  left: CustomerSessionStorageOwner,
  right: CustomerSessionStorageOwner,
): boolean {
  return left.boundaryEpoch === right.boundaryEpoch
    && left.boundaryCustomerId === right.boundaryCustomerId;
}
