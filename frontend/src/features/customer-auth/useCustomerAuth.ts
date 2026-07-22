import { createContext, createElement, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api } from "@/shared/api";
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
    queryClient.removeQueries({
      predicate: ({ queryKey }) => queryKey[0] === "my" || queryKey[0] === "me",
    });
  }, [queryClient]);

  const fetchMe = useCallback(async () => {
    try {
      const me = await api<CustomerUserResponse>("/me");
      clearCustomerQueries();
      setUser(me);
      return me;
    } catch {
      clearCustomerQueries();
      setUser(null);
      return null;
    } finally {
      setIsLoading(false);
    }
  }, [clearCustomerQueries]);

  useEffect(() => {
    void fetchMe();
  }, [fetchMe]);

  const login = useCallback(
    async (email: string, password: string): Promise<CustomerUser> => {
      const me = await api<CustomerUserResponse>("/auth/login", {
        method: "POST",
        body: { email, password },
      });
      clearCustomerQueries();
      setUser(me);
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
      clearCustomerQueries();
      setUser(me);
      return me;
    },
    [clearCustomerQueries],
  );

  const logout = useCallback(async () => {
    await api("/auth/logout", { method: "POST" });
    clearCustomerQueries();
    setUser(null);
  }, [clearCustomerQueries]);

  const withdraw = useCallback(async () => {
    await api("/me", { method: "DELETE" });
    clearCustomerQueries();
    setUser(null);
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
