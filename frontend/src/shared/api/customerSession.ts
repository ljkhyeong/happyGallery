type CustomerSessionExpiredListener = () => void;

const expiredListeners = new Set<CustomerSessionExpiredListener>();
let sessionVersion = 0;

export function subscribeToCustomerSessionExpired(
  listener: CustomerSessionExpiredListener,
): () => void {
  expiredListeners.add(listener);
  return () => expiredListeners.delete(listener);
}

export function currentCustomerSessionVersion(): number {
  return sessionVersion;
}

export function isCurrentCustomerSessionVersion(version: number): boolean {
  return version === sessionVersion;
}

export function advanceCustomerSessionVersion(): void {
  sessionVersion += 1;
}

export function publishCustomerSessionExpired(requestVersion: number): void {
  if (!isCurrentCustomerSessionVersion(requestVersion)) return;
  advanceCustomerSessionVersion();
  expiredListeners.forEach((listener) => listener());
}
