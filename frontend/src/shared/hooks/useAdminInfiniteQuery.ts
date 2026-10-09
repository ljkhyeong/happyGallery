import { useEffect } from "react";
import {
  useInfiniteQuery,
  type InfiniteData,
  type QueryKey,
  type UseInfiniteQueryOptions,
  type UseInfiniteQueryResult,
} from "@tanstack/react-query";
import { isAdminSessionUnauthorized } from "./adminSessionUnauthorized";

/** useAdminQuery와 같이 관리자 세션 만료(401 UNAUTHORIZED)에서만 onAuthError를 부르는 커서 목록 조회 */
export function useAdminInfiniteQuery<
  TQueryFnData,
  TError = Error,
  TData = InfiniteData<TQueryFnData>,
  TQueryKey extends QueryKey = QueryKey,
  TPageParam = unknown,
>(
  onAuthError: () => void,
  options: UseInfiniteQueryOptions<TQueryFnData, TError, TData, TQueryKey, TPageParam>,
): UseInfiniteQueryResult<TData, TError> {
  const query = useInfiniteQuery(options);

  useEffect(() => {
    if (isAdminSessionUnauthorized(query.error)) {
      onAuthError();
    }
  }, [onAuthError, query.error]);

  return query;
}
