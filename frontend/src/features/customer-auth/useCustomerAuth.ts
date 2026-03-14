import { useState, useCallback, useEffect } from "react";
import { api } from "@/shared/api";

interface MeResponse {
  id: number;
  email: string;
  name: string;
  phone: string;
  phoneVerified: boolean;
}

export interface CustomerUser {
  id: number;
  email: string;
  name: string;
  phone: string;
  phoneVerified: boolean;
}

export function useCustomerAuth() {
  const [user, setUser] = useState<CustomerUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchMe = useCallback(async () => {
    try {
      const me = await api<MeResponse>("/me");
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
        const me = await api<MeResponse>("/auth/login", {
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
        const me = await api<MeResponse>("/auth/signup", {
          method: "POST",
          body: { email, password, name, phone },
        });
        setUser(me);
        return true;
      } catch {
        return false;
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

  return {
    user,
    isAuthenticated: user !== null,
    isLoading,
    login,
    signup,
    logout,
  };
}
