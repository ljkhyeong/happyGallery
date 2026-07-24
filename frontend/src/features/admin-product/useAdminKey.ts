import { useState, useCallback } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api, clearAdminQueryCache } from "@/shared/api";

const TOKEN_KEY = "hg_admin_token";

interface LoginResponse {
  token: string;
}

export function useAdminKey() {
  const queryClient = useQueryClient();
  const [adminKey, setAdminKeyState] = useState(
    () => sessionStorage.getItem(TOKEN_KEY) ?? "",
  );

  const setAdminKey = useCallback((token: string) => {
    clearAdminQueryCache(queryClient);
    sessionStorage.setItem(TOKEN_KEY, token);
    setAdminKeyState(token);
  }, [queryClient]);

  const clearAdminKey = useCallback(() => {
    clearAdminQueryCache(queryClient);
    sessionStorage.removeItem(TOKEN_KEY);
    setAdminKeyState("");
  }, [queryClient]);

  const logout = useCallback(async () => {
    if (!adminKey) return;
    await api("/admin/auth/logout", {
      method: "POST",
      headers: { Authorization: `Bearer ${adminKey}` },
    });
    clearAdminKey();
  }, [adminKey, clearAdminKey]);

  const login = useCallback(
    async (username: string, password: string): Promise<void> => {
      const result = await api<LoginResponse>("/admin/auth/login", {
        method: "POST",
        body: { username, password },
      });
      setAdminKey(result.token);
    },
    [setAdminKey],
  );

  return {
    adminKey,
    setAdminKey,
    clearAdminKey,
    login,
    logout,
    isAuthenticated: adminKey.length > 0,
  };
}
