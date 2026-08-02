import {
  createContext,
  createElement,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  getCurrentCustomer,
  loginCustomer,
  logoutCustomer,
  signupCustomer,
  withdrawMyAccount,
  type CustomerUserResponse,
} from "@/generated/api/customerAuth";
import {
  ApiError,
  captureCustomerSession,
  clearCustomerQueryCache,
  currentCustomerSessionVersion,
  isCurrentCustomerSession,
  markCustomerSessionActive,
  markCustomerSessionInactive,
  publishCustomerSessionBoundary,
  requireCurrentCustomerSession,
  runForCurrentCustomer,
  subscribeToCustomerSessionExpired,
  synchronizeCustomerSessionBoundary,
  type CustomerSessionSnapshot,
} from "@/shared/api";
import { normalizePhone } from "@/shared/validation/phone";
import type { PolicyAcceptance } from "@/features/policy-consent/types";

export type CustomerUser = CustomerUserResponse;
export type CustomerAuthStatus =
  | "unknown"
  | "authenticated"
  | "unauthenticated"
  | "error";

interface CustomerAuthContextValue {
  user: CustomerUser | null;
  sessionVersion: number;
  status: CustomerAuthStatus;
  error: unknown;
  isAuthenticated: boolean;
  isLoading: boolean;
  isRefreshing: boolean;
  login: (email: string, password: string) => Promise<CustomerUser>;
  signup: (
    email: string,
    password: string,
    name: string,
    phone: string,
    verificationCode: string,
    policyAcceptance: PolicyAcceptance,
  ) => Promise<CustomerUser>;
  logout: () => Promise<void>;
  withdraw: () => Promise<void>;
  refresh: () => Promise<CustomerUser | null>;
}

interface FetchMeRequest {
  snapshot: CustomerSessionSnapshot;
  promise: Promise<CustomerUser | null>;
}

const CustomerAuthContext = createContext<CustomerAuthContextValue | null>(null);

export function CustomerAuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [user, setUser] = useState<CustomerUser | null>(null);
  const userIdRef = useRef<number | null>(null);
  const authStatusRef = useRef<CustomerAuthStatus>("unknown");
  const fetchMeRequestRef = useRef<FetchMeRequest | null>(null);
  const [sessionVersion, setSessionVersion] = useState(currentCustomerSessionVersion);
  const [status, setStatus] = useState<CustomerAuthStatus>("unknown");
  const [error, setError] = useState<unknown>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const updateStatus = useCallback((nextStatus: CustomerAuthStatus) => {
    authStatusRef.current = nextStatus;
    setStatus(nextStatus);
  }, []);

  const clearCustomerQueries = useCallback(() => {
    clearCustomerQueryCache(queryClient);
  }, [queryClient]);

  const expireCustomerSession = useCallback(() => {
    clearCustomerQueries();
    userIdRef.current = null;
    setSessionVersion(currentCustomerSessionVersion());
    setUser(null);
    setError(null);
    updateStatus("unauthenticated");
    setIsLoading(false);
    setIsRefreshing(false);
  }, [clearCustomerQueries, updateStatus]);

  const publishSessionBoundary = useCallback((customerId: number | null) => {
    publishCustomerSessionBoundary(customerId);
    setSessionVersion(currentCustomerSessionVersion());
  }, []);

  const fetchMe = useCallback(() => {
    const requestSnapshot = captureCustomerSession();
    const inFlightRequest = fetchMeRequestRef.current;
    if (
      inFlightRequest
      && inFlightRequest.snapshot.version === requestSnapshot.version
      && inFlightRequest.snapshot.boundaryEpoch === requestSnapshot.boundaryEpoch
      && inFlightRequest.snapshot.boundaryCustomerId
        === requestSnapshot.boundaryCustomerId
      && isCurrentCustomerSession(requestSnapshot)
    ) {
      return inFlightRequest.promise;
    }

    if (authStatusRef.current === "error") {
      setError(null);
      updateStatus("unknown");
      setIsLoading(true);
    }
    setIsRefreshing(true);

    const request = (async (): Promise<CustomerUser | null> => {
      let activeSnapshot = requestSnapshot;
      try {
        const me = await getCurrentCustomer();
        requireCurrentCustomerSession(requestSnapshot);
        if (requestSnapshot.boundaryCustomerId !== me.id) {
          publishSessionBoundary(me.id);
          activeSnapshot = captureCustomerSession();
          clearCustomerQueries();
        }
        if (markCustomerSessionActive(me.id)) {
          setSessionVersion(currentCustomerSessionVersion());
          activeSnapshot = captureCustomerSession();
        }
        userIdRef.current = me.id;
        setUser(me);
        setError(null);
        updateStatus("authenticated");
        return me;
      } catch (error) {
        requireCurrentCustomerSession(requestSnapshot);
        if (
          error instanceof ApiError
          && error.status === 401
          && error.code === "UNAUTHORIZED"
        ) {
          markCustomerSessionInactive();
          userIdRef.current = null;
          setUser(null);
          setError(null);
          updateStatus("unauthenticated");
          return null;
        }
        if (authStatusRef.current === "unknown") {
          setError(error);
          updateStatus("error");
        }
        throw error;
      } finally {
        if (isCurrentCustomerSession(activeSnapshot)) {
          setIsLoading(false);
          setIsRefreshing(false);
        }
      }
    })();
    fetchMeRequestRef.current = { snapshot: requestSnapshot, promise: request };
    const clearRequest = () => {
      if (fetchMeRequestRef.current?.promise === request) {
        fetchMeRequestRef.current = null;
      }
    };
    void request.then(clearRequest, clearRequest);
    return request;
  }, [clearCustomerQueries, publishSessionBoundary, updateStatus]);

  useEffect(
    () => subscribeToCustomerSessionExpired(expireCustomerSession),
    [expireCustomerSession],
  );

  useEffect(() => {
    const reconcileSharedBoundary = (forceRefresh: boolean) => {
      const boundaryChanged = synchronizeCustomerSessionBoundary();
      if (boundaryChanged) {
        clearCustomerQueries();
        userIdRef.current = null;
        setSessionVersion(currentCustomerSessionVersion());
        setUser(null);
        setError(null);
        updateStatus("unknown");
        setIsLoading(true);
      }
      if (boundaryChanged || forceRefresh) {
        void fetchMe().catch(() => undefined);
      }
    };
    const handleStorage = () => reconcileSharedBoundary(false);
    const handlePageShow = () => reconcileSharedBoundary(true);
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        reconcileSharedBoundary(true);
      }
    };

    window.addEventListener("storage", handleStorage);
    window.addEventListener("pageshow", handlePageShow);
    document.addEventListener("visibilitychange", handleVisibilityChange);
    reconcileSharedBoundary(true);
    return () => {
      window.removeEventListener("storage", handleStorage);
      window.removeEventListener("pageshow", handlePageShow);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [clearCustomerQueries, fetchMe, updateStatus]);

  const login = useCallback(
    async (email: string, password: string): Promise<CustomerUser> => {
      const me = await runForCurrentCustomer(() =>
        loginCustomer({ email, password }));
      publishSessionBoundary(me.id);
      clearCustomerQueries();
      userIdRef.current = me.id;
      setUser(me);
      setError(null);
      updateStatus("authenticated");
      setIsLoading(false);
      setIsRefreshing(false);
      return me;
    },
    [clearCustomerQueries, publishSessionBoundary, updateStatus],
  );

  const signup = useCallback(
    async (
      email: string,
      password: string,
      name: string,
      phone: string,
      verificationCode: string,
      policyAcceptance: PolicyAcceptance,
    ): Promise<CustomerUser> => {
      const me = await runForCurrentCustomer(() =>
        signupCustomer({
          email,
          password,
          name,
          phone: normalizePhone(phone),
          verificationCode,
          policyAcceptance,
        }));
      publishSessionBoundary(me.id);
      clearCustomerQueries();
      userIdRef.current = me.id;
      setUser(me);
      setError(null);
      updateStatus("authenticated");
      setIsLoading(false);
      setIsRefreshing(false);
      return me;
    },
    [clearCustomerQueries, publishSessionBoundary, updateStatus],
  );

  const logout = useCallback(async () => {
    await runForCurrentCustomer(() => logoutCustomer());
    publishSessionBoundary(null);
    clearCustomerQueries();
    userIdRef.current = null;
    setUser(null);
    setError(null);
    updateStatus("unauthenticated");
    setIsLoading(false);
    setIsRefreshing(false);
  }, [clearCustomerQueries, publishSessionBoundary, updateStatus]);

  const withdraw = useCallback(async () => {
    await runForCurrentCustomer(() => withdrawMyAccount());
    publishSessionBoundary(null);
    clearCustomerQueries();
    userIdRef.current = null;
    setUser(null);
    setError(null);
    updateStatus("unauthenticated");
    setIsLoading(false);
    setIsRefreshing(false);
  }, [clearCustomerQueries, publishSessionBoundary, updateStatus]);

  return createElement(
    CustomerAuthContext,
    {
      value: {
        user,
        sessionVersion,
        status,
        error,
        isAuthenticated: user !== null,
        isLoading: isLoading || status === "error",
        isRefreshing,
        login,
        signup,
        logout,
        withdraw,
        refresh: fetchMe,
      },
    },
    children,
  );
}

export function useCustomerAuth() {
  const context = useContext(CustomerAuthContext);
  if (!context) {
    throw new Error("useCustomerAuth must be used within CustomerAuthProvider");
  }
  return context;
}
