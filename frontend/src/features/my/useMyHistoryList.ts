import {
  useInfiniteQuery,
  type InfiniteData,
  type QueryKey,
  type UseInfiniteQueryOptions,
} from "@tanstack/react-query";
import { cursorPageParams, flattenCursorPages, runForCurrentCustomer, type CursorPage } from "@/shared/api";
import { useDebouncedValue } from "@/shared/hooks/useDebouncedValue";
import { useMyListFilters } from "./useMyListFilters";

export interface MyHistoryFilters<TStatus extends string, TSort extends string> {
  keyword: string | undefined;
  status: TStatus | undefined;
  sort: TSort;
}

type HistoryRefetchInterval<TPage> = UseInfiniteQueryOptions<
  TPage,
  Error,
  InfiniteData<TPage, string | undefined>,
  QueryKey,
  string | undefined
>["refetchInterval"];

interface Options<TPage extends CursorPage, TStatus extends string, TSort extends string> {
  /** 필터 객체를 덧붙일 이력 key 접두사. */
  queryKey: QueryKey;
  fetchPage: (
    cursor: string | undefined,
    signal: AbortSignal,
    filters: MyHistoryFilters<TStatus, TSort>,
  ) => Promise<TPage>;
  statusValues: readonly TStatus[];
  sortOptions: readonly { value: TSort; label: string }[];
  defaultSort: TSort;
  refetchInterval?: HistoryRefetchInterval<TPage>;
}

/** 내 정보 예약·주문·이용권 이력: 주소의 검색 조건, 입력 지연, 커서 페이지 조회를 함께 맡는다. */
export function useMyHistoryList<TPage extends CursorPage, TStatus extends string, TSort extends string>({
  queryKey,
  fetchPage,
  statusValues,
  sortOptions,
  defaultSort,
  refetchInterval,
}: Options<TPage, TStatus, TSort>) {
  const filters = useMyListFilters({
    defaultSort,
    statusValues,
    sortValues: sortOptions.map((option) => option.value),
  });
  const debouncedSearch = useDebouncedValue(filters.searchQuery, 300);
  const requestFilters: MyHistoryFilters<TStatus, TSort> = {
    keyword: debouncedSearch.trim() || undefined,
    status: filters.statusFilter === "ALL" ? undefined : filters.statusFilter as TStatus,
    sort: filters.sortValue as TSort,
  };
  const query = useInfiniteQuery({
    queryKey: [...queryKey, requestFilters],
    queryFn: ({ pageParam, signal }): Promise<TPage> =>
      runForCurrentCustomer(() => fetchPage(pageParam, signal, requestFilters)),
    ...cursorPageParams,
    refetchInterval,
  });
  return { filters, query, items: flattenCursorPages(query.data) };
}
