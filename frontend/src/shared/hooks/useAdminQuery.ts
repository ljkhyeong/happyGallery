import { useEffect } from "react";
import {
  useQuery,
  type QueryKey,
  type UseQueryOptions,
  type UseQueryResult,
} from "@tanstack/react-query";
import { ApiError } from "@/shared/api";

export function useAdminQuery<
  TQueryFnData = unknown,
  TError = Error,
  TData = TQueryFnData,
  TQueryKey extends QueryKey = QueryKey,
>(
  onAuthError: () => void,
  options: UseQueryOptions<TQueryFnData, TError, TData, TQueryKey>,
): UseQueryResult<TData, TError> {
  const query = useQuery(options);

  useEffect(() => {
    if (query.error instanceof ApiError && query.error.status === 401) {
      onAuthError();
    }
  }, [onAuthError, query.error]);

  return query;
}
