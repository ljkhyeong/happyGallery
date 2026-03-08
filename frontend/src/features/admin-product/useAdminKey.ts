import { useState, useCallback } from "react";

const STORAGE_KEY = "hg_admin_key";

export function useAdminKey() {
  const [adminKey, setAdminKeyState] = useState(
    () => sessionStorage.getItem(STORAGE_KEY) ?? "",
  );

  const setAdminKey = useCallback((key: string) => {
    sessionStorage.setItem(STORAGE_KEY, key);
    setAdminKeyState(key);
  }, []);

  const clearAdminKey = useCallback(() => {
    sessionStorage.removeItem(STORAGE_KEY);
    setAdminKeyState("");
  }, []);

  return { adminKey, setAdminKey, clearAdminKey, isAuthenticated: adminKey.length > 0 };
}
