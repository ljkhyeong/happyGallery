const GUEST_CART_LOCK_NAME = "hg_guest_cart";

type GuestCartOperation<T> = () => T | PromiseLike<T>;

export class GuestCartLockUnavailableError extends Error {
  constructor() {
    super("이 브라우저에서는 안전한 장바구니 병합을 지원하지 않습니다.");
    this.name = "GuestCartLockUnavailableError";
  }
}

export function editGuestCartExclusive<T>(
  operation: GuestCartOperation<T>,
): Promise<T> {
  if (!navigator.locks) {
    return Promise.resolve().then(operation);
  }
  return navigator.locks.request(GUEST_CART_LOCK_NAME, operation);
}

export function mergeGuestCartExclusive<T>(
  operation: GuestCartOperation<T>,
): Promise<T> {
  if (!navigator.locks) {
    return Promise.reject(new GuestCartLockUnavailableError());
  }
  return navigator.locks.request(GUEST_CART_LOCK_NAME, operation);
}
