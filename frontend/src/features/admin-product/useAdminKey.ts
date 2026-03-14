import { useState, useCallback } from "react";
import { api } from "@/shared/api";

const TOKEN_KEY = "hg_admin_token";

interface LoginResponse {
  token: string;
}

export function useAdminKey() {
  const [adminKey, setAdminKeyState] = useState(
    () => sessionStorage.getItem(TOKEN_KEY) ?? "",
  );

  const setAdminKey = useCallback((token: string) => {
    sessionStorage.setItem(TOKEN_KEY, token);
    setAdminKeyState(token);
  }, []);

  const clearAdminKey = useCallback(() => {
    const token = sessionStorage.getItem(TOKEN_KEY);
    if (token) {
      api("/admin/auth/logout", {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      }).catch(() => {});
    }
    sessionStorage.removeItem(TOKEN_KEY);
    setAdminKeyState("");
  }, []);

  const login = useCallback(
    async (username: string, password: string): Promise<boolean> => {
      try {
        const result = await api<LoginResponse>("/admin/auth/login", {
          method: "POST",
          body: { username, password },
        });
        setAdminKey(result.token);
        return true;
      } catch {
        return false;
      }
    },
    [setAdminKey],
  );

  return { adminKey, setAdminKey, clearAdminKey, login, isAuthenticated: adminKey.length > 0 };
}
