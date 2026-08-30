import { useMutation, type UseMutationOptions } from "@tanstack/react-query";
import { isAdminSessionUnauthorized } from "./adminSessionUnauthorized";

/**
 * 관리자 세션 자체가 무효한 경우에만 `onAuthError`를 호출한다.
 *
 * `INVALID_CREDENTIALS`는 현재 비밀번호나 MFA 코드 입력 오류에도 사용되므로
 * HTTP 상태만 보고 로그인 상태를 제거하면 안 된다.
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
      if (isAdminSessionUnauthorized(err)) {
        onAuthError();
      }
      userOnError?.(...args);
    },
  });
}
