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
          throw new Error("관리자 로그인 정보를 확인하지 못했습니다. 다시 로그인해 주세요.");
        }
        setAdminKey(result.token);
        return result;
      }

      if (!result.challengeToken) {
        throw new Error("2단계 인증을 시작할 수 없습니다. 다시 로그인해 주세요.");
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
