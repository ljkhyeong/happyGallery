import { useCallback, useState } from "react";

export function useCursorHistory() {
  const [cursor, setCursor] = useState<string | undefined>();
  const [history, setHistory] = useState<(string | undefined)[]>([]);

  const showNextPage = useCallback((nextCursor: string | null | undefined) => {
    if (!nextCursor) return;
    setHistory((current) => [...current, cursor]);
    setCursor(nextCursor);
  }, [cursor]);

  const showPreviousPage = useCallback(() => {
    if (history.length === 0) return;
    setCursor(history[history.length - 1]);
    setHistory(history.slice(0, -1));
  }, [history]);

  const resetCursor = useCallback(() => {
    setCursor(undefined);
    setHistory([]);
  }, []);

  return {
    cursor,
    hasPreviousPage: history.length > 0,
    showNextPage,
    showPreviousPage,
    resetCursor,
  };
}
