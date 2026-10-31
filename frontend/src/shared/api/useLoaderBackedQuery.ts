import { useEffect, useRef } from "react";
import {
  hashKey,
  useQuery,
  useQueryClient,
  type QueryKey,
  type UseQueryOptions,
  type UseQueryResult,
} from "@tanstack/react-query";

type LoaderBackedQueryOptions<TData, TError, TQueryKey extends QueryKey> =
  Omit<UseQueryOptions<TData, TError, TData, TQueryKey>, "initialData">;

export interface LoaderBackedQueryResult<TData, TError> {
  data: TData | undefined;
  error: TError | null;
  isLoading: boolean;
  query: UseQueryResult<TData, TError>;
}

/** loader 값이 항상 있으면 첫 화면부터 데이터가 있어 로딩 상태가 없다. 갱신 실패만 error로 알린다. */
export interface RequiredLoaderBackedQueryResult<TData, TError> {
  data: TData;
  error: TError | null;
  query: UseQueryResult<TData, TError>;
}

export function useLoaderBackedQuery<
  TData,
  TError = Error,
  TQueryKey extends QueryKey = QueryKey,
>(
  options: LoaderBackedQueryOptions<TData, TError, TQueryKey>,
  loaderData: NoInfer<TData>,
): RequiredLoaderBackedQueryResult<TData, TError>;

export function useLoaderBackedQuery<
  TData,
  TError = Error,
  TQueryKey extends QueryKey = QueryKey,
>(
  options: LoaderBackedQueryOptions<TData, TError, TQueryKey>,
  loaderData?: TData,
): LoaderBackedQueryResult<TData, TError>;

export function useLoaderBackedQuery<
  TData,
  TError = Error,
  TQueryKey extends QueryKey = QueryKey,
>(
  options: LoaderBackedQueryOptions<TData, TError, TQueryKey>,
  loaderData?: TData,
): LoaderBackedQueryResult<TData, TError> {
  const queryClient = useQueryClient();
  const synchronizedLoaderData = useRef<TData | undefined>(undefined);
  const { queryKey } = options;
  // 호출부가 렌더마다 새 key 배열을 만들어도 내용이 같으면 같은 key로 본다.
  // effect는 해시로 변화를 감지하고, 최신 배열은 ref로 읽어 호출부가 useMemo로 고정할 필요가 없다.
  const queryKeyHash = hashKey(queryKey);
  const latestQueryKey = useRef(queryKey);
  latestQueryKey.current = queryKey;
  const query = useQuery({ ...options, initialData: loaderData });
  const hasNewLoaderData = loaderData !== undefined
    && synchronizedLoaderData.current !== loaderData;

  useEffect(() => {
    if (loaderData === undefined) return;

    synchronizedLoaderData.current = loaderData;
    const key = latestQueryKey.current;
    if (queryClient.getQueryData(key) !== loaderData) {
      queryClient.setQueryData<TData, TQueryKey, TData>(key, loaderData);
    }
  }, [loaderData, queryClient, queryKeyHash]);

  return {
    data: hasNewLoaderData ? loaderData : query.data,
    error: hasNewLoaderData ? null : query.error,
    isLoading: hasNewLoaderData ? false : query.isLoading,
    query,
  };
}
