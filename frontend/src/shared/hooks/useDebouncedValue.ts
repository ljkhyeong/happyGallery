import { useEffect, useState } from "react";

/** 값 변경 후 delay(ms) 동안 추가 변경이 없으면 최종 값을 반환한다. */
export function useDebouncedValue<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debounced;
}
