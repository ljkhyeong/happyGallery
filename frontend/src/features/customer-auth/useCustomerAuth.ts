import { createContext, createElement, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  api,
  advanceCustomerSessionVersion,
  clearCustomerQueryCache,
  currentCustomerSessionVersion,
  isCurrentCustomerSessionVersion,
  subscribeToCustomerSessionExpired,
} from "@/shared/api";
import { normalizePhone } from "@/shared/validation/phone";

interface CustomerUserResponse {
  id: number;
  email: string | null;
  name: string;
  phone: string | null;
  phoneVerified: boolean;
  localPasswordEnabled: boolean;
}

export interface CustomerUser {
  id: number;
  email: string | null;
  name: string;
  phone: string | null;
  phoneVerified: boolean;
  localPasswordEnabled: boolean;
}

interface CustomerAuthContextValue {
  user: CustomerUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<CustomerUser>;
  signup: (
    email: string,
    password: string,
    name: string,
    phone: string,
    verificationCode: string,
  ) => Promise<CustomerUser>;
  logout: () => Promise<void>;
  withdraw: () => Promise<void>;
  refresh: () => Promise<CustomerUser | null>;
}

const CustomerAuthContext = createContext<CustomerAuthContextValue | null>(null);

export function CustomerAuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [user, setUser] = useState<CustomerUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const clearCustomerQueries = useCallback(() => {
    clearCustomerQueryCache(queryClient);
  }, [queryClient]);

  const expireCustomerSession = useCallback(() => {
    clearCustomerQueries();
    setUser(null);
    setIsLoading(false);
  }, [clearCustomerQueries]);

  const fetchMe = useCallback(async () => {
    const requestVersion = currentCustomerSessionVersion();
    try {
      const me = await api<CustomerUserResponse>("/me");
      if (!isCurrentCustomerSessionVersion(requestVersion)) return null;
      clearCustomerQueries();
      setUser(me);
      return me;
    } catch {
      if (!isCurrentCustomerSessionVersion(requestVersion)) return null;
      clearCustomerQueries();
      setUser(null);
      return null;
    } finally {
      if (isCurrentCustomerSessionVersion(requestVersion)) {
        setIsLoading(false);
      }
    }
  }, [clearCustomerQueries]);

  useEffect(
    () => subscribeToCustomerSessionExpired(expireCustomerSession),
    [expireCustomerSession],
  );

  useEffect(() => {
    void fetchMe();
  }, [fetchMe]);

  const login = useCallback(
    async (email: string, password: string): Promise<CustomerUser> => {
      const me = await api<CustomerUserResponse>("/auth/login", {
        method: "POST",
        body: { email, password },
      });
      advanceCustomerSessionVersion();
      clearCustomerQueries();
      setUser(me);
      setIsLoading(false);
      return me;
    },
    [clearCustomerQueries],
  );

  const signup = useCallback(
    async (
      email: string,
      password: string,
      name: string,
      phone: string,
      verificationCode: string,
    ): Promise<CustomerUser> => {
      const me = await api<CustomerUserResponse>("/auth/signup", {
        method: "POST",
        body: {
          email,
          password,
          name,
          phone: normalizePhone(phone),
          verificationCode,
        },
      });
      advanceCustomerSessionVersion();
      clearCustomerQueries();
      setUser(me);
      setIsLoading(false);
      return me;
    },
    [clearCustomerQueries],
  );

  const logout = useCallback(async () => {
    await api("/auth/logout", { method: "POST" });
    advanceCustomerSessionVersion();
    clearCustomerQueries();
    setUser(null);
    setIsLoading(false);
  }, [clearCustomerQueries]);

  const withdraw = useCallback(async () => {
    await api("/me", { method: "DELETE" });
    advanceCustomerSessionVersion();
    clearCustomerQueries();
    setUser(null);
    setIsLoading(false);
  }, [clearCustomerQueries]);

  return createElement(
    CustomerAuthContext,
    {
      value: {
        user,
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
