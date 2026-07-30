import { useEffect } from "react";
import {
  useQuery,
  type QueryKey,
  type UseQueryOptions,
  type UseQueryResult,
} from "@tanstack/react-query";
import { isAdminSessionUnauthorized } from "./adminSessionUnauthorized";

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
    if (isAdminSessionUnauthorized(query.error)) {
      onAuthError();
    }
  }, [onAuthError, query.error]);

  return query;
}
