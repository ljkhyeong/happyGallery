type CustomerSessionExpiredListener = () => void;

interface PersistedCustomerSessionBoundary {
  epoch: string;
  customerId: number | null;
}

interface CustomerSessionBoundaryRead {
  available: boolean;
  boundary: PersistedCustomerSessionBoundary | null;
}

interface LocalBoundaryFallback {
  boundary: PersistedCustomerSessionBoundary;
  persistedBeforeWrite: PersistedCustomerSessionBoundary | null;
  persistedBeforeWriteKnown: boolean;
}

export interface CustomerSessionSnapshot {
  version: number;
  boundaryEpoch: string | null;
  boundaryCustomerId: number | null;
}

export interface CustomerSessionOwnedState {
  customerSession: CustomerSessionSnapshot;
}

const CUSTOMER_SESSION_BOUNDARY_KEY = "hg_customer_session_boundary";
const expiredListeners = new Set<CustomerSessionExpiredListener>();
let sessionVersion = 0;
const initialBoundaryRead = readCustomerSessionBoundary();
let observedBoundary = initialBoundaryRead.boundary;
let localBoundaryFallback: LocalBoundaryFallback | null = null;
// undefined는 이 탭이 아직 /me로 실제 세션 주체를 확인하지 않은 상태다.
let activeCustomerId: number | null | undefined;
let advanceOnInitialMemberObservation = true;

function readCustomerSessionBoundary(): CustomerSessionBoundaryRead {
  if (typeof window === "undefined") {
    return { available: false, boundary: null };
  }

  let value: string | null;
  try {
    value = window.localStorage.getItem(CUSTOMER_SESSION_BOUNDARY_KEY);
  } catch {
    return { available: false, boundary: null };
  }
  if (!value) return { available: true, boundary: null };

  try {
    const parsed = JSON.parse(value) as Partial<PersistedCustomerSessionBoundary>;
    const validCustomerId = parsed.customerId === null
      || (
        typeof parsed.customerId === "number"
        && Number.isSafeInteger(parsed.customerId)
        && parsed.customerId > 0
      );
    if (
      typeof parsed.epoch !== "string"
      || parsed.epoch.length === 0
      || !validCustomerId
    ) {
      return { available: true, boundary: null };
    }
    return {
      available: true,
      boundary: {
        epoch: parsed.epoch,
        customerId: parsed.customerId ?? null,
      },
    };
  } catch {
    return { available: true, boundary: null };
  }
}

function currentCustomerSessionBoundary(): PersistedCustomerSessionBoundary | null {
  const currentBoundaryRead = readCustomerSessionBoundary();
  const fallback = localBoundaryFallback;
  if (!fallback) {
    return currentBoundaryRead.available
      ? currentBoundaryRead.boundary
      : observedBoundary;
  }
  if (!currentBoundaryRead.available) return fallback.boundary;
  if (sameBoundary(currentBoundaryRead.boundary, fallback.boundary)) {
    return fallback.boundary;
  }
  if (
    fallback.persistedBeforeWriteKnown
    && !sameBoundary(
      currentBoundaryRead.boundary,
      fallback.persistedBeforeWrite,
    )
  ) {
    return currentBoundaryRead.boundary;
  }
  return fallback.boundary;
}

function sameBoundary(
  left: PersistedCustomerSessionBoundary | null,
  right: PersistedCustomerSessionBoundary | null,
): boolean {
  return left?.epoch === right?.epoch
    && left?.customerId === right?.customerId;
}

function createBoundaryEpoch(): string {
  const suffix = globalThis.crypto?.randomUUID?.()
    ?? Math.random().toString(36).slice(2);
  return `${Date.now()}:${suffix}`;
}

function storeCustomerSessionBoundary(
  boundary: PersistedCustomerSessionBoundary,
): void {
  const persistedBeforeWrite = readCustomerSessionBoundary();
  if (typeof window === "undefined") {
    localBoundaryFallback = {
      boundary,
      persistedBeforeWrite: null,
      persistedBeforeWriteKnown: false,
    };
    return;
  }

  try {
    window.localStorage.setItem(
      CUSTOMER_SESSION_BOUNDARY_KEY,
      JSON.stringify(boundary),
    );
    localBoundaryFallback = null;
  } catch {
    const persistedAfterWrite = readCustomerSessionBoundary();
    if (
      persistedAfterWrite.available
      && sameBoundary(persistedAfterWrite.boundary, boundary)
    ) {
      localBoundaryFallback = null;
      return;
    }
    localBoundaryFallback = {
      boundary,
      persistedBeforeWrite: persistedBeforeWrite.boundary,
      persistedBeforeWriteKnown: persistedBeforeWrite.available,
    };
  }
}

export class CustomerSessionChangedError extends Error {
  constructor() {
    super("회원 세션이 변경되어 이전 요청의 결과를 적용하지 않습니다.");
    this.name = "CustomerSessionChangedError";
  }
}

export function subscribeToCustomerSessionExpired(
  listener: CustomerSessionExpiredListener,
): () => void {
  expiredListeners.add(listener);
  return () => expiredListeners.delete(listener);
}

export function currentCustomerSessionVersion(): number {
  return sessionVersion;
}

export function captureCustomerSession(): CustomerSessionSnapshot {
  return {
    version: sessionVersion,
    boundaryEpoch: observedBoundary?.epoch ?? null,
    boundaryCustomerId: observedBoundary?.customerId ?? null,
  };
}

export function isCurrentCustomerSession(
  snapshot: CustomerSessionSnapshot,
): boolean {
  const currentBoundary = currentCustomerSessionBoundary();
  return snapshot.version === sessionVersion
    && snapshot.boundaryEpoch === (currentBoundary?.epoch ?? null)
    && snapshot.boundaryCustomerId === (currentBoundary?.customerId ?? null);
}

function isCustomerSessionSnapshot(
  value: unknown,
): value is CustomerSessionSnapshot {
  if (typeof value !== "object" || value === null) return false;
  const snapshot = value as Partial<CustomerSessionSnapshot>;
  return typeof snapshot.version === "number"
    && Number.isSafeInteger(snapshot.version)
    && snapshot.version >= 0
    && (
      snapshot.boundaryEpoch === null
      || (
        typeof snapshot.boundaryEpoch === "string"
        && snapshot.boundaryEpoch.length > 0
      )
    )
    && (
      snapshot.boundaryCustomerId === null
      || (
        typeof snapshot.boundaryCustomerId === "number"
        && Number.isSafeInteger(snapshot.boundaryCustomerId)
        && snapshot.boundaryCustomerId > 0
      )
    );
}

export function isCurrentCustomerSessionState(
  value: unknown,
): value is CustomerSessionOwnedState {
  if (typeof value !== "object" || value === null) return false;
  const state = value as Partial<CustomerSessionOwnedState>;
  return isCustomerSessionSnapshot(state.customerSession)
    && isCurrentCustomerSession(state.customerSession);
}

function advanceCustomerSessionVersion(): void {
  sessionVersion += 1;
}

export function currentCustomerSessionUserId(): number | null {
  return activeCustomerId ?? null;
}

export function markCustomerSessionActive(customerId: number): boolean {
  const initialMemberObserved =
    activeCustomerId === undefined && advanceOnInitialMemberObservation;
  activeCustomerId = customerId;
  advanceOnInitialMemberObservation = false;
  if (initialMemberObserved) {
    advanceCustomerSessionVersion();
  }
  return initialMemberObserved;
}

export function markCustomerSessionInactive(): void {
  activeCustomerId = null;
  advanceOnInitialMemberObservation = false;
}

export function publishCustomerSessionBoundary(customerId: number | null): void {
  const boundary: PersistedCustomerSessionBoundary = {
    epoch: createBoundaryEpoch(),
    customerId,
  };
  storeCustomerSessionBoundary(boundary);
  observedBoundary = boundary;
  activeCustomerId = customerId;
  advanceOnInitialMemberObservation = false;
  advanceCustomerSessionVersion();
}

export function synchronizeCustomerSessionBoundary(): boolean {
  const currentBoundaryRead = readCustomerSessionBoundary();
  if (!currentBoundaryRead.available) return false;
  const currentBoundary = currentBoundaryRead.boundary;
  const fallback = localBoundaryFallback;
  if (fallback) {
    if (sameBoundary(currentBoundary, fallback.boundary)) {
      localBoundaryFallback = null;
    } else if (
      !fallback.persistedBeforeWriteKnown
      || sameBoundary(currentBoundary, fallback.persistedBeforeWrite)
    ) {
      return false;
    } else {
      localBoundaryFallback = null;
    }
  }
  if (sameBoundary(observedBoundary, currentBoundary)) {
    return false;
  }

  observedBoundary = currentBoundary;
  activeCustomerId = undefined;
  advanceOnInitialMemberObservation = false;
  advanceCustomerSessionVersion();
  return true;
}

export function requireCurrentCustomerSession(
  snapshot: CustomerSessionSnapshot,
): void {
  if (!isCurrentCustomerSession(snapshot)) {
    throw new CustomerSessionChangedError();
  }
}

export async function runForCustomerSession<T>(
  snapshot: CustomerSessionSnapshot,
  operation: () => Promise<T>,
): Promise<T> {
  requireCurrentCustomerSession(snapshot);
  try {
    const result = await operation();
    requireCurrentCustomerSession(snapshot);
    return result;
  } catch (error) {
    requireCurrentCustomerSession(snapshot);
    throw error;
  }
}

type CurrentCustomerEffect<T, R> = (
  result: T,
  requireCurrent: () => void,
) => R | Promise<R>;

export function runForCurrentCustomer<T>(
  operation: () => Promise<T>,
): Promise<T>;

export function runForCurrentCustomer<T, R>(
  operation: () => Promise<T>,
  effect: CurrentCustomerEffect<T, R>,
): Promise<R>;

export async function runForCurrentCustomer<T, R>(
  operation: () => Promise<T>,
  effect?: CurrentCustomerEffect<T, R>,
): Promise<T | R> {
  const snapshot = captureCustomerSession();
  const requireCurrent = () => requireCurrentCustomerSession(snapshot);
  requireCurrent();
  let result: T;
  try {
    result = await operation();
  } catch (error) {
    requireCurrent();
    throw error;
  }
  requireCurrent();
  if (!effect) {
    return result;
  }
  let effectResult: R;
  try {
    effectResult = await effect(result, requireCurrent);
  } catch (error) {
    requireCurrent();
    throw error;
  }
  requireCurrent();
  return effectResult;
}

export function publishCustomerSessionExpired(
  requestSnapshot: CustomerSessionSnapshot,
): void {
  if (!isCurrentCustomerSession(requestSnapshot)) return;
  if (
    (activeCustomerId === undefined || activeCustomerId === null)
    && requestSnapshot.boundaryCustomerId === null
  ) {
    return;
  }
  publishCustomerSessionBoundary(null);
  expiredListeners.forEach((listener) => listener());
}
