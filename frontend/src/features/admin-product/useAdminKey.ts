import { useState, useCallback } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  loginAdmin,
  logoutAdmin,
  verifyAdminMfa,
  type AdminAuthResponse,
} from "@/features/admin-auth/api";
import { clearAdminQueryCache } from "@/shared/api";

const TOKEN_KEY = "hg_admin_token";

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
    const token = adminKey;
    clearAdminKey();
    if (!token) return;
    await logoutAdmin(token);
  }, [adminKey, clearAdminKey]);

  const acceptAuthentication = useCallback(
    (result: AdminAuthResponse): AdminAuthResponse => {
      if (result.status === "AUTHENTICATED") {
        if (!result.token) {
          throw new Error("관리자 인증 응답에 세션 토큰이 없습니다.");
        }
        setAdminKey(result.token);
        return result;
      }

      if (!result.challengeToken) {
        throw new Error("관리자 인증 응답에 2단계 인증 정보가 없습니다.");
      }
      return result;
    },
    [setAdminKey],
  );

  const login = useCallback(
    async (username: string, password: string): Promise<AdminAuthResponse> => {
      return acceptAuthentication(await loginAdmin(username, password));
    },
    [acceptAuthentication],
  );

  const verifyMfa = useCallback(
    async (challengeToken: string, code: string): Promise<AdminAuthResponse> => {
      return acceptAuthentication(await verifyAdminMfa(challengeToken, code));
    },
    [acceptAuthentication],
  );

  return {
    adminKey,
    clearAdminKey,
    login,
    verifyMfa,
    logout,
    isAuthenticated: adminKey.length > 0,
  };
}
