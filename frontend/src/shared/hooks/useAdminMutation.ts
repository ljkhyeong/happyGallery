import { useMutation, type UseMutationOptions } from "@tanstack/react-query";
import { ApiError } from "@/shared/api";

/**
 * Admin mutation 래퍼 — 401 응답 시 `onAuthError`를 자동 호출한다.
 *
 * 호출자의 `onError`가 있으면 401 체크 후 함께 실행한다.
 */
export function useAdminMutation<TData = unknown, TVariables = void, TContext = unknown>(
  onAuthError: () => void,
  options: UseMutationOptions<TData, Error, TVariables, TContext>,
) {
  const { onError: userOnError, ...rest } = options;
  return useMutation<TData, Error, TVariables, TContext>({
    ...rest,
    onError: (...args) => {
      const [err] = args;
      if (err instanceof ApiError && err.status === 401) onAuthError();
      userOnError?.(...args);
    },
  });
}
