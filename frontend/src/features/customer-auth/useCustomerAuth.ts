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

interface CustomerAuthContextValue {
  user: CustomerUser | null;
  sessionVersion: number;
  isAuthenticated: boolean;
  isLoading: boolean;
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
  const fetchMeRequestRef = useRef<FetchMeRequest | null>(null);
  const [sessionVersion, setSessionVersion] = useState(currentCustomerSessionVersion);
  const [isLoading, setIsLoading] = useState(true);

  const clearCustomerQueries = useCallback(() => {
    clearCustomerQueryCache(queryClient);
  }, [queryClient]);

  const expireCustomerSession = useCallback(() => {
    clearCustomerQueries();
    userIdRef.current = null;
    setSessionVersion(currentCustomerSessionVersion());
    setUser(null);
    setIsLoading(false);
  }, [clearCustomerQueries]);

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
          return null;
        }
        throw error;
      } finally {
        if (isCurrentCustomerSession(activeSnapshot)) {
          setIsLoading(false);
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
  }, [clearCustomerQueries, publishSessionBoundary]);

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
  }, [clearCustomerQueries, fetchMe]);

  const login = useCallback(
    async (email: string, password: string): Promise<CustomerUser> => {
      const me = await runForCurrentCustomer(() =>
        loginCustomer({ email, password }));
      publishSessionBoundary(me.id);
      clearCustomerQueries();
      userIdRef.current = me.id;
      setUser(me);
      setIsLoading(false);
      return me;
    },
    [clearCustomerQueries, publishSessionBoundary],
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
      setIsLoading(false);
      return me;
    },
    [clearCustomerQueries, publishSessionBoundary],
  );

  const logout = useCallback(async () => {
    await runForCurrentCustomer(() => logoutCustomer());
    publishSessionBoundary(null);
    clearCustomerQueries();
    userIdRef.current = null;
    setUser(null);
    setIsLoading(false);
  }, [clearCustomerQueries, publishSessionBoundary]);

  const withdraw = useCallback(async () => {
    await runForCurrentCustomer(() => withdrawMyAccount());
    publishSessionBoundary(null);
    clearCustomerQueries();
    userIdRef.current = null;
    setUser(null);
    setIsLoading(false);
  }, [clearCustomerQueries, publishSessionBoundary]);

  return createElement(
    CustomerAuthContext,
    {
      value: {
        user,
        sessionVersion,
        isAuthenticated: user !== null,
        isLoading,
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
