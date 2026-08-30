import { useEffect, useRef } from "react";
import {
  useQuery,
  useQueryClient,
  type QueryKey,
  type UseQueryOptions,
  type UseQueryResult,
} from "@tanstack/react-query";

interface LoaderBackedQueryResult<TData, TError> {
  data: TData | undefined;
  error: TError | null;
  isLoading: boolean;
  query: UseQueryResult<TData, TError>;
}

export function useLoaderBackedQuery<
  TData,
  TError = Error,
  TQueryKey extends QueryKey = QueryKey,
>(
  options: Omit<UseQueryOptions<TData, TError, TData, TQueryKey>, "initialData">,
  loaderData?: TData,
): LoaderBackedQueryResult<TData, TError> {
  const queryClient = useQueryClient();
  const synchronizedLoaderData = useRef<TData | undefined>(undefined);
  const { queryKey } = options;
  const query = useQuery({ ...options, initialData: loaderData });
  const hasNewLoaderData = loaderData !== undefined
    && synchronizedLoaderData.current !== loaderData;

  useEffect(() => {
    if (loaderData === undefined) return;

    synchronizedLoaderData.current = loaderData;
    if (queryClient.getQueryData(queryKey) !== loaderData) {
      queryClient.setQueryData<TData, TQueryKey, TData>(queryKey, loaderData);
    }
  }, [loaderData, queryClient, queryKey]);

  return {
    data: hasNewLoaderData ? loaderData : query.data,
    error: hasNewLoaderData ? null : query.error,
    isLoading: hasNewLoaderData ? false : query.isLoading,
    query,
  };
}
