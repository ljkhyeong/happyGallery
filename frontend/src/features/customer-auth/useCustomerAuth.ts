import { createContext, createElement, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import { api } from "@/shared/api";

interface CustomerUserResponse {
  id: number;
  email: string;
  name: string;
  phone: string;
  phoneVerified: boolean;
  provider: string;
}

interface SocialLoginResponse {
  user: CustomerUserResponse;
  newUser: boolean;
}

export interface CustomerUser {
  id: number;
  email: string;
  name: string;
  phone: string;
  phoneVerified: boolean;
  provider: string;
}

interface CustomerAuthContextValue {
  user: CustomerUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<boolean>;
  signup: (email: string, password: string, name: string, phone: string) => Promise<boolean>;
  socialLogin: (code: string, redirectUri: string) => Promise<{ ok: boolean; newUser: boolean }>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
}

const CustomerAuthContext = createContext<CustomerAuthContextValue | null>(null);

function normalizePhone(phone: string) {
  return phone.replace(/\D/g, "");
}

export function CustomerAuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CustomerUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchMe = useCallback(async () => {
    try {
      const me = await api<CustomerUserResponse>("/me");
      setUser(me);
    } catch {
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMe();
  }, [fetchMe]);

  const login = useCallback(
    async (email: string, password: string): Promise<boolean> => {
      try {
        const me = await api<CustomerUserResponse>("/auth/login", {
          method: "POST",
          body: { email, password },
        });
        setUser(me);
        return true;
      } catch {
        return false;
      }
    },
    [],
  );

  const signup = useCallback(
    async (
      email: string,
      password: string,
      name: string,
      phone: string,
    ): Promise<boolean> => {
      try {
        const me = await api<CustomerUserResponse>("/auth/signup", {
          method: "POST",
          body: { email, password, name, phone: normalizePhone(phone) },
        });
        setUser(me);
        return true;
      } catch {
        return false;
      }
    },
    [],
  );

  const socialLogin = useCallback(
    async (code: string, redirectUri: string): Promise<{ ok: boolean; newUser: boolean }> => {
      try {
        const res = await api<SocialLoginResponse>("/auth/social/google", {
          method: "POST",
          body: { code, redirectUri },
        });
        setUser(res.user);
        return { ok: true, newUser: res.newUser };
      } catch {
        return { ok: false, newUser: false };
      }
    },
    [],
  );

  const logout = useCallback(async () => {
    try {
      await api("/auth/logout", { method: "POST" });
    } catch {
      // ignore
    }
    setUser(null);
  }, []);

  return createElement(
    CustomerAuthContext,
    {
      value: {
        user,
        isAuthenticated: user !== null,
        isLoading,
        login,
        signup,
        socialLogin,
        logout,
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
